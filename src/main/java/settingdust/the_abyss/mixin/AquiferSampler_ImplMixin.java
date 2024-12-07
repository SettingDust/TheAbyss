package settingdust.the_abyss.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import settingdust.the_abyss.TheAbyss;

@Mixin(AquiferSampler.Impl.class)
public class AquiferSampler_ImplMixin {
//    @Inject(method = "apply", at = @At("RETURN"))
//    private void the_abyss$hook(
//        final DensityFunction.NoisePos pos,
//        final double density,
//        final CallbackInfoReturnable<BlockState> cir
//    ) {
//        if (pos.blockY() > 63 || pos.blockY() < 57 || pos.blockX() < 200 || pos.blockZ() < 200) return;
//        if (cir.getReturnValue() == null || !cir.getReturnValue().isAir()) return;
//        TheAbyss.INSTANCE.getLOGGER().info(
//            pos.blockX() + " " + pos.blockY() + " " + pos.blockZ() + " Density: " + density + " BlockState: " +
//            cir.getReturnValue());
//    }
}
