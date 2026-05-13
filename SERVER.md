## Server Access & Management

### SSH into server
ssh root@64.227.160.209
(enter root password when prompted)

### Check app status
systemctl status aavita-api

### Start the app
systemctl start aavita-api

### Stop the app
systemctl stop aavita-api

### Restart the app (after deploying new JAR)
systemctl restart aavita-api

### View live logs
tail -f /var/log/syslog | grep aavita

### Deploy new JAR (run on Mac)
mvn clean package -DskipTests
scp target/aavita-api-1.0.0-SNAPSHOT.jar root@64.227.160.209:/root/
ssh root@64.227.160.209
systemctl restart aavita-api

### Swagger URL
http://64.227.160.209:9090/swagger-ui.html