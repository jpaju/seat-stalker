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

## Development environment

The application could be run locally either using SBT or Azure Functions Core Tools. Both approaches require Telegram configuration in order to send notifications.

### SBT

1. Create `.env` file to the root folder of the project and add the following configuration in it
   ```
   telegram_chatId=<chat-id>
   telegram_token=<token>
   ```
2. Start SBT and run `run` command

### Azure Functions

1. Make sure [Azure Functions Core Tools (v4)](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local) are installed
2. Start local blob storage emulator by running `docker compose up`
3. Package the project into a jar file by running `sbt assembly`
4. Navigate to `azure-functions` folder
5. Create configuration file `local.settings.json` and add the following configuration:

   ```json
   {
     "IsEncrypted": false,
     "Values": {
       "AzureWebJobsStorage": "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;",
       "FUNCTIONS_WORKER_RUNTIME": "java",
       "telegram_chatId": "<chat-id>",
       "telegram_token": "<token>"
     }
   }
   ```

6. Run `func start`
