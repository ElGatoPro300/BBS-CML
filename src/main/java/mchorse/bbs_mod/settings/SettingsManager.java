package mchorse.bbs_mod.settings;

import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsManager
{
    public final Map<String, Settings> modules = new LinkedHashMap<>();

    public void reload()
    {
        for (Settings settings : this.modules.values())
        {
            this.load(settings, settings.file);
        }
    }

    public boolean load(Settings settings, File file)
    {
        if (file == null)
        {
            return false;
        }

        BaseType data = this.readSettingsData(file);

        if (data == null)
        {
            File backup = this.backupFile(file);

            data = this.readSettingsData(backup);

            if (data != null)
            {
                try
                {
                    Files.copy(backup.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                settings.fromData(data);

                return true;
            }

            /* Quarantine empty/corrupt files so a later saveLater from migrations
             * does not silently look like "user wiped their settings". */
            if (file.isFile())
            {
                this.quarantineCorrupt(file);
            }

            settings.save(file);

            return false;
        }

        settings.fromData(data);

        return true;
    }

    private BaseType readSettingsData(File file)
    {
        if (file == null || !file.isFile() || file.length() <= 0L)
        {
            return null;
        }

        try
        {
            BaseType data = DataToString.read(file);

            return data != null && data.isMap() ? data : null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private File backupFile(File file)
    {
        return new File(file.getAbsolutePath() + ".bak");
    }

    private void quarantineCorrupt(File file)
    {
        File corrupt = new File(file.getAbsolutePath() + ".corrupt");

        try
        {
            Files.move(file.toPath(), corrupt.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception e)
        {
            e.printStackTrace();

            if (!file.delete())
            {
                file.deleteOnExit();
            }
        }
    }
}
