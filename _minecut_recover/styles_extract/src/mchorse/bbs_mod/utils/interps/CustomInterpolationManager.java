package mchorse.bbs_mod.utils.interps;

import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomInterpolationManager
{
    public static final CustomInterpolationManager INSTANCE = new CustomInterpolationManager();
    
    private File folder;
    private Map<String, CustomInterpolation> interpolations = new HashMap<>();

    public CustomInterpolationManager()
    {
        this.folder = new File("config/bbs/settings/presets/interpolations");
    }

    public File getFolder()
    {
        return this.folder;
    }

    public void load()
    {
        this.interpolations.clear();
        
        if (!this.folder.exists())
        {
            this.folder.mkdirs();
            return;
        }

        File[] files = this.folder.listFiles();
        
        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (file.isFile() && file.getName().endsWith(".json"))
            {
                String name = file.getName().substring(0, file.getName().lastIndexOf("."));
                CustomInterpolation interp = new CustomInterpolation(name);
                
                try
                {
                    MapType data = DataToString.mapFromString(IOUtils.readText(file));
                    interp.fromData(data);
                    this.interpolations.put(name, interp);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void save(CustomInterpolation interp)
    {
        if (!this.folder.exists())
        {
            this.folder.mkdirs();
        }
        
        File file = new File(this.folder, interp.getKey() + ".json");
        
        try
        {
            IOUtils.writeText(file, DataToString.toString(interp.toData(), true));
            this.interpolations.put(interp.getKey(), interp);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public List<CustomInterpolation> getList()
    {
        return new ArrayList<>(this.interpolations.values());
    }

    public CustomInterpolation get(String key)
    {
        if (this.interpolations.isEmpty())
        {
            this.load();
        }

        return this.interpolations.get(key);
    }
}
