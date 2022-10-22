package fi.jpaju

import fi.jpaju.stalker.StalkerApp
import zio.*

object Main extends ZIOAppDefault:
  val run = StalkerApp.run
