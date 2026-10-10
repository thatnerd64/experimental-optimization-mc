# Full Retro 1.2.5 — client, browser (WebAssembly experiments)

| | |
|---|---|
| Modpack | Full Retro |
| Minecraft / loader | 1.2.5 / Forge 3.4.9 |
| Target | client, browser |
| Engine | Bytecoder (WasmGC and JS); OpenJDK 21 Zero compiled to WebAssembly with Emscripten |
| Status | Research prototypes |
| Source | [thatnerd64/1.2.5-website](https://github.com/thatnerd64/1.2.5-website), paths `Bytecoder-Versions/` and `Wasm-JVM-Versions/` |
| Live | none |

## What it is

Two engine experiments for the same pack, kept to answer engine questions. Neither
is a shipped port.

- **Bytecoder** (`Bytecoder-Versions/`): an AOT compiler to WasmGC or ES6 JavaScript.
  Includes diagnostic benchmarks and a technical report.
- **OpenJDK Zero in WASM** (`Wasm-JVM-Versions/`): a real JVM interpreter in a
  WebAssembly module that runs unmodified bytecode in the browser. Needs
  SharedArrayBuffer.

## Status and measurements

Recorded in each folder's README and report. There is no end-to-end title screen for
either engine.

## Known gaps

- Bytecoder and the Zero interpreter have not run the full pack to the title screen.

## Related

- [client-teavm](client-teavm.md): the engine that runs in production.
