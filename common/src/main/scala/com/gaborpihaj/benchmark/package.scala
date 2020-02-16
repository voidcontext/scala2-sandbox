package com.gaborpihaj

package object benchmark {
  type Attempt[A] = Either[Throwable, A]
  type BenchmarkResult[A] = List[TimeResult[Attempt[A]]]
}
