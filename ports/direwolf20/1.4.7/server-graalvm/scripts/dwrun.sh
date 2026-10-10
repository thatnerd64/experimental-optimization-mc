#!/bin/bash
# usage: dwrun.sh <dir> ; boots baseline server in <dir>, waits for Done, prints summary
cd "$1" || exit 1
export JAVA_HOME=$(ls -d /root/build1710-tools/jdk8u504*); export PATH=$JAVA_HOME/bin:$PATH
rm -rf world server.log* ForgeModLoader* crash-reports run.log
START=$(date +%s)
nohup java -Xmx3G -Djava.security.properties=/root/dw20-server/jar-policy.properties -jar server.jar nogui > run.log 2>&1 < /dev/null &
PID=$!
for i in $(seq 1 100); do
  grep -qE "Done \(|This crash report" run.log && break
  kill -0 $PID 2>/dev/null || break
  sleep 3
done
echo "elapsed $(( $(date +%s) - START ))s pid=$PID alive=$(kill -0 $PID 2>/dev/null && echo yes || echo no)"
grep -nE "Done \(|rash report|Encountered" run.log | head -5
tail -3 run.log | cut -c1-220
