package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1937.class)
public class WorldMixin
{
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", at = @At("HEAD"), require = 0)
    public void onSetBlockStateThreeArgs(class_2338 pos, class_2680 state, int flags, CallbackInfoReturnable<Boolean> info)
    {
        this.captureBeforeSetBlockState(pos);
    }

    @Inject(method = "breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;I)Z", at = @At("HEAD"), require = 0)
    public void onBreakBlockFourArgs(class_2338 pos, boolean drop, class_1297 breakingEntity, int maxUpdateDepth, CallbackInfoReturnable<Boolean> info)
    {
        this.captureBeforeSetBlockState(pos);
    }

    private void captureBeforeSetBlockState(class_2338 pos)
    {
        if ((Object) this instanceof class_3218 world)
        {
            BBSMod.getActions().changedBlock(pos, world.method_8320(pos), world.method_8321(pos));
        }
    }
}
