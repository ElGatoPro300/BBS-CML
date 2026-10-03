package mchorse.bbs_mod.items;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.mixin.StructureTemplateAccessor;
import mchorse.bbs_mod.mixin.StructureTemplatePalettedListAccessor;
import mchorse.bbs_mod.resources.Link;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructurePickerExporter
{
    public static String export(ServerLevel world, List<BlockPos> blocks)
    {
        return export(world, blocks, null);
    }

    public static String export(ServerLevel world, List<BlockPos> blocks, String customName)
    {
        if (blocks.isEmpty())
        {
            return null;
        }

        BlockPos min = blocks.getFirst();
        BlockPos max = blocks.getFirst();

        for (BlockPos pos : blocks)
        {
            min = StructurePickerSelection.min(min, pos);
            max = StructurePickerSelection.max(max, pos);
        }

        Vec3i size = max.subtract(min).offset(1, 1, 1);
        StructureTemplate template = new StructureTemplate();

        template.fillFromWorld(world, min, size, true, List.of(Blocks.STRUCTURE_VOID));
        filterTemplate(template, min, new HashSet<>(blocks));

        File folder = BBSMod.getAssetsPath("structures");

        if (!folder.exists())
        {
            folder.mkdirs();
        }

        String fileName = resolveFileName(folder, customName);
        File file = new File(folder, fileName);

        try
        {
            CompoundTag nbt = new CompoundTag();

            template.save(nbt);
            NbtIo.writeCompressed(nbt, file.toPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return null;
        }

        /* Same path style StructureForm / ExtraFormSection already load from assets. */
        return "structures/" + fileName;
    }

    /**
     * Overwrite an existing structure file with the current selection (same path / name).
     */
    public static boolean exportOverwrite(ServerLevel world, List<BlockPos> blocks, String structurePath)
    {
        if (blocks == null || blocks.isEmpty() || structurePath == null || structurePath.isEmpty())
        {
            return false;
        }

        File file = StructurePickerExporter.resolveWritableStructureFile(structurePath);

        if (file == null)
        {
            return false;
        }

        BlockPos min = blocks.getFirst();
        BlockPos max = blocks.getFirst();

        for (BlockPos pos : blocks)
        {
            min = StructurePickerSelection.min(min, pos);
            max = StructurePickerSelection.max(max, pos);
        }

        Vec3i size = max.subtract(min).offset(1, 1, 1);
        StructureTemplate template = new StructureTemplate();

        template.fillFromWorld(world, min, size, true, List.of(Blocks.STRUCTURE_VOID));
        filterTemplate(template, min, new HashSet<>(blocks));

        try
        {
            CompoundTag nbt = new CompoundTag();

            template.save(nbt);
            NbtIo.writeCompressed(nbt, file.toPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    private static File resolveWritableStructureFile(String structurePath)
    {
        if (structurePath == null || structurePath.isEmpty())
        {
            return null;
        }

        Link link = Link.create(structurePath);

        if (link.source.equals("bbs"))
        {
            return null;
        }

        File folder = BBSMod.getAssetsPath("structures");

        return new File(folder, link.path);
    }

    private static String resolveFileName(File folder, String customName)
    {
        if (customName != null && !customName.trim().isEmpty())
        {
            String name = customName.trim();

            if (!name.endsWith(".nbt"))
            {
                name = name + ".nbt";
            }

            return name;
        }

        return "pick_" + System.currentTimeMillis() + ".nbt";
    }

    private static String displayNameOf(String customName, String structurePath)
    {
        if (customName != null && !customName.trim().isEmpty())
        {
            return customName.trim();
        }

        if (structurePath == null || structurePath.isEmpty())
        {
            return "Structure";
        }

        String path = structurePath;
        int slash = path.lastIndexOf('/');

        if (slash >= 0 && slash + 1 < path.length())
        {
            path = path.substring(slash + 1);
        }

        if (path.endsWith(".nbt"))
        {
            path = path.substring(0, path.length() - 4);
        }

        return path.isEmpty() ? "Structure" : path;
    }

    public static boolean placeModelBlock(ServerLevel world, BlockPos center, String structurePath)
    {
        return placeModelBlock(world, center, structurePath, null);
    }

    public static boolean placeModelBlock(ServerLevel world, BlockPos center, String structurePath, String customName)
    {
        if (structurePath == null || structurePath.isEmpty())
        {
            return false;
        }

        String displayName = displayNameOf(customName, structurePath);

        if (world.getBlockState(center).is(BBSMod.MODEL_BLOCK))
        {
            BlockEntity blockEntity = world.getBlockEntity(center);

            if (blockEntity instanceof ModelBlockEntity modelBlockEntity)
            {
                StructureForm form = new StructureForm();

                form.structureFile.set(structurePath);

                ModelProperties properties = modelBlockEntity.getProperties();

                properties.setForm(form);
                properties.setName(displayName);
                properties.setHitbox(true);
                modelBlockEntity.setChanged();
                world.sendBlockUpdated(center, world.getBlockState(center), world.getBlockState(center), 3);

                return true;
            }
        }

        StructureForm form = new StructureForm();

        form.structureFile.set(structurePath);

        BlockState modelState = BBSMod.MODEL_BLOCK.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, world.getFluidState(center).is(Fluids.WATER))
            .setValue(ModelBlock.LIGHT_LEVEL, 0);

        if (!world.setBlock(center, modelState, 3))
        {
            return false;
        }

        BlockEntity blockEntity = world.getBlockEntity(center);

        if (!(blockEntity instanceof ModelBlockEntity modelBlockEntity))
        {
            return false;
        }

        ModelProperties properties = modelBlockEntity.getProperties();

        properties.setForm(form);
        properties.setName(displayName);
        properties.setHitbox(true);
        modelBlockEntity.setChanged();
        world.sendBlockUpdated(center, modelState, modelState, 3);

        return true;
    }

    public static void removeBlocks(ServerLevel world, List<BlockPos> blocks)
    {
        for (BlockPos pos : blocks)
        {
            /* Never break model blocks (e.g. one just placed at the selection center) */
            if (world.getBlockState(pos).is(BBSMod.MODEL_BLOCK))
            {
                continue;
            }

            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public static List<BlockSnapshot> captureBlocks(ServerLevel world, List<BlockPos> blocks)
    {
        List<BlockSnapshot> snapshots = new ArrayList<>();

        for (BlockPos pos : blocks)
        {
            if (world.getBlockState(pos).is(BBSMod.MODEL_BLOCK))
            {
                continue;
            }

            snapshots.add(StructurePickerExporter.captureBlock(world, pos));
        }

        return snapshots;
    }

    public static BlockSnapshot captureBlock(ServerLevel world, BlockPos pos)
    {
        BlockPos immutable = pos.immutable();
        BlockState state = world.getBlockState(immutable);
        BlockEntity entity = world.getBlockEntity(immutable);
        CompoundTag nbt = entity == null ? null : entity.saveCustomOnly(world.registryAccess());

        return new BlockSnapshot(immutable, state, nbt);
    }

    public static void restoreBlocks(ServerLevel world, List<BlockSnapshot> snapshots)
    {
        for (BlockSnapshot snapshot : snapshots)
        {
            StructurePickerExporter.restoreBlock(world, snapshot);
        }
    }

    public static void restoreBlock(ServerLevel world, BlockSnapshot snapshot)
    {
        if (snapshot == null)
        {
            return;
        }

        world.setBlock(snapshot.pos(), snapshot.state(), 3);

        if (snapshot.nbt() != null)
        {
            BlockEntity blockEntity = BlockEntity.loadStatic(snapshot.pos(), snapshot.state(), snapshot.nbt(), world.registryAccess());

            if (blockEntity != null)
            {
                world.setBlockEntity(blockEntity);
            }
        }
    }

    public record BlockSnapshot(BlockPos pos, BlockState state, CompoundTag nbt)
    {
    }

    public record PlaceResult(BlockPos min, BlockPos max, List<BlockSnapshot> previousBlocks)
    {
    }

    public record TemplateSize(int x, int y, int z)
    {
        public boolean isEmpty()
        {
            return this.x <= 0 || this.y <= 0 || this.z <= 0;
        }
    }

    public static StructureTemplate loadTemplate(ServerLevel world, String pathString)
    {
        CompoundTag nbt = StructurePickerExporter.readStructureNbt(pathString);

        if (nbt == null || world == null)
        {
            return null;
        }

        StructureTemplate template = new StructureTemplate();

        template.load(world.holderLookup(Registries.BLOCK), nbt);

        return template;
    }

    public static TemplateSize readTemplateSize(String pathString)
    {
        CompoundTag nbt = StructurePickerExporter.readStructureNbt(pathString);

        if (nbt == null)
        {
            return new TemplateSize(0, 0, 0);
        }

        int[] size = nbt.getIntArray("size").orElse(null);

        if (size != null && size.length >= 3)
        {
            return new TemplateSize(size[0], size[1], size[2]);
        }

        ListTag sizeList = nbt.getList("size").orElse(null);

        if (sizeList != null && sizeList.size() >= 3)
        {
            return new TemplateSize(sizeList.getInt(0).orElse(0), sizeList.getInt(1).orElse(0), sizeList.getInt(2).orElse(0));
        }

        return new TemplateSize(0, 0, 0);
    }

    public static List<BlockPos> readSelectedOffsets(String pathString)
    {
        List<BlockPos> offsets = new ArrayList<>();
        CompoundTag root = StructurePickerExporter.readStructureNbt(pathString);

        if (root == null)
        {
            return offsets;
        }

        ListTag palette = root.getList("palette").orElse(null);

        if (palette == null)
        {
            return offsets;
        }

        boolean[] air = new boolean[palette.size()];

        for (int i = 0; i < palette.size(); i++)
        {
            CompoundTag paletteEntry = palette.getCompound(i).orElse(null);
            air[i] = paletteEntry == null || StructurePickerExporter.isAirPaletteEntry(paletteEntry);
        }

        ListTag blocks = root.getList("blocks").orElse(null);

        if (blocks == null)
        {
            return offsets;
        }

        for (int i = 0; i < blocks.size(); i++)
        {
            CompoundTag entry = blocks.getCompound(i).orElse(null);

            if (entry == null)
            {
                continue;
            }

            int state = entry.getInt("state").orElse(-1);

            if (state < 0 || state >= air.length || air[state])
            {
                continue;
            }

            ListTag pos = entry.getList("pos").orElse(null);

            if (pos == null || pos.size() < 3)
            {
                continue;
            }

            offsets.add(new BlockPos(pos.getInt(0).orElse(0), pos.getInt(1).orElse(0), pos.getInt(2).orElse(0)));
        }

        return offsets;
    }

    private static boolean isAirPaletteEntry(CompoundTag entry)
    {
        String name = entry.getString("Name").orElse("");

        return name.isEmpty() || name.equals("minecraft:air") || name.equals("minecraft:cave_air") || name.equals("minecraft:void_air") || name.equals("minecraft:structure_void");
    }

    public static List<BlockSnapshot> captureVolume(ServerLevel world, BlockPos min, BlockPos max)
    {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = min.getX(); x <= max.getX(); x++)
        {
            for (int y = min.getY(); y <= max.getY(); y++)
            {
                for (int z = min.getZ(); z <= max.getZ(); z++)
                {
                    snapshots.add(StructurePickerExporter.captureBlock(world, mutable.set(x, y, z)));
                }
            }
        }

        return snapshots;
    }

    public static PlaceResult placeStructure(ServerLevel world, String pathString, BlockPos origin)
    {
        StructureTemplate template = StructurePickerExporter.loadTemplate(world, pathString);

        if (template == null)
        {
            return null;
        }

        Vec3i size = template.getSize();

        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0)
        {
            return null;
        }

        BlockPos min = origin.immutable();
        BlockPos max = min.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        List<BlockSnapshot> previous = StructurePickerExporter.captureVolume(world, min, max);
        StructurePlaceSettings data = new StructurePlaceSettings();

        template.placeInWorld(world, min, min, data, world.getRandom(), 3);

        return new PlaceResult(min, max, previous);
    }

    /**
     * Block position whose center matches the structure form's render pivot,
     * so the rendered structure lines up with the original world blocks.
     */
    public static BlockPos getPlacementPos(BlockPos min, BlockPos max)
    {
        int sizeX = max.getX() - min.getX() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;

        return new BlockPos(
            min.getX() + (sizeX - 1) / 2,
            min.getY(),
            min.getZ() + (sizeZ - 1) / 2
        );
    }

    private static void filterTemplate(StructureTemplate template, BlockPos origin, Set<BlockPos> selected)
    {
        StructureTemplateAccessor accessor = (StructureTemplateAccessor) template;

        for (StructureTemplate.Palette list : accessor.bbs$getBlockInfoLists())
        {
            StructureTemplatePalettedListAccessor palette = (StructureTemplatePalettedListAccessor) (Object) list;

            palette.bbs$getInfos().removeIf((info) -> !selected.contains(origin.offset(info.pos())));
        }

        accessor.bbs$getBlockInfoLists().removeIf((list) -> ((StructureTemplatePalettedListAccessor) (Object) list).bbs$getInfos().isEmpty());
    }

    public static CompoundTag readStructureNbt(String pathString)
    {
        if (pathString == null || pathString.isEmpty())
        {
            return null;
        }

        Link link = Link.create(pathString);
        File file = BBSMod.getProvider().getFile(link);

        try
        {
            if (file != null && file.exists())
            {
                return NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
            }

            try (InputStream stream = BBSMod.getProvider().getAsset(link))
            {
                if (stream == null)
                {
                    return null;
                }

                return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return null;
        }
    }
}
