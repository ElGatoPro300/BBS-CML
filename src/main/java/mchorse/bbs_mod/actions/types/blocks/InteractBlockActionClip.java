package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.values.ValueBlockHitResult;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;

public class InteractBlockActionClip extends ActionClip
{
    public final ValueBlockHitResult hit = new ValueBlockHitResult("hit");
    public final ValueBoolean hand = new ValueBoolean("hand", true);

    public InteractBlockActionClip()
    {
        super();

        this.add(this.hit);
        this.add(this.hand);
    }

    @Override
    public void shift(double dx, double dy, double dz)
    {
        super.shift(dx, dy, dz);

        this.hit.shift(dx, dy, dz);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        this.applyPositionRotation(player, replay, tick);

        BlockHitResult result = this.hit.getHitResult();
        
        if (player.level().getBlockState(result.getBlockPos()).getBlock() instanceof ChestBlock)
        {
            player.openReplayChest(replay.getId(), result.getBlockPos());
            return;
        }
        
        InteractionHand hand = this.hand.get() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);

        player.gameMode.useItemOn(player, player.level(), stack, hand, result);
    }

    @Override
    protected Clip create()
    {
        return new InteractBlockActionClip();
    }
}
