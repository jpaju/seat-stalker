package fi.jpaju.azurefunction

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.TimerTrigger
import fi.jpaju.*
import fi.jpaju.restaurant.*
import fi.jpaju.stalker.*
import fi.jpaju.telegram.*
import sttp.client3.httpclient.zio.*
import zio.*

import java.time.LocalDate
import java.util.Optional

class SeatStalkerTimerFunction:
  @FunctionName("SeatStalkerTimerFunction")
  def run(
      @TimerTrigger(name = "timerInfo", schedule = "5 0 0 * * *") timerInfo: String,
      context: ExecutionContext
  ): Unit =
    val program =
      ZIO.log(s"Timer triggered, TimerInfo: $timerInfo") *>
        ZIO.serviceWithZIO[StalkerApp](_.run)

    ZIOAppRunner.runThrowOnError(
      program.provide(
        LiveStalkerApp.layer,
        LiveStalkerJobRunner.layer,
        StalkerApp.hardcodedJobsRepositoryLayer,
        ApplicationConfig.layer,
        HttpClientZioBackend.layer(),
        TableOnlineIntegration.layer,
        LiveTelegramService.layer,
        Logging.azFunctionLoggerLayer(context.getLogger)
      )
    )
