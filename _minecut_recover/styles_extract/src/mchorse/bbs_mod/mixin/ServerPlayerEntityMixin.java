package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.item.ItemDropActionClip;
import net.minecraft.class_1542;
import net.minecraft.class_243;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_3222.class)
public class ServerPlayerEntityMixin
{
    @Inject(method = "dropItem", at = @At("RETURN"))
    public void onDropItem(CallbackInfoReturnable<class_1542> info)
    {
        class_1542 entity = info.getReturnValue();

        if (entity != null)
        {
            class_3222 player = (class_3222) (Object) this;
            BBSMod.getActions().addAction(player, () ->
            {
                ItemDropActionClip actionClip = new ItemDropActionClip();
                class_243 velocity = entity.method_18798();
                class_243 pos = entity.method_19538();

                actionClip.velocityX.set((float) velocity.field_1352);
                actionClip.velocityY.set((float) velocity.field_1351);
                actionClip.velocityZ.set((float) velocity.field_1350);
                actionClip.posX.set(pos.field_1352);
                actionClip.posY.set(pos.field_1351);
                actionClip.posZ.set(pos.field_1350);
                actionClip.itemStack.set(entity.method_6983().method_7972());

                return actionClip;
            });
        }
    }
}