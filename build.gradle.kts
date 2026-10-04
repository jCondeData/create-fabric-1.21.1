import java.util.zip.ZipFile

// versions
// https://parchmentmc.org/docs/getting-started
val parchmentVersion = "2024.11.17"
// https://fabricmc.net/develop/
val minecraftVersion = "1.21.1"
val loaderVersion = "0.19.3"
val fapiVersion = "0.115.1+1.21.1"

// in-house dependencies
val flywheelVersion = "1.0.6-44"
val ponderVersion = "1.0.69"
val registrateVersion = "1.3.77-MC1.21.1"
val milkLibVersion = "1.1.0"

// external dependencies
val configApiVersion = "21.1.3"
val nightConfigVersion = "3.6.3"
val jsr305Version = "3.0.2"

// compat
// https://modrinth.com/mod/cc-tweaked/versions
val ccVersion = "1.115.1"
// for CC - https://modrinth.com/mod/cloth-config/versions
val clothVersion = "15.0.140+fabric"
// https://modrinth.com/mod/jei/versions
val jeiVersion = "19.21.0.247"
// https://modrinth.com/mod/rei/versions
val reiVersion = "16.0.799"
// https://modrinth.com/mod/emi/versions
val emiVersion = "1.1.20+1.21.1"
// https://modrinth.com/mod/botania
val botaniaVersion = "1.19.2-436-FABRIC"
// https://modrinth.com/mod/modmenu/versions
val modmenuVersion = "11.0.3"
// https://modrinth.com/mod/sandwichable/versions
val sandwichableVersion = "1.3.1+1.20.1"
// https://modrinth.com/mod/sodium
val sodiumVersion = "mc1.21.1-0.6.9-fabric"
// https://github.com/emilyploszaj/trinkets/releases/
val trinketsVersion = "3.10.0"
// for Trinkets - https://modrinth.com/mod/cardinal-components-api/versions
val ccaVersion = "6.1.2"
// https://modrinth.com/mod/journeymap
val jmVersion = "1.21.1-6.0.0-beta.39+fabric"
// check the jm jar, it's JiJ
val jmApiVersion = "1.20-1.9-SNAPSHOT"
// https://modrinth.com/mod/xaeros-world-map/versions - fabric-1.21.1-1.44.2 (Modrinth version id)
val xaeroWorldMapVersion = "L2nO7ZYD"

// dev stuff
val ccRuntime = false
val recipeViewer = "emi" // jei, rei, or emi

plugins {
    id("fabric-loom") version "1.10.+"
    id("maven-publish")
}

version = "6.0.10.0+mc$minecraftVersion"

group = "com.simibubi.create"
base.archivesName = "create-fabric"

repositories {
    maven(uri("libs/maven")) { content { includeGroup("local.milklib") } } // patched Milk Lib
    maven("https://maven.parchmentmc.org") // Parchment
    maven("https://maven.fabricmc.net") // FAPI, Loader
    maven("https://maven.createmod.net") // Ponder, Flywheel
    maven("https://mvn.devos.one/snapshots") // Registrate, Forge Tags, Milk Lib
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven") // Forge Config API Port
    maven("https://maven.shedaniel.me") // REI and deps
    maven("https://api.modrinth.com/maven") {
        // LazyDFU, Sodium, Sandwichable
        content { includeGroupAndSubgroups("maven.modrinth") }
    }
    maven("https://maven.terraformersmc.com") // Mod Menu, Trinkets
    maven("https://maven.squiddev.cc") // CC:T
    maven("https://modmaven.dev") // Botania
    maven("https://maven.jamieswhiteshirt.com/libs-release") {
        // Reach Entity Attributes
        content { includeGroup("com.jamieswhiteshirt") }
    }
    maven("https://maven.ladysnake.org/releases") // CCA, for Trinkets
    maven("https://maven.saps.dev/releases") // FTB
    maven("https://maven.architectury.dev") // Architectury API
    maven("https://jm.gserv.me/repository/maven-public/") // Journey map
}

val ponder = file("Ponder")

