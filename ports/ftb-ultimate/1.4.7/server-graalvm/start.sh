#!/bin/sh
set -eu
cd "$(dirname "$0")"
exec ./ultimate-server "-Xmx${ULTIMATE_HEAP:-4g}" nogui "$@"
