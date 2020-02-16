package com.gaborpihaj

import cats.effect._
import fs2._
import fs2.io._


import java.io.{InputStream, FileOutputStream}
import java.net.{HttpURLConnection, URL}

package object fetchfile {


  def fetch[F[_]: Concurrent: ContextShift](
    url: URL,
    out: Resource[F, FileOutputStream],
    chunkSize: Int,
    ec: Blocker,
    progressConsumer: Pipe[F, Byte, Unit]
  ): F[Unit] = {
    val streamResources = for {
      inStream  <- initConnection(url)
      outStream <- out
    } yield (inStream, outStream)

    streamResources.use {
      case (inStream, outStream) =>
        readInputStream[F](Sync[F].delay(inStream), chunkSize, ec)
          .observe(progressConsumer)
          .through(writeOutputStream[F](Sync[F].delay(outStream), ec))
          .compile
          .drain
    }
  }


  //  Like progess 3 but with tagged types instead of value objects
  // private[this] def progress3(): Pipe[F, Byte, Unit] = ???

  private[this] def initConnection[F[_]: Sync](url: URL): Resource[F, InputStream] = {
    Resource.make {
      Sync[F].delay {
        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestProperty(
          "User-Agent",
          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
        )
        connection.connect()
        connection.getInputStream()
      }
    } { inputStream =>
      Sync[F].delay(inputStream.close())
    }
  }

}
