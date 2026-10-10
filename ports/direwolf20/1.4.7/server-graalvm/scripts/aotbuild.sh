#!/bin/bash
# rebuild the AOT tool and re-dump classes.jar
export JAVA_HOME=$(ls -d /root/build1710-tools/jdk8u504*); export PATH=$JAVA_HOME/bin:$PATH
cd /root/dw20-server
CP=base-server.jar:$(ls aotwork/game/lib/*.jar | tr "\n" ":")
OUT=${OUT:-out}; rm -rf aotwork/bin/* aotwork/$OUT
javac -nowarn -source 1.6 -target 1.6 -cp "$CP" -d aotwork/bin ServerAot.java Patches.java 2>&1 | grep -v "^warning\|^Note" | head -20
cd aotwork; rm -rf $OUT
timeout 280 java -Djava.security.properties=/root/dw20-server/jar-policy.properties $JOPTS -cp "../base-server.jar:$(ls game/lib/*.jar | tr "\n" ":")bin" retro.aot.ServerAot game ../base-server.jar $OUT > aot.log 2>&1
echo exit=$?; grep -c "transformer error" aot.log; tail -1 aot.log | cut -c1-300
