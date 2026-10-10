# Direwolf20 1.4.7 — client, browser (TeaVM)

| | |
|---|---|
| Modpack | Direwolf20 5.3.2 |
| Minecraft / loader | 1.4.7 / Forge 6.6.2.534 (FML 4.7.35) |
| Target | client, browser |
| Engine | TeaVM, same toolchain as the 1.2.5 and FTB ports |
| Status | In progress; not committed |
| Source | Local working tree `1.4.7/Direwolf20/` in [1.2.5-website](https://github.com/thatnerd64/1.2.5-website). Untracked; not on any branch yet. |
| Live | none |

## What it is

The same approach as the FTB port, applied to Direwolf20 1.4.7. Has an AOT pass
output (`build/aot/`) and TeaVM build output (`dist/`, `dist-remote/`).

## Status and measurements

Not verified end to end. No commit exists for this port.

## Known gaps

- Commit the folder to a branch once the build is verified.
- Confirm that the title screen and a world load work before claiming a status.

## Related

- [server-graalvm](server-graalvm.md): the server for this pack, which is complete.
