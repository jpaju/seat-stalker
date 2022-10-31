package fi.jpaju.stalker

import fi.jpaju.*
import fi.jpaju.restaurant.*
import fi.jpaju.telegram.*
import sttp.client3.httpclient.zio.*
import zio.*

import java.time.LocalDate

object StalkerApp:
  val kaskis    = Restaurant(RestaurantId("291"), "Kaskis")
  val metsäMäki = Restaurant(RestaurantId("1286"), "Ravintola Metsämäki")

  val restaurants    = List(kaskis, metsäMäki)
  val jobDefinitions = restaurants.map(StalkerJobDefinition(_, PersonCount(2)))

  val app =
    for
      _             <- ZIO.log("Seat stalker started")
      service       <- ZIO.service[StalkerJobRunner]
      (duration, _) <- ZIO.foreachPar(jobDefinitions)(service.runJob).timed
      _             <- ZIO.log(s"Seat stalker finished successfully, took ${duration.toMillis} ms")
    yield ()

  val run =
    app.provide(
      LiveStalkerJobRunner.layer,
      ApplicationConfig.layer,
      HttpClientZioBackend.layer(),
      TableOnlineIntegration.layer,
      LiveTelegramService.layer
    )
