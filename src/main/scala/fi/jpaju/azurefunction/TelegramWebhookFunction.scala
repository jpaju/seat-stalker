package fi.jpaju.azurefunction

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import fi.jpaju.*
import zio.*

class TelegramWebhookFunction:
  @FunctionName("TelegramWebhookFunction")
  def run(
      @HttpTrigger(
        name = "req",
        methods = Array(HttpMethod.POST),
        authLevel = AuthorizationLevel.ANONYMOUS,
        route = "telegram/webhook"
      ) request: HttpRequestMessage[String],
      context: ExecutionContext
  ): HttpResponseMessage =
    val program = for
      _       <- ZIO.log(s"Telegram webhook called")
      _       <- ZIO.log(s"Request URI: ${request.getUri}")
      _       <- ZIO.log(s"Request query params: ${request.getQueryParameters}")
      _       <- ZIO.log(s"Request headers: ${request.getHeaders}")
      _       <- ZIO.log(s"Request body: ${request.getBody}")
      response = request.createResponseBuilder(HttpStatus.OK).build()
    yield response

    val loggerLayer = Logging.azFunctionLoggerLayer(context.getLogger)
    ZIOAppRunner.runThrowOnError(program.provide(loggerLayer))
