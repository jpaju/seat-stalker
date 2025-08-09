variable "subscription_id" {
  type        = string
  description = "Azure subscription ID"
  default     = "7ef2b999-13ea-4839-a556-6ba8043da44f"
}

variable "project_name" {
  type        = string
  default     = "seat-stalker"
}

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
