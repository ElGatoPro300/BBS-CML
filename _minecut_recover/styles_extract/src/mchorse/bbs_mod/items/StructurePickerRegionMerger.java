package mchorse.bbs_mod.items;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.class_2338;

public class StructurePickerRegionMerger
{
    public static List<MergedRegion> merge(Set<class_2338> blocks)
    {
        return StructurePickerRegionMerger.merge((Collection<class_2338>) blocks);
    }

    public static List<MergedRegion> merge(Collection<class_2338> blocks)
    {
        if (blocks == null || blocks.isEmpty())
        {
            return List.of();
        }

        LongOpenHashSet open = new LongOpenHashSet(blocks.size());

        for (class_2338 pos : blocks)
        {
            open.add(pos.method_10063());
        }

        List<MergedRegion> merged = new ArrayList<>();

        while (!open.isEmpty())
        {
            long seed = open.iterator().nextLong();
            int x0 = class_2338.method_10061(seed);
            int y0 = class_2338.method_10071(seed);
            int z0 = class_2338.method_10083(seed);
            int x1 = x0;
            int y1 = y0;
            int z1 = z0;
            boolean expanded;

            do
            {
                expanded = false;

                if (StructurePickerRegionMerger.canFill(open, x0 - 1, y0, z0, x1, y1, z1))
                {
                    x0--;
                    expanded = true;
                }

                if (StructurePickerRegionMerger.canFill(open, x0, y0, z0, x1 + 1, y1, z1))
                {
                    x1++;
                    expanded = true;
                }

                if (StructurePickerRegionMerger.canFill(open, x0, y0, z0 - 1, x1, y1, z1))
                {
                    z0--;
                    expanded = true;
                }

                if (StructurePickerRegionMerger.canFill(open, x0, y0, z0, x1, y1, z1 + 1))
                {
                    z1++;
                    expanded = true;
                }

                if (StructurePickerRegionMerger.canFill(open, x0, y0 - 1, z0, x1, y1, z1))
                {
                    y0--;
                    expanded = true;
                }

                if (StructurePickerRegionMerger.canFill(open, x0, y0, z0, x1, y1 + 1, z1))
                {
                    y1++;
                    expanded = true;
                }
            }
            while (expanded);

            for (int x = x0; x <= x1; x++)
            {
                for (int y = y0; y <= y1; y++)
                {
                    for (int z = z0; z <= z1; z++)
                    {
                        open.remove(class_2338.method_10064(x, y, z));
                    }
                }
            }

            StructurePickerMode mode = y0 == y1 ? StructurePickerMode.RECTANGLE : StructurePickerMode.CUBE;

            merged.add(new MergedRegion(new class_2338(x0, y0, z0), new class_2338(x1, y1, z1), mode));
        }

        return merged;
    }

    private static boolean canFill(LongOpenHashSet open, int x0, int y0, int z0, int x1, int y1, int z1)
    {
        for (int x = x0; x <= x1; x++)
        {
            for (int y = y0; y <= y1; y++)
            {
                for (int z = z0; z <= z1; z++)
                {
                    if (!open.contains(class_2338.method_10064(x, y, z)))
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public record MergedRegion(class_2338 min, class_2338 max, StructurePickerMode mode)
    {
    }
}
