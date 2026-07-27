package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared ids / allocation for Minecut Model timeline track palette drops.
 * WORLD channels are intentionally excluded.
 */
public final class ModelTrackIds
{
    public static final String VISIBLE = "visible";
    public static final String RENDER = "render";
    public static final String LIGHTING = "lighting";
    public static final String RENDER_DEPTH = "render_depth";
    public static final String TRANSFORM = "transform";
    public static final String TRANSFORM_OVERLAY = "transform_overlay";
    public static final String POSE = "pose";
    public static final String POSE_OVERLAY = "pose_overlay";
    public static final String ANCHOR = "anchor";
    public static final String LOOK_AT = "look_at";
    public static final String INVERSE_KINEMATICS = "inverse_kinematics";
    public static final String ILLUSION = "illusion";
    public static final String ILLUSION_OVERLAY = "illusion_overlay";
    public static final String COLOR = "color";
    public static final String COLOR_OVERLAY = "color_overlay";
    public static final String OPACITY = "opacity";
    public static final String GLOW = "glow";
    public static final String TEXTURE = "texture";
    public static final String PAINT = "paint_color";

    /** Palette row types that allocate a new overlay instance on each drop. */
    public static final Set<String> STACKABLE_PALETTE_TYPES = Set.of(
        TRANSFORM_OVERLAY, POSE_OVERLAY, COLOR_OVERLAY, ILLUSION_OVERLAY
    );

    /**
     * Base palette ids that auto-stack: first drop = base track, later drops = overlays.
     * These rows stay visible in the Tracks menu so overlays can be added again.
     */
    public static final Set<String> AUTO_OVERLAY_BASES = Set.of(
        TRANSFORM, POSE, COLOR, ILLUSION
    );

    /** Singleton palette rows — only one line per replay. */
    public static final List<String> SINGLETON_PALETTE_TYPES = Collections.unmodifiableList(Arrays.asList(
        VISIBLE, TRANSFORM, POSE, LIGHTING, RENDER_DEPTH, ANCHOR, LOOK_AT, INVERSE_KINEMATICS,
        ILLUSION, COLOR, OPACITY, GLOW, TEXTURE, PAINT
    ));

    private ModelTrackIds()
    {
    }

    /** Overlay family base for a palette/base id, or {@code null} if not auto-stackable. */
    public static String overlayFamilyOf(String paletteType)
    {
        if (paletteType == null)
        {
            return null;
        }

        if (paletteType.equals(POSE) || paletteType.equals(POSE_OVERLAY) || paletteType.startsWith(POSE_OVERLAY))
        {
            return POSE_OVERLAY;
        }

        if (paletteType.equals(TRANSFORM) || paletteType.equals(TRANSFORM_OVERLAY) || paletteType.startsWith(TRANSFORM_OVERLAY))
        {
            return TRANSFORM_OVERLAY;
        }

        if (paletteType.equals(COLOR) || paletteType.equals(COLOR_OVERLAY) || paletteType.startsWith(COLOR_OVERLAY))
        {
            return COLOR_OVERLAY;
        }

        if (paletteType.equals(ILLUSION) || paletteType.equals(ILLUSION_OVERLAY) || paletteType.startsWith(ILLUSION_OVERLAY))
        {
            return ILLUSION_OVERLAY;
        }

        return null;
    }

    /**
     * Resolve a Tracks-palette id into the concrete channel to insert.
     * Auto-overlay bases stack into overlays when the base is already present;
     * true singletons return {@code null} when already present.
     */
    public static String resolveFromPalette(List<String> order, String paletteType)
    {
        if (paletteType == null)
        {
            return null;
        }

        if (isStackablePaletteType(paletteType))
        {
            return allocateNextOverlayId(order, paletteType);
        }

        if (AUTO_OVERLAY_BASES.contains(paletteType) && isPaletteTypeAlreadyPresent(order, paletteType))
        {
            String overlayBase = overlayFamilyOf(paletteType);

            return overlayBase == null ? null : allocateNextOverlayId(order, overlayBase);
        }

        if (isPaletteTypeAlreadyPresent(order, paletteType))
        {
            return null;
        }

        return paletteType;
    }

    /**
     * True when the selected replay already has this palette row's base track
     * (further drops would only create overlays / duplicates).
     */
    public static boolean isPaletteTypeAlreadyPresent(List<String> order, String paletteType)
    {
        if (order == null || paletteType == null)
        {
            return false;
        }

        return order.contains(paletteType);
    }

    /** True when the form exposes this property (Pose only on models, Color only if registered, …). */
    public static boolean isApplicableToForm(Form form, String trackId)
    {
        if (form == null || trackId == null || trackId.isEmpty())
        {
            return false;
        }

        return FormUtils.getProperty(form, trackId) != null;
    }

