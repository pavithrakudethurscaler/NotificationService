# Notification Service

Customer notifications for the ecom platform (HLD 3.6). Port **8086**.
Receives events from the User, Order and Payment Services, stores each notification, and emails it.

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/internal/api/v1/events` | `X-Internal-Token` | Event intake: `{"eventId", "type", "occurredAt", "data": {...}}` |
| GET | `/api/v1/notifications` | Bearer | My notifications, newest first |

Event types: `USER_REGISTERED`, `PASSWORD_RESET_REQUESTED`, `ORDER_CONFIRMED`, `ORDER_SHIPPED`, `ORDER_DELIVERED`,
`ORDER_CANCELLED`, `PAYMENT_RECEIVED`, `PAYMENT_REFUND_PENDING`. A repeated `eventId` is ignored.
Failed deliveries are retried every minute, up to 5 attempts.

Events arrive over REST today; the envelope matches the planned Kafka message format (HLD 3.6).

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `INTERNAL_API_TOKEN` | – | Shared secret for event intake and user lookups; must match the other services |
| `USERSERVICE_URL` | `http://localhost:8082` | Token validation and email lookup |
| `EMAIL_SENDER` | `log` | `log` (development: emails are written to the log) or `smtp` |
| `EMAIL_FROM` | `no-reply@ecom.local` | Sender address |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | `localhost`, `587` | SMTP server, e.g. Amazon SES: `email-smtp.<region>.amazonaws.com` |
| `SMTP_AUTH`, `SMTP_STARTTLS` | `true`, `true` | Turn off for a local test mail server without login/TLS (e.g. MailHog) |

The `log` sender writes full emails, including password reset links, to the log. Use `smtp` outside development.

## Run

```bash
./gradlew build
java -jar build/libs/notificationservice-0.0.1-SNAPSHOT.jar
```
