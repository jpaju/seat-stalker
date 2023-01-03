package fi.jpaju.restaurant

import sttp.client3.*
import sttp.client3.ziojson.*
import zio.*
import zio.json.*
import zio.prelude.Validation
import zio.stream.*

import java.time.*

/** Service for interacting with TableOnline API
  *
  * API docs: http://gol-api-doc.s3-website-eu-west-1.amazonaws.com/#introduction
  */
case class TableOnlineIntegration(sttpBackend: SttpBackend[Task, Any]) extends TableService:
  import TableOnlineIntegration.*

  override def checkAvailableTables(parameters: CheckTablesParameters): UStream[AvailableTable] =
    val responseStream = ZStream.fromZIO(fetchPeriods(parameters))

    responseStream.flatMap {
      case PeriodsResponse(Nil, None) => ZStream.empty

      case PeriodsResponse(Nil, Some(nextAvailableDate)) =>
        val parametersWithNewDate = parameters.copy(startingFrom = nextAvailableDate.atStartOfDay)
        checkAvailableTables(parametersWithNewDate)

      case PeriodsResponse(periods, _) =>
        val tables                = toAvailableTables(periods, parameters.startingFrom)
        val nextday               = parameters.startingFrom.plusDays(1)
        val parametersWithNextDay = parameters.copy(startingFrom = nextday)
        ZStream.fromIterable(tables) ++ checkAvailableTables(parametersWithNextDay)
    }

  private def fetchPeriods(parameters: CheckTablesParameters): UIO[PeriodsResponse] =
    val date         = parameters.startingFrom.toLocalDate
    val queryParams  = Map("persons" -> parameters.persons, "date" -> date.toString)
    val restaurantId = parameters.restaurant.id
    val url          = uri"https://service.tableonline.fi/public/r/$restaurantId/periods?$queryParams"
    val request      = basicRequest
      .get(url)
      .response(asJson[PeriodsResponse])
      .responseGetRight

    sttpBackend
      .send(request)
      .map(_.body)
      .orDie

  private def toAvailableTables(periods: List[Period], dateTime: LocalDateTime): List[AvailableTable] =
    val minTime = dateTime.toLocalTime
    val date    = dateTime.toLocalDate

    import math.Ordered.orderingToOrdered
    given Ordering[LocalTime] = Ordering.by(_.toSecondOfDay)

    periods
      .flatMap { period =>
        period.hours
          .filter(_.time >= minTime)
          .map { hour =>
            PersonCount.make(period.persons).map(AvailableTable(hour.time.atDate(date), _))
          }
      }
      .collect { case Validation.Success(_, tables) => tables }

object TableOnlineIntegration:
  private case class PeriodsResponse(
      periods: List[Period],
      @jsonField("next_available_date") nextAvailableDate: Option[LocalDate]
  )

  private case class Period(persons: Int, hours: List[Hour])
  private case class Hour(time: LocalTime)

  private given JsonDecoder[Hour]            = DeriveJsonDecoder.gen[Hour]
  private given JsonDecoder[Period]          = DeriveJsonDecoder.gen[Period]
  private given JsonDecoder[PeriodsResponse] = DeriveJsonDecoder.gen[PeriodsResponse]

  val layer = ZLayer.fromFunction(TableOnlineIntegration.apply)
