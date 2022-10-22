package fi.jpaju.azurefunction

import zio.*

import java.util.logging.Logger

object Logging:
  def azFunLoggerLayer(logger: Logger) =
    val zLogger = azLoggerToZIO(logger)
    Runtime.removeDefaultLoggers ++ Runtime.addLogger(zLogger)

  def azLoggerToZIO(azLogger: Logger): ZLogger[String, Unit] =
    def getLogFn(level: LogLevel): String => Unit =
      level match
        case LogLevel.Trace   => azLogger.finest
        case LogLevel.Debug   => azLogger.finer
        case LogLevel.Info    => azLogger.info
        case LogLevel.Warning => azLogger.warning
        case LogLevel.Error   => azLogger.severe
        case LogLevel.Fatal   => azLogger.severe
        case _                => _ => ()

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
