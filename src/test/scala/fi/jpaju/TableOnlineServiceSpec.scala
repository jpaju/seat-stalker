package fi.jpaju

import sttp.client3.*
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.testing.*
import sttp.model.*
import zio.*
import zio.test.Assertion.*
import zio.test.*

import java.time.*

object TableOnlineServiceSpec extends ZIOSpecDefault:
  override def spec = suite("TableOnlineServiceSpec")(
    test("make request with correct url and parameters") {
      val parameters = CheckSeatsParameters(
        RestaurantId("1312"),
        LocalDate.parse("2021-03-01"),
        SeatCount(5)
      )

      val recordingBackend = new RecordingSttpBackend(
        AsyncHttpClientZioBackend.stub.whenAnyRequest
          .thenRespond(noPeriodsJson)
      )

      def assertCorrectRequest(request: Request[?, ?]): TestResult =
        val uri                                      = request.uri
        val expectedQueryParams: Map[String, String] = Map(
          "persons" -> parameters.seats.toString,
          "date"    -> parameters.from.toString
        )

        assert(uri.host)(equalTo(Some("service.tableonline.fi"))) &&
        assert(uri.path)(equalTo(List("public", "r", parameters.restaurantId, "periods"))) &&
        assert(uri.paramsMap)(equalTo(expectedQueryParams))
      end assertCorrectRequest

      withTableOnlineService(recordingBackend) {
        for
          service <- ZIO.service[AvailableSeatsService]
          _       <- service.checkAvailableSeats(parameters)
          request  = recordingBackend.allInteractions.head._1
        yield assertCorrectRequest(request)
      }
    },
    test("when next_available_date is null, then returns Seats.NotAvailable") {
      val sttpBackendStub = AsyncHttpClientZioBackend.stub.whenAnyRequest
        .thenRespond(noPeriodsJson)

      withTableOnlineService(sttpBackendStub) {
        val parameters = defaultParameters

        for
          service <- ZIO.service[AvailableSeatsService]
          result  <- service.checkAvailableSeats(parameters)
        yield assertTrue(result == Seats.NotAvailable)
      }
    },
    test("when periods available, then returns Seats.Available") {
      val responseData = List(
        (SeatCount(2), LocalTime.parse("12:00:00")),
        (SeatCount(34), LocalTime.parse("21:30:00")),
        (SeatCount(12), LocalTime.parse("13:18:00"))
      )

      val responseJson    = periodsJson(responseData)
      val sttpBackendStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond(responseJson)

      withTableOnlineService(sttpBackendStub) {
        val parameters     = defaultParameters
        val expectedResult = Seats.Available(
          responseData.map { (seatCount, time) =>
            AvailableSeat(time.atDate(parameters.from), seatCount)
          }
        )

        for
          service <- ZIO.service[AvailableSeatsService]
          result  <- service.checkAvailableSeats(parameters)
        yield assertTrue(result == expectedResult)
      }
    }
  )

  // =============================================== Helpers ===============================================

  private def withTableOnlineService[R, E, A](sttpBackend: SttpBackend[Task, Any])(
      test: ZIO[R & AvailableSeatsService & SttpBackend[Task, Any], E, A]
  ): ZIO[R, E, A] =
    test.provideSome[R](TableOnlineSeatsService.layer, ZLayer.succeed(sttpBackend))

  private def defaultParameters: CheckSeatsParameters =
    CheckSeatsParameters(
      RestaurantId("123"),
      LocalDate.parse("2021-03-01"),
      SeatCount(2)
    )

  private def noPeriodsJson: String = """
    {
        "periods": [],
        "next_available_date": null
    }
    """

  private def periodsJson(periods: List[(SeatCount, LocalTime)]): String =
    def singlePeriodJson(seatCount: SeatCount, time: LocalTime): String = s"""
      {
          "period": "lunch",
          "persons": $seatCount,
          "hours": [
              {
                  "object": "hour",
                  "time": "${time.toString}",
                  "booking_state": "validated"
              }
          ]
      }
      """

    s"""{ "periods": [ ${periods.map(singlePeriodJson).mkString(",")} ] }"""
  end periodsJson
