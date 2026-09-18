package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.blocks.CloseContainerActionClip;
import mchorse.bbs_mod.actions.types.blocks.InteractBlockActionClip;
import mchorse.bbs_mod.actions.types.chat.CommandActionClip;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.brigadier.ParseResults;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin
{
    private static final Map<UUID, BlockPos> OPEN_CONTAINERS = new HashMap<>();

    @Shadow
    public ServerPlayer player;

    @Inject(method = "parseCommand", at = @At("HEAD"))
    public void onParse(String command, CallbackInfoReturnable<ParseResults<CommandSourceStack>> info)
    {
        BBSMod.getActions().addAction(this.player, () ->
        {
            CommandActionClip clip = new CommandActionClip();

            clip.command.set(command);

            return clip;
        });
    }

    @Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult redirectOnBlockInteract(ServerPlayerGameMode manager, ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult)
    {
        BlockPos interactedPos = hitResult.getBlockPos();

        BBSMod.getActions().addAction(this.player, () ->
        {
            InteractBlockActionClip clip = new InteractBlockActionClip();

            clip.hit.setHitResult(hitResult);
            clip.hand.set(hand == InteractionHand.MAIN_HAND);

            return clip;
        });

        if (world.getBlockState(interactedPos).getBlock() instanceof ChestBlock)
        {
            OPEN_CONTAINERS.put(this.player.getUUID(), interactedPos.immutable());
        }
        else if (world.getBlockState(interactedPos).getBlock() instanceof AbstractFurnaceBlock)
        {
            OPEN_CONTAINERS.put(this.player.getUUID(), interactedPos.immutable());
        }

        return manager.useItemOn(player, world, stack, hand, hitResult);
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"))
    private void onCloseHandledScreen(ServerboundContainerClosePacket packet, CallbackInfo ci)
    {
        BlockPos containerPos = OPEN_CONTAINERS.remove(this.player.getUUID());

        if (containerPos == null)
        {
            return;
        }

        BBSMod.getActions().addAction(this.player, () ->
        {
            CloseContainerActionClip clip = new CloseContainerActionClip();
            BlockState state = this.player.level().getBlockState(containerPos);

            clip.x.set(containerPos.getX());
            clip.y.set(containerPos.getY());
            clip.z.set(containerPos.getZ());

            if (state.getBlock() instanceof AbstractFurnaceBlock)
            {
                clip.applyState.set(true);
                clip.state.set(state);
            }

            return clip;
        });
    }
}
