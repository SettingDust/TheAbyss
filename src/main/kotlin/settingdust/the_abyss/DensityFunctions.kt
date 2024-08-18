package settingdust.the_abyss

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import net.minecraft.util.dynamic.CodecHolder
import net.minecraft.util.math.MathHelper
import net.minecraft.world.gen.densityfunction.DensityFunction
import net.minecraft.world.gen.densityfunction.DensityFunction.DensityFunctionVisitor

/**
 * https://gitlab.com/Akjosch/worldgen-helpers/-/blob/master/src/main/java/de/vernideas/mc/worldgen/densityfunction/Distance.java?ref_type=heads
 */
data class Distance(val max: Double, val min: Double = 0.0) : DensityFunction.Base {
    override fun getCodecHolder() = CODEC_HOLDER

    override fun maxValue() = 1.0

    override fun minValue() = 0.0

    override fun sample(pos: DensityFunction.NoisePos): Double {
        val distanceSquared = pos.blockX() * pos.blockX() + pos.blockZ() * pos.blockZ() + 0.0
        val distance = sqrt(distanceSquared)
        return when {
            (min > 0.0 || max <= min) && distance <= min -> 0.0
            else -> distance / max
        }
    }

    init {
        require(!(max < min)) { String.format("Max smaller than min: %f < %f", max, min) }
    }

    companion object {
        val CODEC: MapCodec<Distance> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        Codec.doubleRange(1.0, 1000000000.0)
                            .fieldOf("max")
                            .forGetter(Distance::max),
                        Codec.doubleRange(0.0, 1000000000.0)
                            .optionalFieldOf("min", 0.0)
                            .forGetter(Distance::min))
                    .apply(instance, ::Distance)
            }
        private val CODEC_HOLDER: CodecHolder<Distance> = CodecHolder.of(CODEC)
    }
}

/**
 * https://gitlab.com/Akjosch/worldgen-helpers/-/blob/master/src/main/java/de/vernideas/mc/worldgen/densityfunction/Lerp.java?ref_type=heads
 */
data class Lerp(
    val value1: DensityFunction,
    val value2: DensityFunction,
    val alpha: DensityFunction
) : DensityFunction {
    override fun sample(pos: DensityFunction.NoisePos): Double {
        val value = alpha.sample(pos)
        return when {
            value <= 0 -> value1.sample(pos)
            value >= 1 -> value2.sample(pos)
            else -> (1 - value) * value1.sample(pos) + value * value2.sample(pos)
        }
    }

    override fun fill(densities: DoubleArray, applier: DensityFunction.EachApplier) {
        val alphas = DoubleArray(densities.size)
        alpha.fill(alphas, applier)
        for (i in densities.indices) {
            when {
                alphas[i] <= 0.0 -> densities[i] = value1.sample(applier.at(i))
                alphas[i] >= 1.0 -> densities[i] = value2.sample(applier.at(i))
                else ->
                    densities[i] =
                        (1 - alphas[i]) * value1.sample(applier.at(i)) +
                            alphas[i] * value2.sample(applier.at(i))
            }
        }
    }

    override fun apply(visitor: DensityFunction.DensityFunctionVisitor): DensityFunction {
        return Lerp(value1.apply(visitor), value2.apply(visitor), alpha.apply(visitor))
    }

    override fun minValue() = min(value1.minValue(), value2.minValue())

    override fun maxValue() = max(value1.maxValue(), value2.maxValue())

    override fun getCodecHolder() = CODEC_HOLDER

    companion object {
        val CODEC: MapCodec<Lerp> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        DensityFunction.FUNCTION_CODEC.fieldOf("value1").forGetter(Lerp::value1),
                        DensityFunction.FUNCTION_CODEC.fieldOf("value2").forGetter(Lerp::value2),
                        DensityFunction.FUNCTION_CODEC.fieldOf("alpha").forGetter(Lerp::alpha))
                    .apply(instance, ::Lerp)
            }
        private val CODEC_HOLDER: CodecHolder<out DensityFunction> = CodecHolder.of(CODEC)
    }
}

