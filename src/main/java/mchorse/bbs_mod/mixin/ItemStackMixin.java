package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.item.UseBlockItemActionClip;
import mchorse.bbs_mod.actions.types.item.UseItemActionClip;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin
{
    @Inject(method = "use", at = @At("HEAD"))
    public void onUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info)
    {
        if (user instanceof ServerPlayer player)
        {
            ItemStack stack = user.getItemInHand(hand);

            if (stack.getUseDuration(user) > 0)
            {
                return;
            }
            BBSMod.getActions().addAction(player, () ->
            {
                UseItemActionClip clip = new UseItemActionClip();

                clip.itemStack.set(stack.copy());
                clip.hand.set(hand == InteractionHand.MAIN_HAND);

                return clip;
            });
        }
    }

    @Inject(method = "useOn", at = @At("HEAD"))
    public void onUseOnBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> info)
    {
        if (context.getPlayer() instanceof ServerPlayer player)
        {
            BBSMod.getActions().addAction(player, () ->
            {
                UseBlockItemActionClip clip = new UseBlockItemActionClip();

                clip.hit.setHitResult(context);
                clip.itemStack.set(context.getItemInHand().copy());
                clip.hand.set(context.getHand() == InteractionHand.MAIN_HAND);

                return clip;
            });
        }
    }
}