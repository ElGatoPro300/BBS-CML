package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.FormUtilsClient;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MobForm body uses a private Immediate. Builtin held meshes (trident) only
 * tessellate correctly on the world entity Immediate, same as player-form items.
 */
@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureRendererMixin
{
    @WrapOperation(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        )
    )
    private void bbs$routeBuiltinHeldItems(
        HeldItemRenderer renderer,
        LivingEntity entity,
        ItemStack stack,
        ModelTransformationMode mode,
        boolean leftHanded,
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        Operation<Void> original
    )
    {
        VertexConsumerProvider routed = FormUtilsClient.routeMobFormBuiltinItemConsumers(stack, mode, consumers);

        try
        {
            original.call(renderer, entity, stack, mode, leftHanded, matrices, routed, light);
        }
        finally
        {
            if (routed != consumers)
            {
                FormUtilsClient.clearBuiltinItemTint();
            }
        }
    }
}
