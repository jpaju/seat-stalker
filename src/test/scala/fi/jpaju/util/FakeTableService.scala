package fi.jpaju
package util

import fi.jpaju.restaurant.*
import zio.*

case class FakeTableService(tables: Ref[Map[RestaurantId, TableStatus]]) extends TableService:
  def checkAvailableTables(parameters: CheckTablesParameters): UIO[TableStatus] =
    tables.get.map(_.get(parameters.restaurant.id).getOrElse(TableStatus.NotAvailable(parameters.restaurant)))

object FakeTableService:
  val layer = ZLayer.fromZIO {
    Ref
      .make(Map.empty[RestaurantId, TableStatus])
      .map(FakeTableService.apply)
  }
