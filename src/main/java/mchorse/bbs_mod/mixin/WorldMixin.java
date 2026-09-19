package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class WorldMixin
{
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", at = @At("HEAD"), require = 0)
    public void onSetBlockStateThreeArgs(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> info)
    {
        this.captureBeforeSetBlockState(pos);
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), require = 0)
    public void onBreakBlockFourArgs(BlockPos pos, boolean drop, Entity breakingEntity, int maxUpdateDepth, CallbackInfoReturnable<Boolean> info)
    {
        this.captureBeforeSetBlockState(pos);
    }

    private void captureBeforeSetBlockState(BlockPos pos)
    {
        if ((Object) this instanceof ServerLevel world)
        {
            BBSMod.getActions().changedBlock(pos, world.getBlockState(pos), world.getBlockEntity(pos));
        }
    }
}
