package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.HotbarClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UIHotbarClip extends UIClip<HotbarClip>
{
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    private static final Map<String, String> CHANNEL_TO_GROUP = new HashMap<>();
    static {
        CHANNEL_TO_GROUP.put("layout", "layout");
        CHANNEL_TO_GROUP.put("right_offhand", "layout");

        CHANNEL_TO_GROUP.put("selected_slot", "inventory");
        CHANNEL_TO_GROUP.put("offhand_slot", "inventory");
        CHANNEL_TO_GROUP.put("slot_0", "inventory");
        CHANNEL_TO_GROUP.put("slot_1", "inventory");
        CHANNEL_TO_GROUP.put("slot_2", "inventory");
        CHANNEL_TO_GROUP.put("slot_3", "inventory");
        CHANNEL_TO_GROUP.put("slot_4", "inventory");
        CHANNEL_TO_GROUP.put("slot_5", "inventory");
        CHANNEL_TO_GROUP.put("slot_6", "inventory");
        CHANNEL_TO_GROUP.put("slot_7", "inventory");
        CHANNEL_TO_GROUP.put("slot_8", "inventory");

        CHANNEL_TO_GROUP.put("health", "health");
        CHANNEL_TO_GROUP.put("health_container", "health");
        CHANNEL_TO_GROUP.put("absorption", "health");
        CHANNEL_TO_GROUP.put("absorption_container", "health");
        CHANNEL_TO_GROUP.put("heart_type", "health");
        CHANNEL_TO_GROUP.put("hardcore", "health");
        CHANNEL_TO_GROUP.put("heart_regeneration", "health");
        CHANNEL_TO_GROUP.put("armor", "health");

        CHANNEL_TO_GROUP.put("hunger", "hunger");
        CHANNEL_TO_GROUP.put("hunger_effect", "hunger");
        CHANNEL_TO_GROUP.put("air", "hunger");

        CHANNEL_TO_GROUP.put("experience", "experience");
        CHANNEL_TO_GROUP.put("experience_level", "experience");
        CHANNEL_TO_GROUP.put("attack_cooldown", "experience");
        CHANNEL_TO_GROUP.put("show_attack_cooldown", "experience");
        CHANNEL_TO_GROUP.put("mount_health", "experience");
        CHANNEL_TO_GROUP.put("mount_health_container", "experience");
        CHANNEL_TO_GROUP.put("horse_jump", "experience");
        CHANNEL_TO_GROUP.put("show_horse_jump", "experience");

        CHANNEL_TO_GROUP.put("show_hotbar", "visibility");
        CHANNEL_TO_GROUP.put("show_health", "visibility");
        CHANNEL_TO_GROUP.put("show_armor", "visibility");
        CHANNEL_TO_GROUP.put("show_hunger", "visibility");
        CHANNEL_TO_GROUP.put("show_air", "visibility");
        CHANNEL_TO_GROUP.put("show_experience", "visibility");
    }

    private final Set<String> collapsedGroups = new HashSet<>();

    public UIHotbarClip(HotbarClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("hotbar_keyframes");

        this.edit = new UIButton(UIKeys.CAMERA_PANELS_EDIT_KEYFRAMES, (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.edit.keys().register(Keys.FORMS_EDIT, () -> this.edit.clickItself());
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_HOTBAR), this.edit).marginTop(12));
    }

    @Override
    public void fillData()
    {
        super.fillData();
        this.ensureHardcoreIsBoolean();
        this.updateKeyframeSheets();
    }

    public void updateKeyframeSheets()
    {
        this.keyframes.setChannels(this.clip.channels);

        List<UIKeyframeSheet> rawSheets = new ArrayList<>(this.keyframes.view.getGraph().getSheets());
        List<UIKeyframeSheet> groupedSheets = new ArrayList<>();

        Map<String, List<UIKeyframeSheet>> groupMap = new LinkedHashMap<>();
        groupMap.put("layout", new ArrayList<>());
        groupMap.put("inventory", new ArrayList<>());
        groupMap.put("health", new ArrayList<>());
        groupMap.put("hunger", new ArrayList<>());
        groupMap.put("experience", new ArrayList<>());
        groupMap.put("visibility", new ArrayList<>());

        for (UIKeyframeSheet sheet : rawSheets)
        {
            sheet.title = this.getTrackTitle(sheet.id);
            String group = CHANNEL_TO_GROUP.getOrDefault(sheet.id, "layout");
            groupMap.computeIfAbsent(group, k -> new ArrayList<>()).add(sheet);
        }

        for (Map.Entry<String, List<UIKeyframeSheet>> entry : groupMap.entrySet())
        {
            String groupKey = entry.getKey();
            List<UIKeyframeSheet> children = entry.getValue();

            if (children.isEmpty())
            {
                continue;
            }

            boolean isExpanded = !this.collapsedGroups.contains(groupKey);
            IKey groupTitle = this.getGroupTitle(groupKey);

            UIKeyframeSheet header = UIKeyframeSheet.groupHeader(
                "__group_hotbar_" + groupKey,
                groupTitle,
                Colors.LIGHTEST_GRAY & Colors.RGB,
                groupKey,
                isExpanded,
                () ->
                {
                    if (this.collapsedGroups.contains(groupKey))
                    {
                        this.collapsedGroups.remove(groupKey);
                    }
                    else
                    {
                        this.collapsedGroups.add(groupKey);
                    }
                    this.updateKeyframeSheets();
                }
            );
            header.level = 0;

            groupedSheets.add(header);

            if (isExpanded)
            {
                for (UIKeyframeSheet child : children)
                {
                    child.level = 1;
                    groupedSheets.add(child);
                }
            }
        }

        List<UIKeyframeSheet> graphSheets = this.keyframes.view.getGraph().getSheets();

        graphSheets.clear();
        graphSheets.addAll(groupedSheets);
    }

    private IKey getGroupTitle(String groupKey)
    {
        return switch (groupKey)
        {
            case "layout" -> UIKeys.CAMERA_CLIPS_GROUP_LAYOUT;
            case "inventory" -> UIKeys.CAMERA_CLIPS_GROUP_INVENTORY;
            case "health" -> UIKeys.CAMERA_CLIPS_GROUP_HEALTH;
            case "hunger" -> UIKeys.CAMERA_CLIPS_GROUP_HUNGER;
            case "experience" -> UIKeys.CAMERA_CLIPS_GROUP_EXPERIENCE;
            case "visibility" -> UIKeys.CAMERA_CLIPS_GROUP_VISIBILITY;
            default -> IKey.constant(groupKey);
        };
    }

    @Override
    protected UIKeyframeEditor resolveClipEmbeddableView(String undoId)
    {
        return undoId.equals(this.keyframes.getUndoId()) ? this.keyframes : null;
    }

    private IKey getTrackTitle(String id)
    {
        return switch (id)
        {
            case "selected_slot" -> UIKeys.C_CLIP.get("bbs:selected_slot");
            case "slot_0" -> UIKeys.C_CLIP.get("bbs:slot_0");
            case "slot_1" -> UIKeys.C_CLIP.get("bbs:slot_1");
            case "slot_2" -> UIKeys.C_CLIP.get("bbs:slot_2");
            case "slot_3" -> UIKeys.C_CLIP.get("bbs:slot_3");
            case "slot_4" -> UIKeys.C_CLIP.get("bbs:slot_4");
            case "slot_5" -> UIKeys.C_CLIP.get("bbs:slot_5");
            case "slot_6" -> UIKeys.C_CLIP.get("bbs:slot_6");
            case "slot_7" -> UIKeys.C_CLIP.get("bbs:slot_7");
            case "slot_8" -> UIKeys.C_CLIP.get("bbs:slot_8");
            case "offhand_slot" -> UIKeys.C_CLIP.get("bbs:offhand_slot");
            case "health" -> UIKeys.C_CLIP.get("bbs:health");
            case "health_container" -> UIKeys.C_CLIP.get("bbs:health_container");
            case "absorption" -> UIKeys.C_CLIP.get("bbs:absorption");
            case "absorption_container" -> UIKeys.C_CLIP.get("bbs:absorption_container");
            case "heart_type" -> UIKeys.C_CLIP.get("bbs:heart_type");
            case "hardcore" -> UIKeys.C_CLIP.get("bbs:hardcore");
            case "heart_regeneration" -> UIKeys.C_CLIP.get("bbs:heart_regeneration");
            case "hunger_effect" -> UIKeys.C_CLIP.get("bbs:hunger_effect");
            case "armor" -> UIKeys.C_CLIP.get("bbs:armor");
            case "hunger" -> UIKeys.C_CLIP.get("bbs:hunger");
            case "air" -> UIKeys.C_CLIP.get("bbs:air");
            case "experience" -> UIKeys.C_CLIP.get("bbs:experience");
            case "experience_level" -> UIKeys.C_CLIP.get("bbs:experience_level");
            case "right_offhand" -> UIKeys.C_CLIP.get("bbs:right_offhand");
            case "show_hotbar" -> UIKeys.C_CLIP.get("bbs:show_hotbar");
            case "show_health" -> UIKeys.C_CLIP.get("bbs:show_health");
            case "show_armor" -> UIKeys.C_CLIP.get("bbs:show_armor");
            case "show_hunger" -> UIKeys.C_CLIP.get("bbs:show_hunger");
            case "show_air" -> UIKeys.C_CLIP.get("bbs:show_air");
            case "show_experience" -> UIKeys.C_CLIP.get("bbs:show_experience");
            case "mount_health" -> UIKeys.C_CLIP.get("bbs:mount_health");
            case "mount_health_container" -> UIKeys.C_CLIP.get("bbs:mount_health_container");
            case "horse_jump" -> UIKeys.C_CLIP.get("bbs:horse_jump");
            case "show_horse_jump" -> UIKeys.C_CLIP.get("bbs:show_horse_jump");
            case "attack_cooldown" -> UIKeys.C_CLIP.get("bbs:attack_cooldown");
            case "show_attack_cooldown" -> UIKeys.C_CLIP.get("bbs:show_attack_cooldown");
            case "layout" -> UIKeys.C_CLIP.get("bbs:layout");
            default -> IKey.constant(id);
        };
    }

    private void ensureHardcoreIsBoolean()
    {
        if (this.clip.hardcore.getFactory() == KeyframeFactories.BOOLEAN)
        {
            return;
        }

        MapType data = this.clip.hardcore.toData().asMap();

        data.putString("type", "boolean");
        this.clip.hardcore.fromData(data);
    }
}
