# Direwolf20 1.4.7 — GraalVM native server

Minecraft 1.4.7 / Forge 6.6.2.534. Linux x86_64.

Run `./start.sh`. No Java installation or Forge server jar is required.
Set the heap with `DW20_HEAP=2g ./start.sh` (3 GB is the default; the native image idles around 300 MB).
The initial configuration listens on port 25565 and enables online authentication.
Edit `server.properties` before starting if different settings are needed. Servers for 1.4.7 clients are usually run with
`online-mode=false` for testing; treat that setting as insecure on a public address.

Keep the bundled `mods`, `coremods`, `lib`, `config` directories and `.so` files.
The mod jars remain necessary for Forge's discovery and mod resources; executable Java classes are compiled into
`dw20-server`. Adding or changing mods requires a new native build. Config changes that affect enum creation
(tool and armor materials) also require rebuilding.

Client-only mods (Rei's Minimap, Inventory Tweaks) are excluded from the dedicated server set.

The port retains the project's enum, class-loader, typed-array and mod-certificate adaptations. No long-duration or
exhaustive per-mod gameplay testing has been performed. Back up existing worlds before using an experimental server.

The native compiler reports reflection warnings for unavailable client classes. The server logs the same caught
version-check and integration messages as the stock server (for example the Buildcraft and Applied Energistics
version lookups, which need internet access).
