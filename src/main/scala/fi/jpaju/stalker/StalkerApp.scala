package fi.jpaju
package stalker

import fi.jpaju.restaurant.*
import zio.*

case class RunSuccesses(jobCount: Int, duration: Duration)
case class RunErrors(failures: Set[Cause[Nothing]])

trait StalkerApp:
  def run: IO[RunErrors, RunSuccesses]

case class LiveStalkerApp(
    runner: StalkerJobRunner,
    repository: StalkerJobRepository
) extends StalkerApp:
  def run: IO[RunErrors, RunSuccesses] =
    for
      _      <- ZIO.log("Seat stalker started")
      jobs   <- repository.getAll
      result <- executeJobs(jobs)
      _      <- ZIO.log("Seat stalker finished")
    yield result

  def executeJobs(jobs: Set[StalkerJobDefinition]): IO[RunErrors, RunSuccesses] =
    val executeInParallel = ZIO.foreachPar(jobs) { job => runner.runJob(job).cause }
    val results           = gatherResults(executeInParallel)

    results.tapBoth(
      errors => ZIO.log(s"Some jobs failed: ${errors.failures}"),
      stats => ZIO.log(s"All jobs succeeded, took ${stats.duration.toMillis}ms to run ${stats.jobCount} jobs")
    )

  def gatherResults(runJobs: UIO[Set[Cause[Nothing]]]): IO[RunErrors, RunSuccesses] =
    runJobs.timed.flatMap { (duration, jobResults) =>
      val (successes, failures) = jobResults.partition(_.isEmpty)

      if failures.isEmpty then ZIO.succeed(RunSuccesses(successes.size, duration))
      else ZIO.fail(RunErrors(failures))
    }

object LiveStalkerApp:
  val layer = ZLayer.fromFunction(LiveStalkerApp.apply)

object StalkerApp:
  // Hard code desired reastaurants for now
  val kaskis    = Restaurant(RestaurantId("291"), "Kaskis")
  val metsämäki = Restaurant(RestaurantId("1286"), "Ravintola Metsämäki")

  val jobs = Set(
    StalkerJobDefinition(kaskis, PersonCount(2)),
    StalkerJobDefinition(metsämäki, PersonCount(2))
  )

  val hardcodedJobsRepositoryLayer = InMemoryStalkerJobRepository.layerFromJobs(jobs)
