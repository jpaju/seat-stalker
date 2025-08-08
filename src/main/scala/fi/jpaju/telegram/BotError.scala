package fi.jpaju.telegram

enum BotError:
  case ParseError(message: String)
  case CommandError(message: String)
  case DeliveryError(error: MessageDeliveryError)
  case AuthenticationError(message: String)
