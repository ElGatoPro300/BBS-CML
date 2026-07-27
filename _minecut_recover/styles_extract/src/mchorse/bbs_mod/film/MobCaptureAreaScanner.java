package mchorse.bbs_mod.film;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_238;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_7923;
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
        public final List<class_1297> entities = new ArrayList<>();

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
        class_310 mc = class_310.method_1551();
        class_746 player = mc.field_1724;
        class_638 world = mc.field_1687;

        if (player == null || world == null || size <= 0D)
        {
            return buckets;
        }

        double half = size / 2D;
        int bottom = world.method_31607();
        int top = bottom + world.method_8597().comp_653();
        class_238 box = new class_238(
            player.method_23317() - half, bottom, player.method_23321() - half,
            player.method_23317() + half, top, player.method_23321() + half
        );

        for (class_1297 entity : world.method_8333(player, box, MobCaptureAreaScanner::canScan))
        {
            Form form = Morph.captureFormFromEntity(player, entity);

            if (form == null)
            {
                continue;
            }

            String typeId = class_7923.field_41177.method_10221(entity.method_5864()).toString();
            TypeBucket bucket = buckets.get(typeId);

            if (bucket == null)
            {
                class_2561 name = entity.method_5864().method_5897();

                bucket = new TypeBucket(typeId, name.getString());
                buckets.put(typeId, bucket);
            }

            bucket.entities.add(entity);
        }

        for (TypeBucket bucket : buckets.values())
        {
            bucket.entities.sort(Comparator.comparingDouble((entity) -> player.method_5707(entity.method_19538())));
        }

        List<Map.Entry<String, TypeBucket>> sortedEntries = new ArrayList<>(buckets.entrySet());

        sortedEntries.sort(Comparator.comparingDouble((entry) ->
        {
            List<class_1297> entities = entry.getValue().entities;

            if (entities.isEmpty())
            {
                return Double.MAX_VALUE;
            }

            return player.method_5707(entities.get(0).method_19538());
        }));

        Map<String, TypeBucket> sortedBuckets = new LinkedHashMap<>();

        for (Map.Entry<String, TypeBucket> entry : sortedEntries)
        {
            sortedBuckets.put(entry.getKey(), entry.getValue());
        }

        return sortedBuckets;
    }

    private static boolean canScan(class_1297 entity)
    {
        return !(entity instanceof class_1657);
    }

    public static int getDistanceBlocks(class_1297 entity, class_746 player)
    {
        if (player == null)
        {
            return 0;
        }

        return (int) Math.round(player.method_19538().method_1022(entity.method_19538()));
    }

    public static String getEntityLabel(class_1297 entity, int index, class_746 player)
    {
        if (entity.method_16914())
        {
            String name = entity.method_5797().getString();
            int distance = getDistanceBlocks(entity, player);

            return name + " (" + (int) entity.method_23317() + ", " + (int) entity.method_23318() + ", " + (int) entity.method_23321() + ") · " + distance + " blocks";
        }

        int distance = getDistanceBlocks(entity, player);

        return "#" + (index + 1) + " (" + (int) entity.method_23317() + ", " + (int) entity.method_23318() + ", " + (int) entity.method_23321() + ") · " + distance + " blocks";
    }
}
