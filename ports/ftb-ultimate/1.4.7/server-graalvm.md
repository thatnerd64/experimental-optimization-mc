# FTB Ultimate 1.4.7 — server, native (GraalVM)

| | |
|---|---|
| Modpack | Original FTB Ultimate 1.1.2 (92 loaded mod containers) |
| Minecraft / loader | 1.4.7 / Forge 6.6.2.534 |
| Target | dedicated server, Linux x86_64 |
| Engine | Oracle GraalVM 21.0.12 native-image, `-O2`, `-march=compatibility` |
| Status | Working |
| Source | This repo, branch `main`, [`server-graalvm/`](server-graalvm/) |
| Live | none |

## What it is

The original Ultimate dedicated server compiled into a native executable, adapted
from the Direwolf20 1.4.7 port. No Java installation or Forge server jar is required
at runtime. Mod jars remain on disk for Forge discovery and resources; changing
the mod set requires rebuilding.

## Build

Supply your own Ultimate 1.1.2 server inputs, JDK 8, GraalVM 21 native-image, GCC,
and Python 3.11 or later. Build instructions and scripts are in
[`server-graalvm/README.md`](server-graalvm/README.md). The runtime bundle launches
with `./start.sh`; set `ULTIMATE_HEAP` to change its default 4 GB heap limit.

## Status and measurements

All 92 mod containers load on the stock, transformed, and native servers.
Verified server-list ping, encrypted Forge login, chunk streaming, saving and
clean shutdown, plus loading a world saved by the stock Java 8 server and running
the packaged launcher after relocation.

A 120-second native flight covered approximately 2,310 blocks and received
7.3 MB of server data. Fresh native startup was approximately 16 seconds with
298 MiB RSS and 16 threads at startup. These are observations with a 2-second
startup polling interval, not a controlled performance comparison.

## Known gaps

- Exhaustive per-mod gameplay, multiple players and multi-hour operation are untested.
- Tests used loopback and `online-mode=false`; online account authentication is untested.
- A caught legacy Mystcraft/NEI client API error occurs during startup. Native-image
  also reports reflection warnings for client types and several inner classes.
- The reference port's enum, class-loader, signature-check and typed-array
  adaptations remain necessary. Enum-affecting config changes require rebuilding.

## Related

- [Validation details](server-graalvm/VALIDATION.md).
- [Runtime instructions](server-graalvm/RUNTIME.md).
- [Direwolf20 1.4.7 native port](../../direwolf20/1.4.7/server-graalvm.md).
