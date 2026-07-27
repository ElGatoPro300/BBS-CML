package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MorphRenderer;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_897;
import net.minecraft.class_898;
import net.minecraft.class_922;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_898.class)
public class EntityRenderDispatcherMixin
{
    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        )
    )
    private <E extends class_1297> void wrapRender(
        class_897<E> renderer, E entity, float yaw, float tickDelta,
        class_4587 matrices, class_4597 vcp, int light,
        Operation<Void> original
    ) {
        if (entity instanceof class_1309 livingEntity)
        {
            float whiteOverlayProgress = 0;

            if (renderer instanceof LivingEntityRendererInvoker invoker)
            {
                whiteOverlayProgress = invoker.bbs$getAnimationCounter(livingEntity, tickDelta);
            }

            int o = class_922.method_23622(livingEntity, whiteOverlayProgress);

            if (MorphRenderer.renderLivingEntity(livingEntity, yaw, tickDelta, matrices, vcp, light, o))
            {
                return;
            }
        }

        original.call(renderer, entity, yaw, tickDelta, matrices, vcp, light);
    }
}