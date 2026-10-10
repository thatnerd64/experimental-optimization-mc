#!/bin/bash
# usage: runaot.sh <rundir> ; JVM boot of pre-transformed classes + glue; waits for Done/crash
R=$1; D=/root/dw20-server
export JAVA_HOME=$(ls -d /root/build1710-tools/jdk8u504*); export PATH=$JAVA_HOME/bin:$PATH
mkdir -p $R && cd $R
for d in lib mods coremods config; do [ -d $d ] || cp -r $D/base/$d .; done
[ -f server.jar ] || cp $D/base-server.jar server.jar
cp $D/base/server.properties . 2>/dev/null
rm -rf world server.log* ForgeModLoader* crash-reports run.log
START=$(date +%s)
nohup ${JBIN:-java} -Xmx3G $JOPTS -Djava.security.properties=$D/jar-policy.properties -cp "$D/glue/bin:${CLS:-$D/aotwork/out}/classes.jar:${CLS:-$D/aotwork/out}/resources.jar" cpw.mods.fml.relauncher.ServerBoot nogui > run.log 2>&1 < /dev/null &
PID=$!
for i in $(seq 1 100); do
  grep -qE "Done \(|This crash report|STDERR\] Exception|Caused by" run.log && break
  kill -0 $PID 2>/dev/null || break
  sleep 3
done
sleep 2; grep -q "Done (" run.log || kill $PID 2>/dev/null
echo "elapsed $(( $(date +%s) - START ))s pid=$PID alive=$(kill -0 $PID 2>/dev/null && echo yes || echo no)"
grep -nE "Done \(|rash report|Encountered" run.log | head -5
tail -${2:-6} run.log | cut -c1-230
