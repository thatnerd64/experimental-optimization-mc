#!/bin/bash
# usage: bench.sh <mode: base|aot|native> <seconds>   -- boots a fresh world, connects the bot, reports CPU/RSS/bytes/errors
MODE=$1; SECS=${2:-60}; D=/root/dw20-server; R=$D/bench-$MODE
export JAVA_HOME=$(ls -d /root/build1710-tools/jdk8u504*)
mkdir -p $R && cd $R
for d in lib mods coremods config; do rm -rf $d; cp -r $D/base/$d .; done
cp $D/base-server.jar server.jar; cp $D/base/server.properties .; grep -q allow-flight server.properties || echo "allow-flight=true" >> server.properties
rm -rf world server.log* ForgeModLoader* crash-reports run.log in.fifo; mkfifo in.fifo; exec 3<>in.fifo
START=$(date +%s.%N)
case $MODE in
  base)   nohup $JAVA_HOME/bin/java -Xmx3G -Djava.security.properties=$D/jar-policy.properties -jar server.jar nogui > run.log 2>&1 <&3 & ;;
  aot)    nohup /opt/graalvm/bin/java -Xmx3G -cp "$D/glue/bin:$D/aotwork/out/classes.jar:$D/aotwork/out/resources.jar" cpw.mods.fml.relauncher.ServerBoot nogui > run.log 2>&1 <&3 & ;;
  native) nohup $D/native/dw20-server nogui > run.log 2>&1 <&3 & ;;
esac
PID=$!
for i in $(seq 1 200); do grep -qE "Done \(|This crash report" run.log && break; kill -0 $PID 2>/dev/null || break; sleep 0.5; done
BOOT=$(awk -v a=$(date +%s.%N) -v b=$START "BEGIN{printf \"%.1f\", a-b}")
grep -q "Done (" run.log || { echo "$MODE: FAILED to boot"; tail -3 run.log; kill $PID; exit 1; }
sleep 3
cpu() { awk '{printf "%.1f", ($14+$15)/100}' /proc/$PID/stat; }
rss() { awk '/VmRSS/{printf "%.0f", $2/1024}' /proc/$PID/status; }
CPU0=$(cpu); RSS0=$(rss); THR=$(ls /proc/$PID/task | wc -l)
BOTOUT=$(/opt/graalvm/bin/java -cp $D/base/lib/guava-12.0.1.jar $D/Bot.java 127.0.0.1 25590 Tester $SECS $R/ForgeModLoader-server-0.log $R/run.log 2>&1 | tail -3 | tr '\n' ' ')
CPU1=$(cpu); RSS1=$(rss)
sleep 2
echo stop >&3; for i in $(seq 1 30); do kill -0 $PID 2>/dev/null || break; sleep 1; done; kill $PID 2>/dev/null; exec 3>&-
ERRS=$(grep -cE "Exception|Error" run.log)
echo "$MODE: boot=${BOOT}s rss_idle=${RSS0}MB rss_after=${RSS1}MB threads=$THR cpu_boot=${CPU0}s cpu_session=$(awk -v a=$CPU1 -v b=$CPU0 "BEGIN{printf \"%.1f\", a-b}")s errors_in_log=$ERRS | bot: $BOTOUT"
