#!/usr/bin/env bash
set -u

cd /mnt/c/Users/USER/Desktop/high-load-request-management-system || exit 1

API="http://localhost:8088/api/v1/perf/requests"

USER_A="11111111-1111-4111-8111-111111111111"
EMAIL_A="perf-test@hlrms.local"

USER_B="22222222-2222-4222-8222-222222222222"
EMAIL_B="r10-second-user@hlrms.local"

ROLES="USER"

RUN_TAG="$(date +%Y%m%d-%H%M%S)"
PREFIX="P19-R10-${RUN_TAG}"
LOG="/tmp/${PREFIX}.txt"

exec > >(tee "$LOG") 2>&1

PG="$(docker compose ps -q postgres)"
RABBIT="$(docker compose ps -q rabbitmq)"

if [ -z "$PG" ] || [ -z "$RABBIT" ]; then
    echo "❌ PostgreSQL or RabbitMQ container missing"
    exit 1
fi

RMQ_USER="$(
docker inspect "$RABBIT" \
  --format '{{range .Config.Env}}{{println .}}{{end}}' |
sed -n 's/^RABBITMQ_DEFAULT_USER=//p'
)"

RMQ_PASS="$(
docker inspect "$RABBIT" \
  --format '{{range .Config.Env}}{{println .}}{{end}}' |
sed -n 's/^RABBITMQ_DEFAULT_PASS=//p'
)"

RMQ_BIND="$(docker compose port rabbitmq 15672 | head -n1)"
RMQ_PORT="${RMQ_BIND##*:}"

QUEUE_URL="http://127.0.0.1:${RMQ_PORT}/api/queues/%2F/hlrms.request.processing.queue"
DLQ_URL="http://127.0.0.1:${RMQ_PORT}/api/queues/%2F/hlrms.request.processing.dlq"

cleanup() {
    docker compose start redis >/dev/null 2>&1 || true
}
trap cleanup EXIT

db_scalar() {
    local SQL="$1"

    docker exec "$PG" sh -lc "
      export PGPASSWORD=\"\$POSTGRES_PASSWORD\"
      psql \
        -U \"\$POSTGRES_USER\" \
        -d \"\$POSTGRES_DB\" \
        -Atc \"$SQL\"
    "
}

outbox_state() {
    db_scalar "
      SELECT
        count(*) FILTER (WHERE status='PENDING')
        || '|' ||
        count(*) FILTER (WHERE status='PROCESSING')
        || '|' ||
        count(*) FILTER (WHERE status='FAILED')
      FROM outbox_events;
    "
}

qstats() {
    curl -fsS \
      -u "$RMQ_USER:$RMQ_PASS" \
      "$QUEUE_URL" |
    python3 -c '
import json,sys
d=json.load(sys.stdin)
print(
 int(d.get("messages_ready") or 0),
 int(d.get("messages_unacknowledged") or 0),
 int(d.get("consumers") or 0)
)
'
}

dlq_count() {
    curl -fsS \
      -u "$RMQ_USER:$RMQ_PASS" \
      "$DLQ_URL" |
    python3 -c '
import json,sys
d=json.load(sys.stdin)
print(int(d.get("messages_ready") or 0))
'
}

http_call() {
    local LABEL="$1"
    local USER_ID="$2"
    local EMAIL="$3"
    local KEY="$4"
    local BODY="$5"

    local H="/tmp/${PREFIX}-${LABEL}.headers"
    local B="/tmp/${PREFIX}-${LABEL}.body"

    rm -f "$H" "$B"

    HTTP_STATUS="$(
      curl \
        -sS \
        --connect-timeout 3 \
        --max-time 30 \
        -D "$H" \
        -o "$B" \
        -w '%{http_code}' \
        -X POST \
        "$API" \
        -H 'Content-Type: application/json' \
        -H "X-User-Id: $USER_ID" \
        -H "X-User-Email: $EMAIL" \
        -H "X-User-Roles: $ROLES" \
        -H "Idempotency-Key: $KEY" \
        -H "X-Correlation-ID: ${PREFIX}-${LABEL}-$(date +%s%N)" \
        --data-binary "$BODY"
    )"

    HTTP_REPLAYED="$(
      python3 - "$H" <<'PY'
from pathlib import Path
import sys

value=""
for line in Path(sys.argv[1]).read_text(errors="ignore").splitlines():
    if line.lower().startswith("idempotency-replayed:"):
        value=line.split(":",1)[1].strip().lower()

