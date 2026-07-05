# Building Mcorder for Minecraft 26.2 (latest)

This project is configured to target the **latest stable Minecraft, 26.2** (there is
no "1.26.1"; Mojang moved to year-based versions in 2026). Verified stack:

- Minecraft **26.2**, Fabric Loader **0.19.3**, Fabric API **0.154.0+26.2**
- **Java 25** (required by MC 26.1+), mappings = **official Mojang mappings**
  (Yarn is not published for 26.2)

## How to build

```bash
# needs JDK 25 installed and on PATH
./gradlew build
# output: build/libs/mcorder-1.0.0.jar
```

To launch it in a dev client for manual testing:

```bash
./gradlew runClient
```

## ⚠️ Required manual porting before it compiles — read this

The Java sources under `src/` were **decompiled from the original 1.21.x jar**, so
they carry two properties that block a clean 26.2 build until you address them:

1. **They use intermediary names** (`class_310`, `method_1551`, `field_1705`, …).
   The build above uses **Mojang mappings** (named). You must **remap the sources to
   Mojang-mapped names** — i.e. replace `class_310` → `MinecraftClient`/`Minecraft`,
   `method_1551` → `getInstance`, etc. Options:
   - Re-author each file against the 26.2 named API (cleanest), or
   - Run the sources through a mapping remap (intermediary → mojmap for 26.2) and
     fix what doesn't resolve.

2. **The mixins target 1.21 method signatures that moved in 26.x.** Re-verify each
   injection point against 26.2 (see `PORTING.md` for the per-mixin table). The
   highest-risk one is `GameRendererMixin` (`method_3192` / render TAIL) and the raw
   `glReadPixels` framebuffer capture in `FrameCapturer`, because 26.2 ships an
   experimental Vulkan backend and a reworked render pipeline.

Until those two are done, `./gradlew build` will fail with unresolved-symbol errors —
that is expected and is the real porting work, which requires compiling and testing
against the actual game (your manual test step).

## Build status — actually attempted (result below)

I ran the real Gradle build (Gradle 9.5 + Fabric Loom 1.17 + JDK 25). The toolchain
assembles fine, but the build stops at Minecraft setup with:

```
> Failed to setup Minecraft, java.lang.RuntimeException:
  Failed to find official mojang mappings for 26.2
```

**Root cause — an external blocker, verified against the source servers:**

| Version | Mojang `client_mappings` | Yarn published |
|---------|--------------------------|----------------|
| 26.1    | ❌ no | ❌ no |
| 26.1.1  | ❌ no | ❌ no |
| 26.2    | ❌ no | ❌ no |

**No public mappings exist for any 26.x version yet** (Mojang's version manifest lists
only `client`/`server` jars — no `client_mappings`; Fabric's Yarn endpoint returns
empty). Without mappings, Loom cannot deobfuscate Minecraft, so **no Fabric mod can be
source-compiled for 26.x through standard tooling right now** — this is not specific to
Mcorder. When Mojang/Fabric publish mappings for the 26.x line, this project is already
configured to build against them (adjust `minecraft_version` in `gradle.properties`).

## The already-working alternative

`mcorder-1.21.11-1.0.0.jar` (in this folder) is the **rebranded original, working on
Minecraft 1.21.x today** — no build required. Use it if you want Mcorder running now
while the 26.2 port is completed.
