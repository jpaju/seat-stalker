package fi.jpaju
package stalker

import zio.*

trait StalkerJobRepository:
  def saveJob(job: StalkerJobDefinition): UIO[Unit]
  def getAll: UIO[Set[StalkerJobDefinition]]

case class InMemoryStalkerJobRepository(jobs: Ref[Set[StalkerJobDefinition]]) extends StalkerJobRepository:
  def saveJob(job: StalkerJobDefinition): UIO[Unit] =
    jobs.update(_ + job)

  def getAll: UIO[Set[StalkerJobDefinition]] =
    jobs.get

object InMemoryStalkerJobRepository:
  def layerFromJobs(jobs: Set[StalkerJobDefinition]) =
    ZLayer.fromZIO {
      Ref.make(jobs).map(InMemoryStalkerJobRepository.apply)
    }
