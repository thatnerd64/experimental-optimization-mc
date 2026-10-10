# Full Retro 1.2.5 — client, native (GraalVM)

| | |
|---|---|
| Modpack | Full Retro (46 mods) |
| Minecraft / loader | 1.2.5 / Forge 3.4.9 |
| Target | client, native Linux x86_64 (X11/OpenGL) |
| Engine | GraalVM native image |
| Status | Experimental, title screen reached |
| Source | [thatnerd64/1.2.5-website](https://github.com/thatnerd64/1.2.5-website), path `GraalVM-Versions/` |
| Live | none |

## What it is

The same pack compiled ahead of time into a standalone executable with GraalVM
native-image, instead of JavaScript. The repo's launcher can pick GraalVM or TeaVM as
the engine.

## Build

Scripts and configs are in `GraalVM-Versions/scripts` and `GraalVM-Versions/config`.
A desktop compatibility runtime and an `assets.pak` reader are included. Peak build
RAM is about 8.5 GB.

## Status and measurements

- Title screen verified with 46 mods loaded (screenshot in `GraalVM-Versions/`).
- JNI, reflection, and AWT registration is done for the classes the pack needs.

## Known gaps

- Full gameplay and multiplayer are not verified.
- Native-image reflection config must be regenerated when the pack changes.

## Related

- [client-teavm](client-teavm.md): the production engine for this pack.
