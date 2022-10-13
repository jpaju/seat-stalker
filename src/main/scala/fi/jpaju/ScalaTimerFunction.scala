package fi.jpaju

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.TimerTrigger
import zio.*

import java.util.Optional

class ScalaTimerFunction:

  @FunctionName("ScalaTimerFunction")
  def run(
      @TimerTrigger(name = "timerInfo", schedule = "*/15 * * * * *") timerInfo: String,
      context: ExecutionContext
  ): Unit =
    val msg     = s"Scala Timer trigger processing stuff. TimerInfo: $timerInfo"
    val logIt   = ZIO.attempt(context.getLogger.info(msg))
    val program = logIt.repeatN(4)

    val runtime = Runtime.default
    Unsafe.unsafe(unsafe ?=> runtime.unsafe.run(program))
