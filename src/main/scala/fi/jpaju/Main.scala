package fi.jpaju

import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.httpclient.zio.*
import zio.*

import java.time.LocalDate

object SimpleClient extends ZIOAppDefault:
  val date      = LocalDate.parse("2022-10-10")
  val seatCount = SeatCount(2)

  val woolshedParams     = CheckSeatsParameters(RestaurantId("624"), date, seatCount)
  val kaskisParams       = CheckSeatsParameters(RestaurantId("291"), date, seatCount)
  val kakolanRuusuParams = CheckSeatsParameters(RestaurantId("702"), date, seatCount)

  val telegramProgram =
    for
      _               <- ZIO.debug("Starting")
      now             <- Clock.localDateTime
      message          = s"Noniih, laitetaas sit sttp rokkaa $now"
      telegramService <- ZIO.service[TelegramService]
      _               <- telegramService.sendMessage(message)
    yield ()

  val availableSeatsProgram =
    val program =
      for
        now          <- Clock.localDateTime.map(_.toLocalDate)
        seatsService <- ZIO.service[AvailableSeatsService]
        _            <- seatsService.checkAvailableSeats(kaskisParams).debug("Kaskis") <&>
                          seatsService.checkAvailableSeats(kakolanRuusuParams).debug("Kakolan ruusu") <&>
                          seatsService.checkAvailableSeats(woolshedParams).debug("Kakolan ruusu")
      yield ()

  val program =
    for
      _      <- ZIO.debug("Starting")
      config <- ZIO.service[TelegramConfig].debug("Config")
    yield ()

  val run =
    program.provide(
      ApplicationConfig.layer
        // AsyncHttpClientZioBackend.layer()
        // TableOnlineSeatsService.layer,
        // LiveTelegramService.layer,
        // ZLayer.succeed(telegramConfig)
    )
