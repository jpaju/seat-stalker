# Seat Stalker

Application for notifying about free seats in popular restaurants. Uses telegram to send notifications.

## TODO

- [x] Actual logic to check for seats and send notifications accordingly
- [x] Format sent telegram messages nicely
- [x] CI Pipeline to deploy as scheduled Azure Function
- [x] Terraform to manage infrastructure
- [ ] Support multiple chat ids: in webhook use chat id from request and in timer, store chat id where to send the results to
- [ ] Add persistence
  - [ ] Store sent notifications so there are not duplicate notification about the same seat
  - [Neon](https://neon.com/)
  - [Supabase](https://supabase.com/)
  - [CockroachDB serverless](https://www.cockroachlabs.com/lp/serverless/)
  - [Azure SQL](https://azure.microsoft.com/en-us/products/azure-sql/database)
- [ ] Telegram bot webhook
  - [x] Check `secret_token` from request header
  - [ ] Support adding new restaurants dynamically
  - [ ] Autocomplete for bot commands
  - [ ] Support `inline_query` to enable interacting with the bot from any chat/conversation
  - [ ] Register webhook automatically using `setWebhook` during deployment/startup?
- [ ] Combine multiple free seats together in one message

### New integrations

- [ ] [Dinnerbooking.com](https://www.dinnerbooking.com/)

## Development Environment

### Prerequisites

- Scala/SBT for building
- Docker for local blob storage
- Telegram bot token and chat ID

### Configuration

Create a `.env` file in the project root with your Telegram credentials:

```
TELEGRAM_CHATID=<your-chat-id>
TELEGRAM_BOTTOKEN=<your-bot-token>
TELEGRAM_SECRETTOKEN=<your-secret-token>
```

The `.env` file should be loaded into your environment. If using nix-direnv or dotenv, it will be loaded automatically.

### Running the Application

#### Option 1: Direct with SBT

1. Ensure environment variables from `.env` file are loaded before starting SBT.
2. Run `sbt run`

#### Option 2: Azure Functions (Local)

1. Ensure [Azure Functions Core Tools (v4)](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local) are available (automatically included in nix dev shell)
2. Start blob storage: `docker compose up`
3. Build the application: `sbt assembly`
4. Navigate to `azure-functions` folder
5. Start the function: `func start`

## Infrastructure Setup

The application infrastructure is managed with Terraform and applied as part of deployment process in GitHub Actions.

### Prerequisites

1. **Azure setup**
   - Create an Azure subscription
   - Create a service principal for GitHub Actions:
     ```bash
     az ad sp create-for-rbac --name "seat-stalker-github" --role contributor --scopes /subscriptions/{subscription-id} --json-auth
     ```
   - Create storage account for Terraform state:
     ```bash
     az group create --name <resource-group> --location <location>
     az storage account create --name <storage-account> --resource-group <resource-group> --location <location> --sku Standard_LRS
     az storage container create --name tfstate --account-name <storage-account>
     ```
2. **GitHub secrets**
   Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):
   - `AZURE_CREDENTIALS` - JSON output from service principal creation
     - Required properties: `clientId`,` clientSecret`,` subscriptionId`,` tenantId`
   - `TELEGRAM_CHAT_ID` - Your Telegram chat ID
   - `TELEGRAM_BOT_TOKEN` - Your Telegram bot token
   - `TELEGRAM_SECRET_TOKEN` - Your Telegram secret token
   - `EMAIL_ALERT_RECIPIENT` - Email for Azure alerts
