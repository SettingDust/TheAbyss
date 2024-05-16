plugins {
    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)

    alias(catalog.plugins.semver)
    alias(catalog.plugins.rewrite)

    alias(catalog.plugins.fabric.loom)
}

group = "settingdust"

version = semver.semVersion.toString()

allprojects { apply(plugin = catalog.plugins.fabric.loom.get().pluginId) }

fabricApi { configureDataGeneration() }

rewrite {
    activeStyle("settingdust.Style")
    activeRecipe("settingdust.Recipes")
}

repositories {
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    mavenCentral()
}

dependencies {
    minecraft(catalog.minecraft)
    mappings(variantOf(catalog.yarn) { classifier("v2") })

    rewrite(catalog.rewrite.kotlin)
    rewrite(catalog.rewrite.static.analysis)
    rewrite(catalog.rewrite.migrate.java)

    modImplementation(catalog.fabric.loader)
    modImplementation(catalog.fabric.api)
    modImplementation(catalog.fabric.kotlin)

    modRuntimeOnly(catalog.modernfix)

    modImplementation(catalog.worldgen.profiling)
    modImplementation(catalog.worldgen.devtools)
    modImplementation(catalog.patched)
    modImplementation(catalog.moreDensityFunctions)
    modRuntimeOnly(catalog.worldPreview)
}

kotlin { jvmToolchain(21) }
