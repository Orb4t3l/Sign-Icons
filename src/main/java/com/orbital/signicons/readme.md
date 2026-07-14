Type `:grass_block:` on any sign or hanging sign and it renders as the real
grass block icon, live from the game's own item textures — no image assets
to maintain, works with modded items too via `:modid:item_id:`. Also renders
in chat.

**100% client-side.** The sign's stored text is never changed — it still
literally contains `:grass_block:`. Players without the mod just see that
text as-is. Players with the mod see the icon. No packets, no server jar
needed.

## Building

```
./gradlew build
```

Output jar lands in `build/libs/signicons-1.0.0.jar`.

## Currently targeting: Forge 1.21.11

All version/loader info lives in `gradle.properties` — bump `minecraft_version`,
`forge_version`, and `loader_version_range` there for future ports, rather than
editing `build.gradle` directly.

## Porting to a new Minecraft version — read this first

Bumping the Gradle files (done, for 1.21.11) gets the project compiling
against the *toolchain*. It does **not** guarantee the mixins still compile
or work, because every mixin in this project targets specific vanilla
method signatures that were verified against decompiled 1.20.1 source,
one at a time, through actual compiler errors. Minecraft 1.20.1 → 1.21.11
spans several major versions, including the 1.20.5 item-component rewrite,
so real signature changes in the targeted classes are likely, not
hypothetical.

The workflow that actually worked for getting this mod running (repeat it
per mixin, against the new version):

1. Try building.
2. When a mixin fails with "cannot resolve method X" or "no possible
   signatures", open the real vanilla class in your IDE via decompiled
   sources (Fernflower/whatever your IDE gives you) for the *new* Minecraft
   version.
3. Paste the actual class/method here (or just read it yourself) and fix
   the mixin's `method =` / `target =` strings to match reality, rather
   than guessing.

Classes each mixin depends on, and what to re-check first:

- **`SignRendererMixin`** → `net.minecraft.client.renderer.blockentity.SignRenderer`.
  Targets `renderSignText` (two overloads/call sites: the `drawInBatch` call
  and the `getRenderMessages` call). Also depends on `ItemDisplayContext.GUI`
  and `Lighting.setupForFlatItems()`/`setupFor3DItems()` still existing with
  the same names.
- **`SignEditScreenMixin`** → `net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen`.
  Full `HEAD`-injection reimplementation of `renderSignText`, plus an
  `init()` tail-injection that replaces `signField` with a custom
  `TextFieldHelper`. Depends on `TextFieldHelper`'s constructor shape
  staying the same (5 args: getter, setter, clipboard getter, clipboard
  setter, validator predicate).
- **`ChatComponentMixin`** → `net.minecraft.client.gui.components.ChatComponent`.
  The biggest one — full reimplementation of `render()`. Most fragile to
  a version bump since it shadows the most private fields/methods.

If a mixin fails outright and you don't want to fix it immediately, remove
its entry from `signicons.mixins.json`'s `client` array so the other two
can still load — they're fully independent of each other.

## Adding items

Nothing to configure — `IconTextUtil` looks up any registered item by
`ResourceLocation` at parse time, so every vanilla and modded item works
out of the box the moment its mod is installed alongside SignIcons.

## Tuning constants

`ICON_SIZE_MULTIPLIER` and `ICON_ADVANCE_MULTIPLIER` appear in all three
mixins and should be kept identical across them — they're what keeps the
sign-edit preview, the in-world render, and chat visually consistent with
each other. Change them in one place, change them in all three.