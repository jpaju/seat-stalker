package fi.jpaju
package restaurant

import sttp.client4.*
import sttp.client4.testing.*
import sttp.model.*
import zio.*
import zio.stream.*
import zio.test.*

import java.time.*

object TableOnlineServiceSpec extends ZIOSpecDefault:
  override def spec = suite("TableOnlineServiceSpec")(
    test("make request with correct url and parameters") {
      val recordingBackend = RecordingBackend(
        HttpClientZioBackend.stub.whenAnyRequest
          .thenRespondAdjust(noTablesAvailableJson)
      )

      val parameters = CheckTablesParameters(
        restaurant = Restaurant(RestaurantId("42"), "The restaurant"),
        persons = PersonCount(3),
        startingFrom = LocalDate.parse("2021-01-01")
      )

      val expectedQueryParams: Map[String, String] = Map(
        "persons" -> parameters.persons.toString,
        "date"    -> parameters.startingFrom.toLocalDate.toString
      )

      withTableOnlineService(recordingBackend) { service =>
        for
          _      <- service.checkAvailableTables(parameters).runDrain
          request = recordingBackend.allInteractions.head._1
          uri     = request.uri
        yield assert(uri.host)(equalTo(Some("api.tableonline.com"))) &&
          assert(uri.path)(equalTo(List("restaurant", parameters.restaurant.id, "availability"))) &&
          assert(uri.paramsMap)(equalTo(expectedQueryParams))
      }
    },
    test("when response is empty, then returns empty stream") {
      val sttpBackendStub = HttpClientZioBackend.stub.whenAnyRequest
        .thenRespondAdjust(noTablesAvailableJson)

      withTableOnlineService(sttpBackendStub) { service =>
        for results <- service.checkAvailableTables(defaultParameters).collectToList
        yield assertTrue(results.isEmpty)
      }
    },
    test("when tables available for requested date, then they are returned") {
      val availableTimes = List(
        LocalTime.parse("12:00:00"),
        LocalTime.parse("21:30:00"),
        LocalTime.parse("13:18:00")
      )

      val sttpBackendStub = HttpClientZioBackend.stub
        .whenRequestMatchesPartial({
          case r if queriedDateIs(r.uri, defaultParameters.startingFrom) =>
            ResponseStub.adjust(tablesAvailableJson(defaultParameters.startingFrom.toLocalDate, availableTimes))

          case _ => ResponseStub.adjust(noTablesAvailableJson)
        })

      withTableOnlineService(sttpBackendStub) { service =>
        val date           = defaultParameters.startingFrom.toLocalDate
        val expectedResult = availableTimes.map { time =>
          AvailableTable(time.atDate(date), defaultParameters.persons)
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

      val nextAvailableTimes = List(LocalTime.parse("12:00:00"))

      val sttpBackendStub = HttpClientZioBackend.stub
        .whenRequestMatchesPartial({
          case r if queriedDateIs(r.uri, now) =>
            ResponseStub.adjust(noTablesAvailableJson)

          case r if r.uri.path.endsWith(List("availability", "calendar")) =>
            ResponseStub.adjust(tablesAvailableLaterJson(later))

          case r if queriedDateIs(r.uri, later) =>
            ResponseStub.adjust(tablesAvailableJson(later, nextAvailableTimes))

          case _ => ResponseStub.adjust(noTablesAvailableJson)
        })

      withTableOnlineService(sttpBackendStub) { service =>
        val expectedResult = nextAvailableTimes.map { time =>
          AvailableTable(time.atDate(later), parameters.persons)
        }

        for result <- service.checkAvailableTables(parameters).collectToList
        yield assertTrue(result == expectedResult)
      }
    },
    test("only return received tables from a single day that are after requested time") {
      // Table online only allows to query tables by date, not by time.
      // So we need to filter tables from a single day by time of day manually

      val requestedTime = LocalDateTime.parse("2023-01-03T18:00:00")

      val beforeRequestedTime = LocalDateTime.parse("2023-01-03T15:00:00")
      val afterRequestedTime  = LocalDateTime.parse("2023-01-03T21:00:00")

      val availableTimes = List(
        beforeRequestedTime.toLocalTime,
        requestedTime.toLocalTime,
        afterRequestedTime.toLocalTime
      )

      val sttpBackendStub = HttpClientZioBackend.stub
        .whenRequestMatchesPartial({
          case r if queriedDateIs(r.uri, requestedTime) =>
            ResponseStub.adjust(tablesAvailableJson(requestedTime.toLocalDate, availableTimes))

          case _ => ResponseStub.adjust(noTablesAvailableJson)
        })

      val restaurant = Restaurant(RestaurantId("123"), "Test Restaurant")
      val parameters = CheckTablesParameters(restaurant, PersonCount(2), requestedTime)

      withTableOnlineService(sttpBackendStub) { service =>
        val expectedResult = List(
          AvailableTable(requestedTime, parameters.persons),
          AvailableTable(afterRequestedTime, parameters.persons)
        )

        for result <- service.checkAvailableTables(parameters).collectToList
        yield assertTrue(result == expectedResult)
      }
    }
  )

  // =============================================== Helpers ===============================================

  private def queriedDateIs(uri: Uri, date: LocalDate): Boolean =
    uri.paramsMap.get("date").contains(date.toString)

  private def queriedDateIs(uri: Uri, dateTime: LocalDateTime): Boolean =
    queriedDateIs(uri, dateTime.toLocalDate)

  extension [A](stream: UStream[A])
    def collectToList: ZIO[Any, Throwable, List[A]] =
      stream.runCollect.map(_.toList)

  private def withTableOnlineService[R, E, A](sttpBackend: Backend[Task])(
      f: TableService => ZIO[R & Backend[Task], E, A]
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

  private def tablesAvailableJson(date: LocalDate, times: List[LocalTime]): String =
    times
      .map(time => s"""
        {
          "time": "${date}T$time",
          "tables": [
            {
              "request_only": false,
              "waitlist_only": false
            }
          ]
        }
      """)
      .mkString("[", ",", "]")

  private def tablesAvailableLaterJson(later: LocalDate): String =
    s"""
    [
      {
        "date": "$later",
        "persons": {
          "2": {
            "available_times": 1
          }
        }
      }
    ]
    """

  private val noTablesAvailableJson = "[]"
