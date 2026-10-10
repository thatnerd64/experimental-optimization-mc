# Direwolf20 1.4.7 native server (reference port)

Minecraft 1.4.7, Forge 6.6.2.534 (FML 4.7.35), Linux x86_64. FTB Direwolf20 5.3.2, 42 server-side mods.
This is the reference implementation; the
[FTB Ultimate port](https://github.com/thatnerd64/experimental-optimization-mc/tree/main/ports/ftb-ultimate/1.4.7/server-graalvm)
is derived from it.

The server is a drop-in replacement for `java -jar server.jar nogui`: same world format, protocol 51 and
`server.properties`, `mods/`, `coremods/`, `lib/`, `config/` layout. `server.jar` is not needed at runtime.

Run commands below from this source directory. Scripts locate the workspace relative to their own location and use
`JDK8_HOME` and `GRAALVM_HOME` for tools.

A precompiled Linux x86_64 bundle and checksum are attached to the
[GitHub Release](https://github.com/thatnerd64/experimental-optimization-mc/releases/tag/direwolf20-5.3.2-graalvm-v1).
See the [port card](../server-graalvm.md#download-and-run) for installation and
[`RUNTIME.md`](RUNTIME.md) for launch details.

## Inputs and tools

- Direwolf20 5.3.2 `mods/`, `coremods/`, `lib/`, `config/` (the pack's `overrides`, plus the five projects its manifest
  lists, which the pack zip does not contain).
- `base-server.jar`: the vanilla 1.4.7 `server.jar` merged with the Forge 6.6.2.534 universal zip, with `META-INF` removed.
- JDK 8: runs the real FML transformers and the enum recording.
- Oracle GraalVM 21.0.12 with native-image; GCC.
- Python 3.11 or later, tar, shell.

The jars and the original pack archive are private build inputs, not part of this source tree. Runtime mod jars stay
necessary for Forge discovery and mod resources, even though the executable code is compiled into the native image.

## Build sequence

Prepare `base/` from the pack: `mods/`, `coremods/`, `lib/`, `config/`, and a test `server.properties` bound to
`127.0.0.1:25591` with `online-mode=false` and `allow-flight=true`. Client-only mods (Rei's Minimap, Inventory Tweaks)
must not be in `base/mods`; they crash a dedicated server on stock FML too.

```sh
export JDK8_HOME=/path/to/jdk8
export GRAALVM_HOME=/path/to/graalvm
mkdir -p aotwork/game
cp -a base/mods base/coremods base/lib base/config aotwork/game/
python3 runstage.py base
OUT=out-rec JOPTS=-Dretro.enums.record=true bash scripts/aotbuild.sh
OUT=out-rec bash scripts/buildglue.sh
# Keep the supplied recording for reference; EnumLog appends to its output.
mv enum-additions.txt enum-additions.reference.txt
python3 runstage.py record
JOPTS="-Dretro.enums.file=$PWD/enum-additions.txt" bash scripts/aotbuild.sh
bash scripts/buildglue.sh
python3 runstage.py agent
bash scripts/nibuild.sh
python3 runstage.py native --flight --seconds 120
python3 runstage.py native --name native-stock-world --world-from checks/base/world
python3 package.py
python3 runstage.py native --name packaged --bundle dist/dw20-native --seconds 10
```

`runstage.py` intentionally refuses to reuse a test directory; pass `--name` with a new name to repeat a check. Each run
shuts down its own process after testing; logs and test worlds remain under `checks/`.
`scripts/bench.sh base|aot|native [seconds]` repeats the same flight against the stock server, the transformed jar on
GraalVM's JVM, and the native binary, and prints boot time, memory, threads and CPU.

`scripts/nibuild.sh` selects `-O2 -march=compatibility`, uses a 64 GB builder heap and stops on tool failures.
`package.py` creates `dist/dw20-native/` plus `dw20-native-linux-x86_64.tar.gz` and its SHA-256 checksum.
`jar-policy.properties` permits the legacy SHA-1 jar signatures that current Java 8 treats as unsigned; it is passed only
to the build and test JVMs. The pack jars, native executable, generated metadata and test worlds are not distributed
in this source repository.

## How it works

```
base-server.jar + pack jars
        │  aot/ServerAot.java   JDK 8, the real FML relaunch chain with side=SERVER: coremods, access transformers,
        │  aot/Patches.java     @SideOnly stripping. Dumps every class AFTER transformation; ASM fixes below.
        ▼
aotwork/out/classes.jar    post-transform classes (the 230 @SideOnly(CLIENT) ones are dropped)
aotwork/out/resources.jar  non-class resources + retro/origins.txt
        │  + glue/         stub RelaunchClassLoader, ModClassLoader, FMLRelauncher, reflective ASMEventHandler,
        │                  Enums, Origin, and the ServerBoot entry point
        ▼
Loadable.java → stubs.py → prune.py → genconfig.py → native-image
```

`ServerBoot` does what `FMLRelauncher.relaunchServer` did: the real `RelaunchLibraryManager.handleLaunch` for coremod
plugin setup, then `MinecraftServer.fmlReentry`. The class loader is a stub that registers no transformers, because every
class was transformed at build time.

### Build-time patches (`aot/Patches.java`)

- **Signature check.** `FMLModContainer.constructMod` read `CodeSource` certificates, which a closed world does not have.
  Mods are treated as having no declared fingerprint (Railcraft, IC2 and Forestry otherwise abort with
  "Tampering detected").
- **EnumHelper.** Forge adds enum constants through JDK-internal reflection, which is impossible in an image. A recording
  run (`-Dretro.enums.record`) logs the `addEnum` calls (25 for this pack: tool and armor materials); `enumExtend` bakes
  them into the enum initializer, `addEnum` becomes a by-name lookup (the n-th match for repeated names) and `setup()`
  is a no-op. `enum-additions.txt` is that recording; redo it if mod configs change enum arguments.
- **`(T[]) list.toArray()`.** RedPower's `WorldGenMarble` relies on Java 8 returning a typed array from
  `Arrays.asList(..).toArray()`; it is rewritten to `toArray(new T[0])`. It crashed world generation on Java 9 and later.
- **`(RelaunchClassLoader) getClassLoader()` casts** (MiscPeripherals) are routed to the stub loader.
- **`Class.getResource("x.class")` to a jar** (MiscPeripherals demo programs) is mapped back to the jar on disk by
  `retro.glue.Origin`.

### Native-image specifics

- Every pack class is registered for reflection (`genconfig.py`): the packet registry, entity and tile-entity factories,
  `@ForgeSubscribe` handlers and `@Mod` containers all instantiate or invoke reflectively. The GraalVM tracing agent
  (`runstage.py agent`, a boot plus a bot session plus a stop) supplies the JDK and library reflection.
- The builder aborts on enums whose signatures mention a missing class (`getEnumConstants()`), so `stubs.py` generates
  empty stubs for missing **vanilla** types only (obfuscated default-package names and `net.minecraft.*`). Mod-package
  classes stay absent on purpose: mods probe optional integrations (GregTech, for instance) with `Class.forName`.
- Empty stubs are used only for native-image analysis; no optional mod integration is replaced with fake mod classes.

## Test tools

- `scripts/Bot.java`: a protocol-51 client with the FML handshake (a fake `Packet1Login` carrying murmur3("FML"), then
  the mod-list response). With a server log as its sixth argument it echoes the server's spawn teleport and flies
  along +x at 20 blocks/s, which needs `allow-flight=true`.
- `scripts/ping.py`: the 0xFE 0x01 list ping.
- `scripts/hazards.py`, `jarself.py`, `surface.py`: scans of the pack's jars for runtime code generation, jar
  self-introspection and the amount of Minecraft/Forge API the mods use.

## Validation scope

The checks verify startup, the Forge mod handshake, RSA/AES login, chunk streaming, server ticking, world saving and
shutdown. The flight check generates terrain; the stock-world check uses the world produced by the Java 8 baseline.
They do not exercise every machine, recipe, dimension, turtle program or other mod feature. A compiled image must not be
presented as an exhaustive gameplay compatibility guarantee. See [`VALIDATION.md`](VALIDATION.md).
