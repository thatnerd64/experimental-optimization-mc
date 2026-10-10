# experimental-optimization-mc

Index of optimized server ports of popular Minecraft modpacks: the same pack's
dedicated server rebuilt on another runtime for lower memory use and faster boot.

Each port has a card with its status and build notes, and its source is kept in this repo
alongside the card. The Git tree contains no game assets, server packs, world data,
or credentials. Compiled bundles, where available, are attached to GitHub Releases
and linked from the port cards.

## Quick start

Linux x86_64 only. The release bundles already contain the pack's server-side mods and configs, so nothing else
needs to be supplied. One command installs a pack (or updates an existing install and keeps its worlds,
`server.properties` and `config/`):

```sh
curl -fsSL https://raw.githubusercontent.com/thatnerd64/experimental-optimization-mc/main/install.sh | sh -s -- direwolf20
cd direwolf20-server && ./start.sh
```

Use `ftb-ultimate` instead of `direwolf20` for the other pack, and add a directory after the pack name to install
somewhere else. Stop the server before updating it.

For a Pelican panel, import [`pelican/egg-graalvm-native-1.4.7.json`](pelican/egg-graalvm-native-1.4.7.json) and pick
the pack with the Modpack variable; its install script runs the same `install.sh`.

## Ports

| Modpack | Minecraft / loader | Engine | Status | Card |
|---|---|---|---|---|
| Direwolf20 | 1.4.7 / Forge 6.6.2 | GraalVM native image | Working, deployed | [card](ports/direwolf20/1.4.7/server-graalvm.md) · [source](ports/direwolf20/1.4.7/server-graalvm/) · [download](https://github.com/thatnerd64/experimental-optimization-mc/releases/tag/direwolf20-5.3.2-graalvm-v1) |
| FTB Ultimate 1.1.2 | 1.4.7 / Forge 6.6.2 | GraalVM native image | Working | [card](ports/ftb-ultimate/1.4.7/server-graalvm.md) · [source](ports/ftb-ultimate/1.4.7/server-graalvm/) · [download](https://github.com/thatnerd64/experimental-optimization-mc/releases/tag/ftb-ultimate-1.1.2-graalvm-v1) |

## Layout

```
ports/
  <modpack-slug>/
    <mc-version>/
      server-<engine>.md     one card per server port
      server-<engine>/       that port's sources, build scripts and docs
templates/
  port.md                    copy this for a new card
install.sh                   downloads and installs a port's release bundle
pelican/                     one Pelican egg for every port
```

- `<modpack-slug>`: lowercase, hyphenated pack name (`direwolf20`).
- `<mc-version>`: the Minecraft version the port runs (`1.4.7`).
- `<engine>`: the runtime (`graalvm`, ...).

## Status values

- **Working, deployed**: runs and is deployed, but not in active use.
- **Working**: runs end to end, not necessarily deployed.
- **Experimental**: runs in part; known blockers.
- **In progress**: code exists and is being built; not verified end to end.

## Adding a port

1. Copy `templates/port.md` to `ports/<modpack>/<mc-version>/server-<engine>.md`.
2. Put the port's sources in `ports/<modpack>/<mc-version>/server-<engine>/` and fill in the
   table at the top of the card.
3. Attach any compiled bundle to a GitHub Release and link it from the card. Add the pack to
   `install.sh` and to the Modpack variable's rule in the Pelican egg.
4. Add the row to the Ports table above.
5. Never commit game assets, server packs, world saves, account data, IP addresses,
   hostnames of private machines, or credentials.
