package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.repos.IRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ModelRepository implements IRepository<ModelConfig>
{
    private ModelManager manager;

    public ModelRepository(ModelManager manager)
    {
        this.manager = manager;
    }

    @Override
    public ModelConfig create(String id, MapType data)
    {
        ModelConfig config = new ModelConfig(id);

        if (data != null)
        {
            config.fromData(data);
        }

        File folder = new File(this.getFolder(), id);

        if (!folder.exists())
        {
            folder.mkdirs();
        }

        return config;
    }

    @Override
    public void load(String id, Consumer<ModelConfig> callback)
    {
        ModelInstance model = this.manager.loadModel(id);
        ModelConfig config = new ModelConfig(id);

        if (model != null)
        {
            config.fromData(model.toConfig());
        }
        else
        {
            File file = new File(this.getFolder(), id + "/" + ModelManager.CONFIG_FILE);

            if (file.exists())
            {
                try
                {
                    BaseType base = DataToString.fromString(IOUtils.readText(file));

                    if (base.isMap())
                    {
                        config.fromData(base.asMap());
                    }
                }
                catch (Exception e)
                {}
            }
        }

        callback.accept(config);
    }

    @Override
    public void save(String id, MapType data)
    {
        this.saveConfigOnly(id, data);
        this.manager.loadModel(id);
    }

    public void saveConfigOnly(String id, MapType data)
    {
        this.manager.saveConfig(id, data);
    }

    @Override
    public void rename(String id, String name)
    {
        this.manager.renameModel(id, name);
    }

    @Override
    public void delete(String id)
    {
        File folder = BBSMod.getProvider().getFile(Link.assets(ModelManager.MODELS_PREFIX + id));

        if (folder != null && folder.exists())
        {
            IOUtils.deleteFolder(folder);
        }
    }

    @Override
    public void requestKeys(Consumer<Collection<String>> callback)
    {
        List<String> keys = new ArrayList<>(this.manager.getAvailableKeys());

        this.addEmptyFolders(this.getFolder(), "", keys);
        callback.accept(keys);
    }

    /* Recursively find subdirectories that are not themselves model directories
       (i.e. they appear as navigation folders, not as model entries in keys). */
    private void addEmptyFolders(File dir, String prefix, List<String> keys)
    {
        File[] files = dir.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (!file.isDirectory())
            {
                continue;
            }

            String modelKey = prefix + file.getName();
            String folderPath = modelKey + "/";

            /* Directories that are models appear in keys without a trailing slash */
            if (keys.contains(modelKey))
            {
                continue;
            }

            boolean hasContent = keys.stream().anyMatch((k) -> k.startsWith(folderPath));

            if (!hasContent)
            {
                keys.add(folderPath);
            }

            this.addEmptyFolders(file, folderPath, keys);
        }
    }

    @Override
    public File getFolder()
    {
        return BBSMod.getProvider().getFile(Link.assets("models"));
    }

    @Override
    public void addFolder(String path, Consumer<Boolean> callback)
    {
        File folder = new File(this.getFolder(), path);
        boolean result = folder.mkdirs();

        if (callback != null)
        {
            callback.accept(result);
        }
    }

    @Override
    public void renameFolder(String path, String name, Consumer<Boolean> callback)
    {
        File folder = new File(this.getFolder(), path);
        File newFolder = new File(this.getFolder(), name);
        boolean result = folder.renameTo(newFolder);

        if (callback != null)
        {
            callback.accept(result);
        }
    }

    @Override
    public void deleteFolder(String path, Consumer<Boolean> callback)
    {
        File folder = new File(this.getFolder(), path);
        
        if (folder.exists())
        {
            IOUtils.deleteFolder(folder);
        }

        boolean result = !folder.exists();

        if (callback != null)
        {
            callback.accept(result);
        }
    }
}
