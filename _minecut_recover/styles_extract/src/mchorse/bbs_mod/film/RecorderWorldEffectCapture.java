package mchorse.bbs_mod.film;

import mchorse.bbs_mod.actions.types.chat.CommandActionClip;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2487;
import net.minecraft.class_2488;
import net.minecraft.class_2489;
import net.minecraft.class_2499;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_638;
import net.minecraft.class_7923;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared helpers for recording world block changes into replay action clips.
 */
public final class RecorderWorldEffectCapture
{
    private static final int SNOW_TRAIL_RADIUS = 1;
    private static final List<String> SUMMON_NBT_STRIP_KEYS = Arrays.asList("Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "Invulnerable", "PortalCooldown", "UUID", "Dimension", "World");

    private RecorderWorldEffectCapture()
    {}

    public static void captureSnowTrail(Replay replay, Map<Long, class_2680> snapshots, class_1309 entity, int tick, class_638 world)
    {
        class_2338 footing = entity.method_24515();

        for (int dx = -SNOW_TRAIL_RADIUS; dx <= SNOW_TRAIL_RADIUS; dx++)
        {
            for (int dz = -SNOW_TRAIL_RADIUS; dz <= SNOW_TRAIL_RADIUS; dz++)
            {
                class_2338 offset = footing.method_10069(dx, 0, dz);

                RecorderWorldEffectCapture.checkSnowLayer(replay, snapshots, tick, world, offset);
                RecorderWorldEffectCapture.checkSnowLayer(replay, snapshots, tick, world, offset.method_10074());
            }
        }
    }

    private static void checkSnowLayer(Replay replay, Map<Long, class_2680> snapshots, int tick, class_638 world, class_2338 pos)
    {
        class_2680 state = world.method_8320(pos);

        if (!state.method_27852(class_2246.field_10477))
        {
            return;
        }

        long key = pos.method_10063();
        class_2680 previous = snapshots.get(key);
        int layers = state.method_11654(class_2488.field_11518);

        if (layers <= 0)
        {
            return;
        }

        if (previous != null && previous.method_27852(class_2246.field_10477) && previous.method_11654(class_2488.field_11518) >= layers)
        {
            return;
        }

        snapshots.put(key, state);
        RecorderWorldEffectCapture.addSetblockCommand(replay, tick, pos, state);
    }

    public static void addSetblockCommand(Replay replay, int tick, class_2338 pos, class_2680 state)
    {
        BaseValue.edit(replay.actions, (actions) ->
        {
            CommandActionClip commandClip = new CommandActionClip();

            commandClip.tick.set(tick);
            commandClip.duration.set(1);
            commandClip.command.set("setblock " + pos.method_10263() + " " + pos.method_10264() + " " + pos.method_10260() + " " + RecorderWorldEffectCapture.formatSetblockState(state));
            replay.actions.addClip(commandClip);
        });
    }

    public static void addSummonCommand(Replay replay, int tick, class_1297 entity)
    {
        class_2960 typeId = class_7923.field_41177.method_10221(entity.method_5864());
        class_2487 nbt = new class_2487();

        entity.method_5647(nbt);

        for (String key : SUMMON_NBT_STRIP_KEYS)
        {
            nbt.method_10551(key);
        }

        class_243 velocity = entity.method_18798();
        class_2499 motion = new class_2499();

        motion.add(class_2489.method_23241(velocity.field_1352));
        motion.add(class_2489.method_23241(velocity.field_1351));
        motion.add(class_2489.method_23241(velocity.field_1350));
        nbt.method_10566("Motion", motion);

        StringBuilder command = new StringBuilder();

        command.append("summon ");
        command.append(typeId);
        command.append(String.format(Locale.US, " %.5f %.5f %.5f", entity.method_23317(), entity.method_23318(), entity.method_23321()));

        if (!nbt.method_33133())
        {
            command.append(' ');
            command.append(nbt);
        }

        BaseValue.edit(replay.actions, (actions) ->
        {
            CommandActionClip commandClip = new CommandActionClip();

            commandClip.tick.set(tick);
            commandClip.duration.set(1);
            commandClip.command.set(command.toString());
            replay.actions.addClip(commandClip);
        });
    }

    public static String formatSetblockState(class_2680 state)
    {
        String id = class_7923.field_41175.method_10221(state.method_26204()).toString();
        String properties = state.method_11656().entrySet().stream()
            .map((entry) -> entry.getKey().method_11899() + "=" + entry.getValue().toString())
            .collect(Collectors.joining(","));

        if (properties.isEmpty())
        {
            return id;
        }

        return id + "[" + properties + "]";
    }
}
