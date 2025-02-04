val minecraft = "1.20.1"
extra["minecraft"] = minecraft

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/common.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/kotlin.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/fabric.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/modmenu.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/mixin.gradle.kts")

dependencyResolutionManagement.versionCatalogs.named("catalog") {
    library("minecraft-fabric-1.21", "com.mojang", "minecraft").version("1.21")
    library("fabric-loader", "net.fabricmc", "fabric-loader").version("0.16.3")
    // https://modrinth.com/mod/worldgen-devtools/versions
    library(
            "worldgen-devtools",
            "maven.modrinth",
            "worldgen-devtools",
        )
        .version("1.1.0+1.21")

    // https://modrinth.com/mod/worldgen-helpers/versions
    library(
        "worldgen-helpers",
        "maven.modrinth",
        "worldgen-helpers",
    )
        .version("1.1.0+1.20.4")

    // https://modrinth.com/mod/more-density-functions/versions
    library(
            "moreDensityFunctions",
            "maven.modrinth",
            "more-density-functions",
        )
        .version("1.0.3")

    // https://modrinth.com/mod/patched/versions
    library(
            "patched",
            "maven.modrinth",
            "patched",
        )
        .version("3.4.1+$minecraft-fabric")

    // https://modrinth.com/mod/modernfix/versions
    library(
            "modernfix",
            "maven.modrinth",
            "modernfix",
        )
        .version("5.18.3+mc1.21")

    // https://modrinth.com/mod/world-preview/versions
    library(
            "worldPreview",
            "maven.modrinth",
            "world-preview",
        )
        .version("1.3.0-fabric,1.21")


    // https://modrinth.com/mod/distanthorizons/versions
    library(
        "distanthorizons",
        "maven.modrinth",
        "distanthorizons",
    )
        .version("2.1.2-a-1.21")


    // https://modrinth.com/mod/iris/versions
    library(
        "iris",
        "maven.modrinth",
        "iris",
    )
        .version("1.7.1+1.21")


    // https://modrinth.com/mod/sodium/versions
    library(
        "sodium",
        "maven.modrinth",
        "sodium",
    )
        .version("mc1.21-0.5.9")


    // https://modrinth.com/mod/indium/versions
    library(
        "indium",
        "maven.modrinth",
        "indium",
    )
        .version("1.0.33+mc1.21")
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0" }

    rootProject.name = "TheAbyss"

include("versions")
include("versions:1.21")
