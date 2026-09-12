package mchorse.bbs_mod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PoseStack.class)
public class MatrixStackMixin
{
    @Shadow
    private List<PoseStack.Pose> poses;

    @Inject(method = "popPose", at = @At("HEAD"), cancellable = true)
    private void bbs$preventUnderflow(CallbackInfo info)
    {
        if (this.poses.size() <= 1)
        {
            info.cancel();
        }
    }

    @Inject(method = "last", at = @At("HEAD"))
    private void bbs$ensureNotEmptyPeek(CallbackInfoReturnable<PoseStack.Pose> info)
    {
        if (this.poses.isEmpty())
        {
            this.poses.add(new PoseStack().last());
        }
    }

    @Inject(method = "pushPose", at = @At("HEAD"))
    private void bbs$ensureNotEmptyPush(CallbackInfo info)
    {
        if (this.poses.isEmpty())
        {
            this.poses.add(new PoseStack().last());
        }
    }
}
