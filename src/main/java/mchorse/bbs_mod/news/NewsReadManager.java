package mchorse.bbs_mod.news;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.storage.DataFileStorage;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.StringType;

import com.mojang.logging.LogUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

public class NewsReadManager
{
    private static final String READ_NEWS_FILE = "read_news_ids.dat";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Set<String> readIds = new HashSet<>();
    private final DataFileStorage storage;

    public NewsReadManager()
    {
        File dataDir = new File(BBSMod.getSettingsFolder().getParentFile(), "data");
        File newsDir = new File(dataDir, "news");

        if (!newsDir.exists())
        {
            newsDir.mkdirs();
        }

        File configFile = new File(newsDir, READ_NEWS_FILE);
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

            if (data != null && data.isList())
            {
                this.readIds.clear();

                ListType list = data.asList();

                for (BaseType item : list)
                {
                    if (item.isString())
                    {
                        this.readIds.add(item.asString());
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to load read news IDs from " + this.storage.getFile().getAbsolutePath(), e);
        }
    }

    private void save()
    {
        try
        {
            ListType list = new ListType();

            for (String id : this.readIds)
            {
                list.add(new StringType(id));
            }

            this.storage.write(list);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to save read news IDs to " + this.storage.getFile().getAbsolutePath(), e);
        }
    }

    public boolean isRead(String id)
    {
        return id != null && this.readIds.contains(id);
    }

    public void markRead(String id)
    {
        if (id == null)
        {
            return;
        }

        if (this.readIds.add(id))
        {
            this.save();
        }
    }
}

