output "resource_group_name" {
  description = "Name of the Azure resource group"
  value       = azurerm_resource_group.az_resource_group.name
}

output "function_app_name" {
  description = "Name of the Azure Function App"
  value       = azurerm_function_app_flex_consumption.az_function_app.name
}
