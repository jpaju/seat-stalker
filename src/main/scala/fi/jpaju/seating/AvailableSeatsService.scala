package fi.jpaju.seating

import zio.*

import java.time.*

case class CheckSeatsParameters(
    restaurantId: RestaurantId,
    from: LocalDate,
    seats: SeatCount, // TODO Better model the number of seats in a reservation
    maxResults: Option[Int] = None
)

trait AvailableSeatsService:
  def checkAvailableSeats(parameters: CheckSeatsParameters): UIO[SeatStatus]
