
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.39.0"
    }
  }

  backend "azurerm" {
    resource_group_name  = "rg-seat-stalker"
    storage_account_name = "safuncseatstalker"
    container_name       = "tfstate"
    key                  = "terraform.tfstate"
  }
}

provider "azurerm" {
  subscription_id = var.subscription_id
  features {}
}

resource "azurerm_resource_group" "az_resource_group" {
  name     = "rg-${var.project_name}"
  location = "North Europe"
}

resource "azurerm_storage_account" "az_storage_account" {
  name                     = "safuncseatstalker"
  resource_group_name      = azurerm_resource_group.az_resource_group.name
  location                 = azurerm_resource_group.az_resource_group.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"

  allow_nested_items_to_be_public = false
}


# ================================================ Function App & Plan ================================================

resource "azurerm_service_plan" "az_app_service_plan" {
  name                = "ASP-rgseatstalker-bc6b"
  resource_group_name = azurerm_resource_group.az_resource_group.name
  location            = azurerm_resource_group.az_resource_group.location
  os_type             = "Linux"
  sku_name            = "Y1"
}

resource "azurerm_linux_function_app" "az_function_app" {
  name                = "func-${var.project_name}"
  resource_group_name = azurerm_resource_group.az_resource_group.name
  location            = azurerm_resource_group.az_resource_group.location

  storage_account_name       = azurerm_storage_account.az_storage_account.name
  storage_account_access_key = azurerm_storage_account.az_storage_account.primary_access_key
  service_plan_id            = azurerm_service_plan.az_app_service_plan.id

  functions_extension_version = "~4"
  client_certificate_mode     = "Required"

  app_settings = {
    "WEBSITE_MOUNT_ENABLED" = "1"
    "TELEGRAM_CHATID"       = var.telegram_chat_id
    "TELEGRAM_BOTTOKEN"     = var.telegram_bot_token
    "TELEGRAM_SECRETTOKEN"  = var.telegram_secret_token
  }

  https_only              = true
  builtin_logging_enabled = false

  site_config {
    application_insights_connection_string = azurerm_application_insights.az_application_insights.connection_string
    application_insights_key               = azurerm_application_insights.az_application_insights.instrumentation_key

    ftps_state = "Disabled"

    application_stack {
      java_version = "21"
    }
  }

  tags = {
    "hidden-link: /app-insights-conn-string"         = azurerm_application_insights.az_application_insights.connection_string
    "hidden-link: /app-insights-instrumentation-key" = azurerm_application_insights.az_application_insights.instrumentation_key
    "hidden-link: /app-insights-resource-id"         = azurerm_application_insights.az_application_insights.id
  }
}


# =================================================== Log analytics ===================================================

resource "azurerm_log_analytics_workspace" "az_loganalytics" {
  name                = "loganalytics-${var.project_name}"
  location            = azurerm_resource_group.az_resource_group.location
  resource_group_name = azurerm_resource_group.az_resource_group.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

resource "azurerm_application_insights" "az_application_insights" {
  name                = "func-${var.project_name}"
  location            = azurerm_resource_group.az_resource_group.location
  resource_group_name = azurerm_resource_group.az_resource_group.name
  workspace_id        = azurerm_log_analytics_workspace.az_loganalytics.id
  application_type    = "web"

  sampling_percentage                   = 0
  daily_data_cap_in_gb                  = 1
  daily_data_cap_notifications_disabled = false
  internet_ingestion_enabled            = false
}


# ================================================ Email notifications ================================================

resource "azurerm_monitor_action_group" "az_monitor_action_group" {
  name                = "ag-func-failure"
  resource_group_name = azurerm_resource_group.az_resource_group.name
  short_name          = "func-failure"

  email_receiver {
    email_address           = var.email_alert_recipient
    name                    = "Email_-EmailAction-"
    use_common_alert_schema = true
  }
}

resource "azurerm_consumption_budget_resource_group" "az_consumption_budget_resource_group" {
  name              = "budget-${var.project_name}"
  resource_group_id = azurerm_resource_group.az_resource_group.id

  amount     = 10
  time_grain = "Monthly"

  time_period {
    start_date = "2022-10-01T00:00:00Z"
    end_date   = "2030-10-01T00:00:00Z"
  }

  notification {
    contact_emails = [var.email_alert_recipient]
    enabled        = true
    operator       = "GreaterThan"
    threshold      = 50
    threshold_type = "Forecasted"
  }

  notification {
    contact_emails = [var.email_alert_recipient]
    enabled        = true
    operator       = "GreaterThan"
    threshold      = 100
    threshold_type = "Actual"
  }
}

resource "azurerm_monitor_metric_alert" "az_monitor_metric_alert" {
  name                = "Timer function failed"
  description         = "Action will be triggered when timer function fails"
  resource_group_name = azurerm_resource_group.az_resource_group.name
  scopes              = [azurerm_application_insights.az_application_insights.id]
  frequency           = "PT1H"
  window_size         = "PT1H"

  criteria {
    metric_namespace = "Azure.ApplicationInsights"
    metric_name      = "SeatStalkerTimerFunction Failures"
    aggregation      = "Total"
    operator         = "GreaterThanOrEqual"
    threshold        = 1
  }

  action {
    action_group_id = azurerm_monitor_action_group.az_monitor_action_group.id
  }
}
