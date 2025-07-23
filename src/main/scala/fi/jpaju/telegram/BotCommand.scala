package fi.jpaju.telegram

enum BotCommand:
  case Echo(message: String)
  case ListJobs

object BotCommand:
  def fromText(text: String): Option[BotCommand] =
    text match
      case s"/echo$msg" => Some(BotCommand.Echo(msg.trim))
      case "/jobs"      => Some(BotCommand.ListJobs)
      case _            => None
