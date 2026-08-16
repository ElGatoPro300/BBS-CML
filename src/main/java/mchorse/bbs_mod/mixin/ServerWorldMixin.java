package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.blocks.BreakBlockActionClip;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin
{
    @Inject(method = "setBlockBreakingInfo", at = @At("HEAD"))
    public void onSetBlockBreakingInfo(int entityId, BlockPos pos, int progress, CallbackInfo info)
    {
        ServerWorld serverWorld = (ServerWorld) (Object) this;
        Entity entity = serverWorld.getEntityById(entityId);

        if (entity instanceof ServerPlayerEntity player)
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

    @Inject(method = "spawnEntity", at = @At("HEAD"))
    public void onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> info)
    {
        BBSMod.getActions().spawnedEntity(entity);
    }
}