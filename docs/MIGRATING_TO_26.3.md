# Migrating Fabric projects from Minecraft 26.2 to 26.3

This records the CustomModelDataViewer migration on 2026-09-22. It lists the
chosen versions and API changes for other projects; the validation section
records which checks have actually completed.

## Dependencies and toolchain

| Setting | Before | After |
| --- | --- | --- |
| Minecraft | `26.2` | `26.3` |
| Fabric Loader | `0.19.3` | `0.19.5` |
| Fabric API | `0.154.2+26.2` | `0.161.0+26.3` |
| Fabric Loom | `1.17.14` | `1.17.21` |
| Gradle wrapper | `9.5.1` | `9.6.0` |
| Kotlin compiler | `2.4.0` | Unchanged |
| Fabric Language Kotlin | `1.13.12+kotlin.2.4.0` | Unchanged |
| Java toolchain and JVM target | `25` | Unchanged |
| Mod Menu, development runtime only | `20.0.1` | `21.0.0-beta.1` |
| Cloth Config, development runtime only | `26.2.155+fabric` | `26.3.158+fabric` |
| Mod version | `3.0.3` | Unchanged |

Mod Menu's available 26.3 build was a beta when checked. These optional runtime
dependencies are not bundled into the mod. The resulting mod version remains
`3.0.3+26.3`, producing `build/libs/CustomModelDataViewer-3.0.3+26.3.jar`.

Versions live in `gradle.properties`, the plugin block in `build.gradle.kts`, and
`gradle/wrapper/gradle-wrapper.properties`. The existing
`net.fabricmc.fabric-loom` plugin and unobfuscated Minecraft names remain in use;
this 26.2-to-26.3 upgrade does not add mappings or a remapping step.

## Source changes

The input and drop changes are in `gui/CMDVScreen.kt`; resource scanning changes
are in `ResourceListener.kt` and registration in `Custommodeldataviewer.kt`, all
under `src/main/kotlin/cc/modlabs/custommodeldataviewer/`.
The fourth source file is
`src/main/java/cc/modlabs/custommodeldataviewer/mixin/MultiPlayerGameModeMixin.java`.

### Replace raw input values

Minecraft 26.3 uses SDL for input. Replace the active keyboard Escape comparison
and both raw left-click comparisons with Minecraft's constants:

```kotlin
// Before: keyInput.key != 256
keyInput.key != InputConstants.KEY_ESCAPE

// Before: click.button() == 0
click.button() == InputConstants.MOUSE_BUTTON_LEFT
```

The mouse checks are in `mouseClicked` and `mouseReleased`; they control scrollbar
interaction. In 26.3, Escape is `41` and raw left/right mouse buttons are `1`/`3`.
Use the constants rather than copying these values into another project.

Keep the `0`/`1` checks inside `slotClicked`. Vanilla
`AbstractContainerScreen.getContainerClickButton` translates raw SDL left/right
buttons into logical container buttons `0`/`1` before invoking that method.
Hotbar indexes and quick-craft bit fields also retain their existing meanings.
A global replacement of every numeric button comparison would break inventory
actions even though the project could still compile.

The search field already uses vanilla `EditBox`. Its `setFocused` implementation
notifies `Minecraft.onTextInputFocusChange`, so this project needs no additional
SDL text-input handling. Projects with custom text widgets must handle that
focus notification themselves.

### Specify creative-drop prediction

All six item-stack drop calls now pass the new prediction argument, following
vanilla `CreativeModeInventoryScreen`:

```kotlin
import net.minecraft.util.Prediction

player.drop(stack, true, Prediction.PREDICTED)
gameMode.handleCreativeModeItemDrop(stack)
```

This covers outside-screen drops, inventory drops, model-list throws, and hotbar
throws. The existing stack-count handling and server notification stay intact.
`MultiPlayerGameMode.dropItem(player, boolean)` handles a different selected-slot
action; it is not the replacement for these creative item-stack drops.

### Preserve drops from a custom creative screen

In 26.3, `handleCreativeModeItemDrop` rejects container screens unless they are
`CreativeModeInventoryScreen`. The viewer still compiled and cleared its cursor
stack, but vanilla suppressed the packet and no dropped item appeared.
`MultiPlayerGameModeMixin` uses MixinExtras `@Definition`, `@Expression`, and
`@ModifyExpressionValue` to include `CMDVScreen` in that one screen-type check.
The original creative-mode, enabled-feature, nonempty-stack, and drop-spam
handling remain intact. The mixin is registered in the client mixin list.
The mixin configuration declares `"mixinextras": { "minVersion": "0.5.5" }`,
required for expression injection; this version is already supplied by Loader
0.19.5, so no extra library is bundled.
Other custom creative screens should test the resulting world item and server
inventory, because successful compilation and a cleared cursor miss this failure.

### Wait for bound item components

Creating preview `ItemStack`s during the initial title-screen resource reload
failed with `Components not bound yet`. `ResourceListener.refreshItems` now clears
the previous list and returns while `Minecraft.level` is null. It builds previews
only after a world has connected, when the item components are available.

The same listener instance handles resource reloads and
`ClientPlayConnectionEvents.JOIN`. Fabric fires JOIN after vanilla `handleLogin`
has created the client level and player. In-world reloads refresh the list on the
client thread; `DISCONNECT` clears it so another world cannot reuse old previews.
Projects that construct stacks while reading client assets should check their
initialization timing even when their source still compiles.

### Accept selector arrays

Item-model `select` cases allow `"when": "value"` and `"when": ["a", "b"]`.
Reading every case with `asString` failed on valid vanilla chest/spear models.
The collector now visits each string alternative for `custom_model_data`
selectors, creating a preview for each matching value. Other selectors traverse
their nested model once without appending unrelated values to the custom-model
string component. Existing range thresholds remain unchanged.

