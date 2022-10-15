package fi.jpaju

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.TimerTrigger
import zio.*

import java.time.LocalDate
import java.util.Optional

class SeatStalkerTimerFunction:
  @FunctionName("SeatStalkerTimerFunction")
  def run(
      @TimerTrigger(name = "timerInfo", schedule = "*/15 * * * * *") timerInfo: String,
      context: ExecutionContext
  ): Unit =
    val msg     = s"Timer triggered, TimerInfo: $timerInfo"
    val logIt   = ZIO.attempt(context.getLogger.info(msg))
    val program = (logIt *> SeatStalker.run)

    ZIOAppRunner.runToExit(program)
