#!/usr/bin/env bash
# Runs the integration tests against another Minecraft version, in its own run-<version> directory.
#
#   it/check-version.sh <server version> <bot version> [ExcellentCrates jar]
#
# <bot version> must be one the bots support (see README) and speak the server's protocol.
# With an ExcellentCrates jar, that build replaces the Modrinth release, and the run directory is
# named after both (run-<version>-<jar name>).
# The Minecraft EULA must already be accepted in run/eula.txt; it is copied to the new directory.
set -euo pipefail

MC=$1
BOT=$2
EC_JAR=${3:-}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
RUN="run-$MC"
GRADLE_ARGS=(-PtestMc="$MC")
if [ -n "$EC_JAR" ]; then
  EC_JAR=$(cd "$(dirname "$EC_JAR")" && pwd)/$(basename "$EC_JAR")
  RUN="run-$MC-$(basename "$EC_JAR" .jar)"
  GRADLE_ARGS+=(-PtestEcJar="$EC_JAR" -PtestRun="$RUN")
fi
export CC_RUN=$RUN CC_MC=$BOT
cd "$ROOT"
mkdir -p "$RUN/plugins"
cp run/eula.txt "$RUN/eula.txt"
# Test ports from the start; the default port may be taken on this machine.
cp it/fixtures/server.properties "$RUN/server.properties"

PID=""
start_server() {
  rm -f "$RUN/logs/latest.log"
  ./gradlew runServer "${GRADLE_ARGS[@]}" --console=plain > "$RUN/gradle-run.log" 2>&1 &
  PID=$!
  for _ in $(seq 1 600); do
    if grep -qE "Done \(" "$RUN/logs/latest.log" 2>/dev/null; then return 0; fi
    if ! kill -0 "$PID" 2>/dev/null; then echo "server exited during startup; see $RUN/gradle-run.log" >&2; return 1; fi
    sleep 1
  done
  echo "server did not start in time" >&2
  return 1
}
stop_server() {
  node -e "const h=require('./it/harness');(async()=>{await h.rconConnect();await h.cmd('stop');await h.rconClose();})()"
  wait "$PID" || true
}

if [ ! -f "$RUN/plugins/ExcellentCrates/config/crates.interaction.yml" ]; then
  echo "== first start of $MC (generates the plugins' default files)"
  start_server
  stop_server
fi
node it/prepare.js > /dev/null

echo "== testing on $MC with a $BOT bot"
start_server
grep -E "Enabling CrateCinema|Hooked into|Registered|animation profiles|ERROR" "$RUN/logs/latest.log" | head -8
status=0
node it/setup.js > /dev/null && node it/setup.js showcase 3 > /dev/null && node it/setup.js native_test -3 > /dev/null && node it/tests.js || status=$?
echo "server log errors: $(grep -cE 'SEVERE|ERROR|Exception' "$RUN/logs/latest.log" || true)"
stop_server
exit $status
