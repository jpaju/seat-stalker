package fi.jpaju.azurefunction

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.TimerTrigger
import fi.jpaju.restaurant.*
import fi.jpaju.stalker.*
import fi.jpaju.telegram.*
import sttp.client3.httpclient.zio.*
import zio.*

class SeatStalkerTimerFunction:
  @FunctionName("SeatStalkerTimerFunction")
  def run(
      @TimerTrigger(name = "timerInfo", schedule = "5 0 0 * * *") timerInfo: String,
      context: ExecutionContext
  ): Unit =
    val program =
      for
        _ <- ZIO.log(s"Timer triggered, timerInfo: $timerInfo")
        _ <- ZIO.serviceWithZIO[StalkerApp](_.run)
      yield ()

    ZIOAzureFunctionAdapter.runOrThrowError(context) {
      program.provide(
        LiveStalkerApp.layer,
        LiveStalkerJobRunner.layer,
        StalkerApp.hardcodedJobsRepositoryLayer,
        DefaultMessageFormatter.layer,
        HttpClientZioBackend.layer(),
        TableOnlineIntegration.layer,
        LiveTelegramClient.layer
      )
    }
