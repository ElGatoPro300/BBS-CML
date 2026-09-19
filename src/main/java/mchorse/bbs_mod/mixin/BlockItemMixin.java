package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.blocks.PlaceBlockActionClip;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin
{
    @Inject(method = "placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("RETURN"))
    public void onPlace(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> info)
    {
        if (info.getReturnValue() && context.getPlayer() instanceof ServerPlayer player)
        {
            BBSMod.getActions().addAction(player, () ->
            {
                PlaceBlockActionClip clip = new PlaceBlockActionClip();
                BlockPos pos = context.getClickedPos();
                BlockState placedState = context.getLevel().getBlockState(pos);
                BlockEntity blockEntity = context.getLevel().getBlockEntity(pos);

                clip.x.set(pos.getX());
                clip.y.set(pos.getY());
                clip.z.set(pos.getZ());
                clip.state.set(placedState);

                TypedEntityData<BlockEntityType<?>> stackBlockEntityData = context.getItemInHand().get(DataComponents.BLOCK_ENTITY_DATA);

                if (stackBlockEntityData != null)
                {
                    clip.blockEntityNbt.set(stackBlockEntityData.copyTagWithoutId().toString());
                }
                else if (blockEntity != null)
                {
                    clip.blockEntityNbt.set(blockEntity.saveWithFullMetadata(context.getLevel().registryAccess()).toString());
                }

                return clip;
            });
        }
    }
}
