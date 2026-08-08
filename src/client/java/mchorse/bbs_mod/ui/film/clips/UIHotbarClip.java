package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.HotbarClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.minecraft.util.math.MathHelper;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UIHotbarClip extends UIClip<HotbarClip>
{
    public UITrackpad layoutX;
    public UITrackpad layoutY;
    public UITrackpad layoutScale;
    public UITrackpad selectedSlot;
    public UIToggle rightOffhand;
    public UITrackpad health;
    public UITrackpad healthContainer;
    public UITrackpad absorption;
    public UITrackpad absorptionContainer;
    public UIToggle hardcore;
    public UIToggle heartRegeneration;
    public UITrackpad armor;
    public UITrackpad hunger;
    public UIToggle hungerEffect;
    public UITrackpad air;
    public UITrackpad experience;
    public UITrackpad experienceLevel;
    public UITrackpad mountHealth;
    public UITrackpad mountHealthContainer;
    public UITrackpad horseJump;
    public UIToggle showHorseJump;
    public UITrackpad attackCooldown;
    public UIToggle showAttackCooldown;
    public UIToggle showHotbar;
    public UIToggle showHealth;
    public UIToggle showArmor;
    public UIToggle showHunger;
    public UIToggle showAir;
    public UIToggle showExperience;
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    private static final Map<String, String> CHANNEL_TO_GROUP = new HashMap<>();

    static
    {
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

        this.layoutX = this.createLayoutTrackpad(0);
        this.layoutY = this.createLayoutTrackpad(1);
        this.layoutScale = this.createLayoutTrackpad(2);
        this.layoutX.tooltip(UIKeys.C_CLIP.get("bbs:x"));
        this.layoutY.tooltip(UIKeys.C_CLIP.get("bbs:y"));
        this.layoutScale.tooltip(UIKeys.C_CLIP.get("bbs:scale"));

        this.selectedSlot = this.createDoubleTrackpad(this.clip.selectedSlot, true, 0F, 8F, UIKeys.C_CLIP.get("bbs:selected_slot"));
        this.rightOffhand = this.createBooleanField(this.clip.rightOffhand, UIKeys.C_CLIP.get("bbs:right_offhand"));

        this.health = this.createDoubleTrackpad(this.clip.health, false, 0F, null, UIKeys.C_CLIP.get("bbs:health"));
        this.healthContainer = this.createDoubleTrackpad(this.clip.healthContainer, false, 0F, null, UIKeys.C_CLIP.get("bbs:health_container"));
        this.absorption = this.createDoubleTrackpad(this.clip.absorption, false, 0F, null, UIKeys.C_CLIP.get("bbs:absorption"));
        this.absorptionContainer = this.createDoubleTrackpad(this.clip.absorptionContainer, false, 0F, null, UIKeys.C_CLIP.get("bbs:absorption_container"));
        this.hardcore = this.createBooleanField(this.clip.hardcore, UIKeys.C_CLIP.get("bbs:hardcore"));
        this.heartRegeneration = this.createBooleanField(this.clip.heartRegeneration, UIKeys.C_CLIP.get("bbs:heart_regeneration"));
        this.armor = this.createDoubleTrackpad(this.clip.armor, false, 0F, null, UIKeys.C_CLIP.get("bbs:armor"));

        this.hunger = this.createDoubleTrackpad(this.clip.hunger, false, 0F, null, UIKeys.C_CLIP.get("bbs:hunger"));
        this.hungerEffect = this.createBooleanField(this.clip.hungerEffect, UIKeys.C_CLIP.get("bbs:hunger_effect"));
        this.air = this.createDoubleTrackpad(this.clip.air, false, 0F, null, UIKeys.C_CLIP.get("bbs:air"));

        this.experience = this.createDoubleTrackpad(this.clip.experience, false, 0F, 1F, UIKeys.C_CLIP.get("bbs:experience"));
        this.experienceLevel = this.createDoubleTrackpad(this.clip.experienceLevel, true, 0F, null, UIKeys.C_CLIP.get("bbs:experience_level"));
        this.mountHealth = this.createDoubleTrackpad(this.clip.mountHealth, false, 0F, null, UIKeys.C_CLIP.get("bbs:mount_health"));
        this.mountHealthContainer = this.createDoubleTrackpad(this.clip.mountHealthContainer, false, 0F, null, UIKeys.C_CLIP.get("bbs:mount_health_container"));
        this.horseJump = this.createDoubleTrackpad(this.clip.horseJump, false, 0F, 1F, UIKeys.C_CLIP.get("bbs:horse_jump"));
        this.showHorseJump = this.createBooleanField(this.clip.showHorseJump, UIKeys.C_CLIP.get("bbs:show_horse_jump"));
        this.attackCooldown = this.createDoubleTrackpad(this.clip.attackCooldown, false, 0F, 1F, UIKeys.C_CLIP.get("bbs:attack_cooldown"));
        this.showAttackCooldown = this.createBooleanField(this.clip.showAttackCooldown, UIKeys.C_CLIP.get("bbs:show_attack_cooldown"));

        this.showHotbar = this.createBooleanField(this.clip.showHotbar, UIKeys.C_CLIP.get("bbs:show_hotbar"));
        this.showHealth = this.createBooleanField(this.clip.showHealth, UIKeys.C_CLIP.get("bbs:show_health"));
        this.showArmor = this.createBooleanField(this.clip.showArmor, UIKeys.C_CLIP.get("bbs:show_armor"));
        this.showHunger = this.createBooleanField(this.clip.showHunger, UIKeys.C_CLIP.get("bbs:show_hunger"));
        this.showAir = this.createBooleanField(this.clip.showAir, UIKeys.C_CLIP.get("bbs:show_air"));
        this.showExperience = this.createBooleanField(this.clip.showExperience, UIKeys.C_CLIP.get("bbs:show_experience"));

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

    private UITrackpad createLayoutTrackpad(int component)
    {
        return new UITrackpad((v) ->
        {
            int tick = this.getClipTick();
            Vector4f current = this.clip.layout.interpolate(tick);
            Vector4f next = new Vector4f(current);

            if (component == 0)
            {
                next.x = v.floatValue();
            }
            else if (component == 1)
            {
                next.y = v.floatValue();
            }
            else
            {
                next.z = v.floatValue();
            }

            this.clip.layout.insert(tick, next);
            this.fillData();
        });
    }

    private UITrackpad createDoubleTrackpad(KeyframeChannel<? extends Number> channel, boolean integer, Float min, Float max, IKey tooltip)
    {
        UITrackpad trackpad = new UITrackpad((v) ->
        {
            int tick = this.getClipTick();

            if (channel.getFactory() == KeyframeFactories.INTEGER)
            {
                ((KeyframeChannel<Integer>) channel).insert(tick, v.intValue());
            }
            else
            {
                ((KeyframeChannel<Double>) channel).insert(tick, v.doubleValue());
            }

            this.fillData();
        });

        if (integer)
        {
            trackpad.integer();
        }

        if (min != null)
        {
            if (max != null)
            {
                trackpad.limit(min, max);
            }
            else
            {
                trackpad.limit(min);
            }
        }

        if (tooltip != null)
        {
            trackpad.tooltip(tooltip);
        }

        return trackpad;
    }

    private UIToggle createBooleanField(KeyframeChannel<Boolean> channel, IKey label)
    {
        return new UIToggle(label, (b) ->
        {
            channel.insert(this.getClipTick(), b.getValue());
            this.fillData();
        });
    }

    private int getClipTick()
    {
        return MathHelper.clamp(this.editor.getCursor() - this.clip.tick.get(), 0, this.clip.duration.get());
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(
            UIClip.label(UIKeys.CAMERA_CLIPS_GROUP_LAYOUT),
            UI.row(this.layoutX, this.layoutY),
            this.layoutScale,
            this.rightOffhand
        ).marginTop(6));
        this.panels.add(UI.column(
            UIClip.label(UIKeys.CAMERA_CLIPS_GROUP_INVENTORY),
            this.selectedSlot
        ).marginTop(6));
        this.panels.add(UI.column(
            UIClip.label(UIKeys.CAMERA_CLIPS_GROUP_HEALTH),
            UI.row(this.health, this.healthContainer),
            UI.row(this.absorption, this.absorptionContainer),
            this.armor,
            this.hardcore,
            this.heartRegeneration
        ).marginTop(6));
        this.panels.add(UI.column(
            UIClip.label(UIKeys.CAMERA_CLIPS_GROUP_HUNGER),
            this.hunger,
            this.hungerEffect,
            this.air
        ).marginTop(6));
        this.panels.add(UI.column(
            UIClip.label(UIKeys.CAMERA_CLIPS_GROUP_EXPERIENCE),
            UI.row(this.experience, this.experienceLevel),
            UI.row(this.mountHealth, this.mountHealthContainer),
            this.horseJump,
            this.showHorseJump,
            this.attackCooldown,
            this.showAttackCooldown
        ).marginTop(6));
        this.panels.add(UI.column(
            UIClip.label(UIKeys.CAMERA_CLIPS_GROUP_VISIBILITY),
            this.showHotbar,
            this.showHealth,
            this.showArmor,
            this.showHunger,
            this.showAir,
            this.showExperience
        ).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_HOTBAR), this.edit).marginTop(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();
        this.ensureHardcoreIsBoolean();

        int tick = this.getClipTick();
        Vector4f layout = this.clip.layout.interpolate(tick);

        this.layoutX.setValue(layout.x);
        this.layoutY.setValue(layout.y);
        this.layoutScale.setValue(layout.z);
        this.selectedSlot.setValue(this.getNumber(this.clip.selectedSlot, 0D));
        this.rightOffhand.setValue(this.clip.rightOffhand.interpolate(tick, false));
        this.health.setValue(this.getNumber(this.clip.health, 20D));
        this.healthContainer.setValue(this.getNumber(this.clip.healthContainer, 20D));
        this.absorption.setValue(this.getNumber(this.clip.absorption, 0D));
        this.absorptionContainer.setValue(this.getNumber(this.clip.absorptionContainer, 0D));
        this.hardcore.setValue(this.clip.hardcore.interpolate(tick, false));
        this.heartRegeneration.setValue(this.clip.heartRegeneration.interpolate(tick, false));
        this.armor.setValue(this.getNumber(this.clip.armor, 0D));
        this.hunger.setValue(this.getNumber(this.clip.hunger, 20D));
        this.hungerEffect.setValue(this.clip.hungerEffect.interpolate(tick, false));
        this.air.setValue(this.getNumber(this.clip.air, 300D));
        this.experience.setValue(this.getNumber(this.clip.experience, 0D));
        this.experienceLevel.setValue(this.getNumber(this.clip.experienceLevel, 0D));
        this.mountHealth.setValue(this.getNumber(this.clip.mountHealth, 0D));
        this.mountHealthContainer.setValue(this.getNumber(this.clip.mountHealthContainer, 0D));
        this.horseJump.setValue(this.getNumber(this.clip.horseJump, 0D));
        this.showHorseJump.setValue(this.clip.showHorseJump.interpolate(tick, false));
        this.attackCooldown.setValue(this.getNumber(this.clip.attackCooldown, 0D));
        this.showAttackCooldown.setValue(this.clip.showAttackCooldown.interpolate(tick, false));
        this.showHotbar.setValue(this.clip.showHotbar.interpolate(tick, true));
        this.showHealth.setValue(this.clip.showHealth.interpolate(tick, true));
        this.showArmor.setValue(this.clip.showArmor.interpolate(tick, true));
        this.showHunger.setValue(this.clip.showHunger.interpolate(tick, true));
        this.showAir.setValue(this.clip.showAir.interpolate(tick, true));
        this.showExperience.setValue(this.clip.showExperience.interpolate(tick, true));

        this.updateKeyframeSheets();
    }

    private double getNumber(KeyframeChannel<? extends Number> channel, double fallback)
    {
        Number value = channel.interpolate(this.getClipTick());

        return value == null ? fallback : value.doubleValue();
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
