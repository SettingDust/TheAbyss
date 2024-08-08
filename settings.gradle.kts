val minecraft = "1.21"
extra["minecraft"] = minecraft

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/common.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/kotlin.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/fabric.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/mixin.gradle.kts")

dependencyResolutionManagement.versionCatalogs.named("catalog") {

    // https://modrinth.com/mod/worldgen-devtools/versions
    library(
            "worldgen-devtools",
            "maven.modrinth",
            "worldgen-devtools",
        )
        .version("1.1.0+$minecraft")

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
        .version("7.0.0+$minecraft")

    // https://modrinth.com/mod/modernfix/versions
    library(
            "modernfix",
            "maven.modrinth",
            "modernfix",
        )
        .version("5.18.3+mc$minecraft")

    // https://modrinth.com/mod/world-preview/versions
    library(
            "worldPreview",
            "maven.modrinth",
            "world-preview",
        )
        .version("1.3.0-fabric,$minecraft")


    // https://modrinth.com/mod/distanthorizons/versions
    library(
        "distanthorizons",
        "maven.modrinth",
        "distanthorizons",
    )
        .version("2.1.2-a-$minecraft")


    // https://modrinth.com/mod/iris/versions
    library(
        "iris",
        "maven.modrinth",
        "iris",
    )
        .version("1.7.1+$minecraft")


    // https://modrinth.com/mod/sodium/versions
    library(
        "sodium",
        "maven.modrinth",
        "sodium",
    )
        .version("mc$minecraft-0.5.9")


    // https://modrinth.com/mod/indium/versions
    library(
        "indium",
        "maven.modrinth",
        "indium",
    )
        .version("1.0.33+mc$minecraft")


    // https://modrinth.com/mod/indium/versions
    library(
        "indium",
        "maven.modrinth",
        "indium",
    )
        .version("1.0.33+mc$minecraft")
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" }

rootProject.name = "TheAbyss"
