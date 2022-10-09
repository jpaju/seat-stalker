//> using lib "dev.zio::zio:2.0.2"
//> using lib "dev.zio::zio-prelude:1.0.0-RC16"
//> using lib "dev.zio::zio-json:0.3.0"
//> using lib "com.softwaremill.sttp.client3::core:3.8.2"
//> using lib "com.softwaremill.sttp.client3::async-http-client-backend-zio:3.8.2"
//> using lib "com.softwaremill.sttp.client3::zio-json:3.8.2"

//> using file "AvailableSeatsService.scala"
//> using file "TableOnlineService.scala"
//> using file "TelegramService.scala"

import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.*

import java.time.LocalDate

object SimpleClient extends ZIOAppDefault:
  val date      = LocalDate.parse("2022-10-10")
  val seatCount = SeatCount(2)

  val woolshedParams     = CheckSeatsParameters(RestaurantId("624"), date, seatCount)
  val kaskisParams       = CheckSeatsParameters(RestaurantId("291"), date, seatCount)
  val kakolanRuusuParams = CheckSeatsParameters(RestaurantId("702"), date, seatCount)

  val program =
    for
      _               <- ZIO.debug("Starting")
      now             <- Clock.localDateTime
      message          = s"Noniih, laitetaas sit sttp rokkaa $now"
      telegramService <- ZIO.service[TelegramService]
      _               <- telegramService.sendMessage(message)

      now          <- Clock.localDateTime.map(_.toLocalDate)
      seatsService <- ZIO.service[AvailableSeatsService]
      _            <- seatsService.checkAvailableSeats(kaskisParams).debug("Kaskis") <&>
                        seatsService.checkAvailableSeats(kakolanRuusuParams).debug("Kakolan ruusu") <&>
                        seatsService.checkAvailableSeats(woolshedParams).debug("Kakolan ruusu")
    yield ()

  val telegramConfig = ???

  val run =
    program.provide(
      TableOnlineSeatsService.layer,
      HttpClientZioBackend.layer(),
      LiveTelegramService.layer,
      ZLayer.succeed(telegramConfig)
    )
