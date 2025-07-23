package fi.jpaju.telegram

import io.circe.parser.decode
import telegramium.bots
import telegramium.bots.CirceImplicits.given

enum BotCommand:
  case Echo(message: String)
  case ListJobs

object BotCommand:
  def fromText(text: String): Option[BotCommand] =
    text match
      case s"/echo$msg" => Some(BotCommand.Echo(msg.trim))
      case "/jobs"      => Some(BotCommand.ListJobs)
      case _            => None

  def fromUpdateJson(json: String) =
    for
      update  <- decode[bots.Update](json).toOption
      message <- update.message
      text    <- message.text
      command <- BotCommand.fromText(text)
    yield command
