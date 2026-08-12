package mchorse.bbs_mod.video;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.storage.DataFileStorage;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.resources.Link;

import com.mojang.logging.LogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;

public class VideoLikeManager
{
    private static final String LIKED_VIDEOS_FILE = "liked_videos.dat";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LinkedHashMap<String, LikedVideo> likedVideos = new LinkedHashMap<>();
    private DataFileStorage storage;
    private boolean loadErrorLogged = false;
    private boolean saveErrorLogged = false;

    public VideoLikeManager()
    {
        File dataDir = new File(BBSMod.getSettingsFolder().getParentFile(), "data");
        File videoDir = new File(dataDir, "video");

        if (!videoDir.exists())
        {
            videoDir.mkdirs();
        }

        File configFile = new File(videoDir, LIKED_VIDEOS_FILE);
        this.storage = new DataFileStorage(configFile);

        this.loadLikedVideos();
    }

    private void loadLikedVideos()
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
                this.likedVideos.clear();
                ListType listType = data.asList();

                for (BaseType item : listType)
                {
                    if (item.isString())
                    {
                        this.addLoadedLikedVideo(item.asString(), null);
                    }
                    else if (item.isMap())
                    {
                        MapType map = item.asMap();
                        BaseType pathEntry = map.get("path");

                        if (pathEntry != null && pathEntry.isString())
                        {
                            String path = pathEntry.asString();
                            BaseType nameEntry = map.get("name");
                            String display = nameEntry != null && nameEntry.isString() ? nameEntry.asString() : null;
                            this.addLoadedLikedVideo(path, display);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            if (!this.loadErrorLogged)
            {
                LOGGER.error("Failed to load liked videos from " + this.storage.getFile().getAbsolutePath(), e);
                this.loadErrorLogged = true;
            }
        }
    }

    private void saveLikedVideos()
    {
        try
        {
            ListType listData = new ListType();

            for (LikedVideo video : this.likedVideos.values())
            {
                MapType map = new MapType();

                map.put("path", new StringType(video.getPath()));
                map.put("name", new StringType(video.getDisplayName()));
                listData.add(map);
            }

            this.storage.write(listData);
        }
        catch (Exception e)
        {
            if (!this.saveErrorLogged)
            {
                LOGGER.error("Failed to save liked videos to " + this.storage.getFile().getAbsolutePath(), e);

                this.saveErrorLogged = true;
            }
        }
    }

    public void setVideoLiked(String videoName, String displayName, boolean liked)
    {
        String normalized = this.normalizePath(videoName);

        if (normalized == null)
        {
            return;
        }

        this.setVideoLikedNormalized(normalized, displayName, liked);
    }

    private void setVideoLikedNormalized(String normalized, String displayName, boolean liked)
    {
        if (displayName == null)
        {
            displayName = this.getDefaultDisplayName(normalized);
        }

        boolean changed;

        if (liked)
        {
            LikedVideo previous = this.likedVideos.put(normalized, new LikedVideo(normalized, displayName));
            changed = previous == null || !previous.getDisplayName().equals(displayName);
        }
        else
        {
            changed = this.likedVideos.remove(normalized) != null;
        }

        if (changed)
        {
            this.saveLikedVideos();
        }
    }

    public boolean isVideoLiked(String videoName)
    {
        String normalized = this.normalizePath(videoName);

        return normalized != null && this.likedVideos.containsKey(normalized);
    }

    public boolean toggleVideoLiked(String videoName)
    {
        return this.toggleVideoLiked(videoName, null);
    }

    public boolean toggleVideoLiked(String videoName, String displayName)
    {
        String normalized = this.normalizePath(videoName);

        if (normalized == null)
        {
            return false;
        }

        boolean liked = !this.likedVideos.containsKey(normalized);
        this.setVideoLikedNormalized(normalized, displayName, liked);
        LOGGER.info("Toggled liked status for video: " + normalized + " to " + (liked ? "liked" : "unliked"));

        return liked;
    }

    public String getDisplayName(String videoName)
    {
        String normalized = this.normalizePath(videoName);

        if (normalized == null)
        {
            return null;
        }

        LikedVideo video = this.likedVideos.get(normalized);

        return video != null ? video.getDisplayName() : null;
    }

    public List<LikedVideo> getLikedVideos()
    {
        return new ArrayList<>(this.likedVideos.values());
    }

    public void removeVideo(String videoName)
    {
        String normalized = this.normalizePath(videoName);

        if (normalized == null)
        {
            return;
        }

        if (this.likedVideos.remove(normalized) != null)
        {
            this.saveLikedVideos();
            LOGGER.info("Removed video: " + normalized);
        }
    }

    private String normalizePath(String videoName)
    {
        if (videoName == null || videoName.isEmpty())
        {
            return null;
        }

        return Link.create(videoName).toString();
    }

    private String getDefaultDisplayName(String videoName)
    {
        Link link = Link.create(videoName);

        if (link.path == null || link.path.isEmpty())
        {
            return link.toString();
        }

        return link.path;
    }

    private void addLoadedLikedVideo(String videoPath, String displayName)
    {
        String normalized = this.normalizePath(videoPath);

        if (normalized == null)
        {
            return;
        }

        if (displayName == null)
        {
            displayName = this.getDefaultDisplayName(normalized);
        }

        this.likedVideos.put(normalized, new LikedVideo(normalized, displayName));
    }

    public static class LikedVideo
    {
        private final String path;
        private final String displayName;

        public LikedVideo(String path, String displayName)
        {
            this.path = path;
            this.displayName = displayName;
        }

        public String getPath()
        {
            return this.path;
        }

        public String getDisplayName()
        {
            return this.displayName;
        }
    }
}

