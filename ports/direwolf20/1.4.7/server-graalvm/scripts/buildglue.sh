#!/bin/bash
export JAVA_HOME=$(ls -d /root/build1710-tools/jdk8u504*); export PATH=$JAVA_HOME/bin:$PATH
cd /root/dw20-server/glue; rm -rf bin && mkdir bin && javac -nowarn -source 1.7 -target 1.7 -cp ../aotwork/out/classes.jar -d bin $(find src -name "*.java") 2>&1 | grep -v "^warning\|^Note" | head -20
