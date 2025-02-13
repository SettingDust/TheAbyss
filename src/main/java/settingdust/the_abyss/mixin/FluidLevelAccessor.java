package settingdust.the_abyss.mixin;

import net.minecraft.world.gen.chunk.AquiferSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AquiferSampler.FluidLevel.class)
public interface FluidLevelAccessor {
    @Accessor
    int getY();

    @Accessor
    @Mutable
    void setY(int y);
}
