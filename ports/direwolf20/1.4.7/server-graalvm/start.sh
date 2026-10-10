#!/bin/sh
set -eu
cd "$(dirname "$0")"
exec ./dw20-server "-Xmx${DW20_HEAP:-3g}" nogui "$@"
