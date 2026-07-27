package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.utils.ShadowSettings;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;

import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Replay extends ValueGroup
{
    public final ValueForm form = new ValueForm("form");
    public final ReplayKeyframes keyframes = new ReplayKeyframes("keyframes");
    public final FormProperties properties = new FormProperties("properties");
    public final Clips actions = new Clips("actions", BBSMod.getFactoryActionClips());
    public final Inventory inventory = new Inventory("inventory");

    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueString label = new ValueString("label", "");
    public final ValueString nameTag = new ValueString("name_tag", "");
    public final ValueString group = new ValueString("group", "");
    public final ValueBoolean shadow = new ValueBoolean("shadow", true);
    public final ValueFloat shadowSize = new ValueFloat("shadow_size", 0.5F);
    public final ValueFloat shadowSizeZ = new ValueFloat("shadow_size_z", 0.5F);
    public final ValueFloat shadowOpacity = new ValueFloat("shadow_opacity", 1F, 0F, 1F);
    public final ValueFloat shadowOffsetX = new ValueFloat("shadow_offset_x", 0F);
    public final ValueFloat shadowOffsetY = new ValueFloat("shadow_offset_y", 0F);
    public final ValueFloat shadowOffsetZ = new ValueFloat("shadow_offset_z", 0F);
    public final ValueInt looping = new ValueInt("looping", 0);

    public final ValueBoolean actor = new ValueBoolean("actor", false);
    public final ValueBoolean fp = new ValueBoolean("fp", false);
    public final ValueBoolean vanillaMobPlayback = new ValueBoolean("vanilla_mob_playback", false);
    public final ValueBoolean relative = new ValueBoolean("relative", false);
    public final ValuePoint relativeOffset = new ValuePoint("relativeOffset", new Point(0, 0, 0));

    public transient boolean vanillaMobPlaybackSerialized = false;

    private final Map<String, String> customSheetTitles = new HashMap<>();
    private final Map<String, Integer> sheetColors = new HashMap<>();
    public final ValueBoolean axesPreview = new ValueBoolean("axes_preview", false);
    public final ValueString axesPreviewBone = new ValueString("axes_preview_bone", "");
    public final ValueBoolean isGroup = new ValueBoolean("is_group", false);
    public final ValueString uuid = new ValueString("uuid", "");

    /* Ordered Model timeline tracks for Minecut sparse sheets (WORLD is separate). */
    private final List<String> modelTrackOrder = new ArrayList<>();
    private boolean modelTrackOrderExplicit;

    /* Item drop velocity configuration */
    public final ValueBoolean dropItemsOnDeath = new ValueBoolean("drop_items_on_death", false);
    public final ValueFloat dropVelocityMinX = new ValueFloat("drop_velocity_min_x", -0.1F);
    public final ValueFloat dropVelocityMaxX = new ValueFloat("drop_velocity_max_x", 0.1F);
    public final ValueFloat dropVelocityMinY = new ValueFloat("drop_velocity_min_y", 0.1F);
    public final ValueFloat dropVelocityMaxY = new ValueFloat("drop_velocity_max_y", 0.25F);
    public final ValueFloat dropVelocityMinZ = new ValueFloat("drop_velocity_min_z", -0.1F);
    public final ValueFloat dropVelocityMaxZ = new ValueFloat("drop_velocity_max_z", 0.1F);

    public Replay(String id)
    {
        super(id);

        this.add(this.form);
        this.add(this.keyframes);
        this.add(this.properties);
        this.add(this.actions);
        this.add(this.inventory);

        this.add(this.enabled);
        this.add(this.label);
        this.add(this.nameTag);
        this.add(this.group);
        this.add(this.shadow);
        this.add(this.shadowSize);
        this.add(this.shadowSizeZ);
        this.add(this.shadowOpacity);
        this.add(this.shadowOffsetX);
        this.add(this.shadowOffsetY);
        this.add(this.shadowOffsetZ);
        this.add(this.looping);

        this.add(this.actor);
        this.add(this.fp);
        this.add(this.vanillaMobPlayback);
        this.add(this.relative);
        this.add(this.relativeOffset);

        this.add(this.axesPreview);
        this.add(this.axesPreviewBone);
        this.add(this.dropItemsOnDeath);
        this.add(this.dropVelocityMinX);
        this.add(this.dropVelocityMaxX);
        this.add(this.dropVelocityMinY);
        this.add(this.dropVelocityMaxY);
        this.add(this.dropVelocityMinZ);
        this.add(this.dropVelocityMaxZ);
        this.add(this.isGroup);
        this.add(this.uuid);
        
        this.uuid.set(UUID.randomUUID().toString());
    }

    public String getName()
    {
        if (this.isGroup.get())
        {
            return this.label.get().isEmpty() ? "New Group" : this.label.get();
        }

        String label = this.label.get();

        if (!label.isEmpty())
        {
            return label;
        }

        Form form = this.form.get();

        if (form == null)
        {
            return "-";
        }

        return form.getDisplayName();
    }

    public void shift(float tick)
    {
        this.keyframes.shift(tick);
        this.properties.shift(tick);
        this.actions.shift(tick);
    }

    public void applyActions(LivingEntity actor, SuperFakePlayer fakePlayer, Film film, int tick)
    {
        List<Clip> clips = this.actions.getClips(tick);

        for (Clip clip : clips)
        {
            ((ActionClip) clip).apply(actor, fakePlayer, film, this, tick);
        }
    }

    public void applyClientActions(int tick, IEntity entity, Film film)
    {
        tick = this.getTick(tick);

        List<Clip> clips = this.actions.getClips(tick);

        for (Clip clip : clips)
        {
            if (clip instanceof ActionClip actionClip && actionClip.isClient())
            {
                actionClip.applyClient(entity, film, this, tick);
            }
        }
    }

    public int getTick(int tick)
    {
        return this.looping.get() > 0 ? tick % this.looping.get() : tick;
    }

    public String getCustomSheetTitle(String id)
    {
        return this.customSheetTitles.get(id);
    }

    public void setCustomSheetTitle(String id, String title)
    {
        if (title == null || title.isBlank())
        {
            this.customSheetTitles.remove(id);
        }
        else
        {
            this.customSheetTitles.put(id, title);
        }
    }

    public Integer getSheetColor(String id)
    {
        return this.sheetColors.get(id);
    }

    public void setSheetColor(String id, Integer color)
    {
        if (color == null)
        {
            this.sheetColors.remove(id);
        }
        else
        {
            this.sheetColors.put(id, color);
        }
    }

    public boolean hasExplicitModelTrackOrder()
    {
        return this.modelTrackOrderExplicit;
    }

    public List<String> getModelTrackOrder()
    {
        return this.modelTrackOrder;
    }

    /**
     * Seed or migrate Model track order. Minecut sparse sheets call this before building.
     */
    public void ensureModelTrackOrder()
    {
        if (this.modelTrackOrderExplicit)
        {
            return;
        }

        this.modelTrackOrder.clear();
        this.modelTrackOrder.addAll(ModelTrackIds.migrateOrderFromProperties(this));
        this.modelTrackOrderExplicit = true;
    }

    public void seedDefaultModelTrackOrder()
    {
        this.modelTrackOrder.clear();
        this.modelTrackOrder.addAll(ModelTrackIds.buildDefaultsFromSettings());
        this.modelTrackOrderExplicit = true;
    }

    /**
     * Drop sparse / custom Model track order so the classic editor shows the full
     * auto-discovered property track list with default grouping again.
     */
    public void clearModelTrackOrder()
    {
        this.modelTrackOrder.clear();
        this.modelTrackOrderExplicit = false;
    }

    public void setModelTrackOrder(List<String> order)
    {
        this.modelTrackOrder.clear();

        if (order != null)
        {
            this.modelTrackOrder.addAll(order);
        }

        this.modelTrackOrderExplicit = true;
    }

    /**
     * Resolve a palette type into a concrete track id and insert it at {@code index}
     * ({@code < 0} = logical default position). Skips if that track is already present.
     *
     * @return the track id that was inserted, or {@code null} if skipped
     */
    public String insertModelTrackFromPalette(String paletteType, int index)
    {
        if (paletteType == null)
        {
            return null;
        }

        this.ensureModelTrackOrder();

        String trackId = ModelTrackIds.resolveFromPalette(this.modelTrackOrder, paletteType);

        if (trackId == null)
        {
            return null;
        }

        Form form = this.form.get();

        if (form != null)
        {
            ModelTrackIds.ensureChannel(this, form, trackId);
        }

        int at = index;

        if (at < 0)
        {
            at = ModelTrackIds.defaultInsertIndex(this.modelTrackOrder, trackId);
        }

        if (at > this.modelTrackOrder.size())
        {
            at = this.modelTrackOrder.size();
        }

        this.modelTrackOrder.add(at, trackId);
        this.modelTrackOrderExplicit = true;

        return trackId;
    }

    /**
     * Move a Model track in the sparse order. {@code beforeTrackId == null} appends at the end.
     *
     * @return {@code true} if the order changed
     */
    public boolean moveModelTrackBefore(String trackId, String beforeTrackId)
    {
        if (trackId == null || trackId.isEmpty())
        {
            return false;
        }

        this.ensureModelTrackOrder();

        if (!this.modelTrackOrder.contains(trackId))
        {
            return false;
        }

        if (trackId.equals(beforeTrackId))
        {
            return false;
        }

        int from = this.modelTrackOrder.indexOf(trackId);

        this.modelTrackOrder.remove(from);

        if (beforeTrackId == null)
        {
            this.modelTrackOrder.add(trackId);
        }
        else
        {
            int to = this.modelTrackOrder.indexOf(beforeTrackId);

            if (to < 0)
            {
                this.modelTrackOrder.add(trackId);
            }
            else
            {
                this.modelTrackOrder.add(to, trackId);
            }
        }

        this.modelTrackOrderExplicit = true;

        return true;
    }

    /**
     * Remove a Model timeline track from the Minecut sparse order (and its overlay family siblings are left alone).
     *
     * @return {@code true} if something was removed
     */
    public boolean removeModelTrack(String trackId)
    {
        if (trackId == null || trackId.isEmpty())
        {
            return false;
        }

        this.ensureModelTrackOrder();

        if (!this.modelTrackOrder.remove(trackId))
        {
            return false;
        }

        this.modelTrackOrderExplicit = true;

        return true;
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        super.copy(group);

        if (group instanceof Replay other)
        {
            this.customSheetTitles.clear();
            this.customSheetTitles.putAll(other.customSheetTitles);
            this.sheetColors.clear();
            this.sheetColors.putAll(other.sheetColors);
            this.vanillaMobPlaybackSerialized = other.vanillaMobPlaybackSerialized;
            this.modelTrackOrder.clear();
            this.modelTrackOrder.addAll(other.modelTrackOrder);
            this.modelTrackOrderExplicit = other.modelTrackOrderExplicit;
        }
    }

    @Override
    public BaseType toData()
    {
        MapType map = (MapType) super.toData();

        if (!this.customSheetTitles.isEmpty())
        {
            MapType titles = new MapType();

            for (Map.Entry<String, String> entry : this.customSheetTitles.entrySet())
            {
                titles.put(entry.getKey(), new StringType(entry.getValue()));
            }

            map.put("custom_sheet_titles", titles);
        }

        if (!this.sheetColors.isEmpty())
        {
            MapType colors = new MapType();

            for (Map.Entry<String, Integer> entry : this.sheetColors.entrySet())
            {
                colors.put(entry.getKey(), new IntType(entry.getValue()));
            }

            map.put("sheet_colors", colors);
        }

        if (this.modelTrackOrderExplicit)
        {
            ListType tracks = new ListType();

            for (String id : this.modelTrackOrder)
            {
                tracks.add(new StringType(id));
            }

            map.put("model_track_order", tracks);
        }

        return map;
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        if (data instanceof MapType map)
        {
            this.vanillaMobPlaybackSerialized = map.has("vanilla_mob_playback");

            BaseType titlesType = map.get("custom_sheet_titles");

            if (titlesType instanceof MapType titles)
            {
                for (String key : titles.keys())
                {
                    BaseType value = titles.get(key);

                    if (value != null && value.isString())
                    {
                        this.customSheetTitles.put(key, value.asString());
                    }
                }
            }

            BaseType colorsType = map.get("sheet_colors");

            if (colorsType instanceof MapType colors)
            {
                for (String key : colors.keys())
                {
                    BaseType value = colors.get(key);

                    if (value != null && value.isNumeric())
                    {
                        this.sheetColors.put(key, value.asNumeric().intValue());
                    }
                }
            }

            this.modelTrackOrder.clear();
            this.modelTrackOrderExplicit = false;

            BaseType tracksType = map.get("model_track_order");

            if (tracksType instanceof ListType tracks)
            {
                for (BaseType entry : tracks)
                {
                    if (entry != null && entry.isString())
                    {
                        this.modelTrackOrder.add(entry.asString());
                    }
                }

                this.modelTrackOrderExplicit = true;
            }
        }

        /* Pre-XZ-shadow films only had shadow_size — mirror into Z so shape stays circular. */
        boolean migratedShadowZ = false;

        if (data instanceof MapType map && !map.has("shadow_size_z") && map.has("shadow_size"))
        {
            this.shadowSizeZ.set(this.shadowSize.get());
            migratedShadowZ = true;
        }

        this.ensureShadowKeyframes();
        this.applyVanillaPlaybackDefaults();
    }

    private void applyVanillaPlaybackDefaults()
    {}

    private void ensureShadowKeyframes()
    {
        if (this.keyframes.shadowSize.isEmpty())
        {
            ShadowSettings settings = new ShadowSettings(1F, this.shadowSize.get(), this.shadowSizeZ.get());

            settings.offsetX = this.shadowOffsetX.get();
            settings.offsetY = this.shadowOffsetY.get();
            settings.offsetZ = this.shadowOffsetZ.get();

            this.keyframes.shadowSize.insert(0, settings);
        }

        if (this.keyframes.shadowOpacity.isEmpty())
        {
            this.keyframes.shadowOpacity.insert(0, (double) this.shadowOpacity.get());
        }
    }
}
