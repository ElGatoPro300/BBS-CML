package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

import org.spongepowered.asm.mixin.Mixin;

/* TODO(1.21.11 render): EntityRenderManager.render() was completely rewritten to use the
 * state-based pipeline: render(EntityRenderState, CameraRenderState, d, d, d, MatrixStack,
 * OrderedRenderCommandQueue). The old render(Entity, d, d, d, f, MatrixStack,
 * VertexConsumerProvider, int, EntityRenderer) overload no longer exists. The morph render
 * interception (MorphRenderer.renderLivingEntity) needs to be re-hooked into either the
 * new render() method or EntityRenderer.render(EntityRenderState, MatrixStack,
 * VertexConsumerProvider, int) directly. */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin
{
}
