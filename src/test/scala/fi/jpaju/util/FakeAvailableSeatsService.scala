package fi.jpaju
package util

import fi.jpaju.seating.*
import zio.*

case class FakeAvailableSeatsService(seats: Ref[Map[RestaurantId, SeatStatus]]) extends AvailableSeatsService:
  def checkAvailableSeats(parameters: CheckSeatsParameters): UIO[SeatStatus] =
    seats.get.map(_.get(parameters.restaurantId).getOrElse(SeatStatus.NotAvailable))

object FakeAvailableSeatsService:
  val layer = ZLayer.fromZIO {
    Ref
      .make(Map.empty[RestaurantId, SeatStatus])
      .map(FakeAvailableSeatsService.apply)
  }
