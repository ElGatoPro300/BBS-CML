package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MorphMobParticles;
import net.minecraft.class_2394;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_638.class)
public class ClientWorldMixin
{
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(class_2394 parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo info)
    {
        if (MorphMobParticles.shouldSuppress())
        {
            info.cancel();
        }
    }

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZDDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void onAddParticleAlways(class_2394 parameters, boolean alwaysSpawn, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo info)
    {
        if (MorphMobParticles.shouldSuppress())
        {
            info.cancel();
        }
    }

    @Inject(method = "addImportantParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void onAddImportantParticle(class_2394 parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo info)
    {
        if (MorphMobParticles.shouldSuppress())
        {
            info.cancel();
        }
    }
}
