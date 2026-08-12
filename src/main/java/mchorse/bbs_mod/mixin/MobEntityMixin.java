package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Notify recording clients when vanilla converts a mob into another type
 * (villager → zombie villager, hoglin → zoglin, etc.) so autocapture can
 * hide the old replay and start a new one for the successor.
 */
@Mixin(Mob.class)
public class MobEntityMixin
{
    @Inject(method = "convertTo", at = @At("RETURN"))
    private <T extends Mob> void bbs$onConvertTo(EntityType<T> entityType, ConversionParams context, EntitySpawnReason reason, ConversionParams.AfterConversion<T> finalizer, CallbackInfoReturnable<T> cir)
    {
        T converted = cir.getReturnValue();

        if (converted == null)
        {
            return;
        }

        Mob self = (Mob) (Object) this;

        if (!(self.level() instanceof ServerLevel serverWorld))
        {
            return;
        }

        if (!BBSMod.getActions().hasActiveRecorders(serverWorld))
        {
            return;
        }

        BBSMod.getActions().broadcastMobConversion(serverWorld, self.getId(), converted.getId());
    }
}
