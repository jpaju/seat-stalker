# Seat Stalker

Application for notifying about free seats in popular restaurants. Uses telegram to send notifications.

## TODO

- [x] Actual logic to check for seats and send notifications accordingly
- [x] Format sent telegram messages nicely
- [x] CI Pipeline to deploy as scheduled Azure Function
- [ ] Terraform to manage infrastructure
- [ ] Combine multiple free seats together in one message
- [ ] Persist sent notifications about seats so notification about the same seat wont be sent multiple times
- [ ] Support adding new alerts via Telegram bot API?

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
TELEGRAM_TOKEN=<your-bot-token>
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