## Metadata and dependency repositories

- `fabric.mod.json` now declares `"java": ">=25"`; its existing version placeholders
  resolve to Minecraft `26.3` and Loader `>=0.19.5`.
- `custommodeldataviewer.mixins.json` changes `JAVA_21` to `JAVA_25`, matching the
  existing compiler target and Minecraft runtime.
- The ModLabs Maven mirror returned HTTP 502 during resolution. Gradle aborted
  on that repository error even when fallback repositories were present, so the
  broken mirror was removed from both plugin and dependency repositories.
- `settings.gradle.kts` uses Fabric Maven and `gradlePluginPortal()` for plugins.
  Loom supplies the Minecraft/Fabric dependency repositories.
- `build.gradle.kts` adds `https://api.modrinth.com/maven` with
  `content { includeGroup("maven.modrinth") }` for the two development mods.
  This repository repair was specific to this project's build setup.

## Repeating the upgrade in another project

1. Record the existing versions and preserve unrelated local changes. Verify
   Minecraft, Loader, API, Loom, and optional mod versions against their official
   metadata rather than changing only `minecraft_version`.
2. Update the version properties and wrapper. Keep the Java/Kotlin targets and
   metadata consistent; do not change unrelated dependencies without a need.
3. Build once to identify changed method signatures. Search the complete source
   tree for callers of each affected API, including paths outside the reported
   compiler error or UI symptom.
4. Search for raw GLFW constants and numeric input comparisons. Distinguish raw
   input events from logical container actions before changing them.
5. Compare uncertain behavior with the new vanilla source using `genSources`,
   especially prediction flags, inventory actions, text-input focus, and registry
   readiness. Check parsers against valid array forms in vanilla assets.
6. Build and inspect the packaged metadata, then launch the client. Verify search
   typing, Escape, scrollbar drag/release, and inventory actions in a creative
   world with an actual custom-model resource pack. Record build, startup, and
   gameplay evidence separately.

Run these commands from the project root with JDK 25 selected:

```powershell
# Optional when the shell's default Java is another version:
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'

.\gradlew.bat --version
.\gradlew.bat clean build
.\gradlew.bat genSources
.\gradlew.bat runClient
```

Use your own JDK 25 installation path. `runClient` launches the development
instance and loads the optional runtime mods. Building does not publish a
release; publishing tasks were not part of this migration.

## Regression check

With JDK 25 selected, run `.\gradlew.bat runClientGameTest --no-daemon`.
The single client GameTest creates a creative world and checks the startup/JOIN
cache using 54 `custom_model_data` array variants nested under an unrelated
selector. It exercises the viewer button, search, scrolling and scrollbar
drag/release, item pickup/drop, Escape, resource reload, and disconnect clearing.
It waits for an actual dropped `ItemEntity` and checks the server hotbar for the
placed item's custom-model components, rather than checking only the cursor.
The test mod and fixture assets live only in `src/gametest` and are excluded from
the shipped JAR. See the validation results below for the actual run status.

## Validation results

Verified locally on Windows with Temurin 25.0.3 on 2026-09-22:

- `./gradlew build --no-daemon`: passed; final JAR is
  `build/libs/CustomModelDataViewer-3.0.3+26.3.jar`.
- `./gradlew runClientGameTest --no-daemon`: passed in 22 seconds in
  `build/run/clientGameTest`, covering every assertion described above. Both JOIN
  and resource reload produced 55 previews: the built-in icon and 54 fixtures.
- Inspected the rendered screenshot at
  `build/run/clientGameTest/screenshots/0000_cmdv-26.3-search-scroll.png`:
  search text, 45 visible model slots, scrollbar, tooltip, and hotbar rendered.
- Opened the final JAR and checked Minecraft/Java version constraints, the drop
  mixin class and registration, and exclusion of all test classes and fixtures.
- `git diff HEAD --check`: passed. No release was published.

The regression uses a local integrated server. External multiplayer servers,
ImageFrame plugin integration, invisible-frame placement, and arbitrary third-party
resource packs were not separately tested. Existing deprecated Fabric resource
listener APIs remain functional and were retained; their replacement is outside
this migration. The test runtime also reports host performance-counter and offline
account warnings; these did not prevent the game or assertions from completing.

## Sources

- [Fabric 26.3 migration guide](https://www.fabricmc.net/2026/09/15/263.html)
- [Mojang 26.3 metadata, including Java 25 and the client JAR](https://piston-meta.mojang.com/v1/packages/96c00d95a31328714d3811cfade2804bb050e455/26.3.json)
- [Fabric Loader metadata for 26.3](https://meta.fabricmc.net/v2/versions/loader/26.3)
- [Fabric API Maven versions](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml)
- [Loom Maven versions](https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml)
- [Fabric Language Kotlin Maven versions](https://maven.fabricmc.net/net/fabricmc/fabric-language-kotlin/maven-metadata.xml)
- [Mod Menu 21.0.0-beta.1](https://modrinth.com/mod/modmenu/version/lcxkZSq6)
- [Cloth Config 26.3.158+fabric](https://modrinth.com/mod/cloth-config/version/eZ3xIIuk)
- [NeoForged 26.2-to-26.3 vanilla migration primer](https://docs.neoforged.net/primer/docs/26.3/)
- [MixinExtras expressions and typed `instanceof` matching](https://github.com/LlamaLad7/MixinExtras/wiki/Expressions)

Input translation, `EditBox` focus handling, creative-drop prediction, and login
ordering were also checked against the official Minecraft 26.3 client bytecode;
JOIN timing was checked in the resolved Fabric networking module.
