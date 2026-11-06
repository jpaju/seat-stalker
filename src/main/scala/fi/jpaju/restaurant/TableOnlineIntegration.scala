package fi.jpaju.restaurant

import sttp.client4.*
import sttp.client4.ziojson.*
import zio.*
import zio.json.*
import zio.stream.*

import java.time.*
// TODO Remove API link as it's deprecated
// TODO Extract base URL to variable
// TODO Mention restaurant search endpoint: https://service.tableonline.fi/search?q=<search-term>

/** Service for interacting with TableOnline API
  *
  * API docs: http://gol-api-doc.s3-website-eu-west-1.amazonaws.com/#introduction
  */
case class TableOnlineIntegration(sttpBackend: Backend[Task]) extends TableService:
  import TableOnlineIntegration.*

  override def checkAvailableTables(parameters: CheckTablesParameters): UStream[AvailableTable] =
    ZStream.fromZIO(fetchAvailability(parameters).map(_.filter(_.isAvailableWithoutWaitlist))).flatMap {
      case Nil =>
        ZStream.fromZIO(fetchNextAvailableDate(parameters)).flatMap {
          case Some(nextAvailableDate) =>
            checkAvailableTables(parameters.copy(startingFrom = nextAvailableDate.atStartOfDay))
          case None                    => ZStream.empty
        }

      case slots =>
        val tables                 = toAvailableTables(slots, parameters)
        val parametersWithNextDate = parameters.copy(startingFrom = parameters.startingFrom.plusDays(1))
        ZStream.fromIterable(tables) ++ checkAvailableTables(parametersWithNextDate)
    }

  private def fetchAvailability(parameters: CheckTablesParameters): UIO[List[AvailabilitySlot]] =
    val date         = parameters.startingFrom.toLocalDate
    val queryParams  = Map("persons" -> parameters.persons, "date" -> date.toString)
    val restaurantId = parameters.restaurant.id
    val url          = uri"https://api.tableonline.com/restaurant/$restaurantId/availability?$queryParams"
    val request      = basicRequest
      .get(url)
      .response(asJson[List[AvailabilitySlot]])

    request
      .send(sttpBackend)
      .map(_.body)
      .absolve
      .orDie

  private def fetchNextAvailableDate(parameters: CheckTablesParameters): UIO[Option[LocalDate]] =
    val startDate    = parameters.startingFrom.toLocalDate.plusDays(1)
    // A 90-day window covers TableOnline's booking horizon without a separate module metadata request.
    val endDate      = startDate.plusDays(90)
    val queryParams  = Map(
      "start_date" -> startDate.toString,
      "end_date"   -> endDate.toString,
      "persons"    -> parameters.persons
    )
    val restaurantId = parameters.restaurant.id
    val url          = uri"https://api.tableonline.com/restaurant/$restaurantId/availability/calendar?$queryParams"
    val request      = basicRequest
      .get(url)
      .response(asJson[List[CalendarDate]])

    request
      .send(sttpBackend)
      .map(_.body)
      .absolve
      .orDie
      .map(
        _.find(calendarDate =>
          !calendarDate.date.isBefore(startDate) && calendarDate.persons
            .get(parameters.persons.toString)
            .exists(_.availableTimes > 0)
        ).map(_.date)
      )

  private def toAvailableTables(
      slots: List[AvailabilitySlot],
      parameters: CheckTablesParameters
  ): List[AvailableTable] =
    val minTime = parameters.startingFrom

    slots
      .filter(!_.time.isBefore(minTime))
      .map(slot => AvailableTable(slot.time, parameters.persons))

object TableOnlineIntegration:
  private case class AvailabilityTable(@jsonField("waitlist_only") waitlistOnly: Boolean)
  private case class AvailabilitySlot(time: LocalDateTime, tables: List[AvailabilityTable]):
    def isAvailableWithoutWaitlist: Boolean = tables.exists(!_.waitlistOnly)

  private case class CalendarAvailability(@jsonField("available_times") availableTimes: Int)
  private case class CalendarDate(date: LocalDate, persons: Map[String, CalendarAvailability])

  private given JsonDecoder[AvailabilityTable]    = DeriveJsonDecoder.gen[AvailabilityTable]
  private given JsonDecoder[AvailabilitySlot]     = DeriveJsonDecoder.gen[AvailabilitySlot]
  private given JsonDecoder[CalendarAvailability] = DeriveJsonDecoder.gen[CalendarAvailability]
  private given JsonDecoder[CalendarDate]         = DeriveJsonDecoder.gen[CalendarDate]

  val layer = ZLayer.fromFunction(TableOnlineIntegration.apply)
