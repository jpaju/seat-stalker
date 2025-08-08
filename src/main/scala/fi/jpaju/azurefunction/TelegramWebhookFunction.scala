package fi.jpaju.azurefunction

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*

import fi.jpaju.stalker.*
import fi.jpaju.telegram.*
import scala.jdk.CollectionConverters.*
import sttp.client3.httpclient.zio.*
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
    val requestBody = request.getBody
    val headers     = request.getHeaders.asScala.toMap
    val okResponse  = request.createResponseBuilder(HttpStatus.OK).build()

    val program =
      for
        controller <- ZIO.service[BotController]
        _          <- ZIO.log(s"Request body: $requestBody")
        _          <- ZIO.log(s"Headers: $headers")
        _          <- controller.handleWebhook(requestBody, headers).catchAll(handleError(_))
      yield okResponse

    ZIOAzureFunctionAdapter.runOrThrowError(context) {
      program.provide(
        LiveBotController.layer,
        LiveBotCommandHandler.layer,
        StalkerApp.hardcodedJobsRepositoryLayer,
        LiveTelegramClient.layer,
        HttpClientZioBackend.layer()
      )

    }

  private def handleError(error: BotError) = error match
    case BotError.AuthenticationError(message) =>
      ZIO.logWarning(s"Authentication failed: $message")

    case BotError.ParseError(message) =>
      ZIO.logWarning(s"Parse error: $message")

    case BotError.CommandError(message) =>
      ZIO.logWarning(s"Command error: $message")

    case BotError.DeliveryError(error) =>
      ZIO.logWarning(s"Delivery error: $error")
