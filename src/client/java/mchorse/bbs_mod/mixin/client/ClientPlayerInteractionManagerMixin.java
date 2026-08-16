package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.film.RecorderMobCapture;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin
{
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void bbs$onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info)
    {
        RecorderMobCapture.onEntityInteraction(target);
    }

    @Inject(method = "interactEntity", at = @At("HEAD"))
    private void bbs$onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> info)
    {
        RecorderMobCapture.onEntityInteraction(entity);
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"))
    private void bbs$onInteractEntityAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> info)
    {
        RecorderMobCapture.onEntityInteraction(entity);
    }
}
