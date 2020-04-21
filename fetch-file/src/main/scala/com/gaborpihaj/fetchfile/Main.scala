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

    val fileUrl = new URL("http://localhost:8088/100MB.bin")


    val ecResource = Resource.make(IO.delay(Executors.newFixedThreadPool(4)))(ec => IO.delay(ec.shutdown()))
      .map(ExecutionContext.fromExecutorService)

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

    ecResource.use { ec =>
      val blocker = Blocker.liftExecutionContext(ec)

      (for {
        _ <- createBenchmark(downloadAndTrack(fileUrl, blocker, stdoutProgressScalar), repeat)
        _ <- createBenchmark(downloadAndTrack(fileUrl, blocker, stdoutProgressCaseClass), repeat)
        _ <- createBenchmark(downloadAndTrack(fileUrl, blocker, stdoutProgressTagged), repeat)
      } yield ())
        .as(ExitCode.Success)
    }

  }

  private[this] def createBenchmark(computation: IO[TimeResult[Either[Throwable, Unit]]], repeat: Int): IO[Unit] = 
    for {
      result <- benchmark(computation, repeat)
      stats  <- benchmarkStats(result.drop(2))
      _      <- IO.delay(println(stats.show))
      _      <- IO.delay(println())
    } yield ()


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
