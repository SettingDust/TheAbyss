package settingdust.the_abyss

import com.google.common.base.Suppliers
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.worldgen.lithostitched.registry.LithostitchedRegistryKeys
import dev.worldgen.lithostitched.worldgen.densityfunction.WrappedMarkerDensityFunction
import dev.worldgen.lithostitched.worldgen.modifier.Modifier
import dev.worldgen.lithostitched.worldgen.modifier.PriorityBasedModifier
import dev.worldgen.lithostitched.worldgen.modifier.WrapDensityFunctionModifier
import dev.worldgen.lithostitched.worldgen.modifier.WrapNoiseRouterModifier
import dev.worldgen.lithostitched.worldgen.modifier.predicate.ModifierPredicate
import dev.worldgen.lithostitched.worldgen.modifier.util.DensityFunctionWrapper
import net.minecraft.block.Blocks
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.dynamic.CodecHolder
import net.minecraft.util.math.Spline
import net.minecraft.world.World
import net.minecraft.world.dimension.DimensionOptions
import net.minecraft.world.gen.chunk.AquiferSampler
import net.minecraft.world.gen.chunk.NoiseChunkGenerator
import net.minecraft.world.gen.densityfunction.DensityFunction
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes
import settingdust.the_abyss.mixin.FluidLevelAccessor
import settingdust.the_abyss.mixin.NoiseChunkGeneratorAccessor
import settingdust.the_abyss.mixin.SimpleRegistryAccessor

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

        fun NoiseChunkGenerator.wrapAquifer(world: ServerWorld) {
            val registryManager = world.registryManager
            val modifiers =
                registryManager[LithostitchedRegistryKeys.WORLDGEN_MODIFIER].filterIsInstance<WrapAquiferModifier>()
                    .filter { it.dimension == world.registryKey }
                    .sortedBy { it.priority }
                    .map { it.wrapper }
            if (modifiers.isNotEmpty()) {
                val fluidLevelSampler = (this as NoiseChunkGeneratorAccessor).fluidLevelSampler

                val aquiferDensityFunction = AquiferDensityFunction(fluidLevelSampler.get())
                val function =
                    modifiers.fold<DensityFunction, DensityFunction>(aquiferDensityFunction) { acc, modifier ->
                        DensityFunctionWrapper.wrap(acc, modifier)
                    }

                (this as NoiseChunkGeneratorAccessor).fluidLevelSampler = Suppliers.memoize {
                    AquiferSampler.FluidLevelSampler { x, y, z ->
                        val value = function.sample(DensityFunction.UnblendedNoisePos(x, y, z)).toInt()
                        val fluidLevel = AquiferDensityFunction.currentFluidLevel.get()
                        (fluidLevel as FluidLevelAccessor).y = value
                        fluidLevel
                    }
                }
            }
        }
    }

    override fun getPredicate() = modifierPredicate

    override fun getPhase() = Modifier.ModifierPhase.NONE

    override fun applyModifier() {
    }

    override fun codec() = CODEC

    data class AquiferDensityFunction(val sampler: AquiferSampler.FluidLevelSampler) : DensityFunction {
        companion object {
            private val CODEC =
                Codec.unit(AquiferDensityFunction { _, _, _ -> AquiferSampler.FluidLevel(0, Blocks.AIR.defaultState) })

            val CODEC_HOLDER: CodecHolder<out DensityFunction> = CodecHolder.of(CODEC)

            val currentFluidLevel = ThreadLocal<AquiferSampler.FluidLevel>()
        }

        private fun apply(pos: DensityFunction.NoisePos): Double {
            val fluidLevel = sampler.getFluidLevel(pos.blockX(), pos.blockY(), pos.blockZ())
            currentFluidLevel.set(fluidLevel)
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

data class OathIslandModifier(
    val modifierPredicate: ModifierPredicate,
    val dimension: RegistryKey<DimensionOptions>,
    val distance: DensityFunction
) : Modifier {
    companion object {
        val MAP_CODEC = RecordCodecBuilder.mapCodec { instance ->
            Modifier.addModifierFields(instance).and(
                instance.group(
                    RegistryKey.createCodec(RegistryKeys.DIMENSION).fieldOf("dimension")
                        .forGetter(OathIslandModifier::dimension),
                    DensityFunction.FUNCTION_CODEC.fieldOf("distance").forGetter(OathIslandModifier::distance)
                )
            ).apply(instance, ::OathIslandModifier)
        }!!

        val CODEC = MAP_CODEC.codec()
    }

    private val prefix = "${dimension.value.namespace}/${dimension.value.path}/oath_island"

    override fun getPredicate() = modifierPredicate

    override fun getPhase() = Modifier.ModifierPhase.ADD

    private fun Registry<DensityFunction>.tryCreateDistance(
        id: RegistryKey<DensityFunction>,
        distance: DensityFunction
    ) {
        if (id !in this) {
            Registry.register(this, id, distance)
        }
    }

    private fun Registry<DensityFunction>.createContinentsWrapper(distance: RegistryEntry<DensityFunction>): RegistryEntry<DensityFunction> {
        val id = RegistryKey.of(key, TheAbyss.id("$prefix/continents"))
        return if (id !in this) {
            val distance = DensityFunctionTypes.Spline.DensityFunctionWrapper(distance)
            val offset = DensityFunctionTypes.spline(
                Spline.builder(distance)
                    .add(0f, 0.3f, 0f)
                    .add(0.3f, 0.6f, 0f)
                    .add(0.4f, 0.2f, 0f)
                    .add(1f, -0.59f, 0f)
                    .build()
            )
            val scale = DensityFunctionTypes.spline(
                Spline.builder(distance)
                    .add(0f, 0.2f, 0f)
                    .add(0.3f, 0.4f, 0f)
                    .add(0.4f, 0.6f, 0f)
                    .add(1f, 0.4f, 0f)
                    .build()
            )

            getEntry(
                Registry.register(
                    this,
                    id,
                    DensityFunctionTypes.cache2d(
                        DensityFunctionTypes.add(
                            offset,
                            DensityFunctionTypes.mul(
                                scale,
                                DensityFunctionTypes.RegistryEntryHolder(
                                    RegistryEntry.Direct(WrappedMarkerDensityFunction())
                                )
                            )
                        )
                    )
                )
            )
        } else getEntry(id).orElseThrow()
    }

    private fun Registry<Modifier>.createContinentsModifier(
        registryAccess: DynamicRegistryManager,
        wrapper: RegistryEntry<DensityFunction>
    ) {
        val id = TheAbyss.id("$prefix/continents")

        val dimensionRegistry = registryAccess.get(RegistryKeys.DIMENSION)
        val dimensionOption = dimensionRegistry.get(dimension)
        val chunkGenerator = dimensionOption?.chunkGenerator
        require(chunkGenerator is NoiseChunkGenerator) { "Target dimension $dimension chunk generator isn't 'noise'" }
        val generatorSettings = chunkGenerator.settings.value()
        val (continentsId, _) = generatorSettings.noiseRouter.continents.unwrap()
        val modifier = if (continentsId == null) {
            WrapNoiseRouterModifier(
                modifierPredicate,
                1000,
                RegistryKey.of(RegistryKeys.WORLD, dimension.value),
                WrapNoiseRouterModifier.Target.CONTINENTS,
                wrapper
            )
        } else {
            WrapDensityFunctionModifier(
                modifierPredicate,
                1000,
                registryAccess.get(RegistryKeys.DENSITY_FUNCTION).getEntry(continentsId).orElseThrow(),
                wrapper
            )
        }
        Registry.register(
            this,
            id,
            modifier
        )
    }

    override fun applyModifier(registryAccess: DynamicRegistryManager) {
        val densityFunctionRegistry = registryAccess.get(RegistryKeys.DENSITY_FUNCTION)
        val worldModifiersRegistry = registryAccess.get(LithostitchedRegistryKeys.WORLDGEN_MODIFIER)

        (densityFunctionRegistry as SimpleRegistryAccessor).setFrozen(false)
        (worldModifiersRegistry as SimpleRegistryAccessor).setFrozen(false)

        val (distanceId, distance) = distance.unwrap(
            RegistryKey.of(
                densityFunctionRegistry.key,
                TheAbyss.id("$prefix/distance")
            )
        )
        densityFunctionRegistry.tryCreateDistance(distanceId, distance)

        val continentsWrapper = densityFunctionRegistry.createContinentsWrapper(densityFunctionRegistry.getEntry(distanceId).orElseThrow())
        worldModifiersRegistry.createContinentsModifier(registryAccess, continentsWrapper)

        (densityFunctionRegistry as SimpleRegistryAccessor).setFrozen(true)
        (worldModifiersRegistry as SimpleRegistryAccessor).setFrozen(true)
    }

    override fun applyModifier() {
    }

    override fun codec() = CODEC
}

data class AbyssifyModifier(
    val modifierPredicate: ModifierPredicate,
    val priority: Int,
    val dimension: RegistryKey<World>,
    val distance: DensityFunction
) : Modifier {
    companion object {
        val MAP_CODEC = RecordCodecBuilder.mapCodec { instance ->
            Modifier.addModifierFields(instance).and(
                instance.group(
                    PriorityBasedModifier.PRIORITY_CODEC.forGetter(AbyssifyModifier::priority),
                    RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("dimension")
                        .forGetter(AbyssifyModifier::dimension),
                    DensityFunction.FUNCTION_CODEC.fieldOf("distance").forGetter(AbyssifyModifier::distance)
                )
            ).apply(instance, ::AbyssifyModifier)
        }!!

        val CODEC = MAP_CODEC.codec()
    }

    override fun getPredicate() = modifierPredicate

    override fun getPhase() = Modifier.ModifierPhase.MODIFY

    override fun applyModifier() {
    }

    override fun codec() = CODEC
}