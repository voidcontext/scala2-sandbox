package com.gaborpihaj
package benchmark

import cats.Show

final case class BenchmarkStats(totalTime: Long, min: Long, max: Long, executions: Int, errors: Int)

object BenchmarkStats {
  implicit val showBenchmarkStats: Show[BenchmarkStats] = { stats =>
    s"Total time: ${formatNanoTime(stats.totalTime)}, " +
      s"min: ${formatNanoTime(stats.min)}, " +
      s"max: ${formatNanoTime(stats.max)}, " +
      s"avg: ${formatNanoTime(stats.totalTime / stats.executions)}, " +
      s"errors: ${stats.errors}"
  }
}
