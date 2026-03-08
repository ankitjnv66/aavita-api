# Aavita API (Java Spring Boot)

IoT Device Management API - converted from C# .NET to Java Spring Boot.

## Requirements

- Java 17+
- Maven 3.8+
- PostgreSQL (existing AavitaDB schema)
- MQTT broker (e.g. Mosquitto on localhost:1883)

## Build

```bash
mvn clean install
```

## Run

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/aavita-api-1.0.0-SNAPSHOT.jar
```

## Configuration

Edit `src/main/resources/application.yml`:

- **Database**: `spring.datasource.*` - PostgreSQL connection
- **JWT**: `jwt.*` - Signing key, issuers, audiences
- **MQTT**: `mqtt.*` - Broker URL, client ID, subscribe topic

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Register new user |
| `/api/auth/login` | POST | Login (returns JWT) |
| `/api/auth/change-password` | POST | Change password (auth required) |
| `/api/auth/reset-password` | POST | Reset password |
| `/api/devices` | GET, POST | List/Create devices |
| `/api/devices/{id}` | GET, PUT, DELETE | Device CRUD |
| `/api/devices/command` | POST | Send command to device via MQTT |
| `/api/sites` | GET, POST | List/Create sites |
| `/api/sites/{id}` | GET, PUT, DELETE | Site CRUD |
| `/api/users` | GET, POST | List/Create users |
| `/api/users/{id}` | GET, PUT, DELETE | User CRUD |
| `/api/device-digital-pins` | CRUD | Digital pin management |
| `/api/device-pwm` | CRUD | PWM pin management |
| `/api/device-commands` | CRUD | Device command records |

## Swagger UI

http://localhost:5000/swagger-ui.html

## Notes

- **Flyway**: Uses `baseline-on-migrate: true` - if DB has existing schema, Flyway will baseline.
- **Password**: Uses BCrypt (different from original C# SHA256). Existing users need password reset.
- **MQTT**: Connects on startup, subscribes to `+/+/sub`, processes device packets.

## local steps fot testing
What You Need Running Every Time You Test
ServiceKeep Running?How to StartPostgreSQL✅ YesUsually auto-starts with your MacMosquitto MQTT✅ Yesbrew services start mosquittoSpring Boot✅ Yesmvn spring-boot:runngrok✅ Yesngrok http 9090

⚠️ The ngrok Problem for Tomorrow
Every time you restart ngrok, you get a new URL. So tomorrow:

Start ngrok → get new URL e.g. https://xyz999.ngrok-free.app
Go to Google Home Developer Console
Update all 3 URLs with the new ngrok URL:

Authorization URL
Token URL
Cloud Fulfillment URL


Click Save
Then test again


💡 Permanent Solution (When Ready for Production)
When you deploy to a real server, you'll have a fixed HTTPS URL and won't need ngrok at all. Then you only update Google Console once permanently.

📋 Tomorrow's Startup Checklist
Save this for tomorrow:
1. mvn spring-boot:run          ← start app
2. ngrok http 9090              ← start tunnel
3. Copy new ngrok URL
4. Update Google Console URLs   ← paste new URL
5. Test in Google Home app

See you tomorrow! Everything is saved in this conversation. Just say "I'm back" and we'll continue! 👋

## below steps
Step 1 → Start PostgreSQL (usually auto-running)
Step 2 → brew services start mosquitto
Step 3 → mvn spring-boot:run
Step 4 → ngrok http 9090  (new URL will appear)
Step 5 → Update Google Console with new ngrok URL
Step 6 → Test!


## Google Developer update
What To Update in Google Console (When URL Changes)
If tomorrow you get a different URL, go to:

👉 console.home.google.com
Open project → Cloud-to-cloud → Develop
Click on Aavita Smart Home → Edit
Update these 3 fields only:

FieldNew ValueAuthorization URLhttps://NEW-URL/oauth/authorizeToken URLhttps://NEW-URL/oauth/tokenCloud Fulfillment URLhttps://NEW-URL/api/google/fulfillment

