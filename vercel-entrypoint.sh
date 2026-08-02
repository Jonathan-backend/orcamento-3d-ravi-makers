#!/bin/sh
set -eu

/opt/java/openjdk/bin/java \
  -XX:TieredStopAtLevel=1 \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError \
  -jar /app/app.jar \
  --server.port=8081 &

exec /usr/sbin/nginx -c /app/vercel-nginx.conf -g 'daemon off;'
