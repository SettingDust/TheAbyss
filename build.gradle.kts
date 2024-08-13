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

allprojects { apply(plugin = catalog.plugins.fabric.loom.get().pluginId) }

loom { mixin { add("main", "$id.refmap.json") } }

fabricApi { configureDataGeneration() }

kotlin { jvmToolchain(21) }

dependencies {
    minecraft(catalog.minecraft.fabric)
    mappings(variantOf(catalog.mapping.yarn) { classifier("v2") })

    modImplementation(catalog.fabric.loader)
    modImplementation(catalog.fabric.api)
    modImplementation(catalog.fabric.kotlin)

    modRuntimeOnly(catalog.modernfix)

    modImplementation(catalog.worldgen.devtools)
    modImplementation(catalog.patched)
    modRuntimeOnly(catalog.worldPreview)
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
