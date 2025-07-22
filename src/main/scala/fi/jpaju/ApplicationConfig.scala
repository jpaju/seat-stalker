package fi.jpaju

import fi.jpaju.telegram.*

import zio.*
import zio.config.*
import zio.config.magnolia.*

case class ApplicationConfig(telegram: TelegramConfig)

object ApplicationConfig:
  private val config         = deriveConfig[ApplicationConfig]
  private val configProvider = ConfigProvider.fromEnv()

  val layer = ZLayer { configProvider.load(config) }.project(_.telegram)
