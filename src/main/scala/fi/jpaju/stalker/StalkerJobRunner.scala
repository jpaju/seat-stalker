package fi.jpaju.stalker

import fi.jpaju.*
import fi.jpaju.seating.*
import fi.jpaju.telegram.*
import zio.*

import java.time.LocalDate

case class StalkerJobDefinition(
    restaurant: Restaurant,
    seatCount: SeatCount
)

trait StalkerJobRunner:
  def runJob(jobDefinition: StalkerJobDefinition): UIO[Unit]

case class LiveStalkerJobRunner(
    seatsService: AvailableSeatsService,
    telegramService: TelegramService
) extends StalkerJobRunner:
  def runJob(jobDefinition: StalkerJobDefinition): UIO[Unit] =
    for
      now        <- Clock.localDateTime.map(_.toLocalDate)
      _          <- ZIO.logDebug(s"Checking available seats for ${jobDefinition.restaurant.name} at $now")
      seatStatus <- checkSeats(jobDefinition, now)
      _          <- ZIO.logDebug(s"Found seats for ${jobDefinition.restaurant.name}: $seatStatus")
      _          <- sendNotifications(jobDefinition.restaurant, seatStatus)
    yield ()

  private def checkSeats(requirements: StalkerJobDefinition, when: LocalDate): UIO[SeatStatus] =
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

        val retryPolicy = Schedule.exponential(1.second) && Schedule.recurs(5)
        telegramMessage
          .flatMap(telegramService.sendMessage(_).retry(retryPolicy))
          .orDieWith(err => new RuntimeException(s"Failed to send Telegram message: $err"))
      }
      .getOrElse(ZIO.unit)

object LiveStalkerJobRunner:
  val layer = ZLayer.fromFunction(LiveStalkerJobRunner.apply)