print(value)
PY
    )"

    HTTP_ID="$(
      python3 - "$B" <<'PY'
from pathlib import Path
import json,sys

try:
    d=json.loads(Path(sys.argv[1]).read_text())
    print(d.get("id") or "")
except Exception:
    print("")
PY
    )"

    echo "$LABEL status=$HTTP_STATUS replayed=${HTTP_REPLAYED:-<none>} id=${HTTP_ID:-<none>}"
}

echo
echo "=================================================="
echo "P19-R10 FINAL INTEGRITY + IDEMPOTENCY"
echo "RUN=$PREFIX"
echo "=================================================="

# --------------------------------------------------
# PREFLIGHT
# --------------------------------------------------

echo
echo "========== PREFLIGHT =========="

DB_STATE="$(outbox_state)"
read READY UNACK CONSUMERS <<<"$(qstats)"
BASE_DLQ="$(dlq_count)"

echo "Outbox=$DB_STATE"
echo "Rabbit ready=$READY unacked=$UNACK consumers=$CONSUMERS"
echo "DLQ=$BASE_DLQ"

if [ "$DB_STATE" != "0|0|0" ] ||
   [ "$READY" != "0" ] ||
   [ "$UNACK" != "0" ] ||
   [ "$CONSUMERS" -lt 1 ]; then

    echo "❌ PREFLIGHT NOT CLEAN"
    exit 1
fi

echo "PREFLIGHT CLEAN ✅"


# --------------------------------------------------
# R10-A — REPLAY
# --------------------------------------------------

echo
echo "========== R10-A REPLAY =========="

KEY_A="${PREFIX}-A-replay"

BODY_A='{"requestType":"STANDARD","payload":"{\"source\":\"P19-R10\",\"case\":\"A\",\"value\":\"same-payload\"}"}'

http_call A1 "$USER_A" "$EMAIL_A" "$KEY_A" "$BODY_A"

A1_STATUS="$HTTP_STATUS"
A1_REPLAYED="$HTTP_REPLAYED"
A1_ID="$HTTP_ID"

http_call A2 "$USER_A" "$EMAIL_A" "$KEY_A" "$BODY_A"

A2_STATUS="$HTTP_STATUS"
A2_REPLAYED="$HTTP_REPLAYED"
A2_ID="$HTTP_ID"

A_PASS=0

if [ "$A1_STATUS" = "201" ] &&
   [ "$A1_REPLAYED" = "false" ] &&
   [ -n "$A1_ID" ] &&
   [ "$A2_STATUS" = "200" ] &&
   [ "$A2_REPLAYED" = "true" ] &&
   [ "$A2_ID" = "$A1_ID" ]; then

    A_PASS=1
    echo "R10-A REPLAY PASS ✅"
else
    echo "❌ R10-A REPLAY FAILED"
fi


# --------------------------------------------------
# R10-B — CONFLICT
# --------------------------------------------------

echo
echo "========== R10-B FINGERPRINT CONFLICT =========="

BODY_B='{"requestType":"STANDARD","payload":"{\"source\":\"P19-R10\",\"case\":\"A\",\"value\":\"DIFFERENT\"}"}'

http_call B "$USER_A" "$EMAIL_A" "$KEY_A" "$BODY_B"

B_PASS=0

if [ "$HTTP_STATUS" = "409" ]; then
    B_PASS=1
    echo "R10-B CONFLICT PASS ✅"
else
    echo "❌ R10-B expected 409, got $HTTP_STATUS"
fi


# --------------------------------------------------
# R10-C — 100 CONCURRENT DUPLICATES
# --------------------------------------------------

echo
echo "========== R10-C 100 CONCURRENT DUPLICATES =========="

KEY_C="${PREFIX}-C-concurrent"
BODY_C='{"requestType":"STANDARD","payload":"{\"source\":\"P19-R10\",\"case\":\"C\",\"value\":\"concurrent\"}"}'

export R10_API="$API"
export R10_USER="$USER_A"
export R10_EMAIL="$EMAIL_A"
export R10_ROLES="$ROLES"
export R10_KEY="$KEY_C"
export R10_BODY="$BODY_C"
export R10_PREFIX="$PREFIX"

python3 - <<'PY'
import os
import json
import urllib.request
import urllib.error
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed

URL=os.environ["R10_API"]
USER=os.environ["R10_USER"]
EMAIL=os.environ["R10_EMAIL"]
ROLES=os.environ["R10_ROLES"]
KEY=os.environ["R10_KEY"]
BODY=os.environ["R10_BODY"].encode()
PREFIX=os.environ["R10_PREFIX"]

N=100

def one(i):
    req=urllib.request.Request(
        URL,
        data=BODY,
        headers={
            "Content-Type":"application/json",
            "X-User-Id":USER,
            "X-User-Email":EMAIL,
            "X-User-Roles":ROLES,
            "Idempotency-Key":KEY,
            "X-Correlation-ID":f"{PREFIX}-C-{i}",
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(req,timeout=30) as r:
            status=r.status
            replay=r.headers.get("Idempotency-Replayed","").lower()
            body=r.read().decode()

    except urllib.error.HTTPError as e:
        status=e.code
        replay=e.headers.get("Idempotency-Replayed","").lower()
        body=e.read().decode()

    except Exception as e:
        return (-1,"","",repr(e))

    try:
        rid=json.loads(body).get("id","")
    except Exception:
        rid=""

    return (status,replay,rid,"")

results=[]

with ThreadPoolExecutor(max_workers=N) as pool:
    fs=[pool.submit(one,i) for i in range(N)]

    for f in as_completed(fs):
        results.append(f.result())

statuses=Counter(x[0] for x in results)
ids={x[2] for x in results if x[2]}
errors=[x[3] for x in results if x[3]]

other=sum(
    count
    for status,count in statuses.items()
    if status not in (200,201)
)

print("TOTAL=",len(results),sep="")
print("STATUS_201=",statuses.get(201,0),sep="")
print("STATUS_200=",statuses.get(200,0),sep="")
print("STATUS_OTHER=",other,sep="")
print("UNIQUE_IDS=",len(ids),sep="")
print("TRANSPORT_ERRORS=",len(errors),sep="")

ok=(
    len(results)==100
    and statuses.get(201,0)==1
    and statuses.get(200,0)==99
    and other==0
    and len(ids)==1
    and len(errors)==0
)

if ok:
    print("R10-C CONCURRENT IDEMPOTENCY PASS ✅")
    raise SystemExit(0)

print("❌ R10-C CONCURRENT IDEMPOTENCY FAILED")
raise SystemExit(2)
PY

C_RC=$?

if [ "$C_RC" = "0" ]; then
    C_PASS=1
else
    C_PASS=0
fi


# --------------------------------------------------
# R10-D — USER SCOPED
# --------------------------------------------------

echo
echo "========== R10-D USER-SCOPED IDEMPOTENCY =========="

KEY_D="${PREFIX}-D-shared"
BODY_D='{"requestType":"STANDARD","payload":"{\"source\":\"P19-R10\",\"case\":\"D\"}"}'

http_call DA1 "$USER_A" "$EMAIL_A" "$KEY_D" "$BODY_D"
DA1_STATUS="$HTTP_STATUS"
DA1_REPLAYED="$HTTP_REPLAYED"
DA1_ID="$HTTP_ID"

http_call DB1 "$USER_B" "$EMAIL_B" "$KEY_D" "$BODY_D"
DB1_STATUS="$HTTP_STATUS"
DB1_REPLAYED="$HTTP_REPLAYED"
DB1_ID="$HTTP_ID"

http_call DA2 "$USER_A" "$EMAIL_A" "$KEY_D" "$BODY_D"
DA2_STATUS="$HTTP_STATUS"
DA2_REPLAYED="$HTTP_REPLAYED"
DA2_ID="$HTTP_ID"

http_call DB2 "$USER_B" "$EMAIL_B" "$KEY_D" "$BODY_D"
DB2_STATUS="$HTTP_STATUS"
DB2_REPLAYED="$HTTP_REPLAYED"
DB2_ID="$HTTP_ID"

D_PASS=0

if [ "$DA1_STATUS" = "201" ] &&
   [ "$DB1_STATUS" = "201" ] &&
   [ "$DA1_REPLAYED" = "false" ] &&
   [ "$DB1_REPLAYED" = "false" ] &&
   [ "$DA1_ID" != "$DB1_ID" ] &&
   [ "$DA2_STATUS" = "200" ] &&
   [ "$DB2_STATUS" = "200" ] &&
   [ "$DA2_REPLAYED" = "true" ] &&
   [ "$DB2_REPLAYED" = "true" ] &&
   [ "$DA2_ID" = "$DA1_ID" ] &&
   [ "$DB2_ID" = "$DB1_ID" ]; then

    D_PASS=1
    echo "R10-D USER-SCOPED IDEMPOTENCY PASS ✅"
else
    echo "❌ R10-D USER-SCOPED IDEMPOTENCY FAILED"
fi


# --------------------------------------------------
# R10-E — REDIS FAILURE FALLBACK
# --------------------------------------------------

echo
echo "========== R10-E REDIS FAILURE FALLBACK =========="

KEY_E="${PREFIX}-E-redis"
BODY_E='{"requestType":"STANDARD","payload":"{\"source\":\"P19-R10\",\"case\":\"E\"}"}'

docker compose stop redis >/dev/null
sleep 2

if [ -z "$(docker compose ps --status running -q redis)" ]; then
    echo "REDIS CONFIRMED DOWN ✅"
else
    echo "❌ REDIS DID NOT STOP"
fi

http_call E1 "$USER_A" "$EMAIL_A" "$KEY_E" "$BODY_E"
E1_STATUS="$HTTP_STATUS"
E1_REPLAYED="$HTTP_REPLAYED"
E1_ID="$HTTP_ID"

http_call E2 "$USER_A" "$EMAIL_A" "$KEY_E" "$BODY_E"
E2_STATUS="$HTTP_STATUS"
E2_REPLAYED="$HTTP_REPLAYED"
E2_ID="$HTTP_ID"

docker compose start redis >/dev/null

for i in $(seq 1 30); do
    RID="$(docker compose ps --status running -q redis)"

    if [ -n "$RID" ]; then
        echo "REDIS RUNNING ✅"
        break
    fi

    sleep 1
done

sleep 2

http_call E3 "$USER_A" "$EMAIL_A" "$KEY_E" "$BODY_E"
E3_STATUS="$HTTP_STATUS"
E3_REPLAYED="$HTTP_REPLAYED"
E3_ID="$HTTP_ID"

E_PASS=0

if [ "$E1_STATUS" = "201" ] &&
   [ "$E1_REPLAYED" = "false" ] &&
   [ "$E2_STATUS" = "200" ] &&
   [ "$E2_REPLAYED" = "true" ] &&
   [ "$E2_ID" = "$E1_ID" ] &&
   [ "$E3_STATUS" = "200" ] &&
   [ "$E3_REPLAYED" = "true" ] &&
   [ "$E3_ID" = "$E1_ID" ]; then

    E_PASS=1
    echo "R10-E REDIS FALLBACK PASS ✅"
else
    echo "❌ R10-E REDIS FALLBACK FAILED"
fi


# --------------------------------------------------
# PIPELINE DRAIN
# --------------------------------------------------

echo
echo "========== FINAL PIPELINE DRAIN =========="

ZERO=0

for i in $(seq 1 120); do

    DB_STATE="$(outbox_state)"
    read READY UNACK CONSUMERS <<<"$(qstats)"

    echo "[$i] outbox=$DB_STATE ready=$READY unacked=$UNACK consumers=$CONSUMERS"

    if [ "$DB_STATE" = "0|0|0" ] &&
       [ "$READY" = "0" ] &&
       [ "$UNACK" = "0" ]; then
        ZERO=$((ZERO+1))
    else
        ZERO=0
    fi

    [ "$ZERO" -ge 3 ] && break

    sleep 1
done


# --------------------------------------------------
# FINAL RECONCILIATION
# --------------------------------------------------

echo
echo "========== DATABASE RECONCILIATION =========="

REQUEST_ROWS="$(
db_scalar "
SELECT count(*)
FROM requests
WHERE idempotency_key LIKE '${PREFIX}%';
"
)"

OUTBOX_ROWS="$(
db_scalar "
SELECT count(*)
FROM outbox_events o
JOIN requests r ON r.id=o.aggregate_id
WHERE r.idempotency_key LIKE '${PREFIX}%';
"
)"

