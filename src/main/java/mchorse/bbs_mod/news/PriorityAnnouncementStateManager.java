package mchorse.bbs_mod.news;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.storage.DataFileStorage;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.StringType;

import com.mojang.logging.LogUtils;

import java.io.File;

import org.slf4j.Logger;

public class PriorityAnnouncementStateManager
{
    private static final String LAST_SEEN_FILE = "priority_announcement_last_seen_id.dat";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final DataFileStorage storage;
    private String lastSeenId = "";

    public PriorityAnnouncementStateManager()
    {
        File dataDir = new File(BBSMod.getSettingsFolder().getParentFile(), "data");
        File newsDir = new File(dataDir, "news");

        if (!newsDir.exists())
        {
            newsDir.mkdirs();
        }

        File configFile = new File(newsDir, LAST_SEEN_FILE);
        this.storage = new DataFileStorage(configFile);

        this.load();
    }

    private void load()
    {
        if (!this.storage.getFile().exists())
        {
            return;
        }

        try
        {
            BaseType data = this.storage.read();

            if (data != null && data.isString())
            {
                this.lastSeenId = data.asString();
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to load priority announcement state from " + this.storage.getFile().getAbsolutePath(), e);
        }
    }

    private void save()
    {
        try
        {
            this.storage.write(new StringType(this.lastSeenId == null ? "" : this.lastSeenId));
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to save priority announcement state to " + this.storage.getFile().getAbsolutePath(), e);
        }
    }

    public boolean shouldShow(String id)
    {
        return id != null && !id.isEmpty() && !id.equals(this.lastSeenId);
    }

    public void markShown(String id)
    {
        if (id == null || id.isEmpty() || id.equals(this.lastSeenId))
        {
            return;
        }

        this.lastSeenId = id;
        this.save();
    }
}
