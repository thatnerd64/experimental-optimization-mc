#!/bin/bash
# usage: bench.sh <base|aot|native> [seconds]
# Boots a fresh world in checks/bench-<mode>, connects the flying bot (scripts/Bot.java) and prints boot time, RSS, threads and
# CPU seconds. Needs JDK8_HOME (base) / GRAALVM_HOME (aot, bot) and the build outputs from the README sequence.
set -euo pipefail
D=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
MODE=$1; SECS=${2:-60}; R=$D/checks/bench-$MODE
: "${GRAALVM_HOME:?Set GRAALVM_HOME}"
mkdir -p "$R" && cd "$R"
for d in lib mods coremods config; do rm -rf "$d"; cp -r "$D/base/$d" .; done
cp "$D/base/server.properties" .
rm -rf world server.log* ForgeModLoader* crash-reports run.log in.fifo; mkfifo in.fifo; exec 3<>in.fifo
START=$(date +%s.%N)
case $MODE in
  base)   cp "$D/base-server.jar" server.jar
          nohup "$JDK8_HOME/bin/java" -Xmx3G -Djava.security.properties="$D/jar-policy.properties" -jar server.jar nogui > run.log 2>&1 <&3 & ;;
  aot)    nohup "$GRAALVM_HOME/bin/java" -Xmx3G -cp "$D/glue/bin:$D/aotwork/out/classes.jar:$D/aotwork/out/resources.jar" cpw.mods.fml.relauncher.ServerBoot nogui > run.log 2>&1 <&3 & ;;
  native) nohup "$D/native/dw20-server" -Xmx3g nogui > run.log 2>&1 <&3 & ;;
esac
PID=$!
for i in $(seq 1 240); do grep -qE "Done \(|This crash report" run.log && break; kill -0 $PID 2>/dev/null || break; sleep 0.5; done
grep -q "Done (" run.log || { echo "$MODE: failed to boot"; tail -3 run.log; kill $PID; exit 1; }
BOOT=$(awk -v a="$(date +%s.%N)" -v b="$START" 'BEGIN{printf "%.1f", a-b}')
sleep 3
cpu() { awk '{printf "%.1f", ($14+$15)/100}' /proc/$PID/stat; }
rss() { awk '/VmRSS/{printf "%.0f", $2/1024}' /proc/$PID/status; }
CPU0=$(cpu); RSS0=$(rss); THR=$(ls /proc/$PID/task | wc -l)
BOT=$("$GRAALVM_HOME/bin/java" -cp "$D/base/lib/guava-12.0.1.jar" "$D/scripts/Bot.java" 127.0.0.1 25591 BenchBot "$SECS" "$R/ForgeModLoader-server-0.log" "$R/run.log" 2>&1 | tail -2 | tr '\n' ' ')
CPU1=$(cpu); RSS1=$(rss)
echo stop >&3; for i in $(seq 1 30); do kill -0 $PID 2>/dev/null || break; sleep 1; done; kill $PID 2>/dev/null || true; exec 3>&-
echo "$MODE: boot=${BOOT}s rss_idle=${RSS0}MB rss_after=${RSS1}MB threads=$THR cpu_boot=${CPU0}s cpu_session=$(awk -v a=$CPU1 -v b=$CPU0 'BEGIN{printf "%.1f", a-b}')s | bot: $BOT"
