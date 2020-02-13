package com.gaborpihaj.fetchfile

import scala.concurrent.duration.Duration

object Progress {
  final case class ElapsedTime(duration: Duration) extends AnyVal
  final case class Downloaded(bytes: Long) extends AnyVal
  final case class DownloadSpeed(bytesPerSecond: Long) extends AnyVal

  object DownloadSpeed {
    def calc(elapsedTime: ElapsedTime, downloaded: Downloaded): DownloadSpeed = {
      val elapsedSeconds = elapsedTime.duration.toMillis
      DownloadSpeed(((downloaded.bytes) * 1000) / (if (elapsedSeconds == 0) 1L else elapsedSeconds))
    }
  }

}
