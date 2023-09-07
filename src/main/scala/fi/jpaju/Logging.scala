package fi.jpaju

import zio.*
import zio.logging.LogFilter
import zio.logging.LogFormat
import zio.logging.LogGroup

import java.util.Locale
import java.util.logging.Logger

object Logging:
  def formatLogSpanName(name: String): String =
    name
      .toLowerCase(Locale.ENGLISH)
      .replaceAll("[\\s\\t\\n\\r]+", "_") // Replace whitespace with underscore

  def azFunctionLoggerLayer(logger: Logger) =
    val javaZLogger          = javaLoggerToZLogger(logger)
    val loggerWithFormatting = LogFormatter.applyFormat(javaZLogger)
    Runtime.removeDefaultLoggers ++ Runtime.addLogger(loggerWithFormatting)

  private def javaLoggerToZLogger(javaLogger: Logger): ZLogger[String, Unit] = (
      trace: Trace,
      fiberId: FiberId,
      logLevel: LogLevel,
      message: () => Any,
      cause: Cause[Any],
      context: FiberRefs,
      spans: List[LogSpan],
      annotations: Map[String, String]
  ) =>
    val msg = message().toString
    logLevel match
      case LogLevel.All     => javaLogger.finest(msg)
      case LogLevel.Trace   => javaLogger.finer(msg)
      case LogLevel.Debug   => javaLogger.fine(msg)
      case LogLevel.Info    => javaLogger.info(msg)
      case LogLevel.Warning => javaLogger.warning(msg)
      case LogLevel.Error   => javaLogger.severe(msg)
      case LogLevel.Fatal   => javaLogger.severe(msg)
      case _                => ()

  private object LogFormatter:
    def applyFormat[A](logger: ZLogger[String, A]): ZLogger[String, A] =
      // Used to format message if spans or annotations are empty
      val annotationFilter = LogFilter(LogGroup((_, _, _, _, _, _, _, annotations) => annotations), _.nonEmpty)
      val spanFilter       = LogFilter(LogGroup((_, _, _, _, _, _, spans, _) => spans), _.nonEmpty)

      val spans              = (LogFormat.space + LogFormat.bracketed(LogFormat.spans)).filter(spanFilter)
      val annotations        = (LogFormat.space + LogFormat.bracketed(LogFormat.allAnnotations)).filter(annotationFilter)
      val contextInformation = LogFormat.bracketed(LogFormat.enclosingClass) |-| LogFormat.bracketed(LogFormat.fiberId)

      val logFormat = contextInformation + spans + annotations |-| LogFormat.line
      logFormat.toLogger >>> logger

    extension [AIn, AOut, BOut](self: ZLogger[AIn, AOut])
      /** Compose two loggers one after another. The message produced by the first one logger is passed to the second
        * logger. All other parameters are passed unchanged to the second logger.
        */
      def >>>(that: ZLogger[AOut, BOut]): ZLogger[AIn, BOut] = (
          trace: Trace,
          fiberId: FiberId,
          logLevel: LogLevel,
          message: () => AIn,
          cause: Cause[Any],
          context: FiberRefs,
          spans: List[LogSpan],
          annotations: Map[String, String]
      ) =>
        val aOut = () => self(trace, fiberId, logLevel, message, cause, context, spans, annotations)
        that(trace, fiberId, logLevel, aOut, cause, context, spans, annotations)
