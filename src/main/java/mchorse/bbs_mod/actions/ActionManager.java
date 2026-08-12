package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.utils.DataPath;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ActionManager
{
    private List<ActionPlayer> players = new ArrayList<>();
    private Map<ServerPlayer, ActionRecorder> recorders = new HashMap<>();
    private Map<ServerLevel, DamageControl> dc = new HashMap<>();

    public void reset()
    {
        this.players.clear();
        this.recorders.clear();
        this.dc.clear();
    }

    public void tick()
    {
        this.players.removeIf((player) ->
        {
            boolean tick = player.tick();

            if (tick)
            {
                if (player.stopDamage)
                {
                    this.stopDamage(player.getWorld());
                }

                player.stop();
            }

            return tick;
        });

        for (Map.Entry<ServerPlayer, ActionRecorder> entry : this.recorders.entrySet())
        {
            entry.getValue().tick(entry.getKey());
        }
    }

    /* Actions playback */

    public void syncData(String filmId, DataPath key, BaseType data)
    {
        for (ActionPlayer player : this.players)
        {
            if (player.film.getId().equals(filmId))
            {
                player.syncData(key, data);
            }
        }
    }

    public ActionPlayer getPlayer(String filmId)
    {
        for (ActionPlayer player : this.players)
        {
            if (player.film.getId().equals(filmId))
            {
                return player;
            }
        }

        return null;
    }

    public ActionPlayer play(ServerPlayer serverPlayer, ServerLevel world, Film film, int tick)
    {
        return this.play(serverPlayer, world, film, tick, 0, -1, PlayerType.NORMAL);
    }

    public ActionPlayer play(ServerPlayer serverPlayer, ServerLevel world, Film film, int tick, PlayerType type)
    {
        return this.play(serverPlayer, world, film, tick, 0, -1, type);
    }

    public ActionPlayer play(ServerPlayer serverPlayer, ServerLevel world, Film film, int tick, int countdown, int exception, PlayerType type)
    {
        if (film != null)
        {
            ActionPlayer player = new ActionPlayer(serverPlayer, world, film, tick, countdown, exception, type);

            this.players.add(player);
            this.trackDamage(world);

            return player;
        }

        return null;
    }

    public void stop(String filmId)
    {
        Iterator<ActionPlayer> it = this.players.iterator();

        while (it.hasNext())
        {
            ActionPlayer next = it.next();

            if (next.film.getId().equals(filmId))
            {
                this.stopDamage(next.getWorld());
                next.stop();
                it.remove();
            }
        }
    }

    /* Actions recording */

    public void startRecording(Film film, ServerPlayer entity, int tick, int countdown, int replayId)
    {
        ActionPlayer play = this.play(entity, entity.level(), film, tick, countdown, replayId, PlayerType.RECORDING);

        play.stopDamage = false;

        this.recorders.put(entity, new ActionRecorder(film, entity, tick, countdown));
    }

    public void addAction(ServerPlayer entity, Supplier<ActionClip> supplier)
    {
        ActionRecorder recorder = this.recorders.get(entity);

        if (recorder != null && supplier != null)
        {
            ActionClip actionClip = supplier.get();

            if (actionClip != null)
            {
                recorder.add(actionClip);
            }
        }
    }

    public boolean hasActiveRecorders(ServerLevel world)
    {
        if (this.recorders.isEmpty())
        {
            return false;
        }

        for (ServerPlayer player : this.recorders.keySet())
        {
            if (player != null && player.level() == world)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Notify recording clients so autocapture can place combat clips on mob replays.
     */
    public void broadcastMobCombatHit(ServerLevel world, int victimEntityId, int sourceEntityId, float amount, byte kind)
    {
        for (ServerPlayer player : this.recorders.keySet())
        {
            if (player != null && player.level() == world)
            {
                ServerNetwork.sendMobCombatAction(player, victimEntityId, sourceEntityId, amount, kind);
            }
        }
    }

    /**
     * Notify recording clients so autocapture can follow vanilla mob conversions.
     */
    public void broadcastMobConversion(ServerLevel world, int oldEntityId, int newEntityId)
    {
        for (ServerPlayer player : this.recorders.keySet())
        {
            if (player != null && player.level() == world)
            {
                ServerNetwork.sendMobConversion(player, oldEntityId, newEntityId);
            }
        }
    }

    public ActionRecorder stopRecording(ServerPlayer entity)
    {
        ActionRecorder remove = this.recorders.remove(entity);

        this.stop(remove.getFilm().getId());
        this.stopDamage(entity.level());

        return remove;
    }

    /* Damage control */

    public void trackDamage(ServerLevel world)
    {
        DamageControl damageControl = this.dc.get(world);

        if (damageControl == null)
        {
            this.dc.put(world, new DamageControl(world));
        }
        else
        {
            damageControl.nested += 1;
        }
    }

    public void stopDamage(ServerLevel world)
    {
        DamageControl damageControl = this.dc.get(world);

        if (damageControl != null)
        {
            if (damageControl.nested > 0)
            {
                damageControl.nested -= 1;
            }
            else
            {
                damageControl.restore();
                this.dc.remove(world);
            }
        }
    }

    public void resetDamage(ServerLevel world)
    {
        DamageControl dc = this.dc.remove(world);

        if (dc != null)
        {
            dc.restore();
        }
    }

    /**
     * Puts captured blocks/entities back while keeping damage control armed
     * for further film playback.
     */
    public void restoreDamage(ServerLevel world)
    {
        DamageControl damageControl = this.dc.get(world);

        if (damageControl != null)
        {
            damageControl.restore();
        }
    }

    public void changedBlock(BlockPos pos, BlockState state, BlockEntity blockEntity)
    {
        for (DamageControl control : this.dc.values())
        {
            control.addBlock(pos, state, blockEntity);
        }
    }

    public void spawnedEntity(Entity entity)
    {
        for (DamageControl control : this.dc.values())
        {
            control.addEntity(entity);
        }
    }
}