package fi.jpaju.azurefunction

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import fi.jpaju.stalker.*
import fi.jpaju.telegram.*
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
    val okResponse  = request.createResponseBuilder(HttpStatus.OK).build()

    val program =
      for
        _ <- ZIO.log(s"Webhook called, request body: $requestBody")
        _ <- ZIO.serviceWithZIO[BotController](_.handleWebhook(requestBody))
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
