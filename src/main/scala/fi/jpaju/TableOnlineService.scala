package fi.jpaju

import sttp.client3.*
import sttp.client3.ziojson.*
import zio.*
import zio.json.*
import zio.prelude.*

import java.time.*

/** Service for interacting with TableOnline API
  *
  * API docs: http://gol-api-doc.s3-website-eu-west-1.amazonaws.com/#introduction
  */
case class TableOnlineSeatsService(sttpBackend: SttpBackend[Task, Any]) extends AvailableSeatsService:
  import TableOnlineSeatsService.*

  override def checkAvailableSeats(parameters: CheckSeatsParameters): UIO[SeatStatus] =
    val response = fetchPeriods(parameters)

    response.flatMap {
      case PeriodsResponse(Nil, None) =>
        ZIO.succeed(SeatStatus.NotAvailable)

      case PeriodsResponse(Nil, Some(nextAvailableDate)) =>
        val parametersWithNewDate = parameters.copy(from = nextAvailableDate)
        checkAvailableSeats(parametersWithNewDate)

      case PeriodsResponse(periods, _) =>
        val seats  = toAvailableSeats(periods, parameters.from)
        val result = SeatStatus.Available(seats)
        ZIO.succeed(result)
    }

  private def fetchPeriods(parameters: CheckSeatsParameters): UIO[PeriodsResponse] =
    val queryParams  = Map("persons" -> parameters.seats, "date" -> parameters.from.toString)
    val restaurantId = parameters.restaurantId
    val url          = uri"https://service.tableonline.fi/public/r/$restaurantId/periods?$queryParams"
    val request      =
      basicRequest
        .get(url)
        .response(asJson[PeriodsResponse])
        .responseGetRight

    sttpBackend
      .send(request)
      .map(_.body)
      .orDie

  private def toAvailableSeats(periods: List[Period], date: LocalDate) =
    periods
      .flatMap { period =>
        period.hours.map { hour =>
          SeatCount.make(period.persons).map(AvailableSeat(hour.time.atDate(date), _))
        }
      }
      .collect { case Validation.Success(_, seats) => seats }

object TableOnlineSeatsService:
  private case class PeriodsResponse(
      periods: List[Period],
      @jsonField("next_available_date") nextAvailableDate: Option[LocalDate]
  ):
    def toAvailableSeats(requestDate: LocalDate): List[Validation[String, AvailableSeat]] =
      periods.flatMap { period =>
        period.hours.map { hour =>
          SeatCount.make(period.persons).map(AvailableSeat(hour.time.atDate(requestDate), _))
        }
      }

  private case class Period(persons: Int, hours: List[Hour])
  private case class Hour(time: LocalTime)

  private given JsonDecoder[Hour]            = DeriveJsonDecoder.gen[Hour]
  private given JsonDecoder[Period]          = DeriveJsonDecoder.gen[Period]
  private given JsonDecoder[PeriodsResponse] = DeriveJsonDecoder.gen[PeriodsResponse]

  val layer = ZLayer.fromFunction(TableOnlineSeatsService.apply)
