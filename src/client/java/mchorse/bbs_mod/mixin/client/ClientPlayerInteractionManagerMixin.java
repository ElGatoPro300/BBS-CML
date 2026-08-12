package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.film.RecorderMobCapture;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin
{
    @Inject(method = "attack", at = @At("HEAD"))
    private void bbs$onAttackEntity(Player player, Entity target, CallbackInfo info)
    {
        RecorderMobCapture.onEntityInteraction(target);
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void bbs$onInteractEntity(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info)
    {
        RecorderMobCapture.onEntityInteraction(entity);
    }

    @Inject(method = "interactAt", at = @At("HEAD"))
    private void bbs$onInteractEntityAtLocation(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info)
    {
        RecorderMobCapture.onEntityInteraction(entity);
    }
}