// Xaero's World Map ships XaeroLib (home of GuiMap's superclasses) only jar-in-jar, and XaeroLib has
// no standalone Fabric 1.21.1 release. Loom doesn't put nested jars on the classpath, but compiling
// the train map compat (and its mixin refmap) and remapping the world map jar need it.
val xaeroLibJar: File =
    run {
        val worldMapJar =
            configurations
                .detachedConfiguration(
                    dependencies.create("maven.modrinth:xaeros-world-map:$xaeroWorldMapVersion"),
                ).apply { isTransitive = false }
                .singleFile
        val target = file(".gradle/nested-jars/${worldMapJar.nameWithoutExtension}-xaerolib.jar")
        if (!target.isFile) {
            ZipFile(worldMapJar).use { zip ->
                val entry =
                    zip.entries().asSequence().first {
                        it.name.startsWith("META-INF/jars/xaerolib-") && it.name.endsWith(".jar")
                    }
                target.parentFile.mkdirs()
                zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
            }
        }
        target
    }

dependencies {
    // setup
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(
        loom.layered {
            officialMojangMappings { nameSyntheticMembers = false }
            parchment("org.parchmentmc.data:parchment-$minecraftVersion:$parchmentVersion@zip")
        },
    )
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // dependencies
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fapiVersion")

    // Registrate тянет porting_lib common beta.39, и loom подмешивает её injected interfaces
    // (IPlantable, LanguageManagerExt) в jar Minecraft поверх beta.90 — компиляция падает.
    // common объявлен ниже явно в beta.90; остальные модули beta.39 оставлены, иначе modApi
    // теряет апгрейд fabric-api 0.104 -> 0.105.
    modApi(
        include("com.tterrag.registrate_fabric:Registrate:$registrateVersion") {
            exclude(group = "io.github.fabricators_of_create.Porting-Lib")
        },
    )

    modApi(include("com.electronwill.night-config:core:$nightConfigVersion")!!)
    modApi(include("com.electronwill.night-config:toml:$nightConfigVersion")!!)
    modApi(include("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:$configApiVersion")!!)
    modApi(include("dev.engine-room.flywheel:flywheel-fabric-$minecraftVersion:$flywheelVersion")!!)
    // impl Flywheel клиентский, API нужен и серверу (визуалы регистрируются в AllBlocks)
    modApi(include("dev.engine-room.flywheel:flywheel-fabric-api-$minecraftVersion:$flywheelVersion")!!)
    // Milk Lib с maven Create заканчивается 1.18; сборка под 1.21.1 живёт на Modrinth (Tu5LjQoE), лежит в libs/
    // Modrinth build of Milk Lib needs Loom 1.13+, so use the patched copy served from libs/maven; bundled so friends install one jar
    modApi(include("local.milklib:milk-lib:1.1.0-patch+1.21.1")!!)
    api(include("com.google.code.findbugs:jsr305:$jsr305Version")!!)

    if (ponder.exists()) {
        implementation("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion") { isTransitive = false }
        implementation("net.createmod.ponder:Ponder-Common-$minecraftVersion:$ponderVersion")
    } else {
        modRuntimeOnly(include("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion")!!)
        modCompileOnly("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion") {
            exclude(group = "io.github.fabricators_of_create.Porting-Lib")
        }
    }

    // compat
    modCompileOnly("cc.tweaked:cc-tweaked-$minecraftVersion-fabric-api:$ccVersion")

    modCompileOnly("vazkii.botania:Botania:$botaniaVersion") { isTransitive = false }
    modCompileOnly("com.terraformersmc:modmenu:$modmenuVersion")
    modCompileOnly("maven.modrinth:sandwichable:$sandwichableVersion")
    modCompileOnly("maven.modrinth:sodium:$sodiumVersion")

    modCompileOnly("dev.emi:trinkets:$trinketsVersion")
    // for Trinkets
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-base:$ccaVersion")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:$ccaVersion")

    // FIXME - Use gradle.properties for these versions, make change to concealed for this
    modCompileOnly("dev.architectury:architectury-fabric:9.1.12")
