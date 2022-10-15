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
    val program = ZIO.log(s"Timer triggered, TimerInfo: $timerInfo") *> SeatStalker.run

    ZIOAppRunner.runThrowOnError(
      program.provide(Logging.azFunLoggerLayer(context.getLogger))
    )
