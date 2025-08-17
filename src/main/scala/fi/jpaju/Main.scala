package fi.jpaju

import zio.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import fi.jpaju.stalker.*
import fi.jpaju.restaurant.TableOnlineIntegration
import fi.jpaju.telegram.*

object Main extends ZIOAppDefault:
  val run = ZIO
    .serviceWithZIO[StalkerApp](_.run)
    .provide(
      LiveStalkerApp.layer,
      LiveStalkerJobRunner.layer,
      StalkerApp.hardcodedJobsRepositoryLayer,
      DefaultMessageFormatter.layer,
      HttpClientZioBackend.layer(),
      TableOnlineIntegration.layer,
      LiveTelegramClient.layer
    )
