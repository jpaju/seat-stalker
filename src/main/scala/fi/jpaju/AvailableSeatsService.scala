package fi.jpaju

import zio.*
import zio.prelude.*
import zio.prelude.Assertion.*

import java.time.*

type SeatCount = SeatCount.Type
object SeatCount    extends Subtype[Int]:
  override inline def assertion =
    greaterThan(0)

type RestaurantId = RestaurantId.Type
object RestaurantId extends Subtype[String]:
  override inline def assertion =
    hasLength(greaterThan(0))

case class AvailableSeat(time: LocalDateTime, seats: SeatCount)

enum Seats:
  case NotAvailable
  case Available(seats: List[AvailableSeat])

case class CheckSeatsParameters(
    restaurantId: RestaurantId,
    from: LocalDate,
    seats: SeatCount, // TODO Better model the number of seats in a reservation
    maxResults: Option[Int] = None
)

trait AvailableSeatsService:
  def checkAvailableSeats(parameters: CheckSeatsParameters): UIO[Seats]
