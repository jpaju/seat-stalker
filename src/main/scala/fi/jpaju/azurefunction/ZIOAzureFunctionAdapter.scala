package fi.jpaju.azurefunction

import zio.*
import com.microsoft.azure.functions.ExecutionContext
import fi.jpaju.Logging

object ZIOAzureFunctionAdapter:
  private val runtime = Runtime.default

  def runToExit[E, A](context: ExecutionContext)(zio: ZIO[Any, E, A]): Exit[E, A] =
    val loggerLayer = Logging.azFunctionLoggerLayer(context.getLogger)
    val program     = zio.provide(loggerLayer)

    Unsafe.unsafely(ZIOAzureFunctionAdapter.runtime.unsafe.run(program))

  def runOrThrowError[A](context: ExecutionContext)(zio: ZIO[Any, ?, A]): A =
    Unsafe.unsafely(runToExit(context)(zio).getOrThrowFiberFailure())
