# Validation — FTB Ultimate 1.1.2 native server

Built on a Linux x86_64 build host with Oracle GraalVM 21.0.12, `-O2`,
`-march=compatibility`. Native-image completed successfully in 4m 42s
(286 seconds including preparation in the wrapper).

Inputs: original Ultimate 1.1.2, Minecraft 1.4.7, Forge 6.6.2.534.
The installation's mods and configs were copied; no installed client files
were changed. The server set excludes Inventory Tweaks, Voxel client files
and Vanilla NEI Fix. The AOT pass processed 14,976 classes and stripped
256 client-only classes, with zero reported transformer errors. A recording
startup captured 22 enum additions specific to this pack.

## Completed checks

- Stock Java 8 server: 92 mod containers loaded, fresh world generation,
  server-list ping, encrypted Forge login and 30-second client session,
  world save and clean shutdown.
- Transformed Java 8 recording run: startup, encrypted login, world save,
  clean shutdown.
- Transformed Java 21 under the GraalVM metadata agent: all 92 containers,
  encrypted login, client session, world save and clean shutdown.
- Native executable: all 92 containers, fresh world, ping, encrypted login,
  120-second flight. X moved from 214.5 to approximately 2524; 7,330,519 bytes
  received. World data grew to 9.4 MB on disk after saving. Clean exit code 0.
- Native executable with the world saved by the stock Java 8 server: all
  dimensions loaded, ping, encrypted Forge login, 30-second session,
  save and clean exit code 0. Startup detection took approximately 10 seconds.
- Packaged runtime copied to a separate directory and launched with its
  `start.sh`: startup, ping, encrypted Forge login, 10-second client session,
  saving and clean exit code 0. No Forge server jar was included in that runtime.

Fresh native boot to detection of "Done": approximately 16 seconds (2-second
polling interval). At that point `/proc` reported 304,844 kB RSS, about 298 MiB,
and 16 threads. These are observations, not a controlled performance benchmark.

## Limits and warnings

The legacy Mystcraft/NEI plugin logs a caught client API error in both the
stock and native server, with different underlying missing class/method
messages. Both continue loading. GraalVM reports reflection metadata warnings
for missing client classes and several inner classes, including GregTech's
centrifuge internal tank. MiscPeripherals' transformer also emits caught
mod-loading warnings during the AOT pass. These have not prevented the checks
above, but exhaustive mod gameplay and optional reflection paths are untested.

Loopback tests use `online-mode=false`; encryption and the Forge handshake
are still exercised. Online account authentication, real-client gameplay,
multiple simultaneous players and multi-hour operation are untested.
The distributable defaults to `online-mode=true` and port 25565.

Build and test logs were retained in the private build workspace and its
`checks/` directory; they are not included in this source repository.
