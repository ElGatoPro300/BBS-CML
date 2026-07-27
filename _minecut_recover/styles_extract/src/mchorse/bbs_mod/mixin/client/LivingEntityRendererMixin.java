package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.class_1309;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_583;
import net.minecraft.class_630;
import net.minecraft.class_922;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_922.class)
public abstract class LivingEntityRendererMixin
{
    @Shadow
    protected class_583<?> model;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V", ordinal = 0, shift = At.Shift.AFTER))
    public void onSetAngles(class_1309 livingEntity, float f, float g, class_4587 matrixStack, class_4597 vertexConsumerProvider, int i, CallbackInfo info)
    {
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

            Map<String, class_630> parts = MobFormRenderer.resolveModelParts(this.model, livingEntity.getClass());

            if (parts != null)
            {
                for (Map.Entry<String, class_630> entry : parts.entrySet())
                {
                    String key = entry.getKey();
                    class_630 value = entry.getValue();
                    PoseTransform poseTransform = pose.transforms.get(key);

                    if (poseTransform != null && poseTransform.fix > 0F)
                    {
                        Transform transform = new Transform();
                        float fix = poseTransform.fix;

                        transform.translate.x = value.field_3657;
                        transform.translate.y = value.field_3656;
                        transform.translate.z = value.field_3655;
                        transform.rotate.x = value.field_3654;
                        transform.rotate.y = value.field_3675;
                        transform.rotate.z = value.field_3674;
                        transform.scale.x = value.field_37938;
                        transform.scale.y = value.field_37939;
                        transform.scale.z = value.field_37940;

                        value.field_3657 = Lerps.lerp(value.field_3657, poseTransform.pivot.x, fix);
                        value.field_3656 = Lerps.lerp(value.field_3656, poseTransform.pivot.y, fix);
                        value.field_3655 = Lerps.lerp(value.field_3655, poseTransform.pivot.z, fix);
                        value.field_3654 = Lerps.lerp(value.field_3654, poseTransform.rotate.x, fix);
                        value.field_3675 = Lerps.lerp(value.field_3675, poseTransform.rotate.y, fix);
                        value.field_3674 = Lerps.lerp(value.field_3674, poseTransform.rotate.z, fix);
                        value.field_37938 = Lerps.lerp(value.field_37938, poseTransform.scale.x, fix);
                        value.field_37939 = Lerps.lerp(value.field_37939, poseTransform.scale.y, fix);
                        value.field_37940 = Lerps.lerp(value.field_37940, poseTransform.scale.z, fix);

                        MobFormRenderer.getCache().put(value, transform);
                    }
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void onRenderEnd(class_1309 livingEntity, float f, float g, class_4587 matrixStack, class_4597 vertexConsumerProvider, int i, CallbackInfo info)
    {
        for (Map.Entry<class_630, Transform> entry : MobFormRenderer.getCache().entrySet())
        {
            Transform transform = entry.getValue();
            class_630 value = entry.getKey();

            value.field_3657 = transform.translate.x;
            value.field_3656 = transform.translate.y;
            value.field_3655 = transform.translate.z;
            value.field_3654 = transform.rotate.x;
            value.field_3675 = transform.rotate.y;
            value.field_3674 = transform.rotate.z;
            value.field_37938 = transform.scale.x;
            value.field_37939 = transform.scale.y;
            value.field_37940 = transform.scale.z;
        }

        MobFormRenderer.getCache().clear();
    }
}