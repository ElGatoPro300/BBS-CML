package mchorse.bbs_mod.mixin.client.sodium;

import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SodiumBufferBuilder.class)
public interface SodiumBufferBuilderAccessor
{
    @Accessor(value = "builder", remap = false)
    public ExtendedBufferBuilder bbs$getBuilder();
}