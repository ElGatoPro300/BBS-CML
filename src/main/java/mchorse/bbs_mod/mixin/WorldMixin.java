package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin
{
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", at = @At("HEAD"), require = 0)
    public void onSetBlockStateThreeArgs(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> info)
    {
        this.captureBeforeSetBlockState(pos);
    }

    @Inject(method = "breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;I)Z", at = @At("HEAD"), require = 0)
    public void onBreakBlockFourArgs(BlockPos pos, boolean drop, Entity breakingEntity, int maxUpdateDepth, CallbackInfoReturnable<Boolean> info)
    {
        this.captureBeforeSetBlockState(pos);
    }

    private void captureBeforeSetBlockState(BlockPos pos)
    {
        if ((Object) this instanceof ServerWorld world)
        {
            BBSMod.getActions().changedBlock(pos, world.getBlockState(pos), world.getBlockEntity(pos));
        }
    }
}
