package com.gaborpihaj

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.io._

import scala.concurrent.ExecutionContext

import java.io.{InputStream, FileOutputStream, File}
import java.net.{HttpURLConnection, URL}


package object fetchfile {

  def fetch[F[_]: Sync: ContextShift](
    url: URL,
    out: Resource[F, FileOutputStream],
    chunkSize: Int,
    ec: Blocker,
  ): F[Unit] = {
    val streamResources = for {
      inStream <- initConnection(url)
      outStream <- out
    } yield (inStream, outStream)

    streamResources.use {
      case (inStream, outStream) =>
        readInputStream[F](Sync[F].delay(inStream), chunkSize, ec)
          .through(writeOutputStream[F](Sync[F].delay(outStream), ec))
          .compile
          .drain
    }
  }

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
