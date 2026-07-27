package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.blocks.BreakBlockActionClip;
import net.minecraft.class_1297;
import net.minecraft.class_2338;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_3218.class)
public class ServerWorldMixin
{
    @Inject(method = "setBlockBreakingInfo", at = @At("HEAD"))
    public void onSetBlockBreakingInfo(int entityId, class_2338 pos, int progress, CallbackInfo info)
    {
        class_3218 serverWorld = (class_3218) (Object) this;
        class_1297 entity = serverWorld.method_8469(entityId);

        if (entity instanceof class_3222 player)
        {
            BBSMod.getActions().addAction(player, () ->
            {
                BreakBlockActionClip clip = new BreakBlockActionClip();

                clip.x.set(pos.method_10263());
                clip.y.set(pos.method_10264());
                clip.z.set(pos.method_10260());
                clip.progress.set(progress);

                return clip;
            });
        }
    }

    @Inject(method = "spawnEntity", at = @At("HEAD"))
    public void onSpawnEntity(class_1297 entity, CallbackInfoReturnable<Boolean> info)
    {
        BBSMod.getActions().spawnedEntity(entity);
    }
}