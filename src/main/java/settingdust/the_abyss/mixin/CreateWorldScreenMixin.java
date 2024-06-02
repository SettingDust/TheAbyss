package settingdust.the_abyss.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {
    @Shadow
    @Final
    private static Logger LOGGER;

    private CreateWorldScreenMixin(final Text title) {
        super(title);
    }

    @Inject(
        method = "create(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/Screen;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;runTasks(Ljava/util/function/BooleanSupplier;)V"
        ),
        cancellable = true
    )
    private static <R> void datadumper$handleCrash(
        final MinecraftClient client,
        final Screen parent,
        final CallbackInfo ci,
        @Local LocalRef<CompletableFuture<R>> original
    ) {
        original.set(original.get().handle((input, throwable) -> {
            if (throwable != null) {
                LOGGER.warn("Failed to validate datapack", throwable);
                client.setScreen(null);
                ci.cancel();
            }
            return input;
        }));
    }
}
