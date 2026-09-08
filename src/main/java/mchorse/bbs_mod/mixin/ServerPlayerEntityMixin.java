package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.item.ItemDropActionClip;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin
{
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("RETURN"))
    public void onDropItem(CallbackInfoReturnable<ItemEntity> info)
    {
        ItemEntity entity = info.getReturnValue();

        if (entity != null)
        {
            ServerPlayer player = (ServerPlayer) (Object) this;
            BBSMod.getActions().addAction(player, () ->
            {
                ItemDropActionClip actionClip = new ItemDropActionClip();
                Vec3 velocity = entity.getDeltaMovement();
                Vec3 pos = entity.position();

                actionClip.velocityX.set((float) velocity.x);
                actionClip.velocityY.set((float) velocity.y);
                actionClip.velocityZ.set((float) velocity.z);
                actionClip.posX.set(pos.x);
                actionClip.posY.set(pos.y);
                actionClip.posZ.set(pos.z);
                actionClip.itemStack.set(entity.getItem().copy());

                return actionClip;
            });
        }
    }
}