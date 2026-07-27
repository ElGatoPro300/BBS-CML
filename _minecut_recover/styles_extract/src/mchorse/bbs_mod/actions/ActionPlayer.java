package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.MathUtils;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1313;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionPlayer
{
    public Film film;
    public int tick;
    public boolean playing = true;
    public int countdown;
    public int exception;
    public PlayerType type;

    public boolean syncing;
    public boolean stopDamage = true;

    private class_3222 serverPlayer;
    private class_3218 world;
    private int duration;

    private Map<String, class_1309> actors = new HashMap<>();

    private List<class_1799> cachedInventory = new ArrayList<>();
    private Form cachedForm;

    private float cacheHp;
    private float cacheHunger;
    private int cacheXpLevel;
    private float cacheXpProgress;

    public ActionPlayer(class_3222 serverPlayer, class_3218 world, Film film, int tick, int countdown, int exception, PlayerType type)
    {
        this.world = world;
        this.film = film;
        this.tick = tick;
        this.countdown = countdown;
        this.exception = exception;
        this.type = type;

        this.serverPlayer = serverPlayer;
        this.duration = film.camera.calculateDuration();

        this.updateReplayEntities();

        Replay fpReplay = film.getFirstPersonReplay();

        if (this.type == PlayerType.NORMAL && this.serverPlayer != null && fpReplay != null)
        {
            for (int i = 0; i < this.serverPlayer.method_31548().method_5439(); i++)
            {
                this.cachedInventory.add(serverPlayer.method_31548().method_5438(i).method_7972());
                this.serverPlayer.method_31548().method_5447(i, CollectionUtils.getSafe(fpReplay.inventory.getStacks(), i, class_1799.field_8037));
            }

            Morph morph = Morph.getMorph(this.serverPlayer);

            if (morph != null)
            {
                this.cachedForm = FormUtils.copy(morph.getForm());
            }

            ServerNetwork.sendMorphToTracked(this.serverPlayer, fpReplay.form.get());

            this.cacheHp = this.serverPlayer.method_6032();
            this.cacheHunger = this.serverPlayer.method_7344().method_7589();
            this.cacheXpLevel = this.serverPlayer.field_7520;
            this.cacheXpProgress = this.serverPlayer.field_7510;

            this.serverPlayer.method_6033(this.film.hp.get());
            this.serverPlayer.method_7344().method_7581(this.film.hunger.get());
            this.serverPlayer.field_7510 = this.film.xpProgress.get();
            this.serverPlayer.method_14252(this.film.xpLevel.get());
        }
    }

    public void updateReplayEntities()
    {
        for (class_1309 entity : this.actors.values())
        {
            if (!entity.method_31747())
            {
                entity.method_31472();
            }
        }

        this.actors.clear();

        List<Replay> list = this.film.replays.getList();

        for (int i = 0; i < list.size(); i++)
        {
            Replay replay = list.get(i);
            boolean isActor = replay.actor.get() || replay.fp.get();

            if (i == this.exception || !isActor || !replay.enabled.get())
            {
                continue;
            }

            if (replay.fp.get() && this.serverPlayer != null)
            {
                if (this.type == PlayerType.NORMAL)
                {
                    this.actors.put(replay.getId(), this.serverPlayer);
                }
            }
            else
            {
                ActorEntity actor = new ActorEntity(BBSMod.ACTOR_ENTITY, this.world);

                actor.setForm(FormUtils.copy(replay.form.get()));
                actor.setReplayData(this.film, replay, this.tick);

                this.apply(actor, replay, this.tick, false);
                this.actors.put(replay.getId(), actor);
                this.world.method_8649(actor);
            }
        }

        for (class_3222 player : this.world.method_18456())
        {
            ServerNetwork.sendActors(player, this.film.getId(), this.actors);
        }
    }

    public class_3218 getWorld()
    {
        return this.world;
    }

    public void apply(class_1309 actor, Replay replay, float tick, boolean ticking)
    {
        double x = replay.keyframes.x.interpolate(tick);
        double y = replay.keyframes.y.interpolate(tick);
        double z = replay.keyframes.z.interpolate(tick);
        float yawHead = replay.keyframes.headYaw.interpolate(tick).floatValue();
        float yawBody = replay.keyframes.bodyYaw.interpolate(tick).floatValue();
        float pitch = replay.keyframes.pitch.interpolate(tick).floatValue();
        boolean grounded = replay.keyframes.grounded.interpolate(tick) > 0;

        class_243 pos = actor.method_19538();

        if (ticking)
        {
            actor.method_24830(grounded);
            actor.method_5784(class_1313.field_6308, new class_243(x - pos.field_1352, y - pos.field_1351, z - pos.field_1350));
        }

        actor.method_5814(x, y, z);
        actor.method_36456(yawHead);
        actor.method_5847(yawHead);
        actor.method_36457(pitch);
        actor.method_5636(yawBody);
        actor.method_5660(replay.keyframes.sneaking.interpolate(tick) > 0);
        actor.method_24830(grounded);
        actor.method_5673(class_1304.field_6171, replay.keyframes.offHand.interpolate(tick, class_1799.field_8037));
        actor.method_5673(class_1304.field_6169, replay.keyframes.armorHead.interpolate(tick, class_1799.field_8037));
        actor.method_5673(class_1304.field_6174, replay.keyframes.armorChest.interpolate(tick, class_1799.field_8037));
        actor.method_5673(class_1304.field_6172, replay.keyframes.armorLegs.interpolate(tick, class_1799.field_8037));
        actor.method_5673(class_1304.field_6166, replay.keyframes.armorFeet.interpolate(tick, class_1799.field_8037));

        if (actor instanceof class_3222 player)
        {
            int selectedSlot = player.method_31548().field_7545;
            int slot = MathUtils.clamp(replay.keyframes.selectedSlot.interpolate(this.tick), 0, 8);

            if (selectedSlot != slot)
            {
                ServerNetwork.sendSelectedSlot(player, slot);
            }

            actor.method_5673(class_1304.field_6173, replay.keyframes.mainHand.interpolate(tick, class_1799.field_8037));
        }
        else
        {
            actor.method_5673(class_1304.field_6173, replay.keyframes.mainHand.interpolate(tick, class_1799.field_8037));
        }

        double vx = x - replay.keyframes.x.interpolate(tick - 1);
        double vy = y - replay.keyframes.y.interpolate(tick - 1);
        double vz = z - replay.keyframes.z.interpolate(tick - 1);

        if (vy == 0D)
        {
            vy = -0.0784;
        }

        actor.method_18800(vx, vy, vz);

        actor.field_6017 = replay.keyframes.fall.interpolate(tick).floatValue();
    }

    public boolean tick()
    {
        if (this.countdown > 0)
        {
            this.countdown -= 1;

            return false;
        }

        for (Map.Entry<String, class_1309> entry : this.actors.entrySet())
        {
            Replay replay = (Replay) this.film.replays.get(entry.getKey());

            if (replay != null)
            {
                class_1309 actor = entry.getValue();
                
                // Update tick in ActorEntity for accurate item drops on death
                if (actor instanceof ActorEntity actorEntity)
                {
                    actorEntity.updateTick(this.tick);
                }
                
                this.apply(actor, replay, this.tick, true);

            }
        }

        if (!this.playing)
        {
            return false;
        }

        if (this.tick >= 0)
        {
            this.applyAction();
        }

        this.tick += 1;

        return !this.syncing && this.tick >= this.duration;
    }

    private void applyAction()
    {
        SuperFakePlayer fakePlayer = SuperFakePlayer.get(this.world);
        List<Replay> list = this.film.replays.getList();

        for (int i = 0; i < list.size(); i++)
        {
            if (i == this.exception)
            {
                continue;
            }

            Replay replay = list.get(i);

            if (!replay.enabled.get())
            {
                continue;
            }

            class_1309 actor = this.actors.get(replay.getId());

            replay.applyActions(actor, fakePlayer, this.film, this.tick);
        }
    }

    public void syncData(DataPath key, BaseType data)
    {
        BaseValue baseValue = this.resolveValue(key);

        if (baseValue != null)
        {
            baseValue.fromData(data);

            if (baseValue.getId().equals("actor") || baseValue.getId().equals("enabled") || baseValue.getId().equals("replays"))
            {
                this.updateReplayEntities();
            }
        }
    }

    private BaseValue resolveValue(DataPath path)
    {
        BaseValue current = this.film;

        int start = 0;

        if (this.film != null && path.size() > 0)
        {
            String filmId = this.film.getId();

            if (filmId != null && !filmId.isEmpty() && filmId.equals(path.strings.get(0)))
            {
                start = 1;
            }
        }

        for (int i = start; i < path.size(); i++)
        {
            String part = path.strings.get(i);

            if (current instanceof BaseValueGroup group)
            {
                BaseValue next = group.get(part);

                if (next == null)
                {
                    if (group instanceof FormProperties formProps)
                    {
                        if (formProps.getParent() instanceof Replay replay)
                        {
                            next = formProps.getOrCreate(replay.form.get(), part);
                        }
                    }
                }

                if (next == null)
                {
                    return null;
                }

                current = next;
            }
            else
            {
                return null;
            }
        }

        return current;
    }

    public void goTo(int tick)
    {
        this.goTo(this.tick, tick);
    }

    public void goTo(int from, int tick)
    {
        for (Map.Entry<String, class_1309> entry : this.actors.entrySet())
        {
            Replay replay = (Replay) this.film.replays.get(entry.getKey());

            if (replay != null)
            {
                this.apply(entry.getValue(), replay, this.tick, false);
            }
        }

        if (from != tick)
        {
            this.tick = from;

            while (this.tick != tick)
            {
                this.tick += this.tick > tick ? -1 : 1;

                this.applyAction();
            }
        }
    }

    public void stop()
    {
        SuperFakePlayer fakePlayer = SuperFakePlayer.get(this.world);

        for (Replay replay : this.film.replays.getList())
        {
            fakePlayer.closeReplayChest(replay.getId());
        }

        fakePlayer.method_7346();

        for (class_1309 value : this.actors.values())
        {
            if (!value.method_31747())
            {
                value.method_31472();
            }
        }

        if (this.type == PlayerType.NORMAL && this.serverPlayer != null && this.film.getFirstPersonReplay() != null)
        {
            for (int i = 0; i < this.serverPlayer.method_31548().method_5439(); i++)
            {
                this.serverPlayer.method_31548().method_5447(i, this.cachedInventory.get(i));
            }

            ServerNetwork.sendMorphToTracked(this.serverPlayer, this.cachedForm);

            this.serverPlayer.method_6033(this.cacheHp);
            this.serverPlayer.method_7344().method_7581(this.cacheHunger);
            this.serverPlayer.field_7510 = this.cacheXpProgress;
            this.serverPlayer.method_14252(this.cacheXpLevel);
        }
    }

    public void toggle()
    {
        this.playing = !this.playing;
    }
}
