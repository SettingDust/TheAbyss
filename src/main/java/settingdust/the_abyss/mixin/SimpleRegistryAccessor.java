package settingdust.the_abyss.mixin;

import net.minecraft.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleRegistry.class)
public interface SimpleRegistryAccessor {
    @Accessor
    @Mutable
    void setFrozen(boolean frozen);
}
