package mchorse.bbs_mod.forms;

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

/**
 * Manages structure files' like/unlike status
 */
public class StructureLikeManager
{
    private static final String LIKED_STRUCTURES_FILE = "liked_structures.dat";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LinkedHashMap<String, LikedStructure> likedStructures = new LinkedHashMap<>();
    private DataFileStorage storage;
    private boolean loadErrorLogged = false;
    private boolean saveErrorLogged = false;
    
    public StructureLikeManager()
    {
        File dataDir = new File(BBSMod.getSettingsFolder().getParentFile(), "data");
        File structureDir = new File(dataDir, "structures");
        
        if (!structureDir.exists())
        {
            structureDir.mkdirs();
        }

        File configFile = new File(structureDir, LIKED_STRUCTURES_FILE);
        this.storage = new DataFileStorage(configFile);

        this.loadLikedStructures();
    }
    
    private void loadLikedStructures()
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
                this.likedStructures.clear();
                ListType listType = data.asList();

                for (BaseType item : listType)
                {
                    if (item.isString())
                    {
                        this.addLoadedLikedStructure(item.asString(), null);
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
                            
                            this.addLoadedLikedStructure(path, display);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            if (!this.loadErrorLogged)
            {
                LOGGER.error("Failed to load liked structures from " + this.storage.getFile().getAbsolutePath(), e);
                this.loadErrorLogged = true;
            }
        }
    }
    
    private void saveLikedStructures()
    {
        try
        {
            ListType listData = new ListType();

            for (LikedStructure structure : this.likedStructures.values())
            {
                MapType map = new MapType();

                map.put("path", new StringType(structure.getPath()));
                map.put("name", new StringType(structure.getDisplayName()));
                listData.add(map);
            }

            this.storage.write(listData);
        }
        catch (Exception e)
        {
            if (!this.saveErrorLogged)
            {
                LOGGER.error("Failed to save liked structures to " + this.storage.getFile().getAbsolutePath(), e);

                this.saveErrorLogged = true;
            }
        }
    }

    public void setStructureLiked(String structureName, String displayName, boolean liked)
    {
        String normalized = this.normalizePath(structureName);

        if (normalized == null)
        {
            return;
        }

        this.setStructureLikedNormalized(normalized, displayName, liked);
    }

    private void setStructureLikedNormalized(String normalized, String displayName, boolean liked)
    {
        if (displayName == null)
        {
            displayName = this.getDefaultDisplayName(normalized);
        }

        boolean changed;

        if (liked)
        {
            LikedStructure previous = this.likedStructures.put(normalized, new LikedStructure(normalized, displayName));
            changed = previous == null || !previous.getDisplayName().equals(displayName);
        }
        else
        {
            changed = this.likedStructures.remove(normalized) != null;
        }

        if (changed)
        {
            this.saveLikedStructures();
        }
    }
    
    public boolean isStructureLiked(String structureName)
    {
        String normalized = this.normalizePath(structureName);

        return normalized != null && this.likedStructures.containsKey(normalized);
    }
    
    public boolean toggleStructureLiked(String structureName)
    {
        return this.toggleStructureLiked(structureName, null);
    }

    public boolean toggleStructureLiked(String structureName, String displayName)
    {
        String normalized = this.normalizePath(structureName);

        if (normalized == null)
        {
            return false;
        }

        boolean liked = !this.likedStructures.containsKey(normalized);
        this.setStructureLikedNormalized(normalized, displayName, liked);
        LOGGER.info("Toggled liked status for structure: " + normalized + " to " + (liked ? "liked" : "unliked"));

        return liked;
    }
    
    public String getDisplayName(String structureName)
    {
        String normalized = this.normalizePath(structureName);

        if (normalized == null)
        {
            return null;
        }

        LikedStructure structure = this.likedStructures.get(normalized);

        return structure != null ? structure.getDisplayName() : null;
    }

    public List<LikedStructure> getLikedStructures()
    {
        return new ArrayList<>(this.likedStructures.values());
    }
    
    public void removeStructure(String structureName)
    {
        String normalized = this.normalizePath(structureName);

        if (normalized == null)
        {
            return;
        }

        if (this.likedStructures.remove(normalized) != null)
        {
            this.saveLikedStructures();
            LOGGER.info("Removed structure: " + normalized);
        }
    }

    private String normalizePath(String structureName)
    {
        if (structureName == null || structureName.isEmpty())
        {
            return null;
        }

        return Link.create(structureName).toString();
    }

    private String getDefaultDisplayName(String structureName)
    {
        Link link = Link.create(structureName);

        if (link.path == null || link.path.isEmpty())
        {
            return link.toString();
        }

        return link.path;
    }

    private void addLoadedLikedStructure(String structurePath, String displayName)
    {
        String normalized = this.normalizePath(structurePath);

        if (normalized == null)
        {
            return;
        }

        if (displayName == null)
        {
            displayName = this.getDefaultDisplayName(normalized);
        }

        this.likedStructures.put(normalized, new LikedStructure(normalized, displayName));
    }

    public static class LikedStructure
    {
        private final String path;
        private final String displayName;

        public LikedStructure(String path, String displayName)
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
