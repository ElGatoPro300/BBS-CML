package mchorse.bbs_mod.utils.presets;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PresetManager
{
    public static final PresetManager CLIPS = new PresetManager(BBSMod.getSettingsPath("presets/clips"));
    public static final PresetManager PARTICLE_LAYOUTS = new PresetManager(BBSMod.getSettingsPath("presets/particle_layouts"));
    public static final PresetManager BODY_PARTS = new PresetManager(BBSMod.getSettingsPath("presets/body_parts"));
    public static final PresetManager TEXTURES = new PresetManager(BBSMod.getSettingsPath("presets/textures"));
    public static final PresetManager KEYFRAMES = new PresetManager(BBSMod.getSettingsPath("presets/keyframes"));
    public static final PresetManager GUNS = new PresetManager(BBSMod.getSettingsPath("presets/guns"));
    public static final PresetManager ANIMATION_STATES = new PresetManager(BBSMod.getSettingsPath("presets/animation_states"));
    public static final PresetManager SHAPE_GRAPHS = new PresetManager(BBSMod.getSettingsPath("presets/shape_graphs"));
    public static final PresetManager LAYOUTS = new PresetManager(BBSMod.getSettingsPath("presets/layouts"));

    private File folder;

    public PresetManager(File folder)
    {
        this.folder = folder;

        this.folder.mkdirs();
    }

    public File getFolder()
    {
        return this.folder;
    }

    private static String normalizePath(String path)
    {
        if (path == null)
        {
            return "";
        }

        String normalized = path.trim().replace('\\', '/');

        while (normalized.startsWith("/"))
        {
            normalized = normalized.substring(1);
        }

        while (normalized.endsWith("/"))
        {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private File getPresetFile(String id)
    {
        String normalized = normalizePath(id);

        return new File(this.folder, normalized + ".json");
    }

    private File getDirectory(String directory)
    {
        String normalized = normalizePath(directory);

        return normalized.isEmpty() ? this.folder : new File(this.folder, normalized);
    }

    public boolean exists(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return false;
        }

        return this.getPresetFile(id).exists();
    }

    public MapType load(String id)
    {
        File file = this.getPresetFile(id);

        if (!file.exists())
        {
            return null;
        }

        try
        {
            BaseType read = DataToString.read(file);

            if (read.isMap())
            {
                return read.asMap();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public boolean save(String id, MapType mapType)
    {
        if (id == null || id.trim().isEmpty() || mapType == null)
        {
            return false;
        }

        this.folder.mkdirs();

        File file = this.getPresetFile(id);
        File parent = file.getParentFile();

        if (parent != null)
        {
            parent.mkdirs();
        }

        File tempFile = new File(parent == null ? this.folder : parent, file.getName() + ".tmp");

        if (!DataToString.writeSilently(tempFile, mapType, true))
        {
            return false;
        }

        try
        {
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            return true;
        }
        catch (IOException atomicMoveException)
        {
            try
            {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                return true;
            }
            catch (IOException moveException)
            {
                moveException.printStackTrace();
            }
        }

        tempFile.delete();

        return false;
    }

    public boolean delete(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return false;
        }

        File file = this.getPresetFile(id);

        return !file.exists() || file.delete();
    }

    public boolean rename(String from, String to, boolean overwrite)
    {
        if (from == null || to == null)
        {
            return false;
        }

        from = from.trim();
        to = to.trim();

        if (from.isEmpty() || to.isEmpty())
        {
            return false;
        }

        File source = this.getPresetFile(from);
        File target = this.getPresetFile(to);

        if (!source.exists())
        {
            return false;
        }

        if (source.equals(target))
        {
            return true;
        }

        if (target.exists() && !overwrite)
        {
            return false;
        }

        try
        {
            if (overwrite)
            {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            else
            {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            }

            return true;
        }
        catch (IOException atomicMoveException)
        {
            try
            {
                if (overwrite)
                {
                    Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                else
                {
                    Files.move(source.toPath(), target.toPath());
                }

                return true;
            }
            catch (IOException moveException)
            {
                moveException.printStackTrace();
            }
        }

        return false;
    }

    public List<String> getKeys()
    {
        ArrayList<String> keys = new ArrayList<>();

        this.collectKeysRecursive(this.folder, "", keys);
        keys.sort(Comparator.comparing((s) -> s.toLowerCase(Locale.ROOT)));

        return keys;
    }

    public List<String> getKeys(String directory)
    {
        ArrayList<String> strings = new ArrayList<>();
        File dir = this.getDirectory(directory);
        File[] files = dir.listFiles();

        if (files == null)
        {
            return strings;
        }

        String prefix = normalizePath(directory);

        if (!prefix.isEmpty())
        {
            prefix += "/";
        }

        for (File file : files)
        {
            String name = file.getName();

            if (file.isFile() && name.endsWith(".json"))
            {
                strings.add(prefix + name.substring(0, name.length() - 5));
            }
        }

        strings.sort(Comparator.comparing((s) -> s.toLowerCase(Locale.ROOT)));

        return strings;
    }

    public List<String> getFolders(String directory)
    {
        ArrayList<String> strings = new ArrayList<>();
        File dir = this.getDirectory(directory);
        File[] files = dir.listFiles();

        if (files == null)
        {
            return strings;
        }

        for (File file : files)
        {
            if (file.isDirectory())
            {
                strings.add(file.getName());
            }
        }

        strings.sort(Comparator.comparing((s) -> s.toLowerCase(Locale.ROOT)));

        return strings;
    }

    private void collectKeysRecursive(File directory, String prefix, List<String> output)
    {
        File[] files = directory.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            String name = file.getName();

            if (file.isDirectory())
            {
                this.collectKeysRecursive(file, prefix + name + "/", output);
            }
            else if (name.endsWith(".json"))
            {
                output.add(prefix + name.substring(0, name.length() - 5));
            }
        }
    }
}
