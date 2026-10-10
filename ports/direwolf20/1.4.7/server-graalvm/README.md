# Direwolf20 1.4.7 server → GraalVM native image

The dedicated server of the FTB **Direwolf20 5.3.2** pack (Minecraft 1.4.7, Forge 6.6.2.534 / FML 4.7.35, 42 server-side
mods) compiled ahead of time into one native Linux executable. It is a drop-in replacement for
`java -jar server.jar nogui`: same world format, same network protocol (51), same `server.properties`, `mods/`,
`coremods/`, `lib/`, `config/` layout. `server.jar` is not needed at runtime.

Built on a remote x86-64 build host. The scripts assume the work dir `/root/dw20-server` and a JDK 8 and GraalVM layout under
`/root/build1710-tools` and `/opt/graalvm`; deliverable `dist/dw20-native/` (`./start.sh`, heap via `DW20_HEAP=3g`).

## Measured (same seed, same bot, fresh world each; 120 s of continuous flight ≈ 2,270 blocks of newly generated terrain)

| | boot to "Done" | RSS idle → after | threads | CPU: boot | CPU: 120 s flight | world data served |
|---|---|---|---|---|---|---|
| JDK 8, stock FML (baseline) | 20.6 s | 1342 → 1514 MB | 65 | 55.8 s | 89.7 s | 11.75 MB |
| closed-world jar on JDK 21 | 12.6 s | 716 → 841 MB | 70 | 55.5 s | 104.0 s | 11.77 MB |
| **native image (-O2)** | **9.1 s** | **299 → 427 MB** | **15** | **8.6 s** | **68.6 s** | **11.80 MB** |

Caveats: a 2-minute run; HotSpot's JIT cost amortises over a long uptime, so expect the CPU gap to narrow on long runs
(memory and startup advantages stay). Error/warning lines in the logs are identical to the baseline's.
Verified: all 67 mod containers activate identically; a world created by the stock JDK 8 server (with Twilight Forest,
Mystcraft, EnderStorage data) loads and serves; FML handshake + encrypted login + chunk streaming with a protocol-51 bot.
Not verified: long soak (hours), real gameplay of every mod (machines, pipes, turtles...), multiple players.

## Pipeline

```
vanilla server.jar + forge universal ──► base-server.jar
                                           │
 aot/ServerAot.java  (JDK 8, real FML relaunch chain, side=SERVER, all coremods + access transformers + SideTransformer)
 aot/Patches.java    (ASM fixes, below)
                                           ▼
        aotwork/out/classes.jar   (13,739 post-transform classes; 230 @SideOnly(CLIENT) classes dropped)
        aotwork/out/resources.jar (non-class resources + retro/origins.txt)
                                           │  + glue/ (RelaunchClassLoader stub, ModClassLoader, FMLRelauncher,
                                           │           ASMEventHandler, ServerBoot main, Enums, Origin)
                                           ▼
   Loadable.java → stubs.py → prune.py → genconfig.py → native-image (GraalVM 21.0.12, scripts/nibuild.sh)
```

`ServerBoot` does what `FMLRelauncher.relaunchServer` did (real `RelaunchLibraryManager.handleLaunch` for coremod plugin
setup, then `MinecraftServer.fmlReentry`) but with a stub class loader that registers no transformers, because every
class was already transformed at build time.

### Build-time patches (`aot/Patches.java`) and why each exists
* **Signature check** – `FMLModContainer.constructMod` read `CodeSource` certificates; a closed world has none. Mods are
  treated as having no declared fingerprint (Railcraft/IC2/Forestry otherwise abort with "Tampering detected").
* **EnumHelper** – Forge adds enum constants through JDK-internal reflection (dead on JDK 12+, impossible in an image).
  A recording run (`-Dretro.enums.record`) logs the 25 `addEnum` calls (tool/armor materials from IC2, Thaumcraft,
  Twilight Forest, Railcraft, CompactSolars, XyCraft, RedPower, Forestry); `enumExtend` bakes them into the enum
  `<clinit>`, `addEnum` becomes a by-name lookup (n-th match for repeated names), `setup()` is a no-op.
  `enum-additions.txt` is that recording; redo it if mod configs change enum arguments.
* **`(T[]) list.toArray()`** – RedPower's `WorldGenMarble` relies on JDK ≤ 8 returning a typed array from
  `Arrays.asList(..).toArray()`; rewritten to `toArray(new T[0])` (crashed world generation on JDK 9+).
* **`(RelaunchClassLoader) getClassLoader()` casts** (MiscPeripherals) → stub loader lookup.
* **`Class.getResource("x.class")` → jar** (MiscPeripherals demo programs) → `retro.glue.Origin` maps back to the jar on disk.

### Native-image specifics
* Every pack class is registered for reflection (`genconfig.py`): packet registry, entity/tile-entity factories,
  `@ForgeSubscribe` handlers (reflective `ASMEventHandler`), `@Mod` containers all instantiate/invoke reflectively.
  The GraalVM tracing agent (`scripts/agentrun.sh`, boot + player session + stop) supplies the JDK/library reflection.
* The builder aborts on enums whose signatures mention a missing class (`getEnumConstants()`), so `stubs.py` generates
  empty stubs for missing **vanilla** types only (obfuscated default-package names, `net.minecraft.*`). Mod-package
  classes stay absent on purpose: mods probe optional integrations (e.g. GregTech) with `Class.forName`.
* Client-only mods (`reiminimap`, `InvTweaks`) must not be in `mods/` – they crash a dedicated server on stock FML too.
* Built with `x86-64-v3` (AVX2). Add `-march=compatibility` to `nibuild.sh` for older CPUs.

## Contents of this folder

- `aot/`, `glue/`, `Loadable.java`, `stubs.py`, `prune.py`, `genconfig.py`, `enum-additions.txt`: the build sources.
- `scripts/`: the build, agent-run, benchmark, and test scripts.
- `pelican/egg-direwolf20-native.json`: the Pelican panel egg.

Not included: the Direwolf20 pack jars and configs, `base-server.jar`, generated class lists and native-image configs,
world data, and the native binary. Supply your own Direwolf20 5.3.2 server files to rebuild; the pack's mods are not
redistributable here.

## Re-running
`scripts/aotbuild.sh` (env `JOPTS="-Dretro.enums.file=..."`), `scripts/buildglue.sh`, `scripts/agentrun.sh`,
`scripts/nibuild.sh`, `scripts/runnative.sh <dir>`, `scripts/bench.sh base|aot|native <seconds>`.
Test tools: `scripts/Bot.java` (protocol-51 client with FML handshake; `java -cp guava.jar Bot.java host port name secs fmlLog [serverLog]`),
`scripts/ping.py`. On a shared build host, never kill by name; `scripts/dwtools.sh` has `mine_pids <cwd>`.
