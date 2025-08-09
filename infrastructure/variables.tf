variable "telegram_chat_id" {
  type      = string
  sensitive = true
}

variable "telegram_bot_token" {
  type      = string
  sensitive = true
}

variable "telegram_secret_token" {
  type      = string
  sensitive = true
}

variable "email_alert_recipient" {
  type      = string
  sensitive = true
}
