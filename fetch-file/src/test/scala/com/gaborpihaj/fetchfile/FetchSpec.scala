package com.gaborpihaj.fetchfile

import cats.effect._
import fs2._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayInputStream}
import java.net.URL
import java.io.ByteArrayOutputStream

class FetchSpec extends AnyFlatSpec with Matchers {
  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  "fetch" should "use the given backend to create the input stream" in {

    implicit val backend: Backend[IO] =
      (url: URL) => Resource.make(IO.delay(new ByteArrayInputStream(url.toString.getBytes())))(s => IO.delay(s.close()))

    (Blocker[IO].use { blocker =>
      val out = new ByteArrayOutputStream()
      for {
        _        <- fetch(
          new URL("http://example.com/test.file"),
          Resource.fromAutoCloseable(IO.delay(out)),
          1,
          blocker,
          (s: Stream[IO, Byte]) => s.map(_ => ())
        )
        content  <- IO.delay(out.toString)
      } yield content
    }).unsafeRunSync() should be("http://example.com/test.file")
  }
}
