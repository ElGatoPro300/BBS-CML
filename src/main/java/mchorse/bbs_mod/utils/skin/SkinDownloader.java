package mchorse.bbs_mod.utils.skin;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SkinDownloader
{
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String TMP_FOLDER_NAME = "tmp_skins";


    public File downloadSkin(String skinUrl, String playerName) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) new URL(skinUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);

        File tempFile = new File(getTmpFolder(), playerName + ".png");
        Files.copy(connection.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    private File getTmpFolder()
    {
        File tmpFolder = new File(TMP_FOLDER_NAME);
        if (!tmpFolder.exists())
        {
            tmpFolder.mkdirs();
        }
        return tmpFolder;
    }
}