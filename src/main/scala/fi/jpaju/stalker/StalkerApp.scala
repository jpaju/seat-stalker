package fi.jpaju.stalker

import fi.jpaju.*
import fi.jpaju.seating.*
import fi.jpaju.telegram.*

import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.httpclient.zio.*
import zio.*

import java.time.LocalDate

object StalkerApp:
  val kaskis       = Restaurant(RestaurantId("291"), "Kaskis")
  val kakolanRuusu = Restaurant(RestaurantId("702"), "Kakolan ruusu")
  val mami         = Restaurant(RestaurantId("723"), "Mami")

  val restaurants    = List(kaskis, mami, kakolanRuusu)
  val jobDefinitions = restaurants.map(StalkerJobDefinition(_, SeatCount(2)))

  val app =
    for
      _       <- ZIO.log("Seat stalker started")
      service <- ZIO.service[StalkerJobRunner]
      _       <- ZIO.foreachPar(jobDefinitions)(service.runJob)
      _       <- ZIO.log("Seat stalker finished successfully")
    yield ()

  val run =
    app.provide(
      LiveStalkerJobRunner.layer,
      ApplicationConfig.layer,
      AsyncHttpClientZioBackend.layer(),
      TableOnlineSeatsService.layer,
      LiveTelegramService.layer
    )
