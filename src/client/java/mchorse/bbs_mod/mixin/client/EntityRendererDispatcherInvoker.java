package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.render.entity.EntityRenderManager;

import org.spongepowered.asm.mixin.Mixin;

/* TODO(1.21.11 render): EntityRenderManager (formerly EntityRenderDispatcher)
 * no longer has a renderShadow method or the old render(Entity,...) overload.
 * Shadow rendering is now handled internally by the state-based render pipeline.
 * This mixin is an empty stub until shadow-rendering hooks are re-ported. */
@Mixin(EntityRenderManager.class)
public interface EntityRendererDispatcherInvoker
{
}
