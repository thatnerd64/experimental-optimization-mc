# Full Retro 1.2.5 — client, browser (TeaVM)

| | |
|---|---|
| Modpack | Full Retro (46 mods) |
| Minecraft / loader | 1.2.5 / Forge 3.4.9.171 |
| Target | client, browser (WebGL 2, Web Audio) |
| Engine | TeaVM 0.15 with a patched teavm-plugin |
| Status | Playable |
| Source | [thatnerd64/1.2.5-website](https://github.com/thatnerd64/1.2.5-website), branch `main`, root |
| Live | https://freedom.sussy.uk |

## What it is

Minecraft 1.2.5 with Forge and the Full Retro mods, compiled from Java bytecode to
JavaScript. Runs in a static page with no plugin or server-side JVM. Multiplayer goes
through a WebSocket relay (`tools/ws-proxy.js`) to ordinary 1.2.5 servers.

## Build

Requires JDK 17+, about 14 GB RAM for the TeaVM step, and the user's own
`minecraft.jar` (see `input/README.md`). Then `./gradlew build` writes `dist/`.

## Status and measurements

- RTX 2070 SUPER, 1280x720, default settings: title 120–165 fps, in-world idle ~61 fps,
  walking ~69 fps, load to playable ~25 s (2026-10-05).
- Audio fixed 2026-10-06 by supplying the pack's `overrides/resources`.
- Discord Activity wrapper and site build scripts are maintained outside this repo.

## Known gaps

- Long-session stability and mod-specific gameplay are not covered by the benchmark.
- Discord Activity pointer-lock behavior is unverified in Firefox-based browsers.

## Related

- [client-graalvm](client-graalvm.md) and [client-wasm-experiments](client-wasm-experiments.md): other engines for the same pack.
- [client-cheerpj](client-cheerpj.md): a separate CheerpJ deployment of this pack.
