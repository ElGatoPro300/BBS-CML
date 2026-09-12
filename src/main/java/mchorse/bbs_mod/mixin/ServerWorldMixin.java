package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.blocks.BreakBlockActionClip;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerWorldMixin
{
    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
    public void onSetBlockBreakingInfo(int entityId, BlockPos pos, int progress, CallbackInfo info)
    {
        ServerLevel serverWorld = (ServerLevel) (Object) this;
        Entity entity = serverWorld.getEntity(entityId);

        if (entity instanceof ServerPlayer player)
        {
            BBSMod.getActions().addAction(player, () ->
            {
                BreakBlockActionClip clip = new BreakBlockActionClip();

                clip.x.set(pos.getX());
                clip.y.set(pos.getY());
                clip.z.set(pos.getZ());
                clip.progress.set(progress);

                return clip;
            });
        }
    }

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    public void onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> info)
    {
        BBSMod.getActions().spawnedEntity(entity);
    }
}