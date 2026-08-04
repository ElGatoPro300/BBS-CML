package mchorse.bbs_mod.film;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans a horizontal square around the player for entities that can be morphed into replays.
 */
public final class MobCaptureAreaScanner
{
    public static final class TypeBucket
    {
        public final String typeId;
        public final String label;
        public final List<Entity> entities = new ArrayList<>();

        public TypeBucket(String typeId, String label)
        {
            this.typeId = typeId;
            this.label = label;
        }
    }

    private MobCaptureAreaScanner()
    {}

    public static Map<String, TypeBucket> scan(double size)
    {
        Map<String, TypeBucket> buckets = new LinkedHashMap<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;

        if (player == null || world == null || size <= 0D)
        {
            return buckets;
        }

        double half = size / 2D;
        int bottom = world.getBottomY();
        int top = bottom + world.getDimension().logicalHeight();
        Box box = new Box(
            player.getX() - half, bottom, player.getZ() - half,
            player.getX() + half, top, player.getZ() + half
        );

        for (Entity entity : world.getOtherEntities(player, box, MobCaptureAreaScanner::canScan))
        {
            Form form = Morph.captureFormFromEntity(player, entity);

            if (form == null)
            {
                continue;
            }

            String typeId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            TypeBucket bucket = buckets.get(typeId);

            if (bucket == null)
            {
                Text name = entity.getType().getName();

                bucket = new TypeBucket(typeId, name.getString());
                buckets.put(typeId, bucket);
            }

            bucket.entities.add(entity);
        }

        for (TypeBucket bucket : buckets.values())
        {
            bucket.entities.sort(Comparator.comparingDouble((entity) -> player.squaredDistanceTo(entity.getPos())));
        }

        List<Map.Entry<String, TypeBucket>> sortedEntries = new ArrayList<>(buckets.entrySet());

        sortedEntries.sort(Comparator.comparingDouble((entry) ->
        {
            List<Entity> entities = entry.getValue().entities;

            if (entities.isEmpty())
            {
                return Double.MAX_VALUE;
            }

            return player.squaredDistanceTo(entities.get(0).getPos());
        }));

        Map<String, TypeBucket> sortedBuckets = new LinkedHashMap<>();

        for (Map.Entry<String, TypeBucket> entry : sortedEntries)
        {
            sortedBuckets.put(entry.getKey(), entry.getValue());
        }

        return sortedBuckets;
    }

    private static boolean canScan(Entity entity)
    {
        return !(entity instanceof PlayerEntity);
    }

    public static int getDistanceBlocks(Entity entity, ClientPlayerEntity player)
    {
        if (player == null)
        {
            return 0;
        }

        return (int) Math.round(player.getPos().distanceTo(entity.getPos()));
    }

    public static String getEntityLabel(Entity entity, int index, ClientPlayerEntity player)
    {
        if (entity.hasCustomName())
        {
            String name = entity.getCustomName().getString();
            int distance = getDistanceBlocks(entity, player);

            return name + " (" + (int) entity.getX() + ", " + (int) entity.getY() + ", " + (int) entity.getZ() + ") · " + distance + " blocks";
        }

        int distance = getDistanceBlocks(entity, player);

        return "#" + (index + 1) + " (" + (int) entity.getX() + ", " + (int) entity.getY() + ", " + (int) entity.getZ() + ") · " + distance + " blocks";
    }
}
