package com.gaborpihaj.benchmark

import cats.effect.IO
import scala.concurrent.blocking
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.concurrent.Ref

class BenchmarksSpec extends AnyFlatSpec with Matchers {
  val benchmarks = Benchmarks.impl[IO]

  "Benchmarks::time" should "return the value of the computation" in {
    benchmarks.time(IO.pure(5)).unsafeRunSync() should be(5)
  }

  "Benchmarks::measureTime" should "attempt to return the result of the computation" in {
    benchmarks.measureTime(IO.pure(5)).unsafeRunSync().result should be(Right(5))
  }

  it should "return the time measurement" in {
    val result = benchmarks.measureTime(IO.delay( blocking {Thread.sleep(500) } )).unsafeRunSync()

    result.nanoSeconds should be >= (500L * 1000000)
  }

  it should "return the error when an error happens" in {
    val error = new RuntimeException("Error!")
    benchmarks.measureTime(IO.raiseError(error)).unsafeRunSync().result should be(Left(error))
  }

  it should "return the time even when an error happens" in {
    val error = new RuntimeException("Error!")
    val result = benchmarks.measureTime(
      IO.delay( blocking {Thread.sleep(500) } )
        .flatMap(_ => IO.raiseError(error))
    ).unsafeRunSync()

    result.nanoSeconds should be >= (500L * 1000000)
  }

  "Benchmarks::benchmark" should "repeat the computation the given amount of time" in {
    val refIO = Ref.of[IO, Int](0)

    (for {
      ref         <- refIO
      computation = ref.modify(x => (x + 1, x))
      _           <- benchmarks.benchmark(benchmarks.measureTime(computation), 3)
      count       <- ref.get
    } yield count).unsafeRunSync() should be(3)

  }


  it should "attempt to return all results" in {
    val refIO = Ref.of[IO, Int](0)

    (for {
      ref         <- refIO
      computation = ref.modify(x => (x + 1, x))
      result      <- benchmarks.benchmark(benchmarks.measureTime(computation), 3)
    } yield result).unsafeRunSync().map(_.result) should be(List(Right(0), Right(1), Right(2)))

  }

  "Benchmarks::benchmarkStats" should "accumulate stats from a series of benchmarks" in {
    val results: BenchmarkResult[Int] = List(
      TimeResult(50000000L, Right(5)),
      TimeResult(60000000L, Right(3)),
      TimeResult(20000000L, Right(4)),
      TimeResult(10000000L, Left(new RuntimeException("Error..."))),
    )

    benchmarks.benchmarkStats(results).unsafeRunSync() should be(
      BenchmarkStats(140000000L, 10000000L, 60000000L, 4, 1)
    )
  }
}

