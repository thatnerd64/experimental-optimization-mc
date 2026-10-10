# Direwolf20 1.4.7 — server, native (GraalVM)

| | |
|---|---|
| Modpack | Direwolf20 5.3.2 (42 server-side mods, 67 loaded mod containers) |
| Minecraft / loader | 1.4.7 / Forge 6.6.2.534 (FML 4.7.35) |
| Target | dedicated server, Linux x86_64 |
| Engine | Oracle GraalVM 21.0.12 native-image, `-O2`, `-march=compatibility` |
| Status | Working, deployed |
| Source | This repo, branch `main`, [`server-graalvm/`](server-graalvm/) (sources, scripts, Pelican egg) |
| Bundle | [Linux x86_64 native runtime](https://github.com/thatnerd64/experimental-optimization-mc/releases/tag/direwolf20-5.3.2-graalvm-v1) |
| Live | graalvm.baconmc.net (Pelican egg "Direwolf20 1.4.7 (GraalVM native)") |

## What it is

A drop-in replacement for `java -jar server.jar nogui`. It uses the same world format, protocol 51, and
`server.properties`, `mods/`, `coremods/`, `lib/` and `config/` layout. No Java installation or Forge server jar is
required at runtime. Mod jars remain on disk for Forge discovery and resources; changing the mod set requires rebuilding.
This is the reference port; the [FTB Ultimate port](../../ftb-ultimate/1.4.7/server-graalvm.md) is derived from it.

How it is made:

1. `ServerAot` runs the real FML relaunch chain with side=SERVER on JDK 8 and dumps every class after Forge's
   transformers. 230 `@SideOnly(CLIENT)` classes are dropped.
2. `glue/` replaces the class loaders and the FML entry point, and adds a `ServerBoot` main.
3. An agent run on GraalVM, including a bot session, records reflection use.
4. `genconfig.py` registers all pack classes and the agent output for native-image.
5. `native-image` builds the binary (about 517 MB).

## Download and run

Download the [compiled bundle](https://github.com/thatnerd64/experimental-optimization-mc/releases/download/direwolf20-5.3.2-graalvm-v1/dw20-native-linux-x86_64.tar.gz)
and [SHA-256 checksum](https://github.com/thatnerd64/experimental-optimization-mc/releases/download/direwolf20-5.3.2-graalvm-v1/dw20-native-linux-x86_64.tar.gz.sha256), then:

```sh
sha256sum -c dw20-native-linux-x86_64.tar.gz.sha256
tar -xzf dw20-native-linux-x86_64.tar.gz
cd dw20-native
./start.sh
```

Keep the bundled directories and shared libraries. The runtime defaults to port 25565, `online-mode=true` and a 3 GB
heap limit; set `DW20_HEAP` to change it (the server idles around 300 MB). Read the
[runtime instructions](server-graalvm/RUNTIME.md) and [validation limits](server-graalvm/VALIDATION.md) before using an
existing world.

## Build

Supply your own Direwolf20 5.3.2 server inputs, JDK 8, GraalVM 21 native-image, GCC and Python 3.11 or later.
Build instructions and scripts are in [`server-graalvm/README.md`](server-graalvm/README.md). The runtime bundle launches
with `./start.sh`.

## Status and measurements

All 67 mod containers load on the stock, transformed and native servers. Verified server-list ping, encrypted Forge
login, chunk streaming, saving and clean shutdown, plus loading a world saved by the stock Java 8 server and running the
packaged launcher after relocation. Deployed on a Pelican panel with a 2 GB limit: about 310–330 MB in use, "Done" in
about 15 s.

120 seconds of continuous flight by a test bot (about 2,270 blocks of newly generated terrain), same seed, fresh world each
run, measured with `server-graalvm/scripts/bench.sh`. Ranges are repeated runs; all runs served 11–12 MB of world data.

| | Stock JDK 8 server | Transformed jar, GraalVM JVM | Native image |
|---|---|---|---|
| Boot to "Done" | 19.1–21.1 s | 11.6 s | 8.6 s |
| RSS, idle → after flight | 1.32–1.34 GB → 1.50–1.52 GB | 608 MB → 831 MB | 281–288 MB → 372–383 MB |
| Threads | 65–66 | 70 | 15 |
| CPU time during boot | 54–59 s | 51 s | 8 s |
| CPU time during the flight | 90–93 s | 102 s | 68–70 s |

Error and warning lines match the stock server. These are two-minute runs on one machine. HotSpot's JIT cost amortises over
a long uptime, so the CPU gap will probably narrow; the memory and startup advantages should not.

## Known gaps

- Exhaustive per-mod gameplay, multiple players and multi-hour operation are untested.
- Tests used loopback and `online-mode=false`; online account authentication is untested.
- The test bot's flight occasionally fails to start (a first-move race in the harness, not the server), so the benchmark
  retries until the server actually streams terrain.
- Client-only mods must stay out of the server set. A few missing client types need stubs for native-image analysis only.
- Several Forge and mod workarounds are required: Forge `EnumHelper` constants are baked into the enum initializer,
  anti-tamper signature checks are bypassed, and some Java-8-only behaviour is patched.
- Hostname routing for 1.4.7 clients in front of a shared port needs a legacy-aware proxy rule, since the usual
  Minecraft routers read only the 1.7+ handshake.

## Related

- [Build and run instructions](server-graalvm/README.md), [runtime instructions](server-graalvm/RUNTIME.md),
  [validation details](server-graalvm/VALIDATION.md).
- [FTB Ultimate 1.4.7 native port](../../ftb-ultimate/1.4.7/server-graalvm.md), derived from this one.
