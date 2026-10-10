# experimental-optimization-mc

Index of optimized and ported versions of popular Minecraft modpacks: the same pack
moved to another engine (TeaVM, GraalVM, CheerpJ, WebAssembly), another Minecraft
version, or a faster server build.

This repo holds the catalog only. Each port's code lives in its own home repo or
working tree, linked from its card. The repo contains no game assets, server packs,
world data, or credentials.

## Ports

| Modpack | Minecraft / loader | Target | Engine | Status | Card |
|---|---|---|---|---|---|
| Full Retro | 1.2.5 / Forge 3.4.9 | client, browser | TeaVM | Playable in browser | [card](ports/full-retro/1.2.5/client-teavm.md) |
| Full Retro | 1.2.5 / Forge 3.4.9 | client, browser | CheerpJ 4.3 (WASM JVM) | Live, open issues | [card](ports/full-retro/1.2.5/client-cheerpj.md) |
| Full Retro | 1.2.5 / Forge 3.4.9 | client, native | GraalVM native image | Experimental, title screen reached | [card](ports/full-retro/1.2.5/client-graalvm.md) |
| Full Retro | 1.2.5 / Forge 3.4.9 | client, browser | Bytecoder WasmGC, OpenJDK Zero WASM | Research prototypes | [card](ports/full-retro/1.2.5/client-wasm-experiments.md) |
| FTB Infinity Evolved | 1.7.10 / Forge 10.13.4 | client, browser | TeaVM | Title screen reached, multiplayer in progress | [card](ports/ftb-infinity-evolved/1.7.10/client-teavm.md) |
| Direwolf20 | 1.4.7 / Forge 6.6.2 | client, browser | TeaVM | In progress, uncommitted | [card](ports/direwolf20/1.4.7/client-teavm.md) |
| Direwolf20 | 1.4.7 / Forge 6.6.2 | server, native | GraalVM native image | Working, deployed | [card](ports/direwolf20/1.4.7/server-graalvm.md) |
| Civil Era | 1.21.1 / NeoForge 21.1 | client (optimized in place, not a port) | Prism launcher, JVM tuning | Tuned | [card](ports/civil-era/1.21.1/client-prism-tuning.md) |

## Vanilla ports (not modpacks)

These are single-version Minecraft ports in the EaglerPorts GitHub org. They are
listed for reference and are not catalogued as cards.

| Version | Repo |
|---|---|
| 1.13.2 | [EaglerPorts/1.13.2](https://github.com/EaglerPorts/1.13.2) |
| 1.2.5 | [EaglerPorts/1.2.5](https://github.com/EaglerPorts/1.2.5) |
| b1.8.1 | [EaglerPorts/b1.8.1](https://github.com/EaglerPorts/b1.8.1) |
| b1.6-tb3 | [EaglerPorts/b1.6-tb3](https://github.com/EaglerPorts/b1.6-tb3) |
| b1.3_01 | [EaglerPorts/b1.3_01](https://github.com/EaglerPorts/b1.3_01) |
| a1.2.6 | [EaglerPorts/a1.2.6](https://github.com/EaglerPorts/a1.2.6) |
| a1.0.16_02 | [EaglerPorts/a1.0.16_02](https://github.com/EaglerPorts/a1.0.16_02) |
| in-20100223 | [EaglerPorts/in-20100223](https://github.com/EaglerPorts/in-20100223) |
| betaProxy (WebSocket to TCP bridge) | [EaglerPorts/betaProxy](https://github.com/EaglerPorts/betaProxy) |

## Layout

```
ports/
  <modpack-slug>/
    <mc-version>/
      <target>-<engine>.md     one card per port
templates/
  port.md                      copy this for a new card
```

- `<modpack-slug>`: lowercase, hyphenated pack name (`full-retro`, `direwolf20`).
- `<mc-version>`: the Minecraft version the port runs (`1.2.5`, `1.7.10`).
- `<target>`: `client` or `server`.
- `<engine>`: the build or runtime (`teavm`, `graalvm`, `cheerpj`, `prism-tuning`).

## Status values

- **Live**: deployed and used by players.
- **Playable**: runs end to end, not necessarily deployed.
- **Working, deployed**: runs and is deployed, but not in active use.
- **Experimental**: runs in part; known blockers.
- **In progress**: code exists and is being built; not verified end to end.
- **Research**: prototypes that answer an engine question.
- **Tuned**: an existing pack optimized in place, not a port.

## Adding or updating a port

1. Copy `templates/port.md` to `ports/<modpack>/<mc-version>/<target>-<engine>.md`.
2. Fill in the table at the top and link the code's home repo and branch.
3. Add or update the row in the Ports table above.
4. Never commit game assets, server packs, world saves, account data, IP addresses,
   hostnames of private machines, or credentials.

## Candidates not yet catalogued

These repos look port-related but were not reviewed for this index:

- [thatnerd64/mc1.7.10](https://github.com/thatnerd64/mc1.7.10) (private, 2025)
- [thatnerd64/keepinv1.2.5](https://github.com/thatnerd64/keepinv1.2.5) (public, 2025)
- [thatnerd64/greg](https://github.com/thatnerd64/greg) (private, 2025)
- [thatnerd64/bc-1.21.1](https://github.com/thatnerd64/bc-1.21.1) (private, 2026)
