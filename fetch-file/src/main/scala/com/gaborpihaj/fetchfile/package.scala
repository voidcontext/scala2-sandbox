package com.gaborpihaj

import cats.Applicative
import cats.effect._
import fs2._
import fs2.io._

import scala.concurrent.duration._

import java.io.{InputStream, FileOutputStream}
import java.net.{HttpURLConnection, URL}
import java.util.concurrent.atomic.AtomicLong
import java.util.Locale

package object fetchfile {

  def fetch[F[_]: Concurrent: ContextShift](
    url: URL,
    out: Resource[F, FileOutputStream],
    chunkSize: Int,
    ec: Blocker,
  ): F[Unit] = {
    val streamResources = for {
      inStream  <- initConnection(url)
      outStream <- out
    } yield (inStream, outStream)

    val stdoutProgress: Pipe[F, Byte, Unit] = { s => 
      Stream.bracket(Sync[F].delay(System.nanoTime()))(_ => Applicative[F].pure(()))
        .flatMap { startTime =>
          val downloadedBytes = new AtomicLong(0)

          s.chunks.map { chunk =>
            val down = downloadedBytes.addAndGet(chunk.size.toLong)
            val elapsedTime = Duration(System.nanoTime() - startTime, NANOSECONDS)

            if (elapsedTime.toSeconds > 0) {
              val speed = down / elapsedTime.toSeconds

              // TODO: get total size: connection.getContentLengthLong
              println(
                s"\u001b[1A\u001b[100D\u001b[0KDownloaded ${bytesToString(down)} of ??? | ${bytesToString(speed)}/s"
              )
            }
          }
        }
    }

    streamResources.use {
      case (inStream, outStream) =>
        readInputStream[F](Sync[F].delay(inStream), chunkSize, ec)
          .observe(stdoutProgress)
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

  // https://stackoverflow.com/questions/45885151/bytes-in-human-readable-format-with-idiomatic-scala
  private[this] def bytesToString(size: Long): String = {
    val TB = 1L << 40
    val GB = 1L << 30
    val MB = 1L << 20
    val KB = 1L << 10

    val (value, unit) = {
      if (size >= 2 * TB) {
        (size.asInstanceOf[Double] / TB, "TB")
      } else if (size >= 2 * GB) {
        (size.asInstanceOf[Double] / GB, "GB")
      } else if (size >= 2 * MB) {
        (size.asInstanceOf[Double] / MB, "MB")
      } else if (size >= 2 * KB) {
        (size.asInstanceOf[Double] / KB, "KB")
      } else {
        (size.asInstanceOf[Double], "B")
      }
    }
    "%.1f %s".formatLocal(Locale.US, value, unit)
  }
}
