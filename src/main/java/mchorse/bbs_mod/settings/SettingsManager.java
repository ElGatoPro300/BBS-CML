package mchorse.bbs_mod.settings;

import mchorse.bbs_mod.data.DataToString;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SettingsManager
{
    public final Map<String, Settings> modules = new HashMap<>();

    public void reload()
    {
        for (Settings settings : this.modules.values())
        {
            this.load(settings, settings.file);
        }
    }

    public boolean load(Settings settings, File file)
    {
        if (!file.exists())
        {
            settings.save(file);

            return false;
        }

        try
        {
            settings.fromData(DataToString.read(file));

            return true;
        }
        catch (Exception e)
        {}

        return false;
    }
}