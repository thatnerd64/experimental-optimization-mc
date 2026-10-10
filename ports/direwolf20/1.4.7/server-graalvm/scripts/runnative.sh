#!/bin/bash
# usage: runnative.sh <rundir> [tail-lines]; boots the native binary in a fresh server dir, waits for Done/crash, pings
R=$1; D=/root/dw20-server
mkdir -p $R && cd $R
for d in lib mods coremods config; do [ -d $d ] || cp -r $D/base/$d .; done
[ -f server.jar ] || cp $D/base-server.jar server.jar
cp $D/base/server.properties . 2>/dev/null
rm -rf world server.log* ForgeModLoader* crash-reports run.log in.fifo; mkfifo in.fifo; exec 3<>in.fifo
START=$(date +%s)
nohup $D/native/dw20-server nogui > run.log 2>&1 <&3 &
PID=$!
for i in $(seq 1 100); do
  grep -qE "Done \(|This crash report|Exception in thread|Caused by" run.log && break
  kill -0 $PID 2>/dev/null || break
  sleep 2
done
echo "boot: $(( $(date +%s) - START ))s alive=$(kill -0 $PID 2>/dev/null && echo yes || echo no) pid=$PID"
grep -nE "Done \(" run.log | head -2
if grep -q "Done (" run.log; then python3 $D/ping.py 127.0.0.1 25590 2>&1 | tail -1; ps -o rss=,nlwp= -p $PID | awk '{print "RSS " $1/1024 " MB, threads " $2}'; fi
tail -${2:-8} run.log | cut -c1-230
echo $PID > native.pid
