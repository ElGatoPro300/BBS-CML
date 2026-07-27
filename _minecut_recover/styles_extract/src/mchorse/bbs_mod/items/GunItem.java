package mchorse.bbs_mod.items;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.forms.FormUtils;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1271;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_243;

public class GunItem extends class_1792
{
    public static class_1297 actor;

    public GunItem(class_1793 settings)
    {
        super(settings);
    }

    @Override
    public class_1271<class_1799> method_7836(class_1937 world, class_1657 user, class_1268 hand)
    {
        class_1297 owner = actor == null ? user : actor;
        class_1799 stack = user.method_5998(hand);
        GunProperties properties = this.getProperties(stack);

        /* Launch the player */
        if (properties.launch)
        {
            class_243 rotationVector = owner.method_5720().method_1021(properties.launchPower);

            if (properties.launchAdditive)
            {
                owner.method_60491(rotationVector);
            }
            else
            {
                owner.method_18799(rotationVector);
            }

            return new class_1271<>(class_1269.field_5812, stack);
        }

        if (!world.field_9236)
        {
            /* Shoot projectiles */
            int projectiles = Math.max(properties.projectiles, 1);

            for (int i = 0; i < projectiles; i++)
            {
                GunProjectileEntity projectile = new GunProjectileEntity(BBSMod.GUN_PROJECTILE_ENTITY, world);
                float yaw = owner.method_5791() + (float) (properties.scatterY * (Math.random() - 0.5D));
                float pitch = owner.method_36455() + (float) (properties.scatterX * (Math.random() - 0.5D));

                projectile.setProperties(properties);
                projectile.setForm(FormUtils.copy(properties.projectileForm));
                projectile.method_23327(owner.method_23317(), owner.method_23318() + owner.method_18381(owner.method_18376()), owner.method_23321());
                projectile.method_24919(owner, pitch, yaw, 0F, properties.speed, 0F);
                projectile.method_18382();

                world.method_8649(projectile);
            }

            if (!properties.cmdFiring.isEmpty())
            {
                owner.method_5682().method_3734().method_44252(owner.method_5671(), properties.cmdFiring);
            }
        }

        return new class_1271<>(class_1269.field_5811, stack);
    }

    private GunProperties getProperties(class_1799 stack)
    {
        return GunProperties.get(stack);
    }
}