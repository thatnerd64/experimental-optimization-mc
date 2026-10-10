#!/bin/bash
# usage: nibuild.sh [extra native-image args]; builds /root/dw20-server/native/dw20-server
D=/root/dw20-server
mkdir -p $D/native && cd $D/native
# stubs for types the server jar lacks (client-only vanilla classes etc.), native-image classpath only
rm -rf $D/stubsrc $D/stubs.jar $D/stubsbin; python3 -I $D/stubs.py $D/aotwork/out/classes.jar $D/stubsrc || exit 1
mkdir -p $D/stubsbin && /opt/graalvm/bin/javac -nowarn --release 8 -d $D/stubsbin $(find $D/stubsrc -name '*.java') 2>&1 | grep -v "^warning\|^Note" | head -5
(cd $D/stubsbin && /opt/graalvm/bin/jar cf $D/stubs.jar .)
/opt/graalvm/bin/java -Xmx8g $D/Loadable.java $D/aotwork/out/classes.jar $D/aotwork/out/resources.jar $D/unloadable.txt $D/stubs.jar || exit 1
python3 -I $D/prune.py $D/aotwork/out/classes.jar $D/unloadable.txt $D/aotwork/out/classes-ni.jar || exit 1
python3 -I $D/genconfig.py $D/aotwork/out/classes-ni.jar $D/agent-cfg $D/ni-cfg || exit 1
START=$(date +%s)
/opt/graalvm/bin/native-image \
  -cp "$D/glue/bin:$D/aotwork/out/classes-ni.jar:$D/aotwork/out/resources.jar:$D/stubs.jar" \
  cpw.mods.fml.relauncher.ServerBoot dw20-server \
  --no-fallback -H:ConfigurationFileDirectories=$D/ni-cfg \
  --enable-url-protocols=http,https,jar \
  -H:+ReportExceptionStackTraces --report-unsupported-elements-at-runtime \
  -J-Xmx64g -J-XX:+UseParallelGC "$@" 
echo "native-image exit=$? after $(( $(date +%s) - START ))s"
ls -la $D/native | head -4
