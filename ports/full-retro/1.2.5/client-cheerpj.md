# Full Retro 1.2.5 — client, browser (CheerpJ 4.3)

| | |
|---|---|
| Modpack | Full Retro |
| Minecraft / loader | 1.2.5 / Forge |
| Target | client, browser |
| Engine | CheerpJ 4.3 (WebAssembly JVM, Java 8) |
| Status | Live, with open issues |
| Source | Not in version control on the host. Copy in local `~/mc-online`. Remote branch `codex/migrate-minecraft-1.2.5-to-cheerpj-4.2` in [1.2.5-website](https://github.com/thatnerd64/1.2.5-website). |
| Live | https://minecraft.coloma.us |

## What it is

Runs the real 1.2.5 Forge client on CheerpJ instead of compiling it. It includes a
browser launcher, a jar builder that merges Forge into `minecraft.jar` in the browser,
a Range-aware static server, and a WebSocket-to-TCP gateway. Includes an invite-only
account service.

## Build

The host runs the launcher and a local Full Retro server as systemd services. The
browser launcher is plain ES modules with no build step.

## Status and measurements

- Live and playable. Config files are verified by SHA-256 before launch to prevent
  block and item ID collisions.
- Documented in the host project's own memory notes (mc-online).

## Known gaps

- CheerpJ runs Java threads on the browser's one thread. Heavy modpack load is a
  performance risk.
- OpenAL audio is not shimmed by gl4es, so sound is missing.
- The live tree has no git history. Move it into version control before further edits.

## Related

- [client-teavm](client-teavm.md): the TeaVM port of the same pack.
