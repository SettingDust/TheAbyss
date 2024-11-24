package settingdust.the_abyss.mixin;

import com.mojang.datafixers.DataFixer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkStatusChangeListener;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import settingdust.the_abyss.WrapAquiferModifier;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
    @Inject(
        method = "<init>",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;chunkGenerator:Lnet/minecraft/world/gen/chunk/ChunkGenerator;"
        )
    )
    private void the_abyss$replaceAquaifier(
        final ServerWorld world,
        final LevelStorage.Session session,
        final DataFixer dataFixer,
        final StructureTemplateManager structureTemplateManager,
        final Executor executor,
        final ThreadExecutor mainThreadExecutor,
        final ChunkProvider chunkProvider,
        final ChunkGenerator chunkGenerator,
        final WorldGenerationProgressListener worldGenerationProgressListener,
        final ChunkStatusChangeListener chunkStatusChangeListener,
        final Supplier persistentStateManagerFactory,
        final int viewDistance,
        final boolean dsync,
        final CallbackInfo ci
    ) {
        if (chunkGenerator instanceof NoiseChunkGenerator noiseChunkGenerator)
            WrapAquiferModifier.Companion.wrapAquifer(noiseChunkGenerator, world);
    }
}
