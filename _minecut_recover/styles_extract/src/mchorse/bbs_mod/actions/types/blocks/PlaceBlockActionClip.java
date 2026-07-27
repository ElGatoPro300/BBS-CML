package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.mc.ValueBlockState;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1309;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2522;
import net.minecraft.class_2586;

public class PlaceBlockActionClip extends BlockActionClip
{
    public final ValueBlockState state = new ValueBlockState("state");
    public final ValueBoolean drop = new ValueBoolean("drop", false);
    public final ValueString blockEntityNbt = new ValueString("block_entity_nbt", "");

    public PlaceBlockActionClip()
    {
        super();

        this.add(this.state);
        this.add(this.drop);
        this.add(this.blockEntityNbt);
    }

    @Override
    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        class_2338 pos = new class_2338(this.x.get(), this.y.get(), this.z.get());

        if (this.state.get().method_26204() == class_2246.field_10124)
        {
            player.method_37908().method_22352(pos, this.drop.get());
        }
        else
        {
            player.method_37908().method_8501(pos, this.state.get());

            String nbtString = this.blockEntityNbt.get();

            if (!nbtString.isEmpty())
            {
                try
                {
                    class_2487 nbt = class_2522.method_10718(nbtString);
                    nbt.method_10569("x", pos.method_10263());
                    nbt.method_10569("y", pos.method_10264());
                    nbt.method_10569("z", pos.method_10260());
                    class_2586 created = class_2586.method_11005(pos, this.state.get(), nbt, player.method_37908().method_30349());

                    if (created != null)
                    {
                        player.method_37908().method_8544(pos);
                        player.method_37908().method_8438(created);
                        created.method_5431();
                        player.method_37908().method_8413(pos, this.state.get(), this.state.get(), 3);
                    }
                }
                catch (Exception ignored)
                {}
            }
        }
    }

    @Override
    protected Clip create()
    {
        return new PlaceBlockActionClip();
    }
}
