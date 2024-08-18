subprojects {
    apply(plugin = "idea")
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    apply(plugin = rootProject.catalog.plugins.kotlin.jvm.get().pluginId)
    apply(plugin = rootProject.catalog.plugins.kotlin.plugin.serialization.get().pluginId)

    apply(plugin = rootProject.catalog.plugins.fabric.loom.get().pluginId)
}
