package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.factory.IFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clip-type groupings shared by the timeline toolbar add submenus and the
 * context-menu add tabs (camera / action).
 */
public final class TimelineClipTypeGroups
{
    public static final class ClipGroup
    {
        public final IKey label;
        public final List<Link> types;

        public ClipGroup(IKey label, List<Link> types)
        {
            this.label = label;
            this.types = types;
        }
    }

    public static List<ClipGroup> forCamera(IFactory<Clip, ClipFactoryData> factory)
    {
        Map<IKey, List<Link>> buckets = new LinkedHashMap<>();

        buckets.put(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_CAMERA,
            List.of(Link.bbs("idle"), Link.bbs("path"), Link.bbs("keyframe"), Link.bbs("dolly")));
        buckets.put(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_RESOURCE,
            List.of(Link.bbs("curve"), Link.bbs("audio"), Link.bbs("video"), Link.bbs("shake"),
                Link.bbs("translate"), Link.bbs("angle")));
        buckets.put(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_SCREEN,
            List.of(
                Link.bbs("subtitle"),
                Link.bbs("hotbar"),
                Link.bbs("image"),
                Link.bbs("color"),
                Link.bbs("cinematic"),
                Link.bbs("vignette"),
                Link.bbs("letterbox"),
                Link.bbs("grain"),
                Link.bbs("screen_node"),
                Link.bbs("boss_bar"),
                Link.bbs("eye")
            ));
        buckets.put(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_ANCHOR,
            List.of(Link.bbs("look"), Link.bbs("orbit"), Link.bbs("tracker")));

        return buildGroups(factory, buckets, UIKeys.CAMERA_TIMELINE_CLIPS_TABS_EXTRAS);
    }

    public static List<ClipGroup> forAction(IFactory<Clip, ClipFactoryData> factory)
    {
        Map<IKey, List<Link>> buckets = new LinkedHashMap<>();

        buckets.put(UIKeys.ACTION_TIMELINE_CLIPS_TABS_BLOCKS,
            List.of(Link.bbs("place_block"), Link.bbs("interact_block"), Link.bbs("break_block"),
                Link.bbs("close_container")));
        buckets.put(UIKeys.ACTION_TIMELINE_CLIPS_TABS_ITEMS,
            List.of(Link.bbs("use_item"), Link.bbs("use_block_item"), Link.bbs("drop_item")));
        buckets.put(UIKeys.ACTION_TIMELINE_CLIPS_TABS_COMBAT,
            List.of(Link.bbs("attack"), Link.bbs("damage"), Link.bbs("swipe")));
        buckets.put(UIKeys.ACTION_TIMELINE_CLIPS_TABS_OTHER,
            List.of(Link.bbs("chat"), Link.bbs("command")));

        return buildGroups(factory, buckets, UIKeys.ACTION_TIMELINE_CLIPS_TABS_OTHER);
    }

    private static List<ClipGroup> buildGroups(IFactory<Clip, ClipFactoryData> factory,
        Map<IKey, List<Link>> buckets, IKey extrasLabel)
    {
        List<ClipGroup> groups = new ArrayList<>();

        for (Map.Entry<IKey, List<Link>> entry : buckets.entrySet())
        {
            List<Link> present = new ArrayList<>();

            for (Link type : entry.getValue())
            {
                if (factory.getKeys().contains(type))
                {
                    present.add(type);
                }
            }

            if (!present.isEmpty())
            {
                groups.add(new ClipGroup(entry.getKey(), present));
            }
        }

        List<Link> extras = new ArrayList<>();

        for (Link type : factory.getKeys())
        {
            boolean grouped = false;

            for (ClipGroup group : groups)
            {
                if (group.types.contains(type))
                {
                    grouped = true;

                    break;
                }
            }

            if (!grouped)
            {
                extras.add(type);
            }
        }

        if (!extras.isEmpty())
        {
            /* Prefer merging into an existing extras/other bucket when the label matches. */
            ClipGroup existingExtras = null;

            for (ClipGroup group : groups)
            {
                if (group.label == extrasLabel)
                {
                    existingExtras = group;

                    break;
                }
            }

            if (existingExtras != null)
            {
                List<Link> merged = new ArrayList<>(existingExtras.types);

                for (Link type : extras)
                {
                    if (!merged.contains(type))
                    {
                        merged.add(type);
                    }
                }

                groups.set(groups.indexOf(existingExtras), new ClipGroup(extrasLabel, merged));
            }
            else
            {
                groups.add(new ClipGroup(extrasLabel, extras));
            }
        }

        return groups;
    }

    private TimelineClipTypeGroups()
    {}
}
