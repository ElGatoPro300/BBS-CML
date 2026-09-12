package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.mc.ValueBlockState;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

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
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        BlockPos pos = new BlockPos(this.x.get(), this.y.get(), this.z.get());

        if (this.state.get().getBlock() == Blocks.AIR)
        {
            player.level().destroyBlock(pos, this.drop.get());
        }
        else
        {
            player.level().setBlockAndUpdate(pos, this.state.get());

            String nbtString = this.blockEntityNbt.get();

            if (!nbtString.isEmpty())
            {
                try
                {
                    CompoundTag nbt = TagParser.parseCompoundFully(nbtString);
                    nbt.putInt("x", pos.getX());
                    nbt.putInt("y", pos.getY());
                    nbt.putInt("z", pos.getZ());
                    BlockEntity created = BlockEntity.loadStatic(pos, this.state.get(), nbt, player.level().registryAccess());

                    if (created != null)
                    {
                        player.level().removeBlockEntity(pos);
                        player.level().setBlockEntity(created);
                        created.setChanged();
                        player.level().sendBlockUpdated(pos, this.state.get(), this.state.get(), 3);
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