    /**
     * Palette row is shown when the form supports it. True singletons hide once
     * present; Pose / Transform / Color / Illusion stay visible for overlays.
     */
    public static boolean canAddFromPalette(Replay replay, String paletteType)
    {
        if (replay == null || paletteType == null || replay.isGroup.get())
        {
            return false;
        }

        Form form = replay.form.get();

        if (!isApplicableToForm(form, paletteType))
        {
            return false;
        }

        if (AUTO_OVERLAY_BASES.contains(paletteType) || isStackablePaletteType(paletteType))
        {
            return true;
        }

        replay.ensureModelTrackOrder();

        return !isPaletteTypeAlreadyPresent(replay.getModelTrackOrder(), paletteType);
    }

    /** True for transform_overlay / pose_overlayN / color_overlay / … (not the base track). */
    public static boolean isOverlayTrackId(String trackId)
    {
        if (trackId == null || trackId.isEmpty())
        {
            return false;
        }

        String overlayBase = overlayFamilyOf(trackId);

        if (overlayBase == null)
        {
            return false;
        }

        return trackId.equals(overlayBase) || trackId.startsWith(overlayBase);
    }

    /**
     * Logical default insert index for a click-add (near related family / MODEL property order).
     */
    public static int defaultInsertIndex(List<String> order, String trackId)
    {
        if (order == null || trackId == null)
        {
            return 0;
        }

        String overlayBase = overlayFamilyOf(trackId);

        if (overlayBase != null && (trackId.equals(overlayBase) || trackId.startsWith(overlayBase)))
        {
            String base = overlayBase.equals(POSE_OVERLAY) ? POSE
                : overlayBase.equals(TRANSFORM_OVERLAY) ? TRANSFORM
                : overlayBase.equals(COLOR_OVERLAY) ? COLOR
                : ILLUSION;
            int last = -1;

            for (int i = 0; i < order.size(); i++)
            {
                String id = order.get(i);

                if (id.equals(base) || id.equals(overlayBase) || id.startsWith(overlayBase))
                {
                    last = i;
                }
            }

            return last < 0 ? order.size() : last + 1;
        }

        /* Prefer MODEL_PROPERTIES-like order: visible → transform → pose → rest */
        List<String> preferred = Arrays.asList(VISIBLE, LIGHTING, RENDER_DEPTH, TRANSFORM, POSE, COLOR, OPACITY, GLOW, TEXTURE, ANCHOR, LOOK_AT, INVERSE_KINEMATICS, ILLUSION);
        int pref = preferred.indexOf(trackId);

        if (pref < 0)
        {
            return order.size();
        }

        for (int i = 0; i < order.size(); i++)
        {
            int other = preferred.indexOf(order.get(i));

            if (other > pref || (other < 0 && pref >= 0))
            {
                /* insert before first track that should come after us; skip unknown */
                if (other > pref)
                {
                    return i;
                }
            }
        }

        return order.size();
    }

    public static List<String> buildDefaultsFromSettings()
    {
        LinkedHashSet<String> order = new LinkedHashSet<>();

        /* Transform above Pose — matches classic MODEL property order. */
        if (BBSSettings.minecutDefaultTrackTransform != null && BBSSettings.minecutDefaultTrackTransform.get())
        {
            order.add(TRANSFORM);
        }

        if (BBSSettings.minecutDefaultTrackPose != null && BBSSettings.minecutDefaultTrackPose.get())
        {
            order.add(POSE);
        }

        if (BBSSettings.minecutDefaultTrackVisible != null && BBSSettings.minecutDefaultTrackVisible.get())
        {
            order.add(VISIBLE);
        }

        if (BBSSettings.minecutDefaultTrackColor != null && BBSSettings.minecutDefaultTrackColor.get())
        {
            order.add(COLOR);
        }

        if (BBSSettings.minecutDefaultTrackOpacity != null && BBSSettings.minecutDefaultTrackOpacity.get())
        {
            order.add(OPACITY);
        }

        if (order.isEmpty())
        {
            order.add(TRANSFORM);
            order.add(POSE);
        }

        int transformOverlays = BBSSettings.minecutDefaultTransformOverlays != null
            ? BBSSettings.minecutDefaultTransformOverlays.get() : 0;
        int poseOverlays = BBSSettings.minecutDefaultPoseOverlays != null
            ? BBSSettings.minecutDefaultPoseOverlays.get() : 0;
        int colorOverlays = BBSSettings.minecutDefaultColorOverlays != null
            ? BBSSettings.minecutDefaultColorOverlays.get() : 0;

        for (int i = 0; i < transformOverlays; i++)
        {
            order.add(overlayId(TRANSFORM_OVERLAY, i));
        }

        for (int i = 0; i < poseOverlays; i++)
        {
            order.add(overlayId(POSE_OVERLAY, i));
        }

        for (int i = 0; i < colorOverlays; i++)
        {
            order.add(overlayId(COLOR_OVERLAY, i));
        }

        return new ArrayList<>(order);
    }

    /**
     * Overlay instance ids: first is bare {@code base}, then {@code base0}, {@code base1}, …
     */
    public static String overlayId(String base, int instanceIndex)
    {
        if (instanceIndex <= 0)
        {
            return base;
        }

        return base + (instanceIndex - 1);
    }

