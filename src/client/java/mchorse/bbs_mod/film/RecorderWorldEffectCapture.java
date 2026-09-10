package mchorse.bbs_mod.film;

import mchorse.bbs_mod.actions.types.chat.CommandActionClip;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

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

    public static void captureSnowTrail(Replay replay, Map<Long, BlockState> snapshots, LivingEntity entity, int tick, ClientLevel world)
    {
        BlockPos footing = entity.blockPosition();

        for (int dx = -SNOW_TRAIL_RADIUS; dx <= SNOW_TRAIL_RADIUS; dx++)
        {
            for (int dz = -SNOW_TRAIL_RADIUS; dz <= SNOW_TRAIL_RADIUS; dz++)
            {
                BlockPos offset = footing.offset(dx, 0, dz);

                RecorderWorldEffectCapture.checkSnowLayer(replay, snapshots, tick, world, offset);
                RecorderWorldEffectCapture.checkSnowLayer(replay, snapshots, tick, world, offset.below());
            }
        }
    }

    private static void checkSnowLayer(Replay replay, Map<Long, BlockState> snapshots, int tick, ClientLevel world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);

        if (!state.is(Blocks.SNOW))
        {
            return;
        }

        long key = pos.asLong();
        BlockState previous = snapshots.get(key);
        int layers = state.getValue(SnowLayerBlock.LAYERS);

        if (layers <= 0)
        {
            return;
        }

        if (previous != null && previous.is(Blocks.SNOW) && previous.getValue(SnowLayerBlock.LAYERS) >= layers)
        {
            return;
        }

        snapshots.put(key, state);
        RecorderWorldEffectCapture.addSetblockCommand(replay, tick, pos, state);
    }

    public static void addSetblockCommand(Replay replay, int tick, BlockPos pos, BlockState state)
    {
        BaseValue.edit(replay.actions, (actions) ->
        {
            CommandActionClip commandClip = new CommandActionClip();

            commandClip.tick.set(tick);
            commandClip.duration.set(1);
            commandClip.command.set("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + RecorderWorldEffectCapture.formatSetblockState(state));
            replay.actions.addClip(commandClip);
        });
    }

    public static void addSummonCommand(Replay replay, int tick, Entity entity)
    {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveAsPassenger(view);
        CompoundTag nbt = view.buildResult();

        for (String key : SUMMON_NBT_STRIP_KEYS)
        {
            nbt.remove(key);
        }

        Vec3 velocity = entity.getDeltaMovement();
        ListTag motion = new ListTag();

        motion.add(DoubleTag.valueOf(velocity.x));
        motion.add(DoubleTag.valueOf(velocity.y));
        motion.add(DoubleTag.valueOf(velocity.z));
        nbt.put("Motion", motion);

        StringBuilder command = new StringBuilder();

        command.append("summon ");
        command.append(typeId);
        command.append(String.format(Locale.US, " %.5f %.5f %.5f", entity.getX(), entity.getY(), entity.getZ()));

        if (!nbt.isEmpty())
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

    public static String formatSetblockState(BlockState state)
    {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String properties = state.getValues().entrySet().stream()
            .map((entry) -> entry.getKey().getName() + "=" + entry.getValue().toString())
            .collect(Collectors.joining(","));

        if (properties.isEmpty())
        {
            return id;
        }

        return id + "[" + properties + "]";
    }
}
