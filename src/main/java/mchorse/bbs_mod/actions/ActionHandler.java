package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.actions.types.blocks.PlaceBlockActionClip;
import mchorse.bbs_mod.actions.types.chat.ChatActionClip;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ActionHandler
{
    public static void registerHandlers(ActionManager actions)
    {
        ServerMessageEvents.CHAT_MESSAGE.register((SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) ->
        {
            String literalString = message.getContent().getLiteralString();

            if (literalString != null)
            {
                actions.addAction(sender, () ->
                {
                    ChatActionClip clip = new ChatActionClip();

                    clip.message.set(literalString);

                    return clip;
                });
            }
        });

        PlayerBlockBreakEvents.AFTER.register((World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) ->
        {
            if (player instanceof ServerPlayerEntity serverPlayer)
            {
                actions.addAction(serverPlayer, () ->
                {
                    PlaceBlockActionClip clip = new PlaceBlockActionClip();

                    clip.state.set(world.getBlockState(pos));
                    clip.x.set(pos.getX());
                    clip.y.set(pos.getY());
                    clip.z.set(pos.getZ());
                    clip.drop.set(serverPlayer.interactionManager.getGameMode() == GameMode.SURVIVAL);

                    return clip;
                });
            }
        });
    }
}