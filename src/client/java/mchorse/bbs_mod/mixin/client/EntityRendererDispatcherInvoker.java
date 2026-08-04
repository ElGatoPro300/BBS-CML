package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;

import org.joml.Quaternionf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// TODO(1.21.11 render): re-port and re-enable in bbs.client.mixins.json
// EntityRenderDispatcher was renamed to EntityRenderManager; renderShadow no longer
// exists in the state-based render pipeline. @Invoker is an inert string here (mixin
// not applied at runtime) but bbs$renderShadow callers should treat this as a no-op.
@Mixin(EntityRenderManager.class)
public interface EntityRendererDispatcherInvoker
{
    @Invoker("renderShadow")
    public static void bbs$renderShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float opacity, float tickDelta, WorldView world, float radius)
    {}
}