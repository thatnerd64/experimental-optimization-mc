#!/bin/bash
set -euo pipefail
D=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
: "${JDK8_HOME:?Set JDK8_HOME to a JDK 8 installation}"
JAVA_HOME=$JDK8_HOME
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
cd "$D"
CP="base-server.jar:$(find aotwork/game/lib -name '*.jar' -printf '%p:')"
OUT=${OUT:-out}
mkdir -p aotwork/bin "aotwork/$OUT"
javac -nowarn -source 1.6 -target 1.6 -cp "$CP" -d aotwork/bin aot/ServerAot.java aot/Patches.java
cd aotwork
java -Djava.security.properties="$D/jar-policy.properties" ${JOPTS:-} \
  -cp "../base-server.jar:$(find game/lib -name '*.jar' -printf '%p:')bin" \
  retro.aot.ServerAot game ../base-server.jar "$OUT" > aot.log 2>&1
tail -1 aot.log
! grep -q 'transformer error on' aot.log
