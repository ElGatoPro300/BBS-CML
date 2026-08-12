package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.morphing.Morph;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin
{
    @Inject(method = "getRenderOffset", at = @At("HEAD"), cancellable = true)
    public void onPositionOffset(AvatarRenderState state, CallbackInfoReturnable<Vec3> info)
    {
        Level world = Minecraft.getInstance().level;
        Entity entity = world != null ? world.getEntity(state.id) : null;

        if (entity instanceof AbstractClientPlayer abstractClientPlayerEntity)
        {
            Morph morph = Morph.getMorph(abstractClientPlayerEntity);

            if (morph != null && morph.getForm() != null)
            {
                info.setReturnValue(Vec3.ZERO);
            }
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    public void onRenderArmBegin(PoseStack matrices, SubmitNodeCollector queue, int light, Identifier skin, ModelPart arm, boolean sleeve, CallbackInfo info)
    {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        Morph morph = Morph.getMorph(player);

        if (morph != null)
        {
            Form form = morph.getForm();

            if (form != null)
            {
                FormRenderer renderer = FormUtilsClient.getRenderer(form);
                HumanoidModel<?> model = (HumanoidModel<?>) ((AvatarRenderer) (Object) this).getModel();
                InteractionHand hand = model.rightArm == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

                if (renderer != null && renderer.renderArm(matrices, light, player, hand))
                {
                    info.cancel();
                }
            }
        }
    }
}