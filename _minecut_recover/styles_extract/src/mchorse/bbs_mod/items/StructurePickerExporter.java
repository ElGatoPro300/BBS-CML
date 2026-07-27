package mchorse.bbs_mod.items;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.mixin.StructureTemplateAccessor;
import mchorse.bbs_mod.mixin.StructureTemplatePalettedListAccessor;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2382;
import net.minecraft.class_2487;
import net.minecraft.class_2499;
import net.minecraft.class_2505;
import net.minecraft.class_2507;
import net.minecraft.class_2520;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_3218;
import net.minecraft.class_3492;
import net.minecraft.class_3499;
import net.minecraft.class_3612;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructurePickerExporter
{
    public static String export(class_3218 world, List<class_2338> blocks)
    {
        return export(world, blocks, null);
    }

    public static String export(class_3218 world, List<class_2338> blocks, String customName)
    {
        if (blocks.isEmpty())
        {
            return null;
        }

        class_2338 min = blocks.getFirst();
        class_2338 max = blocks.getFirst();

        for (class_2338 pos : blocks)
        {
            min = StructurePickerSelection.min(min, pos);
            max = StructurePickerSelection.max(max, pos);
        }

        class_2382 size = max.method_10059(min).method_10069(1, 1, 1);
        class_3499 template = new class_3499();

        template.method_15174(world, min, size, true, class_2246.field_10369);
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
            class_2487 nbt = new class_2487();

            template.method_15175(nbt);
            class_2507.method_30614(nbt, file.toPath());
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
    public static boolean exportOverwrite(class_3218 world, List<class_2338> blocks, String structurePath)
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

        class_2338 min = blocks.getFirst();
        class_2338 max = blocks.getFirst();

        for (class_2338 pos : blocks)
        {
            min = StructurePickerSelection.min(min, pos);
            max = StructurePickerSelection.max(max, pos);
        }

        class_2382 size = max.method_10059(min).method_10069(1, 1, 1);
        class_3499 template = new class_3499();

        template.method_15174(world, min, size, true, class_2246.field_10369);
        filterTemplate(template, min, new HashSet<>(blocks));

        File parent = file.getParentFile();

        if (parent != null && !parent.exists())
        {
            parent.mkdirs();
        }

        try
        {
            class_2487 nbt = new class_2487();

            template.method_15175(nbt);
            class_2507.method_30614(nbt, file.toPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public static File resolveWritableStructureFile(String pathString)
    {
        if (pathString == null || pathString.isEmpty())
        {
            return null;
        }

        String normalized = pathString;

        if (normalized.startsWith("saved:"))
        {
            normalized = "structures/" + normalized.substring("saved:".length());
        }

        Link link = Link.create(normalized);
        File existing = BBSMod.getProvider().getFile(link);

        if (existing != null)
        {
            return existing;
        }

        String display = StructurePickerExporter.displayNameOf(null, normalized);

        if (display.isEmpty())
        {
            return null;
        }

        File folder = BBSMod.getAssetsPath("structures");

        if (!folder.exists())
        {
            folder.mkdirs();
        }

        String fileName = display.endsWith(".nbt") ? display : display + ".nbt";

        return new File(folder, fileName);
    }

    private static String resolveFileName(File folder, String customName)
    {
        String sanitized = sanitizeFileName(customName);

        if (sanitized.isEmpty())
        {
            return "pick_" + System.currentTimeMillis() + ".nbt";
        }

        String base = sanitized;
        String fileName = base + ".nbt";
        File file = new File(folder, fileName);

        if (!file.exists())
        {
            return fileName;
        }

        return base + "_" + System.currentTimeMillis() + ".nbt";
    }

    public static String sanitizeFileName(String name)
    {
        if (name == null)
        {
            return "";
        }

        String trimmed = name.trim();

        if (trimmed.isEmpty())
        {
            return "";
        }

        StringBuilder builder = new StringBuilder(trimmed.length());

        for (int i = 0; i < trimmed.length(); i++)
        {
            char c = trimmed.charAt(i);

            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.' || c == '[' || c == ']' || c == '!')
            {
                builder.append(c);
            }
            else if (c == ' ')
            {
                builder.append('_');
            }
        }

        return builder.toString();
    }

    public static String displayNameOf(String customName, String structurePath)
    {
        String sanitized = sanitizeFileName(customName);

        if (!sanitized.isEmpty())
        {
            return sanitized;
        }

        if (structurePath != null && !structurePath.isEmpty())
        {
            String file = structurePath;

            if (file.startsWith("assets:"))
            {
                file = file.substring("assets:".length());
            }
            else if (file.startsWith("world:"))
            {
                file = file.substring("world:".length());
            }

            if (file.startsWith("structures/"))
            {
                file = file.substring("structures/".length());
            }

            if (file.endsWith(".nbt"))
            {
                file = file.substring(0, file.length() - 4);
            }

            if (!file.isEmpty())
            {
                return file;
            }
        }

        return "Structure";
    }

    public static boolean placeModelBlock(class_3218 world, class_2338 center, String structurePath)
    {
        return placeModelBlock(world, center, structurePath, null);
    }

    public static boolean placeModelBlock(class_3218 world, class_2338 center, String structurePath, String customName)
    {
        if (structurePath == null || structurePath.isEmpty())
        {
            return false;
        }

        String displayName = displayNameOf(customName, structurePath);

        if (world.method_8320(center).method_27852(BBSMod.MODEL_BLOCK))
        {
            class_2586 blockEntity = world.method_8321(center);

            if (blockEntity instanceof ModelBlockEntity modelBlockEntity)
            {
                StructureForm form = new StructureForm();

                form.structureFile.set(structurePath);

                ModelProperties properties = modelBlockEntity.getProperties();

                properties.setForm(form);
                properties.setName(displayName);
                properties.setHitbox(true);
                modelBlockEntity.method_5431();
                world.method_8413(center, world.method_8320(center), world.method_8320(center), 3);

                return true;
            }
        }

        StructureForm form = new StructureForm();

        form.structureFile.set(structurePath);

        net.minecraft.class_2680 modelState = BBSMod.MODEL_BLOCK.method_9564()
            .method_11657(class_2741.field_12508, world.method_8316(center).method_39360(class_3612.field_15910))
            .method_11657(ModelBlock.LIGHT_LEVEL, 0);

        if (!world.method_8652(center, modelState, 3))
        {
            return false;
        }

        class_2586 blockEntity = world.method_8321(center);

        if (!(blockEntity instanceof ModelBlockEntity modelBlockEntity))
        {
            return false;
        }

        ModelProperties properties = modelBlockEntity.getProperties();

        properties.setForm(form);
        properties.setName(displayName);
        properties.setHitbox(true);
        modelBlockEntity.method_5431();
        world.method_8413(center, modelState, modelState, 3);

        return true;
    }

    public static void removeBlocks(class_3218 world, List<class_2338> blocks)
    {
        for (class_2338 pos : blocks)
        {
            /* Never break model blocks (e.g. one just placed at the selection center) */
            if (world.method_8320(pos).method_27852(BBSMod.MODEL_BLOCK))
            {
                continue;
            }

            world.method_8652(pos, class_2246.field_10124.method_9564(), 3);
        }
    }

    public static List<BlockSnapshot> captureBlocks(class_3218 world, List<class_2338> blocks)
    {
        List<BlockSnapshot> snapshots = new ArrayList<>();

        for (class_2338 pos : blocks)
        {
            if (world.method_8320(pos).method_27852(BBSMod.MODEL_BLOCK))
            {
                continue;
            }

            class_2338 immutable = pos.method_10062();
            class_2680 state = world.method_8320(immutable);
            class_2586 entity = world.method_8321(immutable);
            class_2487 nbt = entity == null ? null : entity.method_38243(world.method_30349());

            snapshots.add(new BlockSnapshot(immutable, state, nbt));
        }

        return snapshots;
    }

    public static BlockSnapshot captureBlock(class_3218 world, class_2338 pos)
    {
        class_2338 immutable = pos.method_10062();
        class_2680 state = world.method_8320(immutable);
        class_2586 entity = world.method_8321(immutable);
        class_2487 nbt = entity == null ? null : entity.method_38243(world.method_30349());

        return new BlockSnapshot(immutable, state, nbt);
    }

    public static void restoreBlocks(class_3218 world, List<BlockSnapshot> snapshots)
    {
        for (BlockSnapshot snapshot : snapshots)
        {
            restoreBlock(world, snapshot);
        }
    }

    public static void restoreBlock(class_3218 world, BlockSnapshot snapshot)
    {
        if (snapshot == null)
        {
            return;
        }

        world.method_8652(snapshot.pos(), snapshot.state(), 3);

        if (snapshot.nbt() != null)
        {
            class_2586 blockEntity = class_2586.method_11005(snapshot.pos(), snapshot.state(), snapshot.nbt(), world.method_30349());

            if (blockEntity != null)
            {
                world.method_8438(blockEntity);
            }
        }
    }

    public record BlockSnapshot(class_2338 pos, class_2680 state, class_2487 nbt)
    {
    }

    public record PlaceResult(class_2338 min, class_2338 max, List<BlockSnapshot> previousBlocks)
    {
    }

    public record TemplateSize(int x, int y, int z)
    {
        public boolean isEmpty()
        {
            return this.x <= 0 || this.y <= 0 || this.z <= 0;
        }
    }

    public static class_3499 loadTemplate(class_3218 world, String pathString)
    {
        class_2487 nbt = StructurePickerExporter.readStructureNbt(pathString);

        if (nbt == null || world == null)
        {
            return null;
        }

        return world.method_14183().method_21891(nbt);
    }

    public static TemplateSize getTemplateSize(String pathString)
    {
        class_2487 nbt = StructurePickerExporter.readStructureNbt(pathString);

        if (nbt == null)
        {
            return new TemplateSize(0, 0, 0);
        }

        if (nbt.method_10573("size", class_2520.field_33261))
        {
            int[] size = nbt.method_10561("size");

            if (size.length >= 3)
            {
                return new TemplateSize(size[0], size[1], size[2]);
            }
        }

        if (nbt.method_10573("size", class_2520.field_33259))
        {
            class_2499 sizeList = nbt.method_10554("size", class_2520.field_33253);

            if (sizeList.size() >= 3)
            {
                return new TemplateSize(sizeList.method_10600(0), sizeList.method_10600(1), sizeList.method_10600(2));
            }
        }

        return new TemplateSize(0, 0, 0);
    }

    /**
     * Relative block offsets (non-air) for translucent blueprint preview.
     */
    public static List<class_2338> loadOccupiedOffsets(String pathString)
    {
        List<class_2338> offsets = new ArrayList<>();
        class_2487 root = StructurePickerExporter.readStructureNbt(pathString);

        if (root == null || !root.method_10573("blocks", class_2520.field_33259) || !root.method_10573("palette", class_2520.field_33259))
        {
            return offsets;
        }

        class_2499 palette = root.method_10554("palette", class_2520.field_33260);
        boolean[] air = new boolean[palette.size()];

        for (int i = 0; i < palette.size(); i++)
        {
            air[i] = StructurePickerExporter.isAirPaletteEntry(palette.method_10602(i));
        }

        class_2499 blocks = root.method_10554("blocks", class_2520.field_33260);

        for (int i = 0; i < blocks.size(); i++)
        {
            class_2487 entry = blocks.method_10602(i);
            int state = entry.method_10550("state");

            if (state < 0 || state >= air.length || air[state])
            {
                continue;
            }

            class_2499 pos = entry.method_10554("pos", class_2520.field_33253);

            if (pos.size() < 3)
            {
                continue;
            }

            offsets.add(new class_2338(pos.method_10600(0), pos.method_10600(1), pos.method_10600(2)));
        }

        return offsets;
    }

    private static boolean isAirPaletteEntry(class_2487 entry)
    {
        String name = entry.method_10558("Name");

        return name.isEmpty()
            || name.equals("minecraft:air")
            || name.equals("minecraft:cave_air")
            || name.equals("minecraft:void_air")
            || name.equals("minecraft:structure_void");
    }

    public static class_2487 readStructureNbt(String pathString)
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
                return class_2507.method_30613(file.toPath(), class_2505.method_53898());
            }

            try (java.io.InputStream stream = BBSMod.getProvider().getAsset(link))
            {
                if (stream == null)
                {
                    return null;
                }

                return class_2507.method_10629(stream, class_2505.method_53898());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return null;
        }
    }

    public static List<BlockSnapshot> captureVolume(class_3218 world, class_2338 min, class_2338 max)
    {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        class_2338.class_2339 mutable = new class_2338.class_2339();

        for (int x = min.method_10263(); x <= max.method_10263(); x++)
        {
            for (int y = min.method_10264(); y <= max.method_10264(); y++)
            {
                for (int z = min.method_10260(); z <= max.method_10260(); z++)
                {
                    snapshots.add(StructurePickerExporter.captureBlock(world, mutable.method_10103(x, y, z)));
                }
            }
        }

        return snapshots;
    }

    public static PlaceResult placeStructure(class_3218 world, String pathString, class_2338 origin)
    {
        class_3499 template = StructurePickerExporter.loadTemplate(world, pathString);

        if (template == null)
        {
            return null;
        }

        class_2382 size = template.method_15160();

        if (size.method_10263() <= 0 || size.method_10264() <= 0 || size.method_10260() <= 0)
        {
            return null;
        }

        class_2338 min = origin.method_10062();
        class_2338 max = min.method_10069(size.method_10263() - 1, size.method_10264() - 1, size.method_10260() - 1);
        List<BlockSnapshot> previous = StructurePickerExporter.captureVolume(world, min, max);
        class_3492 data = new class_3492();

        template.method_15172(world, min, min, data, world.method_8409(), 3);

        return new PlaceResult(min, max, previous);
    }

    /**
     * Block position whose center matches the structure form's render pivot,
     * so the rendered structure lines up with the original world blocks.
     */
    public static class_2338 getPlacementPos(class_2338 min, class_2338 max)
    {
        int sizeX = max.method_10263() - min.method_10263() + 1;
        int sizeZ = max.method_10260() - min.method_10260() + 1;

        return new class_2338(
            min.method_10263() + (sizeX - 1) / 2,
            min.method_10264(),
            min.method_10260() + (sizeZ - 1) / 2
        );
    }

    private static void filterTemplate(class_3499 template, class_2338 origin, Set<class_2338> selected)
    {
        StructureTemplateAccessor accessor = (StructureTemplateAccessor) template;

        for (class_3499.class_5162 list : accessor.bbs$getBlockInfoLists())
        {
            StructureTemplatePalettedListAccessor palette = (StructureTemplatePalettedListAccessor) (Object) list;

            palette.bbs$getInfos().removeIf((info) -> !selected.contains(origin.method_10081(info.comp_1341())));
        }

        accessor.bbs$getBlockInfoLists().removeIf((list) -> ((StructureTemplatePalettedListAccessor) (Object) list).bbs$getInfos().isEmpty());
    }
}
