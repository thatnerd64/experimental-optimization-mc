# FTB Ultimate 1.1.2 — GraalVM native server

Minecraft 1.4.7 / Forge 6.6.2.534. Linux x86_64.

Run `./start.sh`. No Java installation or Forge server jar is required.
Set the heap with `ULTIMATE_HEAP=4g ./start.sh` (4 GB is the default).
The initial configuration listens on port 25565 and enables online authentication.
Edit `server.properties` before starting if different settings are needed.

Keep the bundled `mods`, `coremods`, `lib`, `config` directories and `.so` files.
The mod jars remain necessary for Forge's discovery and mod resources; executable
Java classes are compiled into `ultimate-server`. Adding or changing mods requires
a new native build. Config changes that affect enum creation also require rebuilding.

This is an experimental port adapted from
https://github.com/thatnerd64/experimental-optimization-mc/tree/main/ports/direwolf20/1.4.7/server-graalvm
using the installed original Ultimate pack. Client-only Voxel mods, Inventory Tweaks,
and Vanilla NEI Fix are excluded from the dedicated server.

The port retains the reference project's enum, class-loader, typed-array and mod
certificate adaptations. No long-duration or exhaustive per-mod gameplay testing
has been performed. Back up existing worlds before using an experimental server.

The stock server logs a caught exception during a legacy NEI integration's
initialization. The native server logs a corresponding caught NEI method error;
both proceed to load all 92 mod containers. The native compiler also reports
reflection warnings for unavailable client classes and some inner classes.
The validation results cover startup and protocol/world checks, not every mod's
reflection paths. Online account authentication was not part of the loopback tests.
