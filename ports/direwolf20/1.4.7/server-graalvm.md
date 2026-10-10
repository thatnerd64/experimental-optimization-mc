# Direwolf20 1.4.7 — server, native (GraalVM)

| | |
|---|---|
| Modpack | Direwolf20 5.3.2 (42 server-side mods) |
| Minecraft / loader | 1.4.7 / Forge 6.6.2.534 (FML 4.7.35) |
| Target | dedicated server, native Linux x86_64 |
| Engine | GraalVM 21.0.12 native-image, `-O2`, x86-64-v3 |
| Status | Working, deployed |
| Source | This repo, [`server-graalvm/`](server-graalvm/) (sources, scripts, Pelican egg) |
| Live | graalvm.baconmc.net (Pelican egg 5, "Direwolf20 1.4.7 (GraalVM native)") |

## What it is

A drop-in replacement for `java -jar server.jar nogui`. It uses the same world format,
protocol 51, and `server.properties`, `mods/`, `coremods/`, `lib/`, and `config/`
layout. `server.jar` is not needed at runtime.

How it is made:

1. `ServerAot` runs the real FML relaunch chain with side=SERVER on JDK 8 and dumps
   every class after Forge's transformers. 230 `@SideOnly(CLIENT)` classes are dropped.
2. `glue/` replaces the class loaders and the FML entry point, and adds a `ServerBoot`
   main.
3. An agent run on GraalVM, including a bot session, records reflection use.
4. `genconfig.py` registers all pack classes and the agent output for native-image.
5. `native-image` builds the binary (517 MB).

## Build

Built on a remote build host. The output directory has `start.sh` and the binary.
The heap is set with `DW20_HEAP` (for example `3g`).

## Measurements

120 s of continuous flight by a test bot, same seed, fresh world each run:

| | Stock JDK 8 server | Native image |
|---|---|---|
| Boot to "Done" | 20.6 s | 9.1 s |
| RSS | 1514 MB | 427 MB |
| Threads | 65 | 15 |
| CPU time | 89.7 s | 68.6 s |

Error lines match the stock server, and the native server loads a world made by the stock one.
Deployed on a Pelican panel with a 2 GB limit: about 310–330 MB in use, "Done" in ~15 s.

## Known gaps

- Multi-hour soak test not done.
- Per-mod gameplay, multiple players, and long-uptime CPU are not measured.
- Client-only mods must stay out of the server set. A few mod types need stubs for
  native-image.
- Several Forge and mod workarounds are required: Forge `EnumHelper` constants are
  baked into the enum initializer, anti-tamper signature checks are bypassed, and
  some JDK-8-only behavior is patched.

## Related

- Build and run instructions: [`server-graalvm/README.md`](server-graalvm/README.md).
