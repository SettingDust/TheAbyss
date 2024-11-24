package settingdust.the_abyss.mixin;

import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(NoiseChunkGenerator.class)
public interface NoiseChunkGeneratorAccessor {
    @Mutable
    @Accessor
    void setFluidLevelSampler(Supplier<AquiferSampler.FluidLevelSampler> value);

    @Accessor
    Supplier<AquiferSampler.FluidLevelSampler> getFluidLevelSampler();
}
