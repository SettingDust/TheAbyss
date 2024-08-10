import groovy.lang.Closure

plugins {
    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)

    alias(catalog.plugins.git.version)

    alias(catalog.plugins.fabric.loom)

    alias(catalog.plugins.explosion)
}

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/gradle_issue_15754.gradle.kts")

group = "settingdust"

val gitVersion: Closure<String> by extra
version = gitVersion()

allprojects { apply(plugin = catalog.plugins.fabric.loom.get().pluginId) }

loom { mixin { add("main", "the_abyss.refmap.json") } }

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
