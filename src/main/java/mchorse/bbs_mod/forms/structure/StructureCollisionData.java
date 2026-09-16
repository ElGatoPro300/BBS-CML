package mchorse.bbs_mod.forms.structure;

import mchorse.bbs_mod.items.StructurePickerExporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

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

    public final List<AABB> localBoxes;
    public final AABB localBounds;
    private final Map<Long, int[]> spatialGrid;

    private StructureCollisionData(List<AABB> localBoxes, AABB localBounds, Map<Long, int[]> spatialGrid)
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
    public void forEachOverlapping(AABB localQuery, Consumer<AABB> consumer)
    {
        if (this.localBoxes.isEmpty() || !this.localBounds.intersects(localQuery))
        {
            return;
        }

        if (this.spatialGrid.isEmpty() || this.localBoxes.size() <= 24)
        {
            for (AABB local : this.localBoxes)
            {
                if (local.intersects(localQuery))
                {
                    consumer.accept(local);
                }
            }

            return;
        }

        int minCX = floorDiv(localQuery.minX, CELL);
        int minCY = floorDiv(localQuery.minY, CELL);
        int minCZ = floorDiv(localQuery.minZ, CELL);
        int maxCX = floorDiv(localQuery.maxX, CELL);
        int maxCY = floorDiv(localQuery.maxY, CELL);
        int maxCZ = floorDiv(localQuery.maxZ, CELL);
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
                        AABB local = this.localBoxes.get(index);

                        if (local.intersects(localQuery))
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
        CompoundTag root = StructurePickerExporter.readStructureNbt(path);

        if (root == null || !root.contains("blocks") || !root.contains("palette"))
        {
            return empty();
        }

        List<BlockState> palette = new ArrayList<>();
        ListTag paletteNbt = root.getList("palette").orElse(null);

        if (paletteNbt == null)
        {
            return empty();
        }

        for (int i = 0; i < paletteNbt.size(); i++)
        {
            CompoundTag paletteEntry = paletteNbt.getCompound(i).orElse(null);

            if (paletteEntry != null)
            {
                palette.add(readBlockState(paletteEntry));
            }
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        Set<Long> fullCubes = new HashSet<>();
        List<BlockPos> partialPos = new ArrayList<>();
        List<BlockState> partialStates = new ArrayList<>();
        ListTag blocks = root.getList("blocks").orElse(null);

        if (blocks == null)
        {
            return empty();
        }

        boolean hasAny = false;

        for (int i = 0; i < blocks.size(); i++)
        {
            CompoundTag entry = blocks.getCompound(i).orElse(null);

            if (entry == null)
            {
                continue;
            }

            int stateIndex = entry.getInt("state").orElse(-1);

            if (stateIndex < 0 || stateIndex >= palette.size())
            {
                continue;
            }

            BlockState state = palette.get(stateIndex);

            if (state == null || state.isAir())
            {
                continue;
            }

            ListTag posList = entry.getList("pos").orElse(null);

            if (posList == null || posList.size() < 3)
            {
                continue;
            }

            BlockPos pos = new BlockPos(posList.getInt(0).orElse(0), posList.getInt(1).orElse(0), posList.getInt(2).orElse(0));

            /* Bounds from all non-air blocks — same pivot basis as StructureFormRenderer. */
            hasAny = true;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());

            VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

            if (shape.isEmpty())
            {
                continue;
            }

            if (isFullBlockCube(shape))
            {
                fullCubes.add(packBlock(pos.getX(), pos.getY(), pos.getZ()));
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

        List<AABB> boxes = new ArrayList<>();
        double boundsMinX = Double.POSITIVE_INFINITY;
        double boundsMinY = Double.POSITIVE_INFINITY;
        double boundsMinZ = Double.POSITIVE_INFINITY;
        double boundsMaxX = Double.NEGATIVE_INFINITY;
        double boundsMaxY = Double.NEGATIVE_INFINITY;
        double boundsMaxZ = Double.NEGATIVE_INFINITY;

        for (AABB merged : greedyMergeFullCubes(fullCubes))
        {
            AABB local = new AABB(
                merged.minX - pivotX,
                merged.minY - pivotY,
                merged.minZ - pivotZ,
                merged.maxX - pivotX,
                merged.maxY - pivotY,
                merged.maxZ - pivotZ
            );

            boxes.add(local);
            boundsMinX = Math.min(boundsMinX, local.minX);
            boundsMinY = Math.min(boundsMinY, local.minY);
            boundsMinZ = Math.min(boundsMinZ, local.minZ);
            boundsMaxX = Math.max(boundsMaxX, local.maxX);
            boundsMaxY = Math.max(boundsMaxY, local.maxY);
            boundsMaxZ = Math.max(boundsMaxZ, local.maxZ);
        }

        for (int i = 0; i < partialPos.size(); i++)
        {
            BlockPos pos = partialPos.get(i);
            BlockState state = partialStates.get(i);
            VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

            for (AABB part : shape.toAabbs())
            {
                AABB local = new AABB(
                    pos.getX() - pivotX + part.minX,
                    pos.getY() - pivotY + part.minY,
                    pos.getZ() - pivotZ + part.minZ,
                    pos.getX() - pivotX + part.maxX,
                    pos.getY() - pivotY + part.maxY,
                    pos.getZ() - pivotZ + part.maxZ
                );

                boxes.add(local);
                boundsMinX = Math.min(boundsMinX, local.minX);
                boundsMinY = Math.min(boundsMinY, local.minY);
                boundsMinZ = Math.min(boundsMinZ, local.minZ);
                boundsMaxX = Math.max(boundsMaxX, local.maxX);
                boundsMaxY = Math.max(boundsMaxY, local.maxY);
                boundsMaxZ = Math.max(boundsMaxZ, local.maxZ);
            }
        }

        if (boxes.isEmpty())
        {
            return empty();
        }

        AABB bounds = new AABB(boundsMinX, boundsMinY, boundsMinZ, boundsMaxX, boundsMaxY, boundsMaxZ);

        return new StructureCollisionData(boxes, bounds, buildSpatialGrid(boxes));
    }

    private static StructureCollisionData empty()
    {
        return new StructureCollisionData(List.of(), new AABB(0, 0, 0, 0, 0, 0), Map.of());
    }

    private static boolean isFullBlockCube(VoxelShape shape)
    {
        List<AABB> parts = shape.toAabbs();

        if (parts.size() != 1)
        {
            return false;
        }

        AABB part = parts.get(0);

        return part.minX <= 1.0E-4D && part.minY <= 1.0E-4D && part.minZ <= 1.0E-4D
            && part.maxX >= 1D - 1.0E-4D && part.maxY >= 1D - 1.0E-4D && part.maxZ >= 1D - 1.0E-4D;
    }

    /**
     * Merge adjacent full cubes into large AABBs (greedy meshing in X then Z then Y).
     */
    private static List<AABB> greedyMergeFullCubes(Set<Long> fullCubes)
    {
        List<AABB> merged = new ArrayList<>();
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

            merged.add(new AABB(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1));
        }

        return merged;
    }

    private static Map<Long, int[]> buildSpatialGrid(List<AABB> boxes)
    {
        Map<Long, List<Integer>> temp = new HashMap<>();

        for (int i = 0; i < boxes.size(); i++)
        {
            AABB box = boxes.get(i);
            int minCX = floorDiv(box.minX, CELL);
            int minCY = floorDiv(box.minY, CELL);
            int minCZ = floorDiv(box.minZ, CELL);
            int maxCX = floorDiv(box.maxX - 1.0E-6D, CELL);
            int maxCY = floorDiv(box.maxY - 1.0E-6D, CELL);
            int maxCZ = floorDiv(box.maxZ - 1.0E-6D, CELL);

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
        return BlockPos.asLong(x, y, z);
    }

    private static long packBlock(int x, int y, int z)
    {
        return BlockPos.asLong(x, y, z);
    }

    private static int unpackX(long key)
    {
        return BlockPos.getX(key);
    }

    private static int unpackY(long key)
    {
        return BlockPos.getY(key);
    }

    private static int unpackZ(long key)
    {
        return BlockPos.getZ(key);
    }

    private static BlockState readBlockState(CompoundTag entry)
    {
        if (entry == null)
        {
            return Blocks.AIR.defaultBlockState();
        }

        String name = entry.getString("Name").orElse("");
        Block block;

        try
        {
            block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));

            if (block == null)
            {
                block = Blocks.AIR;
            }
        }
        catch (Exception e)
        {
            block = Blocks.AIR;
        }

        if ("minecraft:jigsaw".equals(name) || block == Blocks.JIGSAW)
        {
            return Blocks.AIR.defaultBlockState();
        }

        BlockState state = block.defaultBlockState();

        if (entry.contains("Properties"))
        {
            CompoundTag props = entry.getCompound("Properties").orElse(null);

            if (props != null)
            {
                for (String key : props.keySet())
                {
                    String value = props.getString(key).orElse("");
                    Property<?> property = block.getStateDefinition().getProperty(key);

                    if (property == null)
                    {
                        continue;
                    }

                    Optional<?> parsed = property.getValue(value);

                    if (parsed.isPresent())
                    {
                        try
                        {
                            @SuppressWarnings({"rawtypes", "unchecked"})
                            Property raw = property;
                            @SuppressWarnings("unchecked")
                            Comparable c = (Comparable) parsed.get();

                            state = state.setValue(raw, c);
                        }
                        catch (Exception ignored)
                        {}
                    }
                }
            }
        }

        return state;
    }
}
