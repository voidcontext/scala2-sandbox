package com.gaborpihaj
package fetchfile

import cats.effect.Sync
import fs2._
import shapeless.tag.@@

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration._
import shapeless.tag

object ProgressScalar {
  def progress[F[_]: Sync](): Pipe[F, Byte, Unit] = { s =>
    Stream.eval(Sync[F].delay(System.nanoTime()))
      .flatMap { startTime =>
          val downloadedBytes = new AtomicLong(0)

          s.chunks.map { chunk =>
            val down = downloadedBytes.addAndGet(chunk.size.toLong)
            val elapsedTime = Duration(System.nanoTime() - startTime, NANOSECONDS)

            if (elapsedTime.toSeconds > 0) {
              val speed = (down * 1000) / elapsedTime.toMillis

              // TODO: get total size: connection.getContentLengthLong
              println(
                s"\u001b[1A\u001b[100D\u001b[0KDownloaded ${bytesToString(down)} of ??? | ${bytesToString(speed)}/s"
              )
            }
          }
      }
  }
}

object ProgressCaseClass {
  final case class ElapsedTime(duration: Duration) extends AnyVal
  final case class Downloaded(bytes: Long) extends AnyVal
  final case class DownloadSpeed(bytesPerSecond: Long) extends AnyVal

  type ProgressConsumer = (Downloaded, ElapsedTime, DownloadSpeed) => Unit

  def progress[F[_]: Sync](f: ProgressConsumer): Pipe[F, Byte, Unit] = { s =>
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

  object DownloadSpeed {
    def calc(elapsedTime: ElapsedTime, downloaded: Downloaded): DownloadSpeed = {
      val elapsedSeconds = elapsedTime.duration.toMillis
      DownloadSpeed(((downloaded.bytes) * 1000) / (if (elapsedSeconds == 0) 1L else elapsedSeconds))
    }
  }
}

object ProgressTagged {
  sealed trait ElapsedTimeTag
  sealed trait DownloadedTag
  sealed trait DownloadSpeedTag

  type ElapsedTime = Duration @@ ElapsedTimeTag
  type Downloaded = Long @@ DownloadedTag
  type DownloadSpeed = Long @@ DownloadSpeedTag

  type ProgressConsumer = (Downloaded, ElapsedTime, DownloadSpeed) => Unit


  object ElapsedTime {
    def apply(elapsedTime: Duration): ElapsedTime = tag[ElapsedTimeTag][Duration](elapsedTime)
  }

  object Downloaded {
    def apply(downloaded: Long): Downloaded = tag[DownloadedTag][Long](downloaded)
  }
  object DownloadSpeed {
    def apply(downloadSpeed: Long): DownloadSpeed = tag[DownloadSpeedTag][Long](downloadSpeed)
    def calc(elapsedTime: ElapsedTime, downloaded: Downloaded): DownloadSpeed = {
      val elapsedSeconds = elapsedTime.toMillis
      DownloadSpeed(((downloaded) * 1000) / (if (elapsedSeconds == 0) 1L else elapsedSeconds))
    }
  }

  def progress[F[_]: Sync](f: ProgressConsumer): Pipe[F, Byte, Unit] = { s =>
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

}
