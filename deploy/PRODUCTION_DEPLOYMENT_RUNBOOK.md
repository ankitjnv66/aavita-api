# Aavita Production Deployment Runbook

This runbook is designed for a single Ubuntu 22.04 Droplet:
- Spring Boot API
- PostgreSQL
- Mosquitto
- Nginx + Let's Encrypt

## 1) Prerequisites

- Domain is purchased and ready (example: `api.aavita.in`)
- DigitalOcean Droplet created (minimum 2 vCPU / 2 GB RAM)
- DNS A record points to Droplet public IP
- SSH access available

## 2) Build artifact locally

From project root:

```bash
mvn clean package -DskipTests
```

Jar expected at:
- `target/aavita-api-1.0.0-SNAPSHOT.jar`

## 3) Base server setup

On Ubuntu server:

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless postgresql postgresql-contrib mosquitto nginx certbot python3-certbot-nginx
sudo adduser --system --group --home /opt/aavita aavita
sudo mkdir -p /opt/aavita/app /opt/aavita/certs /etc/aavita
sudo chown -R aavita:aavita /opt/aavita
```

## 4) Copy files to server

From local machine:

```bash
scp target/aavita-api-1.0.0-SNAPSHOT.jar root@<SERVER_IP>:/opt/aavita/app/
scp deploy/production.env.example root@<SERVER_IP>:/etc/aavita/aavita-api.env
scp deploy/aavita-api.service root@<SERVER_IP>:/etc/systemd/system/aavita-api.service
scp deploy/nginx-aavita.conf root@<SERVER_IP>:/etc/nginx/sites-available/aavita
```

Then edit environment file on server:

```bash
sudo nano /etc/aavita/aavita-api.env
```

Replace all `CHANGE_ME` values before first start.

## 5) Nginx + SSL

On server:

```bash
sudo ln -sf /etc/nginx/sites-available/aavita /etc/nginx/sites-enabled/aavita
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d api.aavita.in
```

## 6) Start API as systemd service

On server:

```bash
sudo systemctl daemon-reload
sudo systemctl enable aavita-api
sudo systemctl restart aavita-api
sudo systemctl status aavita-api
```

Useful logs:

```bash
journalctl -u aavita-api -f
```

## 7) Post-deploy integrations

Update external endpoints to `https://api.aavita.in`:

- Google Home Console
  - Authorization URL: `https://api.aavita.in/oauth/authorize`
  - Token URL: `https://api.aavita.in/oauth/token`
  - Fulfillment URL: `https://api.aavita.in/api/google/fulfillment`
- AWS Lambda env var
  - `TARGET_URL=https://api.aavita.in`
- Alexa console endpoint
  - `https://api.aavita.in/api/alexa/smarthome` (if this is your route)

## 8) Smoke test checklist

- `curl https://api.aavita.in/v3/api-docs` returns JSON
- OAuth authorize/token flow works
- Google Home SYNC works
- Voice EXECUTE triggers MQTT publish
- Alexa discovery/control succeeds
- DB writes visible in `device_commands` and pin state tables

## 9) Manual update procedure (no CI/CD)

```bash
# local
mvn clean package -DskipTests
scp target/aavita-api-1.0.0-SNAPSHOT.jar root@<SERVER_IP>:/opt/aavita/app/

# server
sudo systemctl restart aavita-api
sudo systemctl status aavita-api
```
