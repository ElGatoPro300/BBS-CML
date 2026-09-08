package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.bridge.IEntityRenderState;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin
{
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void onUpdateRenderState(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo info)
    {
        ((IEntityRenderState) state).bbs$setEntity(entity);
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    public void onRenderLabelIfPresent(CallbackInfo info)
    {
        if (FormUtilsClient.getCurrentForm() instanceof MobForm form && form.isPlayer())
        {
            info.cancel();
        }
    }
}