# The Civil Era 1.21.1 — client, Prism tuning (optimized in place)

| | |
|---|---|
| Modpack | The Civil Era (about 234 mods) |
| Minecraft / loader | 1.21.1 / NeoForge 21.1.250 |
| Target | client |
| Engine | Prism Launcher (Flatpak), Zulu 21, JVM flags |
| Status | Tuned, 2026-10-01 |
| Source | Local instance config only. Not in version control. |
| Live | none (plays on a Pelican-hosted server) |

## What it is

Not a port. The existing 1.21.1 pack is tuned for performance on a 16 GB, 8-thread
desktop with an RTX 2070 SUPER.

## Changes (2026-10-01)

- G1 garbage collector flags and `-XX:+UseTransparentHugePages`.
- Heap 6144 MB min and max.
- `biomeBlendRadius` lowered from 7 to 2.
- ModernFix re-enabled. It was disabled earlier after a Create crash; the later
  crashes came from EntityCulling's jar-in-jar.

## Known gaps

- If the pack crashes after this change, ModernFix is the first suspect.
- Server-side tuning is not covered by this card.

## Related

- Backups of the instance are kept on the desktop, not in this repo.
