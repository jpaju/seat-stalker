package fi.jpaju.azurefunction

import zio.*

import java.util.logging.Logger

object Logging:
  def azFunLoggerLayer(logger: Logger) =
    val zLogger = azLoggerToZIO(logger)
    Runtime.removeDefaultLoggers ++ Runtime.addLogger(zLogger)

  def azLoggerToZIO(azLogger: Logger): ZLogger[String, Unit] =
    def getLogFn(level: LogLevel): String => Unit =
      import LogLevel.*
      level match
        case All     => azLogger.finest
        case Trace   => azLogger.finer
        case Debug   => azLogger.fine
        case Info    => azLogger.info
        case Warning => azLogger.warning
        case Error   => azLogger.severe
        case Fatal   => azLogger.severe
        case _       => _ => ()

    new ZLogger[Any, Unit]:
      override def apply(
          trace: Trace,
          fiberId: FiberId,
          logLevel: LogLevel,
          message: () => Any,
          cause: Cause[Any],
          context: FiberRefs,
          spans: List[LogSpan],
          annotations: Map[String, String]
      ): Unit =
        val logFn = getLogFn(logLevel)
        logFn(message().toString)
