package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.actions.types.blocks.PlaceBlockActionClip;
import mchorse.bbs_mod.actions.types.chat.ChatActionClip;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.class_1657;
import net.minecraft.class_1934;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2556;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_3222;
import net.minecraft.class_7471;
import org.jetbrains.annotations.Nullable;

public class ActionHandler
{
    public static void registerHandlers(ActionManager actions)
    {
        ServerMessageEvents.CHAT_MESSAGE.register((class_7471 message, class_3222 sender, class_2556.class_7602 params) ->
        {
            String literalString = message.method_46291().method_54160();

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

        PlayerBlockBreakEvents.AFTER.register((class_1937 world, class_1657 player, class_2338 pos, class_2680 state, @Nullable class_2586 blockEntity) ->
        {
            if (player instanceof class_3222 serverPlayer)
            {
                actions.addAction(serverPlayer, () ->
                {
                    PlaceBlockActionClip clip = new PlaceBlockActionClip();

                    clip.state.set(world.method_8320(pos));
                    clip.x.set(pos.method_10263());
                    clip.y.set(pos.method_10264());
                    clip.z.set(pos.method_10260());
                    clip.drop.set(serverPlayer.field_13974.method_14257() == class_1934.field_9215);

                    return clip;
                });
            }
        });
    }
}