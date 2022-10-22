package fi.jpaju.seating

import zio.prelude.Assertion.*
import zio.prelude.*

import java.time.*

case class Restaurant(
    id: RestaurantId,
    name: String
)

type SeatCount = SeatCount.Type
object SeatCount    extends Subtype[Int]:
  override inline def assertion = greaterThan(0)

type RestaurantId = RestaurantId.Type
object RestaurantId extends Subtype[String]:
  override inline def assertion = hasLength(greaterThan(0))

// TODO Better name
case class AvailableSeat(time: LocalDateTime, seatCount: SeatCount)

// TODO Better name
enum SeatStatus:
  case NotAvailable
  case Available(seats: List[AvailableSeat])

  def availableSeats: Option[List[AvailableSeat]] =
    this match
      case Available(seats) => Some(seats)
      case NotAvailable     => None

  def hasAvailableSeats: Boolean = this match
    case NotAvailable => false
    case Available(_) => true
