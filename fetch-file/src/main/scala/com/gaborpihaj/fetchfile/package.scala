package com.gaborpihaj

import cats.effect._
import cats.implicits._
import fs2._
import fs2.io._

import java.io.{InputStream, OutputStream}
import java.net.URL

package object fetchfile {
  type Backend[F[_]] = URL => Resource[F, InputStream]

  def fetch[F[_]: Concurrent: ContextShift](
    url: URL,
    out: Resource[F, OutputStream],
    chunkSize: Int,
    ec: Blocker,
    progressConsumer: Pipe[F, Byte, Unit]
  )(implicit backend: Backend[F]): F[Unit] = {

    backend(url).product(out).use {
      case (inStream, outStream) =>
        readInputStream[F](Sync[F].delay(inStream), chunkSize, ec)
          //  TODO: use evalTap(f: Byte => F[Unit]) instead of observe
          .observe(progressConsumer)
          .through(writeOutputStream[F](Sync[F].delay(outStream), ec))
          .compile
          .drain
    }
  }
}
