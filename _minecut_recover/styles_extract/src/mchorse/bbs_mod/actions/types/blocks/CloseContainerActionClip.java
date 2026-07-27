package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.mc.ValueBlockState;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1309;
import net.minecraft.class_2338;

public class CloseContainerActionClip extends BlockActionClip
{
    public final ValueBoolean applyState = new ValueBoolean("apply_state", false);
    public final ValueBlockState state = new ValueBlockState("state");

    public CloseContainerActionClip()
    {
        super();

        this.add(this.applyState);
        this.add(this.state);
    }

    @Override
    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        if (this.applyState.get())
        {
            player.method_37908().method_8501(new class_2338(this.x.get(), this.y.get(), this.z.get()), this.state.get());
        }

        player.closeReplayChest(replay.getId());
        player.method_7346();
    }

    @Override
    protected Clip create()
    {
        return new CloseContainerActionClip();
    }
}
