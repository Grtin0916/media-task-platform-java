#!/usr/bin/env bash
set -u

mkdir -p artifacts/logs artifacts/runtime

TS="$(date +%Y%m%d_%H%M%S)"
PORT="${PORT:-18080}"
MVN="./mvnw"
[ -x "$MVN" ] || MVN="mvn"

APP_LOG="artifacts/logs/week12_asset_blueprint_http_runtime_app_v2_${TS}.log"
PROBE_LOG="artifacts/logs/week12_asset_blueprint_http_runtime_probe_v2_${TS}.log"
SUMMARY="artifacts/logs/week12_asset_blueprint_http_runtime_smoke_summary.json"
BODY="artifacts/runtime/week12_asset_blueprint_http_runtime_body_v2_${TS}.json"
HEALTH_BODY="artifacts/runtime/week12_health_v2_${TS}.json"
PIDFILE="artifacts/runtime/week12_asset_blueprint_http_runtime_v2_${TS}.pid"

cleanup() {
  if [ -f "$PIDFILE" ]; then
    PID="$(cat "$PIDFILE" 2>/dev/null || true)"
    if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
      kill "$PID" 2>/dev/null || true
      sleep 2
      kill -9 "$PID" 2>/dev/null || true
    fi
  fi
}
trap cleanup EXIT

echo "[1/4] start Spring Boot runtime for asset-blueprint smoke v2"
(
  "$MVN" -q spring-boot:run \
    -Dspring-boot.run.profiles=k6-smoke \
    -Dspring-boot.run.useTestClasspath=true \
    -Dspring-boot.run.arguments="--server.port=${PORT} --management.health.redis.enabled=false"
) >"$APP_LOG" 2>&1 &

APP_PID=$!
echo "$APP_PID" > "$PIDFILE"
echo "APP_PID=$APP_PID"
echo "APP_LOG=$APP_LOG"

echo
echo "[2/4] wait for HTTP server port, health is diagnostic only"
HEALTH_CODE="000"
SERVER_READY="false"

for i in $(seq 1 50); do
  HEALTH_CODE="$(curl -sS -o "$HEALTH_BODY" -w "%{http_code}" "http://127.0.0.1:${PORT}/actuator/health" || true)"
  echo "try=$i healthCode=$HEALTH_CODE"
  if [ "$HEALTH_CODE" = "200" ] || [ "$HEALTH_CODE" = "503" ]; then
    SERVER_READY="true"
    break
  fi
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "[FAIL] app process exited before probe"
    break
  fi
  sleep 2
done

echo
echo "[3/4] probe business endpoint directly"
ENDPOINT="/api/media-tasks/task-week12-001/asset-blueprint"
HTTP_CODE="000"

if [ "$SERVER_READY" = "true" ]; then
  HTTP_CODE="$(curl -sS -o "$BODY" -w "%{http_code}" "http://127.0.0.1:${PORT}${ENDPOINT}" || true)"
fi

{
  echo "endpoint=$ENDPOINT"
  echo "healthCode=$HEALTH_CODE"
  echo "httpCode=$HTTP_CODE"
  echo "bodyFile=$BODY"
  echo "healthBody=$HEALTH_BODY"
  echo "appLog=$APP_LOG"
} | tee "$PROBE_LOG"

echo
echo "[4/4] build canonical summary"
python3 - <<PY
import json
import datetime
from pathlib import Path

required = [
    "taskId",
    "inputAssetUri",
    "blueprintArtifactUri",
    "blueprintId",
    "timelineArtifactUri",
    "qualityGateStatus",
    "qualityGatePassed",
    "blueprintManifestLinked",
    "timelineArtifactLinked",
]

body_path = Path("${BODY}")
health_path = Path("${HEALTH_BODY}")

body = None
body_parse_error = None
if body_path.exists():
    raw = body_path.read_text(encoding="utf-8", errors="replace")
    try:
        body = json.loads(raw) if raw.strip() else None
    except Exception as exc:
        body_parse_error = str(exc)
        body = {"raw": raw[:2000]}

health = None
if health_path.exists():
    raw_h = health_path.read_text(encoding="utf-8", errors="replace")
    try:
        health = json.loads(raw_h) if raw_h.strip() else None
    except Exception:
        health = {"raw": raw_h[:2000]}

missing = required[:]
if isinstance(body, dict):
    missing = [k for k in required if k not in body]

http_code = "${HTTP_CODE}"
health_code = "${HEALTH_CODE}"

status = "PASS" if http_code == "200" and not missing else "HTTP_RUNTIME_BLOCKED"
if "${SERVER_READY}" != "true":
    status = "APP_NOT_REACHABLE"

summary = {
    "schemaVersion": "week12.asset-blueprint-http-runtime-smoke.v2",
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": status,
    "selectedEndpoint": "${ENDPOINT}",
    "httpCode": http_code,
    "healthCode": health_code,
    "healthIsDiagnosticOnly": True,
    "redisHealthDisabledForSmoke": True,
    "missingFields": missing,
    "requiredFields": required,
    "body": body,
    "bodyParseError": body_parse_error,
    "healthBody": health,
    "appLog": "${APP_LOG}",
    "probeLog": "${PROBE_LOG}",
    "bodyFile": "${BODY}",
    "doesNotClaim": [
        "production deployment",
        "durable artifact registry",
        "object storage lifecycle",
        "full Redis-backed readiness",
        "production SLO"
    ]
}

Path("${SUMMARY}").write_text(json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")
print(json.dumps(summary, indent=2, ensure_ascii=False))
raise SystemExit(0 if status == "PASS" else 4)
PY