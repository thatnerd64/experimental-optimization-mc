#!/bin/bash
# usage: agentrun.sh <rundir> <agent-out-dir> : boot on GraalVM JDK 21 under the native-image agent, ping, tick, stop
R=$1; AG=$2; D=/root/dw20-server
mkdir -p $R $AG && cd $R
for d in lib mods coremods config; do [ -d $d ] || cp -r $D/base/$d .; done
cp $D/base/server.properties . 2>/dev/null
rm -rf world server.log* ForgeModLoader* crash-reports run.log in.fifo; mkfifo in.fifo
exec 3<>in.fifo
START=$(date +%s)
nohup /opt/graalvm/bin/java -Xmx3G -agentlib:native-image-agent=config-merge-dir=$AG,config-write-period-secs=20 \
  -cp "$D/glue/bin:${CLS:-$D/aotwork/out}/classes.jar:${CLS:-$D/aotwork/out}/resources.jar" cpw.mods.fml.relauncher.ServerBoot nogui > run.log 2>&1 <&3 &
PID=$!
for i in $(seq 1 120); do grep -qE "Done \(|This crash report" run.log && break; kill -0 $PID 2>/dev/null || break; sleep 3; done
grep -E "Done \(|rash report" run.log | head -3
python3 $D/ping.py 127.0.0.1 25590 2>&1 | tail -1
/opt/graalvm/bin/java -cp $D/base/lib/guava-12.0.1.jar $D/Bot.java 127.0.0.1 25590 Tester ${TICKS:-40} $R/ForgeModLoader-server-0.log 2>&1 | tail -2
sleep 3
echo "stop" >&3
for i in $(seq 1 40); do kill -0 $PID 2>/dev/null || break; sleep 2; done
echo "exited after $(( $(date +%s) - START ))s alive=$(kill -0 $PID 2>/dev/null && echo yes || echo no)"
kill $PID 2>/dev/null
exec 3>&-
ls -la $AG | head; tail -3 run.log | cut -c1-200
