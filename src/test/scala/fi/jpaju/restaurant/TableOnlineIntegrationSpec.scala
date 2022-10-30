package fi.jpaju
package restaurant

import sttp.client3.*
import sttp.client3.testing.*
import sttp.model.*
import zio.*
import zio.stream.*
import zio.test.*

import java.time.*

object TableOnlineServiceSpec extends ZIOSpecDefault:
  override def spec = suite("TableOnlineServiceSpec")(
    test("make request with correct url and parameters") {
      val recordingBackend = new RecordingSttpBackend(
        AsyncHttpClientZioBackend.stub.whenAnyRequest
          .thenRespond(noTablesAvailableJson)
      )

      val parameters = CheckTablesParameters(
        restaurant = Restaurant(RestaurantId("42"), "The restaurant"),
        persons = PersonCount(3),
        startingFrom = LocalDate.parse("2021-01-01")
      )

      val expectedQueryParams: Map[String, String] = Map(
        "persons" -> parameters.persons.toString,
        "date"    -> parameters.startingFrom.toString
      )

      withTableOnlineService(recordingBackend) { service =>
        for
          _      <- service.checkAvailableTables(parameters).runDrain
          request = recordingBackend.allInteractions.head._1
          uri     = request.uri
        yield assert(uri.host)(equalTo(Some("service.tableonline.fi"))) &&
          assert(uri.path)(equalTo(List("public", "r", parameters.restaurant.id, "periods"))) &&
          assert(uri.paramsMap)(equalTo(expectedQueryParams))
      }
    },
    test("when next_available_date is null, then returns emppty stream") {
      val sttpBackendStub = AsyncHttpClientZioBackend.stub.whenAnyRequest
        .thenRespond(noTablesAvailableJson)

      withTableOnlineService(sttpBackendStub) { service =>
        for results <- service.checkAvailableTables(defaultParameters).collectToList
        yield assertTrue(results.isEmpty)
      }
    },
    test("when tables available for requested date, then they are returned") {
      val responseData = List(
        (PersonCount(2), LocalTime.parse("12:00:00")),
        (PersonCount(34), LocalTime.parse("21:30:00")),
        (PersonCount(12), LocalTime.parse("13:18:00"))
      )

      val sttpBackendStub = AsyncHttpClientZioBackend.stub
        .whenRequestMatchesPartial({
          case r if queriedDateIs(r.uri, defaultParameters.startingFrom) =>
            Response.ok(tablesAvailableNowJson(responseData))

          case _ => Response.ok(noTablesAvailableJson)
        })

      withTableOnlineService(sttpBackendStub) { service =>
        val expectedResult = responseData.map { (personCount, time) =>
          AvailableTable(time.atDate(defaultParameters.startingFrom), personCount)
        }

        for result <- service.checkAvailableTables(defaultParameters).runCollect.map(_.toList)
        yield assertTrue(result == expectedResult)
      }
    },
    test("when no tables available for requested date, return next available tables after requested date") {
      val now        = LocalDate.parse("2021-03-01")
      val later      = LocalDate.parse("2021-03-05")
      val restaurant = Restaurant(RestaurantId("123"), "Test Restaurant")
      val parameters = CheckTablesParameters(restaurant, PersonCount(2), now)

      val nextAvailableTables = List(PersonCount(2) -> LocalTime.parse("12:00:00"))

      val sttpBackendStub = AsyncHttpClientZioBackend.stub
        .whenRequestMatchesPartial({
          case r if queriedDateIs(r.uri, now) =>
            Response.ok(tablesAvailableLaterJson(later))

          case r if queriedDateIs(r.uri, later) =>
            Response.ok(tablesAvailableNowJson(nextAvailableTables))

          case _ => Response.ok(noTablesAvailableJson)
        })

      withTableOnlineService(sttpBackendStub) { service =>
        val expectedResult = nextAvailableTables.map { (personCount, time) =>
          AvailableTable(time.atDate(later), personCount)
        }

        for result <- service.checkAvailableTables(parameters).collectToList
        yield assertTrue(result == expectedResult)
      }
    }
  )

  // =============================================== Helpers ===============================================

  private def queriedDateIs(uri: Uri, date: LocalDate) =
    uri.paramsMap.get("date").contains(date.toString)

  extension [A](stream: UStream[A])
    def collectToList: ZIO[Any, Throwable, List[A]] =
      stream.runCollect.map(_.toList)

  private def withTableOnlineService[R, E, A](sttpBackend: SttpBackend[Task, Any])(
      f: TableService => ZIO[R & SttpBackend[Task, Any], E, A]
  ): ZIO[R, E, A] =
    ZIO
      .serviceWithZIO[TableService](f)
      .provideSome[R](TableOnlineIntegration.layer, ZLayer.succeed(sttpBackend))

  private def defaultParameters: CheckTablesParameters =
    CheckTablesParameters(
      Restaurant(RestaurantId("123"), "Test Restaurant"),
      PersonCount(2),
      LocalDate.parse("2021-03-01")
    )

  // ============================================ Response JSON ============================================

  private def tablesAvailableNowJson(periods: List[(PersonCount, LocalTime)]): String =
    def singlePeriodJson(persons: PersonCount, time: LocalTime): String = s"""
      {
          "period": "lunch",
          "persons": $persons,
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
  end tablesAvailableNowJson

  private def tablesAvailableLaterJson(later: LocalDate): String =
    s"""
    {
        "periods": [],
        "next_available_date": "${later.toString}"
    }
    """

  private def noTablesAvailableJson: String = """
    {
        "periods": [],
        "next_available_date": null
    }
    """
