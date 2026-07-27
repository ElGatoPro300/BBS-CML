package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.film.RecorderMobCapture;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_3966;
import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_636.class)
public class ClientPlayerInteractionManagerMixin
{
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void bbs$onAttackEntity(class_1657 player, class_1297 target, CallbackInfo info)
    {
        RecorderMobCapture.onEntityInteraction(target);
    }

    @Inject(method = "interactEntity", at = @At("HEAD"))
    private void bbs$onInteractEntity(class_1657 player, class_1297 entity, class_1268 hand, CallbackInfoReturnable<class_1269> info)
    {
        RecorderMobCapture.onEntityInteraction(entity);
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"))
    private void bbs$onInteractEntityAtLocation(class_1657 player, class_1297 entity, class_3966 hitResult, class_1268 hand, CallbackInfoReturnable<class_1269> info)
    {
        RecorderMobCapture.onEntityInteraction(entity);
    }
}