//    modCompileOnly("dev.ftb.mods:ftb-chunks-fabric:2001.3.1")
//    modCompileOnly("dev.ftb.mods:ftb-teams-fabric:2001.3.0")
//    modCompileOnly("dev.ftb.mods:ftb-library-fabric:2001.2.4")

    modCompileOnly("maven.modrinth:journeymap:$jmVersion")
    modCompileOnly("info.journeymap:journeymap-api:$jmApiVersion")

    // Xaero's World Map train map overlay (compat.trainmap.XaeroTrainMap, mixin.compat.xaeros)
    modCompileOnly("maven.modrinth:xaeros-world-map:$xaeroWorldMapVersion")
    modCompileOnly(files(xaeroLibJar))

    // EMI
    modCompileOnly("dev.emi:emi-fabric:$emiVersion:api") { isTransitive = false }
    // JEI
    modCompileOnly("mezz.jei:jei-$minecraftVersion-fabric:$jeiVersion") { isTransitive = false }
    // REI
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:$reiVersion")
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-fabric:$reiVersion")

    when (recipeViewer) {
        "jei" -> modLocalRuntime("mezz.jei:jei-$minecraftVersion-fabric:$jeiVersion")
        "rei" -> modLocalRuntime("me.shedaniel:RoughlyEnoughItems-fabric:$reiVersion")
        "emi" -> modLocalRuntime("dev.emi:emi-fabric:$emiVersion")
    }

    // dev env
    modLocalRuntime("com.terraformersmc:modmenu:$modmenuVersion")
    modLocalRuntime("dev.emi:trinkets:$trinketsVersion") { isTransitive = false }
    // for Trinkets
    modLocalRuntime("dev.onyxstudios.cardinal-components-api:cardinal-components-base:$ccaVersion")
    modLocalRuntime("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:$ccaVersion")
    if (ccRuntime) {
        modLocalRuntime("cc.tweaked:cc-tweaked-$minecraftVersion-fabric:$ccVersion")
        modLocalRuntime("maven.modrinth:cloth-config:$clothVersion")
    }
    // have deprecated modules present at runtime only
    modLocalRuntime("net.fabricmc.fabric-api:fabric-api-deprecated:$fapiVersion")
}

sourceSets.named("main") {
    resources {
        srcDir("src/generated/resources")
        exclude(".cache/")
    }
}

val compileGametestFixtures =
    tasks.register<JavaCompile>("compileGametestFixtures") {
        dependsOn(tasks.classes)
        source("src/gametest/java")
        classpath = sourceSets["main"].compileClasspath + sourceSets["main"].output
        destinationDirectory = layout.buildDirectory.dir("classes/java/gametest")
        options.annotationProcessorPath = files()
        options.compilerArgs.addAll(listOf("-proc:none", "-Xlint:all", "-Werror"))
    }
tasks.named("check") { dependsOn(compileGametestFixtures) }

val gametestFixtures =
    tasks.register<Jar>("gametestFixtures") {
        archiveFileName = "create-gametest-fixtures.jar"
        destinationDirectory = layout.buildDirectory.dir("gametest-fixtures")
        from("src/gametest/resources")
        from(compileGametestFixtures.flatMap { it.destinationDirectory })
        entryCompression = org.gradle.api.tasks.bundling.ZipEntryCompression.STORED
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

loom {
    accessWidenerPath = file("src/main/resources/create.accesswidener")

    runs {
        register("datagen") {
            client()
            name("Data Generation")
            vmArg("-Dfabric-api.datagen")
            vmArg("-Dfabric-api.datagen.output-dir=${file("src/generated/resources")}")
            vmArg("-Dfabric-api.datagen.modid=create")
            vmArg("-Dporting_lib.datagen.existing_resources=${file("src/main/resources")}")
        }

        register("gametestServer") {
            server()
            name("Headless GameTests")
            ideConfigGenerated(false) // this run is for CI
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric.addMods=${gametestFixtures.get().archiveFile.get().asFile.absolutePath}")
            vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory.file("gametest-results.xml").get().asFile}")
            vmArg("-Xmx2G")
            runDir("build/gametest")
        }

        named("server") {
            runDir("run/server")
        }

        configureEach {
            vmArg("-XX:+AllowEnhancedClassRedefinition")
            vmArg("-XX:+IgnoreUnrecognizedVMOptions")
            property("mixin.debug.export", "true")
        }
    }
}

tasks.named("runGametestServer") { dependsOn(gametestFixtures) }

