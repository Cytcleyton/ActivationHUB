#!/usr/bin/env bash
# ./run.sh        start the app
# ./run.sh test   run the tests
set -e
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  echo "Java isn't installed. Install Java 21 or later (e.g. brew install openjdk@21)."
  exit 1
fi
major=$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)
if [ "${major:-0}" -lt 21 ]; then
  echo "Found Java ${major}; this app needs Java 21 or later."
  exit 1
fi

version_ok() {
  local v
  v=$("$1" -version 2>/dev/null | awk '/Apache Maven/ {print $3}')
  [ -n "$v" ] && echo "$v" | awk -F. '{ exit !(($1*1000000 + $2*1000 + $3) >= 3006003) }'
}
MVN=""
for candidate in "$(command -v mvn 2>/dev/null)" /opt/homebrew/opt/maven/bin/mvn /usr/local/opt/maven/bin/mvn; do
  if [ -n "$candidate" ] && [ -x "$candidate" ] && version_ok "$candidate"; then
    MVN="$candidate"
    break
  fi
done
if [ -z "$MVN" ]; then
  echo "This project needs Maven 3.6.3 or later. Install it with: brew install maven"
  exit 1
fi

if [ "${1:-}" = "test" ]; then
  exec "$MVN" test
fi

if [ -f .env ]; then
  set -a; . ./.env; set +a
fi

echo "Starting on http://localhost:${SERVER_PORT:-8080} (Ctrl+C to stop)"
exec "$MVN" spring-boot:run
