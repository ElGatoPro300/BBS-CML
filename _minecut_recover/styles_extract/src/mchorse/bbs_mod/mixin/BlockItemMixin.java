package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.blocks.PlaceBlockActionClip;
import net.minecraft.class_1747;
import net.minecraft.class_1750;
import net.minecraft.class_2338;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_3222;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1747.class)
public class BlockItemMixin
{
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("RETURN"))
    public void onPlace(class_1750 context, class_2680 state, CallbackInfoReturnable<Boolean> info)
    {
        if (info.getReturnValue() && context.method_8036() instanceof class_3222 player)
        {
            BBSMod.getActions().addAction(player, () ->
            {
                PlaceBlockActionClip clip = new PlaceBlockActionClip();
                class_2338 pos = context.method_8037();
                class_2680 placedState = context.method_8045().method_8320(pos);
                class_2586 blockEntity = context.method_8045().method_8321(pos);

                clip.x.set(pos.method_10263());
                clip.y.set(pos.method_10264());
                clip.z.set(pos.method_10260());
                clip.state.set(placedState);

                class_9279 stackBlockEntityData = context.method_8041().method_57824(class_9334.field_49611);

                if (stackBlockEntityData != null)
                {
                    clip.blockEntityNbt.set(stackBlockEntityData.method_57463().method_10553().toString());
                }
                else if (blockEntity != null)
                {
                    clip.blockEntityNbt.set(blockEntity.method_38243(context.method_8045().method_30349()).toString());
                }

                return clip;
            });
        }
    }
}
