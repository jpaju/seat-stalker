package fi.jpaju.restaurant

import zio.stream.*

import java.time.*

case class CheckTablesParameters(
    restaurant: Restaurant,
    persons: PersonCount,
    startingFrom: LocalDateTime
)

object CheckTablesParameters:
  def apply(restaurant: Restaurant, persons: PersonCount, startingFrom: LocalDate): CheckTablesParameters =
    CheckTablesParameters(restaurant, persons, startingFrom.atStartOfDay)

trait TableService:
  def checkAvailableTables(parameters: CheckTablesParameters): UStream[AvailableTable]
