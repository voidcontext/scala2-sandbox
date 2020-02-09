package com

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._

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
    val millis = Math.round(t / 1000).toDouble / 1000

    if (millis > 1000) Math.round(millis).toDouble / 1000 + "s"
    else millis + "ms"
  }
}
