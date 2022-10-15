package fi.jpaju

import zio.*

object ZIOAppRunner:
  private val runtime = Runtime.default

  def runToExit[E, A](program: ZIO[Any, E, A]): Exit[E, A] =
    Unsafe.unsafe(unsafe ?=> ZIOAppRunner.runtime.unsafe.run(program))
