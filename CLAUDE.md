# Create 6 → Fabric 1.21.1 (Jesse's private port)

Private build of the Create mod for Fabric 1.21.1, for Jesse's Cobbleverse 1.7.42 server
(friends only, never published; Create's assets are All Rights Reserved).

## Lineage
- Base: [Blockfield/create-fabric](https://github.com/Blockfield/create-fabric) (branch `blockfield`
  here), which finished the Fabricators of Create `mc1.21.1/fabric/dev` port at Create 6.0.2.
- Goal: bring it to upstream Create **6.0.10** (`Creators-of-Create/Create`, tag `mc1.21.1-6.0.10`).
- Platform layer: Fabric API, Porting Lib 3.1.0-beta.90, Fabric Transfer API, Registrate-Fabric,
  Forge Config API Port (its `net.neoforged.neoforge.common.ModConfigSpec` / `net.neoforged.fml.config.*`
  imports are fine — every other `net.neoforged.*` import is a bug).

## Branches
- `main` — the port. Each upstream step lands as a merge commit.
- `blockfield` — untouched Blockfield starting point.
- `upstream-gjf` — upstream commits reformatted with google-java-format `--aosp`
  (Blockfield reformatted everything, so raw upstream conflicts in every file).
  Build the next step with `scripts/upstream-gjf.sh <upstream-commit>`, then
  `git merge --no-ff upstream-gjf`. Tag pushes fail through the session proxy; use branches.

## Upstream steps (end-of-version commits on mc1.21.1/dev)
- [x] 6.0.4 `b703199f48`
- [x] 6.0.6 `fa73b383d5`
- [x] 6.0.7 tag `mc1.21.1-6.0.7`
- [x] 6.0.10 tag `mc1.21.1-6.0.10` (includes 6.0.8, 6.0.9) — current, version 6.0.10.0+mc1.21.1

Per step: `git remote add upstream https://github.com/Creators-of-Create/Create` (once),
fetch `mc1.21.1/dev` + tags, run the gjf script, merge, resolve (keep Fabric side, apply upstream's
change), `./gradlew build -x test`, `./gradlew runDatagen`, `./gradlew runGametestServer`, run gjf on
touched files, commit, push.

## Build / test (2-core, 7 GB container)
```
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # JDK 21, Loom 1.10
./gradlew build -x test --max-workers=1   # compile + codec/mixin/regression checks
./gradlew runGametestServer --max-workers=1
./gradlew runDatagen --max-workers=1      # regenerates src/generated
```
- Datagen gotcha: block loot tables are NOT regenerated (Registrate-Fabric loot provider is
  stripped). If datagen deletes `data/create/loot_table/blocks/*`, restore them with
  `git checkout HEAD -- src/generated/resources/data/create/loot_table/blocks`.
- Output jar `build/libs/create-fabric-*.jar` bundles Porting Lib + Milk Lib (patched copy in
  `libs/maven`) — server and clients need only this jar plus Fabric API.

## Port decisions to keep
- Fluid ingredients: Create's own `foundation.fluid.FluidIngredient` (droplets), never NeoForge's
  `SizedFluidIngredient`. `FluidStack` writes the Fabric format and also reads NeoForge's.
- Never `Transaction.openOuter()` where a transaction may be open — nest with
  `Transaction.openNested(Transaction.getCurrentUnsafe())`.
- Tags: Fabric v2 convention tags; `AllItemTags.WRENCH`, `AllItemTags.FOODS_DOUGH`,
  `AllBlockTags.RELOCATION_NOT_SUPPORTED` stand in for NeoForge `Tags` entries Fabric lacks.
- Diving armor stays untrimmable via `SmithingTrimRecipeMixin` (NeoForge tag `remove` doesn't work).
- Data maps on Porting Lib (`CreateDataMapsImpl.registerDataMaps()`); furnace fuel via `FuelRegistry`.
- Removed compat: FTB, Curios, JourneyMap, EMI, ComputerCraft (CC events are no-ops).
- Recipe viewers: JEI (`compat/jei`) and REI (`compat/rei`, client-only `rei_client` entrypoint,
  mirrors `compat/jei` category by category; dev-test with `val recipeViewer = "rei"`).
  Architectury 13's Fabric `FluidStack#getPatch()` drops components, so never use it there.
- Xaero's World Map train map: compile-only Xaero 1.44.2 + XaeroLib unpacked by build.gradle.kts;
  mixins gated in `CreateMixinPlugin`.

## Known open items
- Not verified in a real client yet: rendering with Sodium 0.8 / Iris, REI layouts and "+" transfer,
  copycat emissive/light after reload (no `AuxiliaryLightManager` on Fabric).
- Ponder (1.0.69) and Flywheel (1.0.6) are older than upstream 6.0.10 bundles (1.0.82 / 1.0.6);
  bumping Ponder means the renamed `ponder-fabric` artifact.
