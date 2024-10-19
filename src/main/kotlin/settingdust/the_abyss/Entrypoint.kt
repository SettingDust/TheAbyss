package settingdust.the_abyss

import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object TheAbyss {
    const val ID: String = "the_abyss"

    fun id(path: String) = Identifier(ID, path)
}

fun init() {
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("distance"), Distance.CODEC_HOLDER.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("lerp"), Lerp.CODEC_HOLDER.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("clamp"), Clamp.CODEC_HOLDER.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("compare"), Compare.CODEC_HOLDER.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("abs_offset"), AbsOffset.CODEC_HOLDER.codec())
}
