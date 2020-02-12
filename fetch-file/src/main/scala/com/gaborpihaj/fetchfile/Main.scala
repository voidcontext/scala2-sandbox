package com.gaborpihaj
package fetchfile

import cats.effect._
import cats.syntax.functor._

import scala.concurrent.ExecutionContext

import java.util.concurrent.Executors
import java.io.{File, FileOutputStream}
import java.net.URL

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val fileUrl = new URL("http://localhost:8088/100MB.zip")

    val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))
    val blocker = Blocker.liftExecutionContext(ec)


    (for {
      tmp                           <- tempFileStream()
      (tempFile, outStreamResource) = tmp
      _                             <- benchmark(fetch[IO](fileUrl, outStreamResource, 8 * 1024, blocker))
      _                             <- IO(println(tempFile.getAbsolutePath()))
      _                             <- IO(ec.shutdown())
    } yield ())
      .as(ExitCode.Success)
  }


  private[this] def tempFileStream(): IO[(File, Resource[IO, FileOutputStream])] =
    for {
      tempFile          <- IO.delay(File.createTempFile("100MB-testfile", ".tmp"))
      outStreamResource <- IO.delay(Resource.fromAutoCloseable(IO.delay(new FileOutputStream(tempFile))))
    } yield (tempFile, outStreamResource)

}
