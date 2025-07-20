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
object PersonCount extends Subtype[Int]:
  override inline def assertion = greaterThan(0)

case class AvailableTable(time: LocalDateTime, persons: PersonCount)

enum TableStatus:
  case NotAvailable(restaurant: Restaurant)
  case Available(restaurant: Restaurant, tables: List[AvailableTable])

  def fold[A](whenNotAvailable: Restaurant => A, whenAvailable: (Restaurant, List[AvailableTable]) => A): A =
    this match
      case NotAvailable(restaurant)      => whenNotAvailable(restaurant)
      case Available(restaurant, tables) => whenAvailable(restaurant, tables)

  def hasTables: Boolean =
    fold(_ => false, (_, tables) => tables.nonEmpty)

  def availableTables: Option[List[AvailableTable]] =
    fold(_ => None, (_, tables) => Some(tables))
