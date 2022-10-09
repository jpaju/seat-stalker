import zio.*
import zio.prelude.*

import java.time.*

object SeatCount extends Subtype[Int]
type SeatCount = SeatCount.Type

object RestaurantId extends Subtype[String]
type RestaurantId = RestaurantId.Type

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