configurations {
    // this avoids remapping ponder when it's local
    named("runtimeClasspath") {
        attributes {
            attribute(Attribute.of("create.marker", String::class.java), "h")
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    exclude("**/*.bbmodel", "**/*.lnk")

    val properties: MutableMap<String, Any> =
        mutableMapOf(
            "version" to version,
            "minecraft_version" to minecraftVersion,
            "loader_version" to loaderVersion,
            // the bundled Porting Lib modules need Fabric API >= 0.116.1
            "fabric_version" to "0.116.1+1.21.1",
            "forge_config_version" to configApiVersion,
            "milk_lib_version" to milkLibVersion,
        )

    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}

java {
    withSourcesJar()
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("10000")
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = "create-fabric-$minecraftVersion"
            from(components["java"])
        }
    }

    repositories {
        maven("https://mvn.devos.one/releases") {
            name = "devOsReleases"
            credentials(PasswordCredentials::class)
        }

        maven("https://mvn.devos.one/snapshots") {
            name = "devOsSnapshots"
            credentials(PasswordCredentials::class)
        }
    }
}

dependencies {
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:blocks:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:accessors:3.1.0-beta.54+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:entity:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:items:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:client_events:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:level_events:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:attributes:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:brewing:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:config:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:chunk_loading:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:obj_loader:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:mixin_extensions:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:loot:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:item_abilities:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:gui_utils:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:render_types:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:base:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:recipe_book_categories:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:common:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:core:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:data:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:fluids:3.1.0-beta.90+1.21.1")!!)
    modImplementation("io.github.fabricators_of_create.Porting-Lib:gametest:3.1.0-beta.90+1.21.1")
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:lazy_registration:3.1.0-beta.54+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:model_loader:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:models:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:tags:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:transfer:3.1.0-beta.90+1.21.1")!!)

    // Registrate-Fabric nests porting_lib_conditions 3.1.0-beta.39 built in the "named" namespace,
    // which crashes Fabric Loader in production unless a newer copy is present. beta.55 is the last
    // published conditions module; bundling it makes the jar boot without other mods' copies.
    modRuntimeOnly(include("io.github.fabricators_of_create.Porting-Lib:conditions:3.1.0-beta.55+1.21.1")!!)
    modRuntimeOnly(include("io.github.fabricators_of_create.Porting-Lib:registry:3.1.0-beta.90+1.21.1")!!)
    modImplementation(include("io.github.fabricators_of_create.Porting-Lib:resources:3.1.0-beta.90+1.21.1")!!) // data maps
    modRuntimeOnly(include("io.github.fabricators_of_create.Porting-Lib:model_data:3.1.0-beta.90+1.21.1")!!)
}

tasks.register<JavaExec>("codecCheck") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath + sourceSets["main"].compileClasspath
    mainClass = "com.simibubi.create.content.processing.recipe.ProcessingOutputCodecCheck"
    workingDir = layout.buildDirectory.get().asFile // Bootstrap пишет logs/ в рабочий каталог
    maxHeapSize = "256m"
}

tasks.register<JavaExec>("trackCollisionCheck") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath + sourceSets["main"].compileClasspath
    mainClass = "com.simibubi.create.content.trains.track.TrackCollisionCheck"
    maxHeapSize = "256m"
}

tasks.register<JavaExec>("mixinCheck") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath + sourceSets["main"].compileClasspath
    mainClass = "com.simibubi.create.foundation.mixin.CreateMixinPluginCheck"
    maxHeapSize = "256m"
}

tasks.register<org.gradle.api.tasks.compile.JavaCompile>("lintJava") {
    dependsOn("testClasses")
    source(sourceSets["main"].allJava, sourceSets["test"].allJava)
    classpath =
        sourceSets["main"].compileClasspath + sourceSets["test"].compileClasspath + sourceSets["main"].output + sourceSets["test"].output
    destinationDirectory = layout.buildDirectory.dir("classes/java/lint")
    options.annotationProcessorPath = files()
    options.compilerArgs.addAll(listOf("-proc:none", "-Xlint:divzero,empty,fallthrough,finally,-removal", "-Werror"))
}
tasks.named("check") { dependsOn("lintJava") }

tasks.register("regressionCheck") {
    group = "verification"
    dependsOn("codecCheck", "trackCollisionCheck", "mixinCheck")
}
tasks.named("check") { dependsOn("regressionCheck") }
