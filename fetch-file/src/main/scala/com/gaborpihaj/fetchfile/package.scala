package com.gaborpihaj

import cats.effect._
import fs2._
import fs2.io._

import scala.concurrent.duration._

import java.io.{InputStream, FileOutputStream}
import java.net.{HttpURLConnection, URL}
import java.util.concurrent.atomic.AtomicLong

package object fetchfile {
  import Progress._

  type ProgressConsumer = (Downloaded, ElapsedTime, DownloadSpeed) => Unit

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

  def progress[F[_]: Sync](): Pipe[F, Byte, Unit] = { s =>
    Stream.eval(Sync[F].delay(System.nanoTime()))
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

  def progress2[F[_]: Sync](f: ProgressConsumer): Pipe[F, Byte, Unit] = { s =>
    Stream.eval(Sync[F].delay(System.nanoTime()))
      .flatMap { startTime =>
        val downloadedBytes = new AtomicLong(0)

        s.chunks.map { chunk =>
          val down = Downloaded(downloadedBytes.addAndGet(chunk.size.toLong))
          val elapsedTime = ElapsedTime(Duration(System.nanoTime() - startTime, NANOSECONDS))
          val downloadSpeed = DownloadSpeed.calc(elapsedTime, down)


          f(down, elapsedTime, downloadSpeed)
        }
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
