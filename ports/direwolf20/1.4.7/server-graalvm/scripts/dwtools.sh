#!/bin/bash
# helpers: mine_pids <dir> prints pids of processes whose cwd is <dir>
mine_pids() { for p in /proc/[0-9]*; do [ "$(readlink $p/cwd 2>/dev/null)" = "$1" ] && echo ${p#/proc/}; done; }
