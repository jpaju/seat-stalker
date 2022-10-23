package fi.jpaju.restaurant

import zio.prelude.Assertion.*
import zio.prelude.*

import java.time.*

case class Restaurant(
    id: RestaurantId,
    name: String
)

type RestaurantId = RestaurantId.Type
object RestaurantId extends Subtype[String]:
  override inline def assertion = hasLength(greaterThan(0))

type PersonCount = PersonCount.Type
object PersonCount  extends Subtype[Int]:
  override inline def assertion = greaterThan(0)

case class AvailableTable(time: LocalDateTime, persons: PersonCount)

enum TableStatus:
  case NotAvailable(restaurant: Restaurant)
  case Available(restaurant: Restaurant, tables: List[AvailableTable])

  def availableTables: Option[List[AvailableTable]] =
    this match
      case Available(_, tables) => Some(tables)
      case _                    => None
