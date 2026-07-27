package mchorse.bbs_mod.forms.structure;

import mchorse.bbs_mod.items.StructurePickerExporter;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_2487;
import net.minecraft.class_2499;
import net.minecraft.class_2520;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2682;
import net.minecraft.class_2769;
import net.minecraft.class_2960;
import net.minecraft.class_7923;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Cached structure-local collision boxes (after structure pivot, before form/model transforms).
 * Full cubes are greedy-meshed and queried through a spatial grid so large structures stay cheap.
 */
public final class StructureCollisionData
{
    private static final Map<String, StructureCollisionData> CACHE = new ConcurrentHashMap<>();
    private static final int CELL = 4;
    private static final ThreadLocal<BitSet> QUERY_SEEN = ThreadLocal.withInitial(BitSet::new);

    public final List<class_238> localBoxes;
    public final class_238 localBounds;
    private final Map<Long, int[]> spatialGrid;

    private StructureCollisionData(List<class_238> localBoxes, class_238 localBounds, Map<Long, int[]> spatialGrid)
    {
        this.localBoxes = List.copyOf(localBoxes);
        this.localBounds = localBounds;
        this.spatialGrid = spatialGrid;
    }

    public static StructureCollisionData get(String structurePath)
    {
        if (structurePath == null || structurePath.isEmpty())
        {
            return null;
        }

        return CACHE.computeIfAbsent(structurePath, StructureCollisionData::build);
    }

    public static void invalidate(String structurePath)
    {
        if (structurePath != null)
        {
            CACHE.remove(structurePath);
        }
    }

    /**
     * Invoke {@code consumer} for every local box whose AABB intersects {@code localQuery}.
     * Uses a coarse spatial grid so large structures only touch nearby cells.
     */
    public void forEachOverlapping(class_238 localQuery, Consumer<class_238> consumer)
    {
        if (this.localBoxes.isEmpty() || !this.localBounds.method_994(localQuery))
        {
            return;
        }

        if (this.spatialGrid.isEmpty() || this.localBoxes.size() <= 24)
        {
            for (class_238 local : this.localBoxes)
            {
                if (local.method_994(localQuery))
                {
                    consumer.accept(local);
                }
            }

            return;
        }

        int minCX = floorDiv(localQuery.field_1323, CELL);
        int minCY = floorDiv(localQuery.field_1322, CELL);
        int minCZ = floorDiv(localQuery.field_1321, CELL);
        int maxCX = floorDiv(localQuery.field_1320, CELL);
        int maxCY = floorDiv(localQuery.field_1325, CELL);
        int maxCZ = floorDiv(localQuery.field_1324, CELL);
        BitSet seen = QUERY_SEEN.get();

        seen.clear();

        for (int cy = minCY; cy <= maxCY; cy++)
        {
            for (int cz = minCZ; cz <= maxCZ; cz++)
            {
                for (int cx = minCX; cx <= maxCX; cx++)
                {
                    int[] indices = this.spatialGrid.get(packCell(cx, cy, cz));

                    if (indices == null)
                    {
                        continue;
                    }

                    for (int index : indices)
                    {
                        if (seen.get(index))
                        {
                            continue;
                        }

                        seen.set(index);
                        class_238 local = this.localBoxes.get(index);

                        if (local.method_994(localQuery))
                        {
                            consumer.accept(local);
                        }
                    }
                }
            }
        }
    }

