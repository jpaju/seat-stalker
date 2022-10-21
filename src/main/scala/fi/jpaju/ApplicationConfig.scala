package fi.jpaju

import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.syntax.*

case class ApplicationConfig(telegram: TelegramConfig)

object ApplicationConfig:
  private val keyDelimiter     = Option('_')
  private val configDescriptor = descriptor[ApplicationConfig]

  private val configSource    = ConfigSource.fromSystemEnv(keyDelimiter = keyDelimiter)
  private val lowerCaseSource = configSource.mapKeys(key => key.toLowerCase) // To combat some Azure madness
  private val combinedSource  = configSource <> lowerCaseSource

  private val configLayer = ZLayer { read(configDescriptor.from(combinedSource)) }
  val layer               = configLayer.narrow(_.telegram)
