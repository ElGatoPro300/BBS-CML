package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import net.minecraft.class_1799;

public abstract class ActionClip extends Clip
{
    public final ValueInt frequency = new ValueInt("frequency", 0, 0, 1000);

    public ActionClip()
    {
        this.add(this.frequency);
    }

    public boolean isClient()
    {
        return false;
    }

    public final void applyClient(IEntity entity, Film film, Replay replay, int tick)
    {
        if (!this.enabled.get())
        {
            return;
        }

        int relaive = tick - this.tick.get();
        int frequency = this.frequency.get();

        if (frequency == 0)
        {
            if (relaive == 0)
            {
                this.applyClientAction(entity, film, replay, tick);
            }
        }
        else if (relaive % frequency == 0)
        {
            this.applyClientAction(entity, film, replay, tick);
        }
    }

    protected void applyClientAction(IEntity entity, Film film, Replay replay, int tick)
    {}

    public final void apply(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        if (!this.enabled.get())
        {
            return;
        }

        int relaive = tick - this.tick.get();
        int frequency = this.frequency.get();

        if (frequency == 0)
        {
            if (relaive == 0)
            {
                this.applyAction(actor, player, film, replay, tick);
            }
        }
        else if (relaive % frequency == 0)
        {
            this.applyAction(actor, player, film, replay, tick);
        }
    }

    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {}

    protected void applyPositionRotation(SuperFakePlayer player, Replay replay, int tick)
    {
        ReplayKeyframes keyframes = replay.keyframes;

        player.method_5814(keyframes.x.interpolate(tick), keyframes.y.interpolate(tick), keyframes.z.interpolate(tick));
        player.method_36456(keyframes.yaw.interpolate(tick).floatValue());
        player.method_5847(keyframes.headYaw.interpolate(tick).floatValue());
        player.method_5636(keyframes.bodyYaw.interpolate(tick).floatValue());
        player.method_36457(keyframes.pitch.interpolate(tick).floatValue());
        player.method_6122(class_1268.field_5808, keyframes.mainHand.interpolate(tick, class_1799.field_8037).method_7972());
        player.method_6122(class_1268.field_5810, keyframes.offHand.interpolate(tick, class_1799.field_8037).method_7972());
    }
}