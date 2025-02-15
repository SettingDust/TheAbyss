package settingdust.the_abyss

import dev.worldgen.lithostitched.registry.LithostitchedBuiltInRegistries
import dev.worldgen.lithostitched.registry.LithostitchedRegistryKeys
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import org.apache.logging.log4j.LogManager

object TheAbyss {
    const val ID: String = "the_abyss"
    val LOGGER = LogManager.getLogger()

    fun id(path: String) = Identifier(ID, path)
}

fun init() {
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("distance"), Distance.CODEC_HOLDER.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("lerp"), Lerp.CODEC_HOLDER.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("clamp"), Clamp.CODEC_HOLDER.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("compare"), Compare.CODEC_HOLDER.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("abs_offset"), AbsOffset.CODEC_HOLDER.codec())
    Registry.register(
        Registries.DENSITY_FUNCTION_TYPE,
        TheAbyss.id("internal/aquifer"),
        WrapAquiferModifier.AquiferDensityFunction.CODEC_HOLDER.codec()
    )

    Registry.register(
        LithostitchedBuiltInRegistries.MODIFIER_TYPE,
        TheAbyss.id("wrap_aquifer"),
        WrapAquiferModifier.CODEC
    )

    Registry.register(
        LithostitchedBuiltInRegistries.MODIFIER_TYPE,
        TheAbyss.id("oath_island"),
        OathIslandModifier.CODEC
    )
    Registry.register(
        LithostitchedBuiltInRegistries.MODIFIER_TYPE,
        TheAbyss.id("abyss"),
        AbyssModifier.CODEC
    )
}
