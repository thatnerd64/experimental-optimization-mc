# Validation — Direwolf20 1.4.7 native server

Built on a Linux x86_64 build host with Oracle GraalVM 21.0.12, `-O2`, `-march=compatibility`.
Native-image completed in 4m 18s, running the sequence in [`README.md`](README.md) from a clean workspace.

Inputs: Direwolf20 5.3.2, Minecraft 1.4.7, Forge 6.6.2.534. The server set is 42 mod jars (67 mod containers); Rei's
Minimap and Inventory Tweaks are excluded because they are client-only. The AOT pass processed 13,969 classes and stripped
230 client-only classes, with zero reported transformer errors. The enum recording captured 25 additions, identical to the
committed `enum-additions.txt`.

## Completed checks

- Stock Java 8 server (`runstage.py base`): all 67 mod containers, fresh world generation, server-list ping, encrypted
  Forge login, 30-second client session, world save and clean shutdown.
- Transformed Java 8 recording run: startup, encrypted login, world save, clean shutdown.
- Transformed Java 21 under the GraalVM metadata agent: all containers, encrypted login, client session, world save and
  clean shutdown.
- Native executable: all containers, fresh world, ping, encrypted Forge login, 120-second flight. X moved from the spawn
  position to 2280; 11,760,836 bytes were received. Clean exit code 0.
- Native executable with the world saved by the stock Java 8 server (including its Twilight Forest dimension, Mystcraft
  and Ender Storage data): all dimensions loaded, ping, encrypted Forge login, session, save and clean exit code 0.
- Packaged runtime copied to a separate directory and launched with its `start.sh`: startup, ping, encrypted Forge login,
  10-second client session, saving and clean exit code 0. No Forge server jar was included in that runtime.
- Deployed on a Pelican panel (2 GB memory limit): about 310–330 MB in use and "Done" in about 15 seconds.

Boot detection used a 2-second polling interval. At "Done" the stock server reported about 1.5 GB RSS and 65 threads; the
native server about 270 MB and 15 threads. For a controlled comparison with CPU time, see the measurements table on the
[port card](../server-graalvm.md#status-and-measurements), produced with `scripts/bench.sh`.

## Limits and warnings

The native compiler reports reflection metadata warnings for missing client classes and several inner classes.
Mod version checks that need internet access (Buildcraft, Applied Energistics) log the same failures as the stock server.
MiscPeripherals' transformer logs caught mod-loading messages during the AOT pass. None of these prevented the checks
above, but exhaustive mod gameplay and optional reflection paths are untested.

Loopback tests use `online-mode=false`; encryption and the Forge handshake are still exercised. Online account
authentication, real-client gameplay, multiple simultaneous players and multi-hour operation are untested. A two-minute
run does not show how HotSpot's JIT cost amortises over a long uptime, so the CPU advantage is likely to shrink with
uptime while the memory and startup advantages remain. The distributable defaults to `online-mode=true` and port 25565.

Build and test logs were retained in the private build workspace and its `checks/` directory; they are not included in
this source repository.
