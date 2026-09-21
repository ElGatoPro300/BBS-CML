package mchorse.bbs_mod.items;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.mixin.StructureTemplateAccessor;
import mchorse.bbs_mod.mixin.StructureTemplatePalettedListAccessor;
import mchorse.bbs_mod.resources.Link;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructurePickerExporter
{
    public static String export(ServerWorld world, List<BlockPos> blocks)
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

        Vec3i size = max.subtract(min).add(1, 1, 1);
        StructureTemplate template = new StructureTemplate();

        template.saveFromWorld(world, min, size, true, Blocks.STRUCTURE_VOID);
        filterTemplate(template, min, new HashSet<>(blocks));

        File generatedFolder = world.getServer().getSavePath(WorldSavePath.GENERATED).toFile();
        File folder = new File(new File(generatedFolder, "minecraft"), "structures");

        if (!folder.exists())
        {
            folder.mkdirs();
        }

        String fileName = "pick_" + System.currentTimeMillis() + ".nbt";
        File file = new File(folder, fileName);

        try
        {
            NbtCompound nbt = new NbtCompound();

            template.writeNbt(nbt);
            NbtIo.writeCompressed(nbt, file.toPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return null;
        }

        return "world:" + fileName;
    }

    public static boolean placeModelBlock(ServerWorld world, BlockPos center, String structurePath)
    {
        if (structurePath == null || structurePath.isEmpty())
        {
            return false;
        }

        if (world.getBlockState(center).isOf(BBSMod.MODEL_BLOCK))
        {
            BlockEntity blockEntity = world.getBlockEntity(center);

            if (blockEntity instanceof ModelBlockEntity modelBlockEntity)
            {
                StructureForm form = new StructureForm();

                form.structureFile.set(structurePath);

                ModelProperties properties = modelBlockEntity.getProperties();

                properties.setForm(form);
                properties.setName("Structure");
                properties.setHitbox(true);
                modelBlockEntity.markDirty();
                world.updateListeners(center, world.getBlockState(center), world.getBlockState(center), 3);

                return true;
            }
        }

        StructureForm form = new StructureForm();

        form.structureFile.set(structurePath);

        BlockState modelState = BBSMod.MODEL_BLOCK.getDefaultState()
            .with(Properties.WATERLOGGED, world.getFluidState(center).isOf(Fluids.WATER))
            .with(ModelBlock.LIGHT_LEVEL, 0);

        if (!world.setBlockState(center, modelState, 3))
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
        properties.setName("Structure");
        properties.setHitbox(true);
        modelBlockEntity.markDirty();
        world.updateListeners(center, modelState, modelState, 3);

        return true;
    }

    public static void removeBlocks(ServerWorld world, List<BlockPos> blocks)
    {
        for (BlockPos pos : blocks)
        {
            /* Never break model blocks (e.g. one just placed at the selection center) */
            if (world.getBlockState(pos).isOf(BBSMod.MODEL_BLOCK))
            {
                continue;
            }

            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }

    public static List<BlockSnapshot> captureBlocks(ServerWorld world, List<BlockPos> blocks)
    {
        List<BlockSnapshot> snapshots = new ArrayList<>();

        for (BlockPos pos : blocks)
        {
            if (world.getBlockState(pos).isOf(BBSMod.MODEL_BLOCK))
            {
                continue;
            }

            snapshots.add(StructurePickerExporter.captureBlock(world, pos));
        }

        return snapshots;
    }

    public static BlockSnapshot captureBlock(ServerWorld world, BlockPos pos)
    {
        BlockPos immutable = pos.toImmutable();
        BlockState state = world.getBlockState(immutable);
        BlockEntity entity = world.getBlockEntity(immutable);
        NbtCompound nbt = entity == null ? null : entity.createNbtWithId(world.getRegistryManager());

        return new BlockSnapshot(immutable, state, nbt);
    }

    public static void restoreBlocks(ServerWorld world, List<BlockSnapshot> snapshots)
    {
        for (BlockSnapshot snapshot : snapshots)
        {
            StructurePickerExporter.restoreBlock(world, snapshot);
        }
    }

    public static void restoreBlock(ServerWorld world, BlockSnapshot snapshot)
    {
        if (snapshot == null)
        {
            return;
        }

        world.setBlockState(snapshot.pos(), snapshot.state(), 3);

        if (snapshot.nbt() != null)
        {
            BlockEntity blockEntity = BlockEntity.createFromNbt(snapshot.pos(), snapshot.state(), snapshot.nbt(), world.getRegistryManager());

            if (blockEntity != null)
            {
                world.addBlockEntity(blockEntity);
            }
        }
    }

    public static StructureTemplate loadTemplate(ServerWorld world, String pathString)
    {
        NbtCompound nbt = StructurePickerExporter.readStructureNbt(pathString);

        if (nbt == null || world == null)
        {
            return null;
        }

        return world.getStructureTemplateManager().createTemplate(nbt);
    }

    public static List<BlockSnapshot> captureVolume(ServerWorld world, BlockPos min, BlockPos max)
    {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

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

    /**
     * Places a saved structure NBT into the world and returns the previous volume
     * so panel undo can restore terrain. Used by PlaceStructure history entries.
     */
    public static PlaceResult placeStructure(ServerWorld world, String pathString, BlockPos origin)
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

        BlockPos min = origin.toImmutable();
        BlockPos max = min.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        List<BlockSnapshot> previous = StructurePickerExporter.captureVolume(world, min, max);
        StructurePlacementData data = new StructurePlacementData();

        template.place(world, min, min, data, world.getRandom(), 3);

        return new PlaceResult(min, max, previous);
    }

    public record BlockSnapshot(BlockPos pos, BlockState state, NbtCompound nbt)
    {
    }

    public record PlaceResult(BlockPos min, BlockPos max, List<BlockSnapshot> previousBlocks)
    {
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

        for (StructureTemplate.PalettedBlockInfoList list : accessor.bbs$getBlockInfoLists())
        {
            StructureTemplatePalettedListAccessor palette = (StructureTemplatePalettedListAccessor) (Object) list;

            palette.bbs$getInfos().removeIf((info) -> !selected.contains(origin.add(info.pos())));
        }

        accessor.bbs$getBlockInfoLists().removeIf((list) -> ((StructureTemplatePalettedListAccessor) (Object) list).bbs$getInfos().isEmpty());
    }

    public static NbtCompound readStructureNbt(String pathString)
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
                return NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
            }

            try (InputStream stream = BBSMod.getProvider().getAsset(link))
            {
                if (stream == null)
                {
                    return null;
                }

                return NbtIo.readCompressed(stream, NbtSizeTracker.ofUnlimitedBytes());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return null;
        }
    }
}