PUBLISHED_ROWS="$(
db_scalar "
SELECT count(*)
FROM outbox_events o
JOIN requests r ON r.id=o.aggregate_id
WHERE r.idempotency_key LIKE '${PREFIX}%'
AND o.status='PUBLISHED';
"
)"

PROCESSED_ROWS="$(
db_scalar "
SELECT count(*)
FROM processed_events p
JOIN requests r ON r.id=p.request_id
WHERE r.idempotency_key LIKE '${PREFIX}%';
"
)"

MAX_PROCESSED_PER_REQUEST="$(
db_scalar "
SELECT COALESCE(max(c),0)
FROM (
    SELECT count(*) c
    FROM processed_events p
    JOIN requests r ON r.id=p.request_id
    WHERE r.idempotency_key LIKE '${PREFIX}%'
    GROUP BY p.request_id
) x;
"
)"

A_ROWS="$(db_scalar "SELECT count(*) FROM requests WHERE idempotency_key='${KEY_A}';")"
C_ROWS="$(db_scalar "SELECT count(*) FROM requests WHERE idempotency_key='${KEY_C}';")"
D_ROWS="$(db_scalar "SELECT count(*) FROM requests WHERE idempotency_key='${KEY_D}';")"
D_USERS="$(db_scalar "SELECT count(DISTINCT user_id) FROM requests WHERE idempotency_key='${KEY_D}';")"
E_ROWS="$(db_scalar "SELECT count(*) FROM requests WHERE idempotency_key='${KEY_E}';")"

