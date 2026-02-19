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
