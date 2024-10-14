package settingdust.the_abyss

import com.bawnorton.mixinsquared.api.MixinCanceller

class TheAbyssMixinCanceller : MixinCanceller {
    override fun shouldCancel(targetClassNames: MutableList<String>, mixinClassName: String): Boolean {
        if (mixinClassName == "com.ishland.c2me.opts.worldgen.vanilla.mixin.aquifer.MixinNoiseChunkGenerator") return true
        return false
    }
}