    private static StructureCollisionData build(String path)
    {
        class_2487 root = StructurePickerExporter.readStructureNbt(path);

        if (root == null || !root.method_10573("blocks", class_2520.field_33259) || !root.method_10573("palette", class_2520.field_33259))
        {
            return empty();
        }

        List<class_2680> palette = new ArrayList<>();
        class_2499 paletteNbt = root.method_10554("palette", class_2520.field_33260);

        for (int i = 0; i < paletteNbt.size(); i++)
        {
            palette.add(readBlockState(paletteNbt.method_10602(i)));
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        Set<Long> fullCubes = new HashSet<>();
        List<class_2338> partialPos = new ArrayList<>();
        List<class_2680> partialStates = new ArrayList<>();
        class_2499 blocks = root.method_10554("blocks", class_2520.field_33260);
        boolean hasAny = false;

        for (int i = 0; i < blocks.size(); i++)
        {
            class_2487 entry = blocks.method_10602(i);
            int stateIndex = entry.method_10550("state");

            if (stateIndex < 0 || stateIndex >= palette.size())
            {
                continue;
            }

            class_2680 state = palette.get(stateIndex);

            if (state == null || state.method_26215())
            {
                continue;
            }

            class_2499 posList = entry.method_10554("pos", class_2520.field_33253);

            if (posList.size() < 3)
            {
                continue;
            }

            class_2338 pos = new class_2338(posList.method_10600(0), posList.method_10600(1), posList.method_10600(2));

            /* Bounds from all non-air blocks — same pivot basis as StructureFormRenderer. */
            hasAny = true;
            minX = Math.min(minX, pos.method_10263());
            minY = Math.min(minY, pos.method_10264());
            minZ = Math.min(minZ, pos.method_10260());
            maxX = Math.max(maxX, pos.method_10263());
            maxY = Math.max(maxY, pos.method_10264());
            maxZ = Math.max(maxZ, pos.method_10260());

            class_265 shape = state.method_26220(class_2682.field_12294, class_2338.field_10980);

            if (shape.method_1110())
            {
                continue;
            }

            if (isFullBlockCube(shape))
            {
                fullCubes.add(packBlock(pos.method_10263(), pos.method_10264(), pos.method_10260()));
            }
            else
            {
                partialPos.add(pos);
                partialStates.add(state);
            }
        }

        if (!hasAny || (fullCubes.isEmpty() && partialPos.isEmpty()))
        {
            return empty();
        }

        /* Same pivot as StructureFormRenderer.calculateRenderInfo / getStructurePivot. */
        float pivotX = (minX + maxX) / 2F;
        float pivotY = minY;
        float pivotZ = (minZ + maxZ) / 2F;
        int widthX = maxX - minX + 1;
        int widthZ = maxZ - minZ + 1;
        float parityX = (widthX % 2 == 1) ? -0.5F : 0F;
        float parityZ = (widthZ % 2 == 1) ? -0.5F : 0F;

        pivotX -= parityX;
        pivotZ -= parityZ;

        List<class_238> boxes = new ArrayList<>();
        double boundsMinX = Double.POSITIVE_INFINITY;
        double boundsMinY = Double.POSITIVE_INFINITY;
        double boundsMinZ = Double.POSITIVE_INFINITY;
        double boundsMaxX = Double.NEGATIVE_INFINITY;
        double boundsMaxY = Double.NEGATIVE_INFINITY;
        double boundsMaxZ = Double.NEGATIVE_INFINITY;

        for (class_238 merged : greedyMergeFullCubes(fullCubes))
        {
            class_238 local = new class_238(
                merged.field_1323 - pivotX,
                merged.field_1322 - pivotY,
                merged.field_1321 - pivotZ,
                merged.field_1320 - pivotX,
                merged.field_1325 - pivotY,
                merged.field_1324 - pivotZ
            );

            boxes.add(local);
            boundsMinX = Math.min(boundsMinX, local.field_1323);
            boundsMinY = Math.min(boundsMinY, local.field_1322);
            boundsMinZ = Math.min(boundsMinZ, local.field_1321);
            boundsMaxX = Math.max(boundsMaxX, local.field_1320);
            boundsMaxY = Math.max(boundsMaxY, local.field_1325);
            boundsMaxZ = Math.max(boundsMaxZ, local.field_1324);
        }

        for (int i = 0; i < partialPos.size(); i++)
        {
            class_2338 pos = partialPos.get(i);
            class_2680 state = partialStates.get(i);
            class_265 shape = state.method_26220(class_2682.field_12294, class_2338.field_10980);

            for (class_238 part : shape.method_1090())
            {
                class_238 local = new class_238(
                    pos.method_10263() - pivotX + part.field_1323,
                    pos.method_10264() - pivotY + part.field_1322,
                    pos.method_10260() - pivotZ + part.field_1321,
                    pos.method_10263() - pivotX + part.field_1320,
                    pos.method_10264() - pivotY + part.field_1325,
                    pos.method_10260() - pivotZ + part.field_1324
                );

                boxes.add(local);
                boundsMinX = Math.min(boundsMinX, local.field_1323);
                boundsMinY = Math.min(boundsMinY, local.field_1322);
                boundsMinZ = Math.min(boundsMinZ, local.field_1321);
                boundsMaxX = Math.max(boundsMaxX, local.field_1320);
                boundsMaxY = Math.max(boundsMaxY, local.field_1325);
                boundsMaxZ = Math.max(boundsMaxZ, local.field_1324);
            }
        }

        if (boxes.isEmpty())
        {
            return empty();
        }

        class_238 bounds = new class_238(boundsMinX, boundsMinY, boundsMinZ, boundsMaxX, boundsMaxY, boundsMaxZ);

        return new StructureCollisionData(boxes, bounds, buildSpatialGrid(boxes));
    }

    private static StructureCollisionData empty()
    {
        return new StructureCollisionData(List.of(), new class_238(0, 0, 0, 0, 0, 0), Map.of());
    }

    private static boolean isFullBlockCube(class_265 shape)
    {
        List<class_238> parts = shape.method_1090();

        if (parts.size() != 1)
        {
            return false;
        }

        class_238 part = parts.get(0);

        return part.field_1323 <= 1.0E-4D && part.field_1322 <= 1.0E-4D && part.field_1321 <= 1.0E-4D
            && part.field_1320 >= 1D - 1.0E-4D && part.field_1325 >= 1D - 1.0E-4D && part.field_1324 >= 1D - 1.0E-4D;
    }

    /**
     * Merge adjacent full cubes into large AABBs (greedy meshing in X then Z then Y).
     */
    private static List<class_238> greedyMergeFullCubes(Set<Long> fullCubes)
    {
        List<class_238> merged = new ArrayList<>();
        Set<Long> remaining = new HashSet<>(fullCubes);

        while (!remaining.isEmpty())
        {
            long key = remaining.iterator().next();
            int x0 = unpackX(key);
            int y0 = unpackY(key);
            int z0 = unpackZ(key);
            int x1 = x0;

            while (remaining.contains(packBlock(x1 + 1, y0, z0)))
            {
                x1++;
            }

            int z1 = z0;

            expandZ:
            while (true)
            {
                for (int x = x0; x <= x1; x++)
                {
                    if (!remaining.contains(packBlock(x, y0, z1 + 1)))
                    {
                        break expandZ;
                    }
                }

                z1++;
            }

            int y1 = y0;

            expandY:
            while (true)
            {
                for (int z = z0; z <= z1; z++)
                {
                    for (int x = x0; x <= x1; x++)
                    {
                        if (!remaining.contains(packBlock(x, y1 + 1, z)))
                        {
                            break expandY;
                        }
                    }
                }

                y1++;
            }

            for (int y = y0; y <= y1; y++)
            {
                for (int z = z0; z <= z1; z++)
                {
                    for (int x = x0; x <= x1; x++)
                    {
                        remaining.remove(packBlock(x, y, z));
                    }
                }
            }

            merged.add(new class_238(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1));
        }

        return merged;
    }

    private static Map<Long, int[]> buildSpatialGrid(List<class_238> boxes)
    {
        Map<Long, List<Integer>> temp = new HashMap<>();

        for (int i = 0; i < boxes.size(); i++)
        {
            class_238 box = boxes.get(i);
            int minCX = floorDiv(box.field_1323, CELL);
            int minCY = floorDiv(box.field_1322, CELL);
            int minCZ = floorDiv(box.field_1321, CELL);
            int maxCX = floorDiv(box.field_1320 - 1.0E-6D, CELL);
            int maxCY = floorDiv(box.field_1325 - 1.0E-6D, CELL);
            int maxCZ = floorDiv(box.field_1324 - 1.0E-6D, CELL);

            for (int cy = minCY; cy <= maxCY; cy++)
            {
                for (int cz = minCZ; cz <= maxCZ; cz++)
                {
                    for (int cx = minCX; cx <= maxCX; cx++)
                    {
                        temp.computeIfAbsent(packCell(cx, cy, cz), k -> new ArrayList<>()).add(i);
                    }
                }
            }
        }

        Map<Long, int[]> grid = new HashMap<>(temp.size());

        for (Map.Entry<Long, List<Integer>> entry : temp.entrySet())
        {
            List<Integer> list = entry.getValue();
            int[] indices = new int[list.size()];

            for (int i = 0; i < list.size(); i++)
            {
                indices[i] = list.get(i);
            }

            grid.put(entry.getKey(), indices);
        }

        return grid;
    }

    private static int floorDiv(double value, int cell)
    {
        return (int) Math.floor(value / cell);
    }

    private static long packCell(int x, int y, int z)
    {
        return class_2338.method_10064(x, y, z);
    }

    private static long packBlock(int x, int y, int z)
    {
        return class_2338.method_10064(x, y, z);
    }

    private static int unpackX(long key)
    {
        return class_2338.method_10061(key);
    }

    private static int unpackY(long key)
    {
        return class_2338.method_10071(key);
    }

    private static int unpackZ(long key)
    {
        return class_2338.method_10083(key);
    }

    private static class_2680 readBlockState(class_2487 entry)
    {
        String name = entry.method_10558("Name");
        class_2248 block;

        try
        {
            block = class_7923.field_41175.method_10223(class_2960.method_60654(name));

            if (block == null)
            {
                block = class_2246.field_10124;
            }
        }
        catch (Exception e)
        {
            block = class_2246.field_10124;
        }

        if ("minecraft:jigsaw".equals(name) || block == class_2246.field_16540)
        {
            return class_2246.field_10124.method_9564();
        }

        class_2680 state = block.method_9564();

        if (entry.method_10573("Properties", class_2520.field_33260))
        {
            class_2487 props = entry.method_10562("Properties");

            for (String key : props.method_10541())
            {
                String value = props.method_10558(key);
                class_2769<?> property = block.method_9595().method_11663(key);

                if (property == null)
                {
                    continue;
                }

                Optional<?> parsed = property.method_11900(value);

                if (parsed.isPresent())
                {
                    try
                    {
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        class_2769 raw = property;
                        @SuppressWarnings("unchecked")
                        Comparable c = (Comparable) parsed.get();

                        state = state.method_11657(raw, c);
                    }
                    catch (Exception ignored)
                    {}
                }
            }
        }

        return state;
    }
}
