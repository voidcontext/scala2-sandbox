package com.gaborpihaj
package fetchfile

import cats.effect._
import cats.implicits._

import scala.concurrent.ExecutionContext

import java.util.concurrent.Executors
import java.io.{File, FileOutputStream}
import java.net.URL
import fs2.Pipe

import com.gaborpihaj.benchmark._

object Main extends IOApp {
  val benchmarkIO = Benchmarks.impl[IO]

  import benchmarkIO._


  def run(args: List[String]): IO[ExitCode] = {

    val fileUrl = new URL("http://localhost:8088/100MB.zip")

    val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
    val blocker = Blocker.liftExecutionContext(ec)

    val stdoutProgressScalar = ProgressScalar.progress[IO]()

    val stdoutProgressCaseClass = ProgressCaseClass.progress[IO] {(downloaded, _, downloadSpeed) =>
      println(
        s"\u001b[1A\u001b[100D\u001b[0KDownloaded ${bytesToString(downloaded.bytes)} of ??? | ${bytesToString(downloadSpeed.bytesPerSecond)}/s"
      )
    }

    val stdoutProgressTagged = ProgressTagged.progress[IO] {(downloaded, _, downloadSpeed) =>
      println(
        s"\u001b[1A\u001b[100D\u001b[0KDownloaded ${bytesToString(downloaded)} of ??? | ${bytesToString(downloadSpeed)}/s"
      )
    }

    val repeat = 12


    (for {
      result1 <- benchmark(downloadAndTrack(fileUrl, blocker, stdoutProgressScalar), repeat)
      stats1  <- benchmarkStats(result1.drop(2))
      _ <- IO.delay(println(stats1.show))
      _ <- IO.delay(println())

      result2 <- benchmark(downloadAndTrack(fileUrl, blocker, stdoutProgressCaseClass), repeat)
      stats2  <- benchmarkStats(result2.drop(2))
      _ <- IO.delay(println(stats2.show))
      _ <- IO.delay(println())

      result3 <- benchmark(downloadAndTrack(fileUrl, blocker, stdoutProgressTagged), repeat)
      stats3  <- benchmarkStats(result3.drop(2))
      _ <- IO.delay(println(stats3.show))
      _ <- IO.delay(println())

      _ <- IO(ec.shutdown())
    } yield ())
      .as(ExitCode.Success)
  }

  private[this] def downloadAndTrack(fileUrl: URL, blocker: Blocker, progress: Pipe[IO, Byte, Unit]): IO[TimeResult[Either[Throwable, Unit]]] =
    for {
      tmp                             <- tempFileStream()
      (tempFile, outStreamResource)   = tmp
      timeResult                      <- measureTime(fetch[IO](fileUrl, outStreamResource, 8 * 1024, blocker, progress))
      _                               <- IO.delay(println())
      _                               <- IO.delay(tempFile.delete())
    } yield timeResult


  private[this] def tempFileStream(): IO[(File, Resource[IO, FileOutputStream])] =
    for {
      tempFile          <- IO.delay(File.createTempFile("100MB-testfile", ".tmp"))
      outStreamResource <- IO.delay(Resource.fromAutoCloseable(IO.delay(new FileOutputStream(tempFile))))
    } yield (tempFile, outStreamResource)
}
