package fi.jpaju

import zio.*

case class FakeAvailableSeatsService(seats: Ref[Map[RestaurantId, SeatStatus]]) extends AvailableSeatsService:
  def checkAvailableSeats(parameters: CheckSeatsParameters): UIO[SeatStatus] =
    seats.get.map(_.get(parameters.restaurantId).getOrElse(SeatStatus.NotAvailable))

object FakeAvailableSeatsService:
  val layer = ZLayer.fromFunction(FakeAvailableSeatsService.apply)
