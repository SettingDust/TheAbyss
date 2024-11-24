package settingdust.the_abyss

import com.google.common.base.Suppliers
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.worldgen.lithostitched.registry.LithostitchedRegistryKeys
import dev.worldgen.lithostitched.worldgen.modifier.Modifier
import dev.worldgen.lithostitched.worldgen.modifier.PriorityBasedModifier
import dev.worldgen.lithostitched.worldgen.modifier.predicate.ModifierPredicate
import dev.worldgen.lithostitched.worldgen.modifier.util.DensityFunctionWrapper
import net.minecraft.block.Blocks
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.dynamic.CodecHolder
import net.minecraft.world.World
import net.minecraft.world.gen.chunk.AquiferSampler
import net.minecraft.world.gen.chunk.NoiseChunkGenerator
import net.minecraft.world.gen.densityfunction.DensityFunction
import settingdust.the_abyss.mixin.FluidLevelAccessor
import settingdust.the_abyss.mixin.NoiseChunkGeneratorAccessor

data class WrapAquiferModifier(
    val modifierPredicate: ModifierPredicate,
    val priority: Int,
    val dimension: RegistryKey<World>,
    val wrapper: DensityFunction
) : Modifier {
    companion object {
        val MAP_CODEC = RecordCodecBuilder.mapCodec { instance ->
            Modifier.addModifierFields(instance).and(
                instance.group(
                    PriorityBasedModifier.PRIORITY_CODEC.forGetter(WrapAquiferModifier::priority),
                    RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("dimension")
                        .forGetter(WrapAquiferModifier::dimension),
                    DensityFunction.FUNCTION_CODEC.fieldOf("wrapper").forGetter(WrapAquiferModifier::wrapper)
                )
            ).apply(instance, ::WrapAquiferModifier)
        }!!

        val CODEC = MAP_CODEC.codec()

        private val fluidLevel = ThreadLocal<AquiferSampler.FluidLevel>()

        fun NoiseChunkGenerator.wrapAquifer(world: ServerWorld) {
            val registryManager = world.registryManager
            val modifiers =
                registryManager[LithostitchedRegistryKeys.WORLDGEN_MODIFIER].filterIsInstance<WrapAquiferModifier>()
                    .filter { it.dimension == world.registryKey }
                    .sortedBy { it.priority }
                    .map { it.wrapper }
            if (modifiers.isNotEmpty()) {
                val fluidLevelSampler = (this as NoiseChunkGeneratorAccessor).fluidLevelSampler

                val aquiferDensityFunction by lazy { AquiferDensityFunction(fluidLevelSampler.get()) }
                val function by lazy {
                    modifiers.fold<DensityFunction, DensityFunction>(
                        aquiferDensityFunction,
                        DensityFunctionWrapper::wrap
                    )
                }

                (this as NoiseChunkGeneratorAccessor).fluidLevelSampler = Suppliers.memoize {
                    AquiferSampler.FluidLevelSampler { x, y, z ->
                        val value = function.sample(DensityFunction.UnblendedNoisePos(x, y, z)).toInt()
                        val blockState = fluidLevel.get().getBlockState(y)
                        fluidLevel.remove()
                        AquiferSampler.FluidLevel(value, blockState)
                    }
                }
            }
        }
    }

    override fun getPredicate() = modifierPredicate

    override fun getPhase() = Modifier.ModifierPhase.MODIFY

    override fun applyModifier() {
    }

    override fun codec() = CODEC


    data class AquiferDensityFunction(val sampler: AquiferSampler.FluidLevelSampler) : DensityFunction {
        companion object {
            private val CODEC =
                Codec.unit(AquiferDensityFunction { _, _, _ -> AquiferSampler.FluidLevel(0, Blocks.AIR.defaultState) })

            val CODEC_HOLDER: CodecHolder<out DensityFunction> = CodecHolder.of(CODEC)
        }

        private fun apply(pos: DensityFunction.NoisePos): Double {
            val fluidLevel = sampler.getFluidLevel(pos.blockX(), pos.blockY(), pos.blockZ())
            WrapAquiferModifier.Companion.fluidLevel.set(fluidLevel)
            val value = (fluidLevel as FluidLevelAccessor).y.toDouble()
            return value
        }

        override fun sample(pos: DensityFunction.NoisePos) = apply(pos)

        override fun fill(densities: DoubleArray, applier: DensityFunction.EachApplier) {
            for (i in densities.indices) {
                densities[i] = apply(applier.at(i))
            }
        }

        override fun apply(visitor: DensityFunction.DensityFunctionVisitor): DensityFunction {
            return visitor.apply(this)
        }

        override fun minValue() = Double.MIN_VALUE

        override fun maxValue() = Double.MAX_VALUE

        override fun getCodecHolder() = CODEC_HOLDER
    }
}