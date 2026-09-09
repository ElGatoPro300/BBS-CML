package mchorse.bbs_mod.actions.types.chat;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ChatActionClip extends ActionClip
{
    public final ValueString message = new ValueString("message", "");

    public ChatActionClip()
    {
        this.add(this.message);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        for (Player entity : player.level().players())
        {
            entity.sendSystemMessage(Component.literal(StringUtils.processColoredText(this.message.get())));
        }
    }

    @Override
    protected Clip create()
    {
        return new ChatActionClip();
    }
}