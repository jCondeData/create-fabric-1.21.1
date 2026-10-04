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
- [ ] 6.0.6 `fa73b383d5`
- [ ] 6.0.7 tag `mc1.21.1-6.0.7`
- [ ] 6.0.10 tag `mc1.21.1-6.0.10` (includes 6.0.8, 6.0.9)

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

## Known open items
- REI compat lives in `compat/rei` (client-only `rei_client` entrypoint, mirrors `compat/jei`
  category by category). Dev-test it with `val recipeViewer = "rei"` in build.gradle.kts.
  Architectury 13's Fabric `FluidStack#getPatch()` drops components, so never use it there.
- `FluidUnit.name` uses `generic.unit.*` keys without the `create.` prefix, so goggle fluid
  tooltips print the raw key; REI compat works around it.
- Packager unwrap runs nested `Transaction.openOuter()` inside a transaction callback
  (`PackagerBlockEntity.unwrapBox` → `DefaultUnpackingHandler`) — likely runtime exception; verify.
- Mechanical arm mode message key double-prefixed (`create.create.mechanical_arm…`), also upstream.
