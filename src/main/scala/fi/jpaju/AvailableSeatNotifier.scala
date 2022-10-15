package fi.jpaju

import zio.*

import java.time.LocalDate

case class SeatRequirements(
    restaurant: Restaurant,
    seatCount: SeatCount
)

trait AvailableSeatNotifier:
  def checkAndNotify(requirements: SeatRequirements): UIO[Unit]

case class LiveAvailableSeatNotifier(
    seatsService: AvailableSeatsService,
    telegramService: TelegramService
) extends AvailableSeatNotifier:
  def checkAndNotify(requirements: SeatRequirements): UIO[Unit] =
    for
      now        <- Clock.localDateTime.map(_.toLocalDate)
      _          <- ZIO.logDebug(s"Checking available seats for ${requirements.restaurant.name} at $now")
      seatStatus <- checkSeats(requirements, now)
      _          <- ZIO.logDebug(s"Found seats for ${requirements.restaurant.name}: $seatStatus")
      _          <- sendNotifications(requirements.restaurant, seatStatus)
    yield ()

  private def checkSeats(requirements: SeatRequirements, when: LocalDate): UIO[SeatStatus] =
    val checkSeatsParameters = CheckSeatsParameters(
      requirements.restaurant.id,
      when,
      requirements.seatCount
    )

    seatsService.checkAvailableSeats(checkSeatsParameters)

  private def sendNotifications(restaurant: Restaurant, seatStatus: SeatStatus): UIO[Unit] =
    seatStatus.availableSeats
      .map { availableSeats =>
        val message         = MessageFormatter.seatsAvailableMessage(restaurant, availableSeats)
        val telegramMessage = ZIO
          .fromEither(TelegramMessageBody.make(message).toEither)
          .orDieWith(validationErrs =>
            new RuntimeException(s"Telegram message was nonempty: ${validationErrs.toString}")
          )

        telegramMessage.flatMap(telegramService.sendMessage(_))
      }
      .getOrElse(ZIO.unit)

object LiveAvailableSeatNotifier:
  val layer = ZLayer.fromFunction(LiveAvailableSeatNotifier.apply)
