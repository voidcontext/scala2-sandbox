package com.gaborpihaj.fetchfile

import cats.effect._
import cats.syntax.functor._

import scala.concurrent.ExecutionContext

import java.io.File
import java.util.concurrent.Executors
import cats.effect.ExitCode

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val fileUrl = "http://ipv4.download.thinkbroadband.com/100MB.zip"
    val tempFile = File.createTempFile("100MB-testfile", ".tmp")

    val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    val blocker = Blocker.liftExecutionContext(ec)

    (for {
      _ <- fetch[IO](fileUrl, tempFile, blocker, blocker)
      _ <- IO(println(tempFile.getAbsolutePath()))
      _ <- IO(ec.shutdown())
    } yield ()).as(ExitCode.Success)
  }
}
