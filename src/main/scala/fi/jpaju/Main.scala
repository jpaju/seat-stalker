package fi.jpaju

import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.httpclient.zio.*
import zio.*

import java.time.LocalDate

object Main extends ZIOAppDefault:
  val kaskis       = Restaurant(RestaurantId("291"), "Kaskis")
  val kakolanRuusu = Restaurant(RestaurantId("702"), "Kakolan ruusu")
  val mami         = Restaurant(RestaurantId("723"), "Mami")

  val seatRequirements = SeatRequirements(kaskis, SeatCount(2))

  val program =
    for
      _ <- ZIO.debug("Starting")
      _ <- ZIO.serviceWithZIO[AvailableSeatNotifier](_.checkAndNotify(seatRequirements))
      _ <- ZIO.debug("Stopping")
    yield ()

  val run =
    program.provide(
      LiveAvailableSeatNotifier.layer,
      ApplicationConfig.layer,
      AsyncHttpClientZioBackend.layer(),
      TableOnlineSeatsService.layer,
      LiveTelegramService.layer
    )
