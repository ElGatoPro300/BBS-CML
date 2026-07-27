package mchorse.bbs_mod.bay4lly;

import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SkinManager
{
    private static final String SKINS_SUBPATH = "models/playerskins";

    public static File getSkinsFolder()
    {
        return BBSMod.getAssetsPath(SKINS_SUBPATH);
    }

    public static File getSkinFile(String playerName)
    {
        return new File(getSkinsFolder(), playerName + ".png");
    }

    public static void saveSkin(String playerName, File skinFile) throws IOException
    {
        File folder = getSkinsFolder();
        if (!folder.exists())
        {
            folder.mkdirs();
        }
        File target = getSkinFile(playerName);
        Files.copy(skinFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void saveSkin(String playerName, byte[] bytes) throws IOException
    {
        File folder = getSkinsFolder();
        if (!folder.exists())
        {
            folder.mkdirs();
        }
        File target = getSkinFile(playerName);
        Files.write(target.toPath(), bytes);
    }
}
