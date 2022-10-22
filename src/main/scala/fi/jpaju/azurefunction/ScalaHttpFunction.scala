package fi.jpaju.azurefunction

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.TimerTrigger
import zio.*

import java.util.Optional

class ScalaHttpFunction:
  @FunctionName("ScalaHttpFunction")
  def run(
      @HttpTrigger(
        name = "req",
        methods = Array(HttpMethod.GET, HttpMethod.POST),
        authLevel = AuthorizationLevel.ANONYMOUS
      ) request: HttpRequestMessage[Optional[String]],
      context: ExecutionContext
  ): HttpResponseMessage =
    val runtime = Runtime.default
    val logIt   = ZIO.attempt(context.getLogger.info("Scala HTTP trigger processed a request."))
    val program = logIt.repeatN(4)
    Unsafe.unsafe(unsafe ?=> runtime.unsafe.run(program))

    request.createResponseBuilder(HttpStatus.OK).body("This is written in Scala. Hello, ").build
