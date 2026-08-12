package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.bridge.IEntityRenderState;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin
{
    @Shadow
    protected EntityModel<?> model;

    @Inject(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setAngles(Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER))
    public void onSetAngles(LivingEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo info)
    {
        Entity entity = ((IEntityRenderState) state).bbs$getEntity();

        if (!(entity instanceof LivingEntity livingEntity))
        {
            return;
        }

        Pose pose = MobFormRenderer.getCurrentPose();
        Pose poseOverlay = MobFormRenderer.getCurrentPoseOverlay();

        if (pose != null)
        {
            pose = pose.copy();

            for (Map.Entry<String, PoseTransform> transformEntry : poseOverlay.transforms.entrySet())
            {
                PoseTransform poseTransform = pose.get(transformEntry.getKey());
                PoseTransform value = transformEntry.getValue();

                if (value.fix != 0)
                {
                    poseTransform.translate.lerp(value.translate, value.fix);
                    poseTransform.scale.lerp(value.scale, value.fix);
                    poseTransform.rotate.lerp(value.rotate, value.fix);
                    poseTransform.rotate2.lerp(value.rotate2, value.fix);
                }
                else
                {
                    poseTransform.translate.add(value.translate);
                    poseTransform.scale.add(value.scale).sub(1, 1, 1);
                    poseTransform.rotate.add(value.rotate);
                    poseTransform.rotate2.add(value.rotate2);
                }
            }

            Map<String, ModelPart> parts = MobFormRenderer.resolveModelParts(this.model, livingEntity.getClass());

            if (parts != null)
            {
                for (Map.Entry<String, ModelPart> entry : parts.entrySet())
                {
                    String key = entry.getKey();
                    ModelPart value = entry.getValue();
                    PoseTransform poseTransform = pose.transforms.get(key);

                    if (poseTransform != null && poseTransform.fix > 0F)
                    {
                        Transform transform = new Transform();
                        float fix = poseTransform.fix;

                        transform.translate.x = value.x;
                        transform.translate.y = value.y;
                        transform.translate.z = value.z;
                        transform.rotate.x = value.xRot;
                        transform.rotate.y = value.yRot;
                        transform.rotate.z = value.zRot;
                        transform.scale.x = value.xScale;
                        transform.scale.y = value.yScale;
                        transform.scale.z = value.zScale;

                        value.x += poseTransform.translate.x;
                        value.y += poseTransform.translate.y;
                        value.z += poseTransform.translate.z;
                        value.xRot += poseTransform.rotate.x;
                        value.yRot += poseTransform.rotate.y;
                        value.zRot += poseTransform.rotate.z;
                        value.xScale += poseTransform.scale.x - 1F;
                        value.yScale += poseTransform.scale.y - 1F;
                        value.zScale += poseTransform.scale.z - 1F;

                        MobFormRenderer.getCache().put(value, transform);
                    }
                }
            }
        }
    }

    @Inject(method = "submit", at = @At("TAIL"))
    public void onRenderEnd(LivingEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo info)
    {
        for (Map.Entry<ModelPart, Transform> entry : MobFormRenderer.getCache().entrySet())
        {
            Transform transform = entry.getValue();
            ModelPart value = entry.getKey();

            value.x = transform.translate.x;
            value.y = transform.translate.y;
            value.z = transform.translate.z;
            value.xRot = transform.rotate.x;
            value.yRot = transform.rotate.y;
            value.zRot = transform.rotate.z;
            value.xScale = transform.scale.x;
            value.yScale = transform.scale.y;
            value.zScale = transform.scale.z;
        }

        MobFormRenderer.getCache().clear();
    }
}