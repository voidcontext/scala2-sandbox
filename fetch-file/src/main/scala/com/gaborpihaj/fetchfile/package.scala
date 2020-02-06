package com.gaborpihaj

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.io._

import scala.concurrent.ExecutionContext

import java.io.{InputStream, FileOutputStream, File}
import java.net.{HttpURLConnection, URL}


package object fetchfile {

  // TODO: use Resource from cats-effect

  def fetch[F[_]](url: String, targetFile: File, inEC: Blocker, outEC: Blocker)(implicit F: Sync[F], CS: ContextShift[F]): F[Unit] = {
    val netUrl = new URL(url)

    fetch(initConnection(netUrl), Resource.fromAutoCloseable(F.delay(new FileOutputStream(targetFile))), inEC, outEC)
  }

  def fetch[F[_]](in: Resource[F, InputStream], out: Resource[F, FileOutputStream], inEC: Blocker, outEC: Blocker)(
    implicit F: Sync[F], CS: ContextShift[F]
  ): F[Unit] = {
    val streamResources = for {
      inStream <- in
      outStream <- out
    } yield (inStream, outStream)

    streamResources.use {
      case (inStream, outStream) =>
        readInputStream[F](F.delay(inStream), 1024, inEC)
          .through(writeOutputStream[F](F.delay(outStream), outEC))
          .compile
          .drain
    }
  }

  private[this] def initConnection[F[_]](url: URL)(implicit F: Sync[F]): Resource[F, InputStream] = {
    Resource.make {
      F.delay {
        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestProperty(
          "User-Agent",
          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
        )
        connection.connect()
        connection.getInputStream()
      }
    } { inputStream =>
      F.delay(inputStream.close())
    }
  }

}
