package fi.jpaju.azurefunction

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
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
    ZIOAzureFunctionAdapter.runOrThrowError(context) {
      for
        _       <- ZIO.log(s"Telegram webhook called")
        _       <- ZIO.log(s"Request URI: ${request.getUri}")
        _       <- ZIO.log(s"Request query params: ${request.getQueryParameters}")
        _       <- ZIO.log(s"Request headers: ${request.getHeaders}")
        _       <- ZIO.log(s"Request body: ${request.getBody}")
        response = request.createResponseBuilder(HttpStatus.OK).build()
      yield response

    }
