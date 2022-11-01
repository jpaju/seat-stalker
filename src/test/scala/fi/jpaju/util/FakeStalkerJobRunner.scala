package fi.jpaju
package util

import fi.jpaju.stalker.*
import zio.*

/** Fake runner to be used in tests. Allows to specify the result of each job.
  *
  * @param jobResults
  *   Map of job definitions to ZIO-results. If a job is not found in the map, defect is reported
  */
case class FakeStalkerJobRunner(jobResults: Ref[Map[StalkerJobDefinition, UIO[Unit]]]) extends StalkerJobRunner:
  override def runJob(jobDefinition: StalkerJobDefinition): UIO[Unit] =
    jobResults.get.flatMap { all =>
      val foundJobResult = all.get(jobDefinition)
      val ifNotFound     = ZIO.dieMessage(s"No job result configured in FakeStalkerJobRunner for job $jobDefinition")
      foundJobResult.getOrElse(ifNotFound)
    }

object FakeStalkerJobRunner:
  val layer = ZLayer.fromZIO {
    Ref
      .make(Map.empty[StalkerJobDefinition, UIO[Unit]])
      .map(FakeStalkerJobRunner.apply)
  }

  def addResults(results: Map[StalkerJobDefinition, UIO[Unit]]): URIO[FakeStalkerJobRunner, Unit] =
    ZIO.serviceWithZIO[FakeStalkerJobRunner](_.jobResults.update(_ ++ results))
