package fi.jpaju
package stalker

import fi.jpaju.restaurant.*
import fi.jpaju.util.*
import zio.*
import zio.test.*

object StalkerAppSpec extends ZIOSpecDefault:

  val job1    = StalkerJobDefinition(Restaurant(RestaurantId("1"), "Test1"), PersonCount(1))
  val job2    = StalkerJobDefinition(Restaurant(RestaurantId("2"), "Test2"), PersonCount(2))
  val job3    = StalkerJobDefinition(Restaurant(RestaurantId("3"), "Test3"), PersonCount(3))
  val allJobs = Set(job1, job2, job3)

  override def spec = suite("StalkerAppSpec")(
    test("when all jobs run successfully, then app should exit with success") {
      val jobResults = Map(
        job1 -> ZIO.unit,
        job2 -> ZIO.unit,
        job3 -> ZIO.unit
      )

      FakeStalkerJobRunner.addResults(jobResults) *> assertStalkerAppExit(succeeds(anything))
    },
    test("when any job fails, then app should exit non-successfully") {
      val jobResults = Map(
        job1 -> ZIO.unit,
        job2 -> ZIO.unit,
        job3 -> ZIO.dieMessage("Job 3 failed")
      )

      FakeStalkerJobRunner.addResults(jobResults) *> assertStalkerAppExit(dies(anything) || fails(anything))
    },
    test("when any job fails, then others should be run to completion") {
      for
        successCounter <- Ref.make(0)
        addSuccess      = successCounter.update(_ + 1)
        jobResults      = Map(
                            job1 -> ZIO.sleep(1.second) *> addSuccess,
                            job2 -> ZIO.sleep(5.second) *> addSuccess,
                            job3 -> ZIO.dieMessage("Job 3 failed") *> addSuccess
                          )
        _              <- FakeStalkerJobRunner.addResults(jobResults)
        _              <- ZIO.serviceWithZIO[StalkerApp](_.run.cause) <& TestClock.adjust(10.seconds)
        successCount   <- successCounter.get
      yield assertTrue(successCount == 2)
    }
  ).provide(
    LiveStalkerApp.layer,
    FakeStalkerJobRunner.layer,
    InMemoryStalkerJobRepository.layerFromJobs(allJobs),
    Runtime.removeDefaultLoggers
  )

  def assertStalkerAppExit(assertion: Assertion[Exit[?, ?]]) =
    ZIO
      .serviceWithZIO[StalkerApp](_.run.exit)
      .map(assert(_)(assertion))
