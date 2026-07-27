package mchorse.bbs_mod.client;

import java.nio.file.Path;
import net.minecraft.class_310;
import net.minecraft.class_32;
import net.minecraft.class_5218;
import net.minecraft.class_7196;

public class WorldLaunchHelper
{
    public static boolean isCurrentWorld(class_310 client, String worldFolder)
    {
        if (worldFolder == null || worldFolder.isEmpty())
        {
            return false;
        }

        if (!client.method_1496() || client.method_1576() == null)
        {
            return false;
        }

        Path currentSave = client.method_1576().method_27050(class_5218.field_24188);

        for (class_32.class_7411 save : client.method_1586().method_235().comp_731())
        {
            if (save.method_43422().equals(worldFolder) && currentSave.equals(save.comp_732()))
            {
                return true;
            }
        }

        String current = currentSave.getFileName().toString();

        return worldFolder.equals(current);
    }

    public static void loadWorld(String worldFolder)
    {
        class_310 client = class_310.method_1551();

        if (WorldLaunchHelper.isCurrentWorld(client, worldFolder))
        {
            return;
        }

        if (client.field_1687 != null)
        {
            client.method_18099();
        }

        class_7196 loader = client.method_41735();

        loader.method_57784(worldFolder, PendingFilmLaunch::clear);
    }
}
