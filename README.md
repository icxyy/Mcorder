# Mcorder

An in-game screen recorder for Minecraft (Fabric) with real-time audio capture.

- **Mod name:** Mcorder
- **Developer:** Iceyy
- **Based on:** Rewind Mod by wadomees (CC0 1.0) — see [`NOTICE.md`](NOTICE.md)

## What's in this folder

```
Mcorder-src/
├── mcorder-1.21.11-1.0.0.jar   ← WORKING rebranded jar for Minecraft 1.21.x (no build needed)
├── build.gradle, settings.gradle, gradle.properties, gradlew, gradle/
│                               ← Fabric Loom project configured for MC 26.2 + Java 25
├── src/main/java/com/mcorder/  ← decompiled + rebranded source (port base)
├── src/main/resources/         ← fabric.mod.json (26.2 target), mcorder.mixins.json, assets, lang
├── LICENSE (CC0 1.0)  NOTICE.md
├── BUILD.md            ← how to build/test the 26.2 target + the remaining port steps
├── PORTING.md          ← per-mixin/API checklist toward MC 26.x (and why 1.26.1 isn't a thing)
└── PERFORMANCE.md      ← RAM/CPU analysis + the optimizations applied
```

## The rebranded jar (`mcorder-1.21.11-1.0.0.jar`)

This is the **original working mod, rebranded** — repackaged for Minecraft **1.21.x**:
- Mod list shows **Mcorder** by **Iceyy**
- All visible in-game text rebranded (`[Rewind]` → `[Mcorder]`, etc.) via a precise
  constant-pool string patch — internal class names are untouched so it still loads.

> Not load-tested here (this sandbox has no Minecraft/JDK). The change set is
> low-risk metadata + string-literal edits, but verify it in a real 1.21.x Fabric
> instance before distributing.

## The source tree (`src/`)

Decompiled with Vineflower and fully rebranded (`com.rewind` → `com.mcorder`,
`RewindModClient` → `McorderClient`, all strings/keys). It uses **intermediary
mappings** and, like all decompiled mod code, **won't `gradlew build` unmodified** —
it's the base for a real re-authoring/port, not a drop-in project. See `PORTING.md`.

## Requirements (current jar)

- Minecraft **1.21.x**, Fabric Loader ≥ 0.19.2, Fabric API, Java ≥ 21
- FFmpeg (the mod downloads it on first run)

## Minecraft 1.26.1 / performance requests

Read [`PORTING.md`](PORTING.md) and [`PERFORMANCE.md`](PERFORMANCE.md) — short version:
"1.26.1" doesn't exist (latest is 26.2, needs Java 25), a version port is real
engineering work requiring a build+test toolchain, and two RAM/CPU fixes are already
applied to the source with more recommended.
