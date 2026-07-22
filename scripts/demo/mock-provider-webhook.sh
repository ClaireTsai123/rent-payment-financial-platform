#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  scripts/demo/mock-provider-webhook.sh --operation-key <operation-key> [--status SUCCEEDED|FAILED|RETURNED|REVERSED]
  scripts/demo/mock-provider-webhook.sh --provider-transaction-id <provider-transaction-id> [--status SUCCEEDED|FAILED|RETURNED|REVERSED]

Environment:
  API_BASE_URL=http://localhost:8080
  ALLOW_NON_LOCAL_DEMO_WEBHOOK=false
  MOCK_PROVIDER_WEBHOOK_SECRET=local-mock-webhook-secret
  PGHOST=localhost
  PGPORT=5432
  PGDATABASE=rent_payment
  PGUSER=rent_payment
  PGPASSWORD=rent_payment
USAGE
}

status="SUCCEEDED"
operation_key=""
provider_transaction_id=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --status)
      status="${2:-}"
      shift 2
      ;;
    --operation-key)
      operation_key="${2:-}"
      shift 2
      ;;
    --provider-transaction-id)
      provider_transaction_id="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

case "$status" in
  SUCCEEDED|FAILED|RETURNED|REVERSED) ;;
  *)
    echo "Unsupported status: $status" >&2
    usage >&2
    exit 2
    ;;
esac

if [[ -n "$operation_key" && -n "$provider_transaction_id" ]]; then
  echo "Use either --operation-key or --provider-transaction-id, not both." >&2
  exit 2
fi

if [[ -z "$operation_key" && -z "$provider_transaction_id" ]]; then
  echo "One of --operation-key or --provider-transaction-id is required." >&2
  usage >&2
  exit 2
fi

if [[ -n "$operation_key" ]]; then
  if ! command -v psql >/dev/null 2>&1; then
    echo "psql is required when using --operation-key. Pass --provider-transaction-id instead." >&2
    exit 2
  fi

  export PGHOST="${PGHOST:-localhost}"
  export PGPORT="${PGPORT:-5432}"
  export PGDATABASE="${PGDATABASE:-rent_payment}"
  export PGUSER="${PGUSER:-rent_payment}"
  export PGPASSWORD="${PGPASSWORD:-rent_payment}"

  provider_transaction_id="$(
    psql -X -A -t -v ON_ERROR_STOP=1 -v operation_key="$operation_key" \
      -c "select pt.provider_transaction_id
          from provider_transactions pt
          join money_movements mm on mm.id = pt.money_movement_id
          where mm.operation_key = :'operation_key'
          order by pt.created_at desc
          limit 1"
  )"

  if [[ -z "$provider_transaction_id" ]]; then
    echo "No provider transaction found for operation key: $operation_key" >&2
    exit 1
  fi
fi

api_base_url="${API_BASE_URL:-http://localhost:8080}"
case "$api_base_url" in
  http://localhost:*|http://127.0.0.1:*|http://0.0.0.0:*) ;;
  *)
    if [[ "${ALLOW_NON_LOCAL_DEMO_WEBHOOK:-false}" != "true" ]]; then
      echo "Refusing to send demo webhook to non-local API_BASE_URL: $api_base_url" >&2
      echo "Set ALLOW_NON_LOCAL_DEMO_WEBHOOK=true only for an intentional non-local test sandbox." >&2
      exit 2
    fi
    ;;
esac

webhook_secret="${MOCK_PROVIDER_WEBHOOK_SECRET:-local-mock-webhook-secret}"
occurred_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
status_token="$(printf '%s' "$status" | tr '[:upper:]' '[:lower:]')"
provider_event_id="local-demo-${status_token}-$(date -u +%Y%m%d%H%M%S)-$$"

payload="$(
  printf '{"providerEventId":"%s","providerTransactionId":"%s","providerStatus":"%s","occurredAt":"%s"}' \
    "$provider_event_id" \
    "$provider_transaction_id" \
    "$status" \
    "$occurred_at"
)"

curl --fail-with-body \
  --request POST \
  --url "${api_base_url}/api/v1/provider-webhooks/mock-provider" \
  --header "Content-Type: application/json" \
  --header "X-Mock-Provider-Signature: ${webhook_secret}" \
  --data "$payload"

printf '\n'
