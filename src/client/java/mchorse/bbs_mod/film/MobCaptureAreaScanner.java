package mchorse.bbs_mod.film;

import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.UIKeys;

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
 * Scans around an origin for entities that can be morphed into replays.
 * {@code size} is the radius in blocks. When {@code includeHeight} is false,
 * only the XZ plane is used (vertical cylinder); when true, range is a 3D sphere.
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
        return scan(size, false);
    }

    public static Map<String, TypeBucket> scan(double size, boolean includeHeight)
    {
        return scan(size, includeHeight, false);
    }

    public static Map<String, TypeBucket> scan(double size, boolean includeHeight, boolean capturePlayers)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (player == null)
        {
            return new LinkedHashMap<>();
        }

        return scan(size, player.getX(), player.getY(), player.getZ(), includeHeight, capturePlayers);
    }

    public static Map<String, TypeBucket> scan(MobCaptureRecordingSetup setup)
    {
        if (setup == null)
        {
            return new LinkedHashMap<>();
        }

        if (setup.usePlayerOrigin)
        {
            return scan(setup.areaSize, setup.includeHeight, setup.capturePlayers);
        }

        return scan(setup.areaSize, setup.originX, setup.originY, setup.originZ, setup.includeHeight, setup.capturePlayers);
    }

    public static Map<String, TypeBucket> scan(double size, double originX, double originY, double originZ)
    {
        return scan(size, originX, originY, originZ, false);
    }

    public static Map<String, TypeBucket> scan(double size, double originX, double originY, double originZ, boolean includeHeight)
    {
        return scan(size, originX, originY, originZ, includeHeight, false);
    }

    public static Map<String, TypeBucket> scan(double size, double originX, double originY, double originZ, boolean includeHeight, boolean capturePlayers)
    {
        Map<String, TypeBucket> buckets = new LinkedHashMap<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;

        if (player == null || world == null || size <= 0D)
        {
            return buckets;
        }

        double radius = size;
        double radiusSq = radius * radius;
        int bottom = world.getBottomY();
        int top = bottom + world.getDimension().logicalHeight();
        Box box;

        if (includeHeight)
        {
            box = new Box(
                originX - radius, originY - radius, originZ - radius,
                originX + radius, originY + radius, originZ + radius
            );
        }
        else
        {
            box = new Box(
                originX - radius, bottom, originZ - radius,
                originX + radius, top, originZ + radius
            );
        }

        for (Entity entity : world.getOtherEntities(player, box, (entity) -> canScan(entity, player, capturePlayers)))
        {
            if (distanceSq(entity, originX, originY, originZ, includeHeight) > radiusSq)
            {
                continue;
            }

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
            bucket.entities.sort(Comparator.comparingDouble((entity) -> distanceSq(entity, originX, originY, originZ, includeHeight)));
        }

        List<Map.Entry<String, TypeBucket>> sortedEntries = new ArrayList<>(buckets.entrySet());

        sortedEntries.sort(Comparator.comparingDouble((entry) ->
        {
            List<Entity> entities = entry.getValue().entities;

            if (entities.isEmpty())
            {
                return Double.MAX_VALUE;
            }

            return distanceSq(entities.get(0), originX, originY, originZ, includeHeight);
        }));

        Map<String, TypeBucket> sortedBuckets = new LinkedHashMap<>();

        for (Map.Entry<String, TypeBucket> entry : sortedEntries)
        {
            sortedBuckets.put(entry.getKey(), entry.getValue());
        }

        return sortedBuckets;
    }

    private static boolean canScan(Entity entity, ClientPlayerEntity player, boolean capturePlayers)
    {
        /* Film ActorEntity bodies are already replays — capturing them creates
         * phantom "actor" entries with leftover nametag/shadow at the death spot. */
        if (entity instanceof ActorEntity || entity == player)
        {
            return false;
        }

        return !(entity instanceof PlayerEntity) || capturePlayers;
    }

    public static double horizontalDistanceSq(Entity entity, double originX, double originZ)
    {
        double dx = entity.getX() - originX;
        double dz = entity.getZ() - originZ;

        return dx * dx + dz * dz;
    }

    public static double distanceSq(Entity entity, double originX, double originY, double originZ, boolean includeHeight)
    {
        double dx = entity.getX() - originX;
        double dz = entity.getZ() - originZ;

        if (!includeHeight)
        {
            return dx * dx + dz * dz;
        }

        double dy = entity.getY() - originY;

        return dx * dx + dy * dy + dz * dz;
    }

    public static int getDistanceBlocks(Entity entity, double originX, double originY, double originZ, boolean includeHeight)
    {
        return (int) Math.round(Math.sqrt(distanceSq(entity, originX, originY, originZ, includeHeight)));
    }

    public static int getHorizontalDistanceBlocks(Entity entity, double originX, double originZ)
    {
        return (int) Math.round(Math.sqrt(horizontalDistanceSq(entity, originX, originZ)));
    }

    public static int getDistanceBlocks(Entity entity, ClientPlayerEntity player)
    {
        if (player == null)
        {
            return 0;
        }

        return getHorizontalDistanceBlocks(entity, player.getX(), player.getZ());
    }

    public static String getEntityLabel(Entity entity, int index, double originX, double originY, double originZ, boolean includeHeight)
    {
        int distance = getDistanceBlocks(entity, originX, originY, originZ, includeHeight);
        String distanceText = UIKeys.FILM_MOB_CAPTURE_DISTANCE.format(String.valueOf(distance)).get();
        String coords = "(" + (int) entity.getX() + ", " + (int) entity.getY() + ", " + (int) entity.getZ() + ") · " + distanceText;

        if (entity.hasCustomName())
        {
            return entity.getCustomName().getString() + " " + coords;
        }

        return "#" + (index + 1) + " " + coords;
    }

    public static String getEntityLabel(Entity entity, int index, double originX, double originZ)
    {
        return getEntityLabel(entity, index, originX, 0D, originZ, false);
    }

    public static String getEntityLabel(Entity entity, int index, ClientPlayerEntity player)
    {
        if (player == null)
        {
            return getEntityLabel(entity, index, 0D, 0D, 0D, false);
        }

        return getEntityLabel(entity, index, player.getX(), player.getY(), player.getZ(), false);
    }
}
