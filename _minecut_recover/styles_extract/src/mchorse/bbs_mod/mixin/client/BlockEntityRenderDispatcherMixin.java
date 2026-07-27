package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.class_2586;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_824;
import net.minecraft.class_827;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_824.class)
public class BlockEntityRenderDispatcherMixin
{
    @Inject(method = "render(Lnet/minecraft/client/render/block/entity/BlockEntityRenderer;Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
    private static void onRenderMain(class_827<?> renderer, class_2586 blockEntity, float tickDelta, class_4587 matrices, class_4597 vertexConsumers, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaBlockEntity(blockEntity))
        {
            info.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderToo(class_2586 blockEntity, float tickDelta, class_4587 matrices, class_4597 vertexConsumers, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaBlockEntity(blockEntity))
        {
            info.cancel();
        }
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    public void onRenderEntity(class_2586 blockEntity, class_4587 matrices, class_4597 vertexConsumers, int light, int overlay, CallbackInfoReturnable<Boolean> info)
    {
        if (BBSRendering.shouldHideChromaBlockEntity(blockEntity))
        {
            info.setReturnValue(false);
        }
    }
}