FINAL_STATE="$(outbox_state)"
read FINAL_READY FINAL_UNACK FINAL_CONSUMERS <<<"$(qstats)"
FINAL_DLQ="$(dlq_count)"

echo
echo "REQUEST_ROWS=$REQUEST_ROWS"
echo "OUTBOX_ROWS=$OUTBOX_ROWS"
echo "PUBLISHED_ROWS=$PUBLISHED_ROWS"
echo "PROCESSED_ROWS=$PROCESSED_ROWS"
echo "MAX_PROCESSED_PER_REQUEST=$MAX_PROCESSED_PER_REQUEST"
echo
echo "A_ROWS=$A_ROWS"
echo "C_ROWS=$C_ROWS"
echo "D_ROWS=$D_ROWS"
echo "D_USERS=$D_USERS"
echo "E_ROWS=$E_ROWS"
echo
echo "FINAL_OUTBOX_STATE=$FINAL_STATE"
echo "FINAL_READY=$FINAL_READY"
echo "FINAL_UNACK=$FINAL_UNACK"
echo "FINAL_CONSUMERS=$FINAL_CONSUMERS"
echo "DLQ=$BASE_DLQ -> $FINAL_DLQ"

DB_PASS=0

if [ "$REQUEST_ROWS" = "5" ] &&
   [ "$OUTBOX_ROWS" = "5" ] &&
   [ "$PUBLISHED_ROWS" = "5" ] &&
   [ "$PROCESSED_ROWS" = "5" ] &&
   [ "$MAX_PROCESSED_PER_REQUEST" = "1" ] &&
   [ "$A_ROWS" = "1" ] &&
   [ "$C_ROWS" = "1" ] &&
   [ "$D_ROWS" = "2" ] &&
   [ "$D_USERS" = "2" ] &&
   [ "$E_ROWS" = "1" ] &&
   [ "$FINAL_STATE" = "0|0|0" ] &&
   [ "$FINAL_READY" = "0" ] &&
   [ "$FINAL_UNACK" = "0" ] &&
   [ "$FINAL_DLQ" = "$BASE_DLQ" ]; then

    DB_PASS=1
    echo "R10 DATABASE RECONCILIATION PASS ✅"
else
    echo "❌ R10 DATABASE RECONCILIATION FAILED"
fi


echo
echo "================ R10 SUMMARY ================"
echo "A_PASS=$A_PASS"
echo "B_PASS=$B_PASS"
echo "C_PASS=$C_PASS"
echo "D_PASS=$D_PASS"
echo "E_PASS=$E_PASS"
echo "DB_PASS=$DB_PASS"
echo

if [ "$A_PASS" = "1" ] &&
   [ "$B_PASS" = "1" ] &&
   [ "$C_PASS" = "1" ] &&
   [ "$D_PASS" = "1" ] &&
   [ "$E_PASS" = "1" ] &&
   [ "$DB_PASS" = "1" ]; then

    RESULT=0

    echo "P19-R10 FINAL INTEGRITY + IDEMPOTENCY PASS ✅"
    echo "P19 LOAD & PERFORMANCE TESTING CORE PLAN COMPLETE ✅"

else

    RESULT=2
    echo "❌ P19-R10 REQUIRES INVESTIGATION"
fi

echo "LOG=$LOG"

trap - EXIT
docker compose start redis >/dev/null 2>&1 || true

exit "$RESULT"
