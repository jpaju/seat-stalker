# Seat Stalker

## TODO
- [x] Actual logic to check for seats and send notifications accordingly
- [x] Format sent telegram messages nicely
- [ ] CI Pipeline to delpoy as scheduled Azure Function
- [ ] Persist sent notications about seats so notification about the same seat wont be sent multiple times
- [ ] Support adding new alerts via Telegram bot API?

## Development enviornment
* Create `.env` file to the root folder of the project and add the following configuration in it
	```
	telegram_chatId=<chat-id>
	telegram_token=<token>
	```
