package mchorse.bbs_mod.utils.interps;

import mchorse.bbs_mod.BBSMod;
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
    {}

    public File getFolder()
    {
        return BBSMod.getSettingsPath("presets/interpolations");
    }

    public void load()
    {
        this.interpolations.clear();
        File folder = this.getFolder();
        
        if (!folder.exists())
        {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles();
        
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
        File folder = this.getFolder();

        if (!folder.exists())
        {
            folder.mkdirs();
        }
        
        File file = new File(folder, interp.getKey() + ".json");
        
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
