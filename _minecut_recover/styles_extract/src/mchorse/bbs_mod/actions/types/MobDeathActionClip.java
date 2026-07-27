package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1937;
import net.minecraft.class_2398;
import net.minecraft.class_5819;

public class MobDeathActionClip extends ActionClip
{
    @Override
    public boolean isClient()
    {
        return true;
    }

    @Override
    protected void applyClientAction(IEntity entity, Film film, Replay replay, int tick)
    {
        class_1937 world = entity.getWorld();

        if (world == null || !world.method_8608())
        {
            return;
        }

        class_5819 random = world.method_8409();
        double x = entity.getX();
        double y = entity.getY() + entity.getEyeHeight() * 0.5D;
        double z = entity.getZ();
        float width = 0.6F;

        for (int i = 0; i < 20; i++)
        {
            double offsetX = (random.method_43058() - 0.5D) * width;
            double offsetY = random.method_43058() * 0.5D;
            double offsetZ = (random.method_43058() - 0.5D) * width;
            double velocityX = random.method_43059() * 0.02D;
            double velocityY = random.method_43059() * 0.02D;
            double velocityZ = random.method_43059() * 0.02D;

            world.method_8406(class_2398.field_11203, x + offsetX, y + offsetY, z + offsetZ, velocityX, velocityY, velocityZ);
        }
    }

    @Override
    protected Clip create()
    {
        return new MobDeathActionClip();
    }
}
