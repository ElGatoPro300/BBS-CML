package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.item.UseBlockItemActionClip;
import mchorse.bbs_mod.actions.types.item.UseItemActionClip;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1271;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1838;
import net.minecraft.class_1937;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1799.class)
public class ItemStackMixin
{
    @Inject(method = "use", at = @At("HEAD"))
    public void onUse(class_1937 world, class_1657 user, class_1268 hand, CallbackInfoReturnable<class_1271<class_1799>> info)
    {
        if (user instanceof class_3222 player)
        {
            class_1799 stack = user.method_5998(hand);

            if (stack.method_7935(user) > 0)
            {
                return;
            }
            BBSMod.getActions().addAction(player, () ->
            {
                UseItemActionClip clip = new UseItemActionClip();

                clip.itemStack.set(stack.method_7972());
                clip.hand.set(hand == class_1268.field_5808);

                return clip;
            });
        }
    }

    @Inject(method = "useOnBlock", at = @At("HEAD"))
    public void onUseOnBlock(class_1838 context, CallbackInfoReturnable<class_1269> info)
    {
        if (context.method_8036() instanceof class_3222 player)
        {
            BBSMod.getActions().addAction(player, () ->
            {
                UseBlockItemActionClip clip = new UseBlockItemActionClip();

                clip.hit.setHitResult(context);
                clip.itemStack.set(context.method_8041().method_7972());
                clip.hand.set(context.method_20287() == class_1268.field_5808);

                return clip;
            });
        }
    }
}