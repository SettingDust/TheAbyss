package settingdust.the_abyss.mixin;

import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NoiseChunkGenerator.class)
public class NoiseChunkGeneratorMixin {
    /**
     * @author SettingDust
     * @reason Fix <a href="https://bugs.mojang.com/browse/MC-237017">MC 237017</a>
     */
    @Overwrite
    private static AquiferSampler.FluidLevelSampler createFluidLevelSampler(ChunkGeneratorSettings settings) {
        return (x, y, z) -> new AquiferSampler.FluidLevel(settings.seaLevel(), settings.defaultFluid());
    }
}
