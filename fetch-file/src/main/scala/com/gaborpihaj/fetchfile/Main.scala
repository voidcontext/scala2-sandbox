package com.gaborpihaj
package fetchfile

import cats.effect._
import cats.syntax.functor._

import scala.concurrent.ExecutionContext

import java.util.concurrent.Executors
import java.io.{File, FileOutputStream}
import java.net.URL
import fs2.Pipe

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val fileUrl = new URL("http://localhost:8088/100MB.zip")

    val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
    val blocker = Blocker.liftExecutionContext(ec)

    val stdoutProgress = progress[IO]()

    val stdoutProgress2 = progress2[IO] {(downloaded, _, downloadSpeed) =>
      println(
        s"\u001b[1A\u001b[100D\u001b[0KDownloaded ${bytesToString(downloaded.bytes)} of ??? | ${bytesToString(downloadSpeed.bytesPerSecond)}/s"
      )
    }


    (for {
      _ <- downloadAndTrack(fileUrl, blocker, stdoutProgress)
      _ <- IO.delay(println())
      _ <- downloadAndTrack(fileUrl, blocker, stdoutProgress2)
      _ <- IO.delay(println())
      _ <- IO(ec.shutdown())
    } yield ())
      .as(ExitCode.Success)
  }

  private[this] def downloadAndTrack(fileUrl: URL, blocker: Blocker, progress: Pipe[IO, Byte, Unit]): IO[Unit] =
    for {
      tmp                             <- tempFileStream()
      (tempFile, outStreamResource)   = tmp
      _                               <- benchmark(fetch[IO](fileUrl, outStreamResource, 8 * 1024, blocker, progress))
      _                               <- IO.delay(println(tempFile.getAbsolutePath()))
      _                               <- IO.delay(println())
    } yield ()


  private[this] def tempFileStream(): IO[(File, Resource[IO, FileOutputStream])] =
    for {
      tempFile          <- IO.delay(File.createTempFile("100MB-testfile", ".tmp"))
      outStreamResource <- IO.delay(Resource.fromAutoCloseable(IO.delay(new FileOutputStream(tempFile))))
    } yield (tempFile, outStreamResource)
}
