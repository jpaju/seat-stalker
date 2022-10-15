package fi.jpaju

import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.httpclient.zio.*
import zio.*

import java.time.LocalDate

object SeatStalker:
  val kaskis       = Restaurant(RestaurantId("291"), "Kaskis")
  val kakolanRuusu = Restaurant(RestaurantId("702"), "Kakolan ruusu")
  val mami         = Restaurant(RestaurantId("723"), "Mami")

  val restaurants      = List(kaskis, mami, kakolanRuusu)
  val seatRequirements = restaurants.map(SeatRequirements(_, SeatCount(2)))

  val app =
    for
      _       <- ZIO.debug("Starting seat stalker")
      service <- ZIO.service[AvailableSeatNotifier]
      _       <- ZIO.foreachPar(seatRequirements)(service.checkAndNotify)
      _       <- ZIO.debug("Seat stalker finished")
    yield ()

  val run =
    app.provide(
      LiveAvailableSeatNotifier.layer,
      ApplicationConfig.layer,
      AsyncHttpClientZioBackend.layer(),
      TableOnlineSeatsService.layer,
      LiveTelegramService.layer
    )
