import groovy.lang.Closure

plugins {
    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)

    alias(catalog.plugins.git.version)

    alias(catalog.plugins.fabric.loom)

    alias(catalog.plugins.explosion)
}

    apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/gradle_issue_15754.gradle.kts")

group = "settingdust.the_abyss"

val gitVersion: Closure<String> by extra
version = gitVersion()

val id: String by rootProject.properties
val name: String by rootProject.properties
val author: String by rootProject.properties
val description: String by rootProject.properties

loom { mixin { add("main", "$id.refmap.json") } }

fabricApi { configureDataGeneration() }

kotlin { jvmToolchain(17) }

dependencies {
    minecraft(catalog.minecraft.fabric)
    mappings(variantOf(catalog.mapping.yarn) { classifier("v2") })

    modImplementation(catalog.fabric.loader)
    modImplementation(catalog.fabric.api)
    modImplementation(catalog.fabric.kotlin)

    modImplementation(catalog.modmenu)
    modCompileOnly("maven.modrinth:tectonic:2.4.1b")
    modRuntimeOnly("maven.modrinth:formations-overworld:1.0.4-1.20.1")
    modRuntimeOnly("maven.modrinth:formations:1.0.2-fabric-mc1.20")
    modRuntimeOnly("maven.modrinth:supermartijn642s-core-lib:1.1.17a-fabric-mc1.20.1")
    modRuntimeOnly("maven.modrinth:supermartijn642s-config-lib:1.1.8a-fabric-mc1.20.1")

    modImplementation("maven.modrinth:lithostitched:1.4.2-fabric,1.20.1")

    modRuntimeOnly("maven.modrinth:world-preview:1.3.1-fabric")
    modRuntimeOnly("maven.modrinth:more-profiling:0.15.0")

    modRuntimeOnly("maven.modrinth:data-dumper:0.5.4")
    modRuntimeOnly("me.lucko:fabric-permissions-api:0.3.1")

    modImplementation(catalog.patched)

//    "com.github.ben-manes.caffeine:caffeine:3.1.8".let {
//        implementation(it)
//        include(it)
//    }

    catalog.mixinsquared.fabric.let {
        implementation(it)
        annotationProcessor(it)
        include(it)
    }
}

val metadata =
    mapOf(
        "group" to group,
        "author" to author,
        "id" to id,
        "name" to name,
        "version" to version,
        "description" to description,
        "source" to "https://github.com/SettingDust/TheAbyss",
        "minecraft" to ">=1.20.1",
        "fabric_loader" to ">=0.15",
        "fabric_kotlin" to ">=1.11",
        "modmenu" to "*",
    )

tasks {
    withType<ProcessResources> {
        inputs.properties(metadata)
        filesMatching(listOf("fabric.mod.json", "*.mixins.json")) { expand(metadata) }
    }
}
