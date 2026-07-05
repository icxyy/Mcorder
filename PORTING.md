# Porting Mcorder toward modern Minecraft

## Version reality check

The request asked for "Minecraft latest version **1.26.1**". **That version does
not exist.** Mojang dropped the `1.x` scheme in 2026. Current versions:

| Version | Notes |
|---------|-------|
| 1.21.x  | What this mod was built for (jar says `1.21.11`) |
| 26.1 / 26.1.1 | First 2026 drop ("Tiny Takeover"), Mar/Apr 2026 — **requires Java 25** |
| **26.2** | Latest full release — experimental Vulkan backend |

Sources: minecraft.wiki version history, Java Edition 26.1 / 26.2 pages.

## Why this is not a mechanical "decompile + bump the version number"

1. **The decompiled code uses intermediary mappings** (`class_310`, `method_1551`,
   `field_1705`, …). These names are **version-specific** and change every release.
   Everything must be recompiled against the target version's intermediary/Yarn
   mappings — you cannot just edit strings.
2. **Mixins hook obfuscated internals that move between versions** (see table below).
3. **Decompiled bytecode does not cleanly recompile** — lambdas, synthetic methods
   and mixin injectors come back subtly wrong. This source tree is for *reading and
   re-authoring*, not `gradlew build` as-is.
4. **26.1+ requires Java 25** and the render pipeline changed significantly, which
   directly affects the frame-capture mixin.

A real port = set up a Fabric/Loom Gradle project, add Yarn mappings for the target
version, and re-author each mixin/API call against the new game. Below is the
checklist for doing that.

## Build environment you need (not available in this sandbox)

- JDK **25** (26.1+) — this sandbox only has a JRE 21, and no Minecraft to test against
- Fabric Loom Gradle plugin, Yarn mappings for the target MC version
- Fabric API + Fabric Loader for the target version

## Per-mixin re-target checklist

| Mixin | Targets (intermediary) | What to re-verify on the new version |
|-------|------------------------|--------------------------------------|
| `GameRendererMixin` | `class_757#method_3192` (GameRenderer.render) `@At("TAIL")`, arg `class_9779` (RenderTickCounter) | Render method signature/arg types changed across 1.21→26.x. Re-find the post-frame injection point. This is where each frame is grabbed. |
| `InGameHudMixin` | HUD render | Re-map to the current HUD render method; HUD rendering was refactored. |
| `ScreenMixin` | Screen lifecycle | Verify the injected method still exists. |
| `AL10Mixin` | LWJGL `AL10` (OpenAL) | Mixing into a **library** class to tap audio is fragile but LWJGL AL10 is fairly stable. Confirm the LWJGL version bundled with the target MC. |

## API surface that will need updating

- **Keybindings** — `class_304`/`class_11900` category enum (`McorderClient`) changed
  again post-1.21. Re-check `KeyBindingHelper.registerKeyBinding`.
- **Window / framebuffer** — `method_22683().method_4489()/method_4506()` (window
  width/height) and the raw GL framebuffer reads in `FrameCapturer`. The new render
  pipeline (and Vulkan path in 26.2) may not expose the default FBO the same way —
  `glReadPixels` on the main framebuffer is the riskiest part of the port.
- **Text / chat** — `class_2561.method_43470` + `field_1705.method_1743()` (chat HUD).
- **Java bump** — set mixin `compatibilityLevel` to `JAVA_25` and `java` depend `>=25`.

## Suggested fabric.mod.json for a 26.x target (draft)

```jsonc
"depends": {
  "fabricloader": ">=0.17.0",   // use the loader version shipped for 26.x
  "minecraft": ">=26.1",
  "java": ">=25",
  "fabric-api": "*"
}
```
…and `mcorder.mixins.json` → `"compatibilityLevel": "JAVA_25"`.

**Bottom line:** the rebrand is done and shippable for 1.21.x. A 26.x port is real
engineering work that requires the toolchain above plus in-game testing — it cannot
be produced or verified from a decompiled jar in this environment.
