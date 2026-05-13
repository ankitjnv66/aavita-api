# Aavita IoT — Next Session Context

> Paste this at the start of the next conversation to resume instantly.

---

## What's Done ✅
- Backend 100% complete — Spring Boot, MQTT, Google Home, Alexa
- Google Home — working end to end
- Alexa — backend + Lambda proxy ready, blocked locally due to amazon.com/.in account mismatch (will resolve in prod)
- AWS Lambda — `arn:aws:lambda:us-east-1:563332534769:function:Smart-Home` (us-east-1)
- Alexa Skill ID — `amzn1.ask.skill.b2600305-1233-444c-80f2-9b2497348c39`
- Flutter app — handled by separate developer
- Stakeholder proposal shared — awaiting approval
- DigitalOcean Droplet live — Ubuntu 22.04, 2GB RAM, Bangalore
- Spring Boot deployed as systemd service — auto starts on reboot
- Swagger live at http://64.227.160.209:9090/swagger-ui.html
- Flyway migration ordering bug fixed
- BCryptPasswordEncoder injection bug fixed

---

## 🔴 NEXT UP — Production Domain Setup

**Waiting on:**
1. Stakeholder approval
2. Domain name (e.g. `api.aavita.in`)

**Plan once domain is ready:**
1. Point domain DNS → 64.227.160.209
2. Install SSL via Let's Encrypt (Nginx + Certbot)
3. Update Google Actions Console + Lambda `TARGET_URL` + Alexa Console → new domain
4. Create fresh amazon.com account → re-enable Alexa skill → test discovery
5. End-to-end voice + API testing

---

## Server Details
- IP: 64.227.160.209
- OS: Ubuntu 22.04, 2GB RAM, Bangalore (BLR1)
- App port: 9090
- DB: PostgreSQL — AavitaDB

## Key Credentials (for reference)
- Test user: `test@aavita.com` / `Aavita@123`
- Alexa shared secret: `Aavita@Alexa#2026`
- Google/Alexa OAuth: client-id=`smarthome-client-001`, secret=`Aavita@Smart#2026`
- Lambda env vars: `TARGET_URL` + `ALEXA_SECRET` (update to prod domain after deployment)