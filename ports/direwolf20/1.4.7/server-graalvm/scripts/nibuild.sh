#!/bin/bash
# usage: nibuild.sh [extra native-image args]; builds native/dw20-server
set -euo pipefail
D=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
: "${GRAALVM_HOME:?Set GRAALVM_HOME to a GraalVM installation with native-image}"
mkdir -p $D/native && cd $D/native
# stubs for types the server jar lacks (client-only vanilla classes etc.), native-image classpath only
rm -rf $D/stubsrc $D/stubs.jar $D/stubsbin; python3 -I $D/stubs.py $D/aotwork/out/classes.jar $D/stubsrc || exit 1
mkdir -p $D/stubsbin
"$GRAALVM_HOME/bin/javac" -nowarn --release 8 -d $D/stubsbin $(find $D/stubsrc -name '*.java')
(cd $D/stubsbin && "$GRAALVM_HOME/bin/jar" cf $D/stubs.jar .)
"$GRAALVM_HOME/bin/java" -Xmx8g $D/Loadable.java $D/aotwork/out/classes.jar $D/aotwork/out/resources.jar $D/unloadable.txt $D/stubs.jar || exit 1
python3 -I $D/prune.py $D/aotwork/out/classes.jar $D/unloadable.txt $D/aotwork/out/classes-ni.jar || exit 1
python3 -I $D/genconfig.py $D/aotwork/out/classes-ni.jar $D/agent-cfg $D/ni-cfg || exit 1
START=$(date +%s)
"$GRAALVM_HOME/bin/native-image" \
  -cp "$D/glue/bin:$D/aotwork/out/classes-ni.jar:$D/aotwork/out/resources.jar:$D/stubs.jar" \
  cpw.mods.fml.relauncher.ServerBoot dw20-server \
  --no-fallback -H:ConfigurationFileDirectories=$D/ni-cfg \
  --enable-url-protocols=http,https,jar \
  -H:+ReportExceptionStackTraces --report-unsupported-elements-at-runtime \
  -J-Xmx64g -J-XX:+UseParallelGC -O2 -march=compatibility "$@"
echo "native-image exit=$? after $(( $(date +%s) - START ))s"
ls -la $D/native | head -4
