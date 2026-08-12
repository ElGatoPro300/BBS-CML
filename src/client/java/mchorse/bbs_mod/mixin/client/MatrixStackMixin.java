package mchorse.bbs_mod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PoseStack.class)
public class MatrixStackMixin
{
    @Inject(method = "popPose", at = @At("HEAD"), cancellable = true)
    private void bbs$preventUnderflow(CallbackInfo info)
    {
        PoseStack self = (PoseStack) (Object) this;

        if (self.poses.size() <= 1)
        {
            info.cancel();
        }
    }

    @Inject(method = "last", at = @At("HEAD"))
    private void bbs$ensureNotEmptyPeek(CallbackInfoReturnable<PoseStack.Pose> info)
    {
        PoseStack self = (PoseStack) (Object) this;

        if (self.poses.isEmpty())
        {
            self.poses.add(new PoseStack().last());
        }
    }

    @Inject(method = "pushPose", at = @At("HEAD"))
    private void bbs$ensureNotEmptyPush(CallbackInfo info)
    {
        PoseStack self = (PoseStack) (Object) this;

        if (self.poses.isEmpty())
        {
            self.poses.add(new PoseStack().last());
        }
    }
}
