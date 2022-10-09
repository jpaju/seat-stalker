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
    val queryParams  = Map("persons" -> parameters.seats, "date" -> parameters.from.toString)
    val restaurantId = parameters.restaurantId
    val url          = uri"https://service.tableonline.fi/public/r/$restaurantId/periods?$queryParams"
    val request      =
      basicRequest
        .get(url)
        .response(asJson[PeriodsResponse])
        .responseGetRight

    val response =
      sttpBackend
        .send(request)
        .map(_.body)
        .orDie

    // TODO If response.nextAvailableDate is Some(date), fetch seats for that date
    response.map { response =>
      if response.isEmpty then SeatStatus.NotAvailable
      else
        SeatStatus.Available(
          response
            .toAvailableSeats(parameters.from)
            .collect { case Validation.Success(_, seats) => seats }
        )
    }

object TableOnlineSeatsService:
  private case class PeriodsResponse(periods: List[Period], nextAvailableDate: Option[LocalDate]):
    def isEmpty: Boolean = periods.isEmpty

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
