package com

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._

import java.util.Locale

package object gaborpihaj {

  def benchmark[F[_]: Sync, A](a: F[A]): F[A] = {
    for {
      t0     <- Sync[F].delay(System.nanoTime())
      result <- a
      t1     <- Sync[F].delay(System.nanoTime())
      _      <- Sync[F].delay(println("Elapsed time: " +  formatNanoTime(t1 - t0)))
    } yield result
  }

  private[this] def formatNanoTime(t: Long): String = {
    val millis = Math.round(t.toDouble / 1000).toDouble / 1000

    if (millis > 1000) s"${Math.round(millis).toDouble / 1000} s"
    else s"${millis} ms"
  }

  // https://stackoverflow.com/questions/45885151/bytes-in-human-readable-format-with-idiomatic-scala
  def bytesToString(size: Long): String = {
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
