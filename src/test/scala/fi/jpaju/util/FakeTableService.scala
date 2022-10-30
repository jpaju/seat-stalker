package fi.jpaju
package util

import fi.jpaju.restaurant.*
import zio.*
import zio.stream.*

case class FakeTableService(tables: Ref[Map[RestaurantId, UStream[AvailableTable]]]) extends TableService:
  def checkAvailableTables(parameters: CheckTablesParameters): UStream[AvailableTable] =
    ZStream.fromZIO {
      tables.get.map(_.get(parameters.restaurant.id).getOrElse(ZStream.empty))
    }.flatten

object FakeTableService:
  def setAvailableTables(statuses: Map[RestaurantId, UStream[AvailableTable]]): URIO[FakeTableService, Unit] =
    ZIO.serviceWithZIO[FakeTableService](_.tables.set(statuses))

  val layer = ZLayer.fromZIO {
    Ref
      .make(Map.empty[RestaurantId, UStream[AvailableTable]])
      .map(FakeTableService.apply)
  }
