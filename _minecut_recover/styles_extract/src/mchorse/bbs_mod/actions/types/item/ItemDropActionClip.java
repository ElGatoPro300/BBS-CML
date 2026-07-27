package mchorse.bbs_mod.actions.types.item;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1309;
import net.minecraft.class_1542;
import net.minecraft.class_1937;

public class ItemDropActionClip extends ItemActionClip
{
    public final ValueDouble posX = new ValueDouble("x", 0D);
    public final ValueDouble posY = new ValueDouble("y", 0D);
    public final ValueDouble posZ = new ValueDouble("z", 0D);
    public final ValueFloat velocityX = new ValueFloat("vx", 0F);
    public final ValueFloat velocityY = new ValueFloat("vy", 0F);
    public final ValueFloat velocityZ = new ValueFloat("vz", 0F);
    public final ValueBoolean relative = new ValueBoolean("relative", false);
    public final ValueBoolean trajectoryPreview = new ValueBoolean("trajectory_preview", false);

    public ItemDropActionClip()
    {
        super();

        this.add(this.posX);
        this.add(this.posY);
        this.add(this.posZ);
        this.add(this.velocityX);
        this.add(this.velocityY);
        this.add(this.velocityZ);
        this.add(this.relative);
        this.add(this.trajectoryPreview);
    }

    public void shift(double dx, double dy, double dz)
    {
        this.posX.set(this.posX.get() + dx);
        this.posY.set(this.posY.get() + dy);
        this.posZ.set(this.posZ.get() + dz);
    }

    @Override
    public boolean isClient()
    {
        return true;
    }

    @Override
    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        this.applyPositionRotation(player, replay, tick);

        double x = this.relative.get() ? this.posX.get() + player.method_19538().field_1352 : this.posX.get();
        double y = this.relative.get() ? this.posY.get() + player.method_19538().field_1351 : this.posY.get();
        double z = this.relative.get() ? this.posZ.get() + player.method_19538().field_1350 : this.posZ.get();
        class_1542 entity = new class_1542(
            player.method_51469(),
            x, y, z, this.itemStack.get().method_7972(),
            this.velocityX.get(), this.velocityY.get(), this.velocityZ.get()
        );

        entity.method_6988();
        player.method_37908().method_8649(entity);
    }

    @Override
    protected void applyClientAction(IEntity entity, Film film, Replay replay, int tick)
    {
        class_1937 world = entity.getWorld();

        if (world == null || !world.method_8608() || this.itemStack.get().method_7960())
        {
            return;
        }

        ReplayKeyframes keyframes = replay.keyframes;
        double replayX = keyframes.x.interpolate(tick);
        double replayY = keyframes.y.interpolate(tick);
        double replayZ = keyframes.z.interpolate(tick);
        double x = this.relative.get() ? replayX + this.posX.get() : this.posX.get();
        double y = this.relative.get() ? replayY + this.posY.get() : this.posY.get();
        double z = this.relative.get() ? replayZ + this.posZ.get() : this.posZ.get();
        class_1542 itemEntity = new class_1542(
            world,
            x, y, z, this.itemStack.get().method_7972(),
            this.velocityX.get(), this.velocityY.get(), this.velocityZ.get()
        );

        itemEntity.method_6988();
        world.method_8649(itemEntity);
    }

    @Override
    protected Clip create()
    {
        return new ItemDropActionClip();
    }
}
