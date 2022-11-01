package fi.jpaju

import zio.*
import fi.jpaju.stalker.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import fi.jpaju.restaurant.TableOnlineIntegration
import fi.jpaju.telegram.LiveTelegramService

object Main extends ZIOAppDefault:
  val run = ZIO
    .serviceWithZIO[StalkerApp](_.run)
    .provide(
      LiveStalkerApp.layer,
      LiveStalkerJobRunner.layer,
      StalkerApp.hardcodedJobsRepositoryLayer,
      ApplicationConfig.layer,
      HttpClientZioBackend.layer(),
      TableOnlineIntegration.layer,
      LiveTelegramService.layer
    )
