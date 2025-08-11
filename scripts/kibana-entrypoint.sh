#!/bin/bash

set -e

ES_HOST="http://elasticsearch:9200"
ES_USER="elastic"
ES_PASSWORD="${ELASTIC_PASSWORD:-changeme}"

echo "✅ Elasticsearch is up. Generating enrollment token..."
TOKEN=$(curl -s -u $ES_USER:$ES_PASSWORD -X POST "$ES_HOST/_security/enroll/kibana" | jq -r '.token')

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to retrieve Kibana enrollment token."
  exit 1
fi

echo "🔐 Using enrollment token: $TOKEN"
export KIBANA_ENROLLMENT_TOKEN=$TOKEN

exec /bin/tini -- /usr/local/bin/kibana-docker
