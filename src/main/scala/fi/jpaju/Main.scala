package fi.jpaju

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

  // TODO Lataa jostain muualta
  val telegramConfig = LiveTelegramService.Config(
    token = "5792135341:AAHgy5CTYumV39UOXsXJqfrba2v-9VZIb94",
    chatId = "-1001817665705"
  )

  val run =
    program.provide(
      TableOnlineSeatsService.layer,
      HttpClientZioBackend.layer(),
      LiveTelegramService.layer,
      ZLayer.succeed(telegramConfig)
    )
