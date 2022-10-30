package fi.jpaju.restaurant

import zio.stream.*

import java.time.*

case class CheckTablesParameters(
    restaurant: Restaurant,
    persons: PersonCount,
    startingFrom: LocalDate
)

trait TableService:
  def checkAvailableTables(parameters: CheckTablesParameters): UStream[AvailableTable]
