#!/bin/bash
set -euo pipefail
D=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
: "${JDK8_HOME:?Set JDK8_HOME to a JDK 8 installation}"
mkdir -p "$D/glue/bin"
"$JDK8_HOME/bin/javac" -nowarn -source 1.7 -target 1.7 \
  -cp "$D/aotwork/${OUT:-out}/classes.jar" -d "$D/glue/bin" $(find "$D/glue/src" -name '*.java')
