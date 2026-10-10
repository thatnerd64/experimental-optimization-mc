# Original FTB Ultimate 1.1.2 native server

Minecraft 1.4.7, Forge 6.6.2.534, Linux x86_64. Derived from the
[Direwolf20 native port](https://github.com/thatnerd64/experimental-optimization-mc/tree/main/ports/direwolf20/1.4.7/server-graalvm).

The build uses a copied pack; keep the installed client instance intact.
Run commands below from this source directory. Scripts locate the workspace
relative to their own location and use `JDK8_HOME` and `GRAALVM_HOME` for tools.

A precompiled Linux x86_64 bundle and checksum are available in the
[GitHub Release](https://github.com/thatnerd64/experimental-optimization-mc/releases/tag/ftb-ultimate-1.1.2-graalvm-v1).
See the [port card](../server-graalvm.md#download-and-run) for installation, or use the repo's
[`install.sh`](../../../../install.sh) or [shared Pelican egg](../../../../pelican/egg-graalvm-native-1.4.7.json).

## Inputs and tools

- Original FTB Ultimate 1.1.2 `mods/`, `coremods/`, `lib/`, `config/`.
- `base-server.jar`: vanilla 1.4.7 server merged with Forge 6.6.2.534,
  reused from the reference build because the Minecraft and Forge versions match.
- JDK 8: run the real FML transformers and enum recording.
- Oracle GraalVM 21.0.12 with native-image; GCC.
- Python 3.11 or later, tar, shell.

The jars and original pack archive are private build inputs, not part of the
port's source code. Runtime mod jars remain necessary for Forge discovery and
mod resources, even though executable code is compiled into the native image.

## Build sequence

Prepare `base/` from the installed pack. Move Inventory Tweaks, Voxel mods,
and `VanillaNeiFix_mc147_1.0.jar` to `removed-client-only/`. Copy the four base
directories to `aotwork/game/`. Use a test `base/server.properties` bound to
`127.0.0.1:25591`, `online-mode=false`, `allow-flight=true`.

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
python3 runstage.py native --name packaged --bundle dist/ultimate-native --seconds 10
```

`runstage.py` intentionally refuses to reuse a test directory. Pass `--name`
with a new name to repeat a check. Each run shuts down its own process after
testing; logs and test worlds remain under `checks/`.

The native build transforms all jars with the real FML server transformer chain,
bakes the pack's recorded enum additions, and uses the reference loader and
reflection adaptations. It retains the reference source's known limitations;
see the [reference port's build notes](../../../direwolf20/1.4.7/server-graalvm/README.md).
Optional client classes may be unreflectable on a
server. Empty missing-vanilla stubs are used only for native-image analysis;
optional mod integrations are not replaced with fake mod classes.

`scripts/nibuild.sh` selects `-O2 -march=compatibility`, uses a 64 GB builder heap,
and stops on tool failures. `package.py` creates `dist/ultimate-native/` plus
`ultimate-native-linux-x86_64.tar.gz` and its SHA-256 checksum. Production defaults
enable online authentication and port 25565. See `RUNTIME.md` for launch details.
The included `jar-policy.properties` permits the legacy signatures needed by
Java 8 for these old jars; it is passed only to the build and test JVMs.
Neither the pack jars, native executable, generated metadata nor test worlds
are distributed in this source repository.

## Validation scope

The checks verify startup, the Forge mod handshake, RSA/AES login, chunk streaming,
server ticking, world saving and shutdown. The flight check generates terrain;
the stock-world check uses the world produced by the Java 8 baseline.
They do not exercise every machine, recipe, dimension, turtle program or other
mod feature. A compiled image must not be presented as an exhaustive gameplay
compatibility guarantee.
Tests use a loopback server with `online-mode=false`; the protocol still uses
encrypted transport. Online account authentication is not validated by these tests.
