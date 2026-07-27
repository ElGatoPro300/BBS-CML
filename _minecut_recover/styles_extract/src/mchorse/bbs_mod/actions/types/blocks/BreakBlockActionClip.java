package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1309;
import net.minecraft.class_2338;

public class BreakBlockActionClip extends BlockActionClip
{
    public final ValueInt progress = new ValueInt("progress", 0);

    public BreakBlockActionClip()
    {
        super();

        this.add(this.progress);
    }

    @Override
    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        player.method_37908().method_8517(player.method_5628(), new class_2338(this.x.get(), this.y.get(), this.z.get()), this.progress.get());
    }

    @Override
    protected Clip create()
    {
        return new BreakBlockActionClip();
    }
}