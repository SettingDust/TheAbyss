package settingdust.the_abyss

import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object TheAbyss {
    const val ID: String = "the_abyss"

    fun id(path: String) = Identifier.of(ID, path)
}

fun init() {
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("distance"), Distance.CODEC.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("lerp"), Lerp.CODEC.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("clamp"), Clamp.CODEC.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("compare"), Compare.CODEC.codec())
    Registry.register(Registries.DENSITY_FUNCTION_TYPE, TheAbyss.id("abs_offset"), AbsOffset.CODEC.codec())
}
