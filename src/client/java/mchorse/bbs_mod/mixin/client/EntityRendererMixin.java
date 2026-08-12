package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.IRenderStateEntityHolder;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin
{
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    public void onRenderLabelIfPresent(CallbackInfo info)
    {
        if (FormUtilsClient.getCurrentForm() instanceof MobForm form && form.isPlayer())
        {
            info.cancel();
        }
    }

    @Inject(method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;", at = @At("RETURN"))
    private void bbs$stashRenderedEntity(Entity entity, float tickDelta, CallbackInfoReturnable<EntityRenderState> info)
    {
        EntityRenderState state = info.getReturnValue();

        if (state instanceof IRenderStateEntityHolder holder)
        {
            holder.bbs$setRenderedEntity(entity, tickDelta);
        }
    }
}