# FTB Infinity Evolved 1.7.10 — client, browser (TeaVM)

| | |
|---|---|
| Modpack | FTB Infinity Evolved (pack 3.1.0, 140 mods) |
| Minecraft / loader | 1.7.10 / Forge 10.13.4.1614 |
| Target | client, browser (WebGL 2, Web Audio) |
| Engine | TeaVM 0.15 with a patched teavm-plugin; FML run ahead of time |
| Status | Title screen reached with all mods loaded; multiplayer in progress |
| Source | [thatnerd64/1.2.5-website](https://github.com/thatnerd64/1.2.5-website), branch `ftb-infinity-evolved-1.7.10`, path `1.7.10/FTB-Infinity-Evolved/` |
| Live | https://freedom.sussy.uk/1.7.10/FTB-Infinity-Evolved/ (build 39; build 41 has the auto-join fix, not yet redeployed) |

## What it is

The FTB pack's real Forge startup is run headlessly on a JDK 8 to record what
Forge's class transformers produce. That output is compiled to JavaScript with TeaVM.
Mod discovery is replayed from a recorded list, so the browser build does not need
the Forge launcher.

## Build

- Ahead-of-time pass: `tools/aot.sh` (JDK 8) writes `build/aot/classes.jar`,
  `discovery.json`, and related files.
- Full build: about 10 minutes on a 12-core, 128 GB machine with `TEAVM_HEAP=80g`.
  Output is about 400 MB.
- Headless verification: `tools/remote-run.sh` runs Chromium on the build machine and
  saves a screenshot. Expect 35–40 minutes under SwiftShader.

## Status and measurements

- Title screen reached in build 37 with 181 mod entries and 0 mod errors.
- Shims added so that Guava, javax.imageio, TChannels, TCollator, and TDelayQueue
  behave like the JDK for the pack's code.
- A capture run records classes generated at run time so they can be compiled in.

## Known gaps

- `?server=` auto-join was broken in build 39 (FML connect latch). Fixed in build 41;
  redeploy once its join test passes.
- Create/load a world with mods is the next verification step.
- The pack's server runs on a separate host and is not covered by this card.

## Related

- [full-retro/client-teavm](../../full-retro/1.2.5/client-teavm.md): the same engine on Minecraft 1.2.5.
