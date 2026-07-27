package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1675;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_3966;

public class AttackActionClip extends ActionClip
{
    public final ValueFloat damage = new ValueFloat("damage", 0F);

    public AttackActionClip()
    {
        super();

        this.add(this.damage);
    }

    @Override
    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        float damage = this.damage.get();

        if (damage <= 0F)
        {
            return;
        }

        this.applyPositionRotation(player, replay, tick);

        double distance = 6D;
        class_239 blockHit = player.method_5745(distance, 1F, false);
        class_243 origin = player.method_5836(1F);
        class_243 rotation = player.method_5828(1F);
        class_243 direction = origin.method_1031(rotation.field_1352 * distance, rotation.field_1351 * distance, rotation.field_1350 * distance);

        double newDistance = blockHit != null ? blockHit.method_17784().method_1025(origin) : distance * distance;
        class_238 box = player.method_5829().method_18804(rotation.method_1021(distance)).method_1009(1, 1, 1);
        class_3966 enittyHit = class_1675.method_18075(actor == null ? player : actor, origin, direction, box, entity -> !entity.method_7325() && entity.method_5863(), newDistance);

        if (enittyHit != null)
        {
            class_1297 entity = enittyHit.method_17782();

            if (entity != null)
            {
                entity.method_5643(player.method_37908().method_48963().method_48812(player), damage);
            }
        }
    }

    @Override
    protected Clip create()
    {
        return new AttackActionClip();
    }
}