    public static int overlayInstanceIndex(String trackId, String base)
    {
        if (trackId == null || base == null)
        {
            return -1;
        }

        if (trackId.equals(base))
        {
            return 0;
        }

        if (!trackId.startsWith(base))
        {
            return -1;
        }

        String suffix = trackId.substring(base.length());

        if (suffix.isEmpty())
        {
            return 0;
        }

        try
        {
            return Integer.parseInt(suffix) + 1;
        }
        catch (NumberFormatException ignored)
        {
            return -1;
        }
    }

    /** Numbered form field index: bare → -1, {@code base0} → 0, … */
    public static int overlayNumberedIndex(String trackId, String base)
    {
        int instance = overlayInstanceIndex(trackId, base);

        if (instance < 0)
        {
            return -2;
        }

        return instance - 1;
    }

    public static String allocateNextOverlayId(List<String> order, String base)
    {
        if (order == null || !order.contains(base))
        {
            return base;
        }

        for (int i = 0; ; i++)
        {
            String id = base + i;

            if (!order.contains(id))
            {
                return id;
            }
        }
    }

    public static boolean isStackablePaletteType(String paletteType)
    {
        return paletteType != null && STACKABLE_PALETTE_TYPES.contains(paletteType);
    }

    public static boolean isModelTrackId(String id)
    {
        if (id == null || id.isEmpty())
        {
            return false;
        }

        if (id.contains(":"))
        {
            String root = id.substring(0, id.indexOf(':'));

            return root.startsWith("pose");
        }

        String lower = id.toLowerCase(Locale.ROOT);

        return lower.equals(VISIBLE) || lower.equals(RENDER) || lower.equals(LIGHTING)
            || lower.equals(RENDER_DEPTH) || lower.equals(TRANSFORM) || lower.startsWith(TRANSFORM_OVERLAY)
            || lower.equals(POSE) || lower.startsWith(POSE_OVERLAY)
            || lower.equals(ANCHOR) || lower.equals(LOOK_AT) || lower.equals(INVERSE_KINEMATICS)
            || lower.equals(ILLUSION) || lower.startsWith(ILLUSION_OVERLAY)
            || lower.equals(COLOR) || lower.startsWith(COLOR_OVERLAY)
            || lower.equals(OPACITY) || lower.equals(GLOW) || lower.equals(TEXTURE)
            || lower.equals(PAINT) || lower.equals("paint") || lower.equals("glow_settings")
            || lower.startsWith("illusion_transform");
    }

    public static boolean isLimbChildOfOrdered(String path, List<String> order)
    {
        if (path == null || !path.contains(":") || order == null)
        {
            return false;
        }

        String root = path.substring(0, path.indexOf(':'));

        if (order.contains(root))
        {
            return true;
        }

        /* Nested form paths: "0/pose:head" when order has "pose". */
        int slash = root.lastIndexOf('/');

        if (slash >= 0)
        {
            String leaf = root.substring(slash + 1);

            return order.contains(leaf);
        }

        return false;
    }

    /** PBR intensity tracks that nest under Texture in the timeline. */
    public static boolean isTextureChildTrack(String path)
    {
        if (path == null || path.isEmpty())
        {
            return false;
        }

        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;

        return name.equals("pbr_normal_intensity") || name.equals("pbr_specular_intensity");
    }

    /**
     * Ensure the form exposes the property for {@code trackId}, then create the keyframe channel.
     */
    public static KeyframeChannel ensureChannel(Replay replay, Form form, String trackId)
    {
        if (replay == null || form == null || trackId == null)
        {
            return null;
        }

        ensureFormProperty(form, trackId);

        return replay.properties.getOrCreate(form, trackId);
    }

    public static void ensureFormProperty(Form form, String trackId)
    {
        if (form == null || trackId == null)
        {
            return;
        }

        int transformN = overlayNumberedIndex(trackId, TRANSFORM_OVERLAY);

        if (transformN >= -1)
        {
            form.ensureTransformOverlay(transformN);

            return;
        }

        int colorN = overlayNumberedIndex(trackId, COLOR_OVERLAY);

        if (colorN >= -1)
        {
            form.ensureColorOverlay(colorN);

            return;
        }

        int illusionN = overlayNumberedIndex(trackId, ILLUSION_OVERLAY);

        if (illusionN >= -1)
        {
            form.ensureIllusionOverlay(illusionN);

            return;
        }

        int poseN = overlayNumberedIndex(trackId, POSE_OVERLAY);

        if (poseN >= -1 && form instanceof ModelForm modelForm)
        {
            modelForm.ensurePoseOverlay(poseN);
        }
    }

    public static List<String> migrateOrderFromProperties(Replay replay)
    {
        LinkedHashSet<String> order = new LinkedHashSet<>();

        if (replay != null && replay.properties != null)
        {
            for (String key : replay.properties.properties.keySet())
            {
                if (isModelTrackId(key) && !key.contains(":"))
                {
                    order.add(key);
                }
            }
        }

        if (order.isEmpty())
        {
            return buildDefaultsFromSettings();
        }

        return new ArrayList<>(order);
    }
}
