package com.gaborpihaj
package benchmark

import cats.effect.Sync
import cats.implicits._
import cats.MonadError

trait Benchmarks[F[_]] {
  def time[A](fa: F[A]): F[A]

  def measureTime[A](fa: F[A]): F[TimeResult[Attempt[A]]]

  def benchmark[A](fa: F[TimeResult[Attempt[A]]], repeat: Int = 10): F[BenchmarkResult[A]]

  def benchmarkStats[A](benchmarkResult: BenchmarkResult[A]): F[BenchmarkStats]
}

object Benchmarks {
  implicit def benchmarkF[F[_]: Sync]: Benchmarks[F] =  new Benchmarks[F] {
    override def time[A](fa: F[A]): F[A] = {
      measureTime(fa).flatMap {
        case TimeResult(time, Left(err)) =>
          Sync[F].delay(println("[ERRPR] Elapsed time: " +  formatNanoTime(time)))
            .flatMap(_ => MonadError[F, Throwable].raiseError(err))
        case TimeResult(time, Right(value)) =>
          Sync[F].delay(println("Elapsed time: " +  formatNanoTime(time)))
            .map(_ => value)
      }
    }

    override def measureTime[A](fa: F[A]): F[TimeResult[Attempt[A]]] = {
      for {
        t0     <- Sync[F].delay(System.nanoTime())
        result <- fa.attempt
        t1     <- Sync[F].delay(System.nanoTime())
      } yield TimeResult(t1 - t0, result)
    }


    override def benchmark[A](fa: F[TimeResult[Either[Throwable, A]]], repeat: Int = 10): F[BenchmarkResult[A]] = {
      List.fill(repeat)(fa)
        .traverse(identity)
    }

    override def benchmarkStats[A](result: BenchmarkResult[A]): F[BenchmarkStats] = {
      Sync[F].delay(
        result.foldLeft(BenchmarkStats(0, -1, 0, 0, 0)) {
          case (stats, TimeResult(time, result)) =>
            BenchmarkStats(
              stats.totalTime + time,
              if (stats.min > time || stats.min == -1) time else stats.min,
              if (stats.max < time) time else stats.max,
              stats.executions + 1,
              stats.errors + (if (result.isLeft) 1 else 0)
            )
        }
      )
    }
  }

  def apply[F[_]: Sync](implicit ev: Benchmarks[F]) = ev
}