data class Clamp(
    val input: DensityFunction,
    val minValue: DensityFunction,
    val maxValue: DensityFunction
) : DensityFunction {
    private fun apply(density: Double, minValue: Double, maxValue: Double): Double {
        return MathHelper.clamp(density, minValue, maxValue)
    }

    override fun sample(pos: DensityFunction.NoisePos): Double {
        return this.apply(input.sample(pos), minValue.sample(pos), maxValue.sample(pos))
    }

    override fun fill(densities: DoubleArray, applier: DensityFunction.EachApplier) {
        input.fill(densities, applier)

        for (i in densities.indices) {
            val pos = applier.at(i)
            densities[i] = this.apply(densities[i], minValue.sample(pos), maxValue.sample(pos))
        }
    }

    override fun apply(visitor: DensityFunctionVisitor): DensityFunction {
        return Clamp(input.apply(visitor), this.minValue, this.maxValue)
    }

    override fun minValue() = minValue.minValue()

    override fun maxValue() = maxValue.maxValue()

    override fun getCodecHolder() = CODEC_HOLDER

    companion object {
        val CODEC: MapCodec<Clamp> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        DensityFunction.FUNCTION_CODEC.fieldOf("input").forGetter(Clamp::input),
                        DensityFunction.FUNCTION_CODEC.fieldOf("min").forGetter { it.minValue },
                        DensityFunction.FUNCTION_CODEC.fieldOf("max").forGetter { it.maxValue })
                    .apply(instance, ::Clamp)
            }
        val CODEC_HOLDER: CodecHolder<Clamp> = CodecHolder.of(CODEC)
    }
}

data class Compare(
    val argument1: DensityFunction,
    val argument2: DensityFunction,
    val gte: DensityFunction,
    val lt: DensityFunction
) : DensityFunction {
    override fun sample(pos: DensityFunction.NoisePos): Double {
        return if (argument1.sample(pos) >= argument2.sample(pos)) gte.sample(pos)
        else lt.sample(pos)
    }

    override fun fill(densities: DoubleArray, applier: DensityFunction.EachApplier) {
        val arguments1 = DoubleArray(densities.size)
        val arguments2 = DoubleArray(densities.size)
        argument1.fill(arguments1, applier)
        argument2.fill(arguments2, applier)

        for (i in densities.indices) {
            if (arguments1[i] >= arguments2[i]) {
                densities[i] = gte.sample(applier.at(i))
            } else {
                densities[i] = lt.sample(applier.at(i))
            }
        }
    }

    override fun apply(visitor: DensityFunctionVisitor): DensityFunction {
        return Compare(
            argument1.apply(visitor),
            argument2.apply(visitor),
            gte.apply(visitor),
            lt.apply(visitor))
    }

    override fun minValue() = lt.minValue()

    override fun maxValue() = gte.maxValue()

    override fun getCodecHolder() = CODEC_HOLDER

    companion object {
        val CODEC: MapCodec<Compare> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        DensityFunction.FUNCTION_CODEC.fieldOf("argument1")
                            .forGetter(Compare::argument1),
                        DensityFunction.FUNCTION_CODEC.fieldOf("argument2")
                            .forGetter(Compare::argument2),
                        DensityFunction.FUNCTION_CODEC.fieldOf("gte").forGetter(Compare::gte),
                        DensityFunction.FUNCTION_CODEC.fieldOf("lt").forGetter(Compare::lt))
                    .apply(instance, ::Compare)
            }
        val CODEC_HOLDER: CodecHolder<Compare> = CodecHolder.of(CODEC)
    }
}

data class AbsOffset(val input: DensityFunction, val offset: DensityFunction) : DensityFunction {
    override fun sample(pos: DensityFunction.NoisePos): Double {
        val result = input.sample(pos)
        return if (result >= 0) result + offset.sample(pos) else result - offset.sample(pos)
    }

    override fun fill(densities: DoubleArray, applier: DensityFunction.EachApplier) {
        input.fill(densities, applier)

        for (i in densities.indices) {
            if (densities[i] >= 0) densities[i] += offset.sample(applier.at(i))
            else densities[i] -= offset.sample(applier.at(i))
        }
    }

    override fun apply(visitor: DensityFunctionVisitor): DensityFunction {
        return AbsOffset(input.apply(visitor), offset.apply(visitor))
    }

    override fun minValue() = input.minValue() - offset.minValue()

    override fun maxValue() = input.maxValue() + offset.maxValue()

    override fun getCodecHolder() = CODEC_HOLDER

    companion object {
        val CODEC: MapCodec<AbsOffset> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        DensityFunction.FUNCTION_CODEC.fieldOf("input").forGetter(AbsOffset::input),
                        DensityFunction.FUNCTION_CODEC.fieldOf("offset")
                            .forGetter(AbsOffset::offset))
                    .apply(instance, ::AbsOffset)
            }
        val CODEC_HOLDER: CodecHolder<AbsOffset> = CodecHolder.of(CODEC)
    }
}
