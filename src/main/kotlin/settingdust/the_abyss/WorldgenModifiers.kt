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
import net.minecraft.registry.entry.RegistryElementCodec
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.world.ServerWorld
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
    val wrapper: RegistryEntry<DensityFunction>
) : Modifier {
    companion object {
        val MAP_CODEC = RecordCodecBuilder.mapCodec { instance ->
            Modifier.addModifierFields(instance).and(
                instance.group(
                    PriorityBasedModifier.PRIORITY_CODEC.forGetter(WrapAquiferModifier::priority),
                    RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("dimension")
                        .forGetter(WrapAquiferModifier::dimension),
                    DensityFunction.REGISTRY_ENTRY_CODEC.fieldOf("wrapper").forGetter(WrapAquiferModifier::wrapper)
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
                    .map { it.wrapper.value() }
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
                        val fluidLevel = fluidLevelSampler.get().getFluidLevel(x, y, z)
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

private fun Registry<DensityFunction>.tryCreateDistance(
    id: RegistryKey<DensityFunction>,
    distance: DensityFunction
) {
    if (id !in this) {
        Registry.register(this, id, distance)
    }
}

data class OathIslandModifier(
    val modifierPredicate: ModifierPredicate,
    val dimension: RegistryKey<DimensionOptions>,
    val distance: RegistryEntry<DensityFunction>,
) : Modifier {
    companion object {
        val MAP_CODEC = RecordCodecBuilder.mapCodec { instance ->
            Modifier.addModifierFields(instance).and(
                instance.group(
                    RegistryKey.createCodec(RegistryKeys.DIMENSION).fieldOf("dimension")
                        .forGetter(OathIslandModifier::dimension),
                    DensityFunction.REGISTRY_ENTRY_CODEC.fieldOf("distance").forGetter(OathIslandModifier::distance)
                )
            ).apply(instance, ::OathIslandModifier)
        }!!

        val CODEC = MAP_CODEC.codec()
    }

    private val prefix = "${dimension.value.namespace}/${dimension.value.path}/oath_island"

    override fun getPredicate() = modifierPredicate

    override fun getPhase() = Modifier.ModifierPhase.ADD

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
        val functionRegistry = registryAccess.get(RegistryKeys.DENSITY_FUNCTION)
        val modifiersRegistry = registryAccess.get(LithostitchedRegistryKeys.WORLDGEN_MODIFIER)

        (functionRegistry as SimpleRegistryAccessor).setFrozen(false)
        (modifiersRegistry as SimpleRegistryAccessor).setFrozen(false)

        val (distanceId, distance) = distance.value().unwrap(
            RegistryKey.of(
                functionRegistry.key,
                TheAbyss.id("$prefix/distance")
            )
        )
        functionRegistry.tryCreateDistance(distanceId, distance)

        val continentsWrapper =
            functionRegistry.createContinentsWrapper(functionRegistry.getEntry(distanceId).orElseThrow())
        modifiersRegistry.createContinentsModifier(registryAccess, continentsWrapper)

        (functionRegistry as SimpleRegistryAccessor).setFrozen(true)
        (modifiersRegistry as SimpleRegistryAccessor).setFrozen(true)
    }

    override fun applyModifier() {
    }

    override fun codec() = CODEC
}

data class AbyssModifier(
    val modifierPredicate: ModifierPredicate,
    val dimension: RegistryKey<DimensionOptions>,
    val distance: RegistryEntry<DensityFunction>,
    val offset: RegistryEntry<DensityFunction>
) : Modifier {
    companion object {
        val MAP_CODEC = RecordCodecBuilder.mapCodec { instance ->
            Modifier.addModifierFields(instance).and(
                instance.group(
                    RegistryKey.createCodec(RegistryKeys.DIMENSION).fieldOf("dimension")
                        .forGetter(AbyssModifier::dimension),
                    DensityFunction.REGISTRY_ENTRY_CODEC.fieldOf("distance").forGetter(AbyssModifier::distance),
                    RegistryElementCodec.of(RegistryKeys.DENSITY_FUNCTION, DensityFunction.CODEC, false)
                        .fieldOf("offset").forGetter(AbyssModifier::offset)
                )
            ).apply(instance, ::AbyssModifier)
        }!!

        val CODEC = MAP_CODEC.codec()
    }

    private val prefix = "${dimension.value.namespace}/${dimension.value.path}/abyss"

    override fun getPredicate() = modifierPredicate

    override fun getPhase() = Modifier.ModifierPhase.ADD

    private fun Registry<DensityFunction>.createAquiferWrapper(distance: RegistryEntry<DensityFunction>): RegistryEntry<DensityFunction> {
        val id = RegistryKey.of(key, TheAbyss.id("$prefix/aquifer"))
        return if (id !in this) {
            val offset = DensityFunctionTypes.rangeChoice(
                distance.value(),
                0.0,
                1.02,
                DensityFunctionTypes.constant(-64.0),
                DensityFunctionTypes.constant(0.0)
            )
            val scale = DensityFunctionTypes.rangeChoice(
                distance.value(),
                0.0,
                1.02,
                DensityFunctionTypes.constant(0.0),
                DensityFunctionTypes.constant(1.0)
            )

            getEntry(
                Registry.register(
                    this,
                    id,
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
        } else getEntry(id).orElseThrow()
    }

    private fun Registry<Modifier>.createAquiferModifier(wrapper: RegistryEntry<DensityFunction>) {
        val id = TheAbyss.id("$prefix/aquifer")

        Registry.register(
            this,
            id,
            WrapAquiferModifier(modifierPredicate, 1000, RegistryKey.of(RegistryKeys.WORLD, dimension.value), wrapper)
        )
    }

    private fun Registry<DensityFunction>.createOffsetWrapper(distance: RegistryEntry<DensityFunction>): RegistryEntry<DensityFunction> {
        val id = RegistryKey.of(key, TheAbyss.id("$prefix/offset"))
        return if (id !in this) {
            val distance = DensityFunctionTypes.Spline.DensityFunctionWrapper(distance)
            val offset = DensityFunctionTypes.spline(
                Spline.builder(distance)
                    .add(0.8f, -3.0f, 30f)
                    .add(1f, 0f, 0f)
                    .build()
            )
            val scale = DensityFunctionTypes.spline(
                Spline.builder(distance)
                    .add(0.8f, 0f, 0f)
                    .add(1f, 1f, 0f)
                    .build()
            )

            getEntry(
                Registry.register(
                    this,
                    id,
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
        } else getEntry(id).orElseThrow()
    }

    private fun Registry<Modifier>.createOffsetModifier(wrapper: RegistryEntry<DensityFunction>) {
        val id = TheAbyss.id("$prefix/offset")
        Registry.register(this, id, WrapDensityFunctionModifier(modifierPredicate, 1000, offset, wrapper))
    }

    override fun applyModifier(registryAccess: DynamicRegistryManager) {
        val functionRegistry = registryAccess.get(RegistryKeys.DENSITY_FUNCTION)
        val modifiersRegistry = registryAccess.get(LithostitchedRegistryKeys.WORLDGEN_MODIFIER)

        (functionRegistry as SimpleRegistryAccessor).setFrozen(false)
        (modifiersRegistry as SimpleRegistryAccessor).setFrozen(false)

        val (distanceId, distance) = distance.value().unwrap(
            RegistryKey.of(
                functionRegistry.key,
                TheAbyss.id("$prefix/distance")
            )
        )
        functionRegistry.tryCreateDistance(distanceId, distance)

        val aquiferWrapper =
            functionRegistry.createAquiferWrapper(functionRegistry.getEntry(distanceId).orElseThrow())
        modifiersRegistry.createAquiferModifier(aquiferWrapper)

        val offsetWrapper =
            functionRegistry.createOffsetWrapper(functionRegistry.getEntry(distanceId).orElseThrow())
        modifiersRegistry.createOffsetModifier(offsetWrapper)


        (functionRegistry as SimpleRegistryAccessor).setFrozen(true)
        (modifiersRegistry as SimpleRegistryAccessor).setFrozen(true)
    }

    override fun applyModifier() {
    }

    override fun codec() = CODEC
}