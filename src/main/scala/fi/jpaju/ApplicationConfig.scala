package fi.jpaju

import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.syntax.*

case class ApplicationConfig(telegram: TelegramConfig)

object ApplicationConfig:
  private val keyDelimiter     = Option('_')
  private val configDescriptor = descriptor[ApplicationConfig]
  private val configSource     =
    ConfigSource.fromSystemEnv(keyDelimiter = keyDelimiter) <> ConfigSource.fromSystemProps(keyDelimiter = keyDelimiter)

  private val configLayer = ZLayer { read(configDescriptor.from(configSource)) }
  val layer               = configLayer.narrow(_.telegram)
