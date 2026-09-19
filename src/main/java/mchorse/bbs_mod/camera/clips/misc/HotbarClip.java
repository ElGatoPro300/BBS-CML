package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.mc.ValueItemStack;
import mchorse.bbs_mod.settings.values.misc.ValueVector4f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.minecraft.world.item.ItemStack;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class HotbarClip extends CameraClip
{
    private static final float MAX_HEALTH_CONTAINER = 1200F; /* 60 rows * 10 hearts * 2 HP */
    private static final Vector4f DEFAULT_LAYOUT = new Vector4f(0F, 0F, 1F, 0F);

    public final KeyframeChannel<Integer> selectedSlot = new KeyframeChannel<>("selected_slot", KeyframeFactories.INTEGER);
    public final KeyframeChannel<ItemStack> slot0 = new KeyframeChannel<>("slot_0", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot1 = new KeyframeChannel<>("slot_1", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot2 = new KeyframeChannel<>("slot_2", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot3 = new KeyframeChannel<>("slot_3", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot4 = new KeyframeChannel<>("slot_4", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot5 = new KeyframeChannel<>("slot_5", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot6 = new KeyframeChannel<>("slot_6", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot7 = new KeyframeChannel<>("slot_7", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot8 = new KeyframeChannel<>("slot_8", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> offhandSlot = new KeyframeChannel<>("offhand_slot", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<Double> health = new KeyframeChannel<>("health", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> healthContainer = new KeyframeChannel<>("health_container", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> absorption = new KeyframeChannel<>("absorption", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> absorptionContainer = new KeyframeChannel<>("absorption_container", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Integer> heartType = new KeyframeChannel<>("heart_type", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Boolean> hardcore = new KeyframeChannel<>("hardcore", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> heartRegeneration = new KeyframeChannel<>("heart_regeneration", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> armor = new KeyframeChannel<>("armor", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> hunger = new KeyframeChannel<>("hunger", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Boolean> hungerEffect = new KeyframeChannel<>("hunger_effect", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> air = new KeyframeChannel<>("air", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> experience = new KeyframeChannel<>("experience", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Integer> experienceLevel = new KeyframeChannel<>("experience_level", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Vector4f> layout = new KeyframeChannel<>("layout", KeyframeFactories.VECTOR4F);

    public final KeyframeChannel<Boolean> rightOffhand = new KeyframeChannel<>("right_offhand", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> showHotbar = new KeyframeChannel<>("show_hotbar", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> showHealth = new KeyframeChannel<>("show_health", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> showArmor = new KeyframeChannel<>("show_armor", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> showHunger = new KeyframeChannel<>("show_hunger", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> showAir = new KeyframeChannel<>("show_air", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> showExperience = new KeyframeChannel<>("show_experience", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> mountHealth = new KeyframeChannel<>("mount_health", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> mountHealthContainer = new KeyframeChannel<>("mount_health_container", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> horseJump = new KeyframeChannel<>("horse_jump", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Boolean> showHorseJump = new KeyframeChannel<>("show_horse_jump", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> attackCooldown = new KeyframeChannel<>("attack_cooldown", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Boolean> showAttackCooldown = new KeyframeChannel<>("show_attack_cooldown", KeyframeFactories.BOOLEAN);

    public final ValueBoolean useKeyframes = new ValueBoolean("use_keyframes", false);
    public final ValueBoolean uniformSeeded = new ValueBoolean("uniform_seeded", false);
    public final HotbarUniform uniform = new HotbarUniform("uniform");

    public final KeyframeChannel[] channels;

    public HotbarClip()
    {
        this.channels = new KeyframeChannel[] {
            this.layout,
            this.selectedSlot,
            this.slot0, this.slot1, this.slot2, this.slot3, this.slot4, this.slot5, this.slot6, this.slot7, this.slot8, this.offhandSlot, this.rightOffhand,
            this.health, this.healthContainer, this.absorption, this.absorptionContainer, this.heartType, this.hardcore, this.heartRegeneration, this.armor, this.hunger, this.hungerEffect, this.air, this.experience, this.experienceLevel,
            this.mountHealth, this.mountHealthContainer, this.horseJump, this.showHorseJump, this.attackCooldown, this.showAttackCooldown,
            this.showHotbar, this.showHealth, this.showArmor, this.showHunger, this.showAir, this.showExperience
        };

        for (KeyframeChannel channel : this.channels)
        {
            this.add(channel);
        }

        this.add(this.useKeyframes);
        this.add(this.uniformSeeded);
        this.add(this.uniform);
    }

    public static List<HotbarState> getHotbars(ClipContext context)
    {
        return context.clipData.get("hotbars", ArrayList::new);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float alpha = this.envelope.factorEnabled(this.duration.get(), t);

        if (alpha <= 0F)
        {
            return;
        }

        HotbarState state = new HotbarState();

        state.selectedSlot = Math.max(0, Math.min(8, this.valueInt(this.selectedSlot, this.uniform.selectedSlot, t, 0)));
        state.items[0] = this.copyItem(this.valueItem(this.slot0, this.uniform.slot0, t, ItemStack.EMPTY));
        state.items[1] = this.copyItem(this.valueItem(this.slot1, this.uniform.slot1, t, ItemStack.EMPTY));
        state.items[2] = this.copyItem(this.valueItem(this.slot2, this.uniform.slot2, t, ItemStack.EMPTY));
        state.items[3] = this.copyItem(this.valueItem(this.slot3, this.uniform.slot3, t, ItemStack.EMPTY));
        state.items[4] = this.copyItem(this.valueItem(this.slot4, this.uniform.slot4, t, ItemStack.EMPTY));
        state.items[5] = this.copyItem(this.valueItem(this.slot5, this.uniform.slot5, t, ItemStack.EMPTY));
        state.items[6] = this.copyItem(this.valueItem(this.slot6, this.uniform.slot6, t, ItemStack.EMPTY));
        state.items[7] = this.copyItem(this.valueItem(this.slot7, this.uniform.slot7, t, ItemStack.EMPTY));
        state.items[8] = this.copyItem(this.valueItem(this.slot8, this.uniform.slot8, t, ItemStack.EMPTY));
        state.offhandItem = this.copyItem(this.valueItem(this.offhandSlot, this.uniform.offhandSlot, t, ItemStack.EMPTY));
        state.rightOffhand = this.valueBoolean(this.rightOffhand, this.uniform.rightOffhand, t, false);
        state.showHotbar = this.valueBoolean(this.showHotbar, this.uniform.showHotbar, t, true);
        state.showHealth = this.valueBoolean(this.showHealth, this.uniform.showHealth, t, true);
        state.showArmor = this.valueBoolean(this.showArmor, this.uniform.showArmor, t, true);
        state.showHunger = this.valueBoolean(this.showHunger, this.uniform.showHunger, t, true);
        state.showAir = this.valueBoolean(this.showAir, this.uniform.showAir, t, true);
        state.showExperience = this.valueBoolean(this.showExperience, this.uniform.showExperience, t, true);
        state.mountHealthContainer = this.clampHealthContainer(this.valueDouble(this.mountHealthContainer, this.uniform.mountHealthContainer, t, 0D));
        state.mountHealth = this.clampHealth(this.valueDouble(this.mountHealth, this.uniform.mountHealth, t, 0D), state.mountHealthContainer);
        state.horseJump = Math.max(0F, Math.min(1F, (float) this.valueDouble(this.horseJump, this.uniform.horseJump, t, 0D)));
        state.showHorseJump = this.valueBoolean(this.showHorseJump, this.uniform.showHorseJump, t, false);
        state.attackCooldown = Math.max(0F, Math.min(1F, (float) this.valueDouble(this.attackCooldown, this.uniform.attackCooldown, t, 0D)));
        state.showAttackCooldown = this.valueBoolean(this.showAttackCooldown, this.uniform.showAttackCooldown, t, false);
        state.healthContainer = this.clampHealthContainer(this.valueDouble(this.healthContainer, this.uniform.healthContainer, t, 20D));
        state.health = this.clampHealth(this.valueDouble(this.health, this.uniform.health, t, 20D), state.healthContainer);
        state.absorptionContainer = this.clampHealthContainer(this.valueDouble(this.absorptionContainer, this.uniform.absorptionContainer, t, 0D));
        state.absorption = this.clampHealth(this.valueDouble(this.absorption, this.uniform.absorption, t, 0D), state.absorptionContainer);
        state.heartType = this.clampHeartType(this.valueInt(this.heartType, this.uniform.heartType, t, HotbarState.HEART_NORMAL));
        state.hardcore = this.valueHardcore(t);
        state.heartRegeneration = this.valueBoolean(this.heartRegeneration, this.uniform.heartRegeneration, t, false);
        state.armor = this.clampStat(this.valueDouble(this.armor, this.uniform.armor, t, 0D));
        state.hunger = this.clampStat(this.valueDouble(this.hunger, this.uniform.hunger, t, 20D));
        state.hungerEffect = this.valueBoolean(this.hungerEffect, this.uniform.hungerEffect, t, false);
        state.air = this.clampAir(this.valueDouble(this.air, this.uniform.air, t, 300D));
        state.experience = this.clampExperience(this.valueDouble(this.experience, this.uniform.experience, t, 0D));
        state.experienceLevel = this.clampExperienceLevel(this.valueInt(this.experienceLevel, this.uniform.experienceLevel, t, 0));
        Vector4f layout = this.valueVector4f(this.layout, this.uniform.layout, t, DEFAULT_LAYOUT);
        state.x = layout.x;
        state.y = layout.y;
        state.scale = Math.max(0.05F, layout.z);
        state.alpha = alpha;
        state.renderOrder = context.applied;

        getHotbars(context).add(state);
    }

    /**
     * Copy the current keyframed values into uniform storage the first time
     * keyframe mode is disabled, without modifying the keyframe channels.
     */
    public void ensureUniformSeeded(float tick)
    {
        if (this.uniformSeeded.get())
        {
            return;
        }

        this.uniform.layout.set(new Vector4f(this.interpVector4f(this.layout, tick, DEFAULT_LAYOUT)));
        this.uniform.selectedSlot.set(this.interpInt(this.selectedSlot, tick, 0));
        this.uniform.slot0.set(this.copyItem(this.interpItem(this.slot0, tick, ItemStack.EMPTY)));
        this.uniform.slot1.set(this.copyItem(this.interpItem(this.slot1, tick, ItemStack.EMPTY)));
        this.uniform.slot2.set(this.copyItem(this.interpItem(this.slot2, tick, ItemStack.EMPTY)));
        this.uniform.slot3.set(this.copyItem(this.interpItem(this.slot3, tick, ItemStack.EMPTY)));
        this.uniform.slot4.set(this.copyItem(this.interpItem(this.slot4, tick, ItemStack.EMPTY)));
        this.uniform.slot5.set(this.copyItem(this.interpItem(this.slot5, tick, ItemStack.EMPTY)));
        this.uniform.slot6.set(this.copyItem(this.interpItem(this.slot6, tick, ItemStack.EMPTY)));
        this.uniform.slot7.set(this.copyItem(this.interpItem(this.slot7, tick, ItemStack.EMPTY)));
        this.uniform.slot8.set(this.copyItem(this.interpItem(this.slot8, tick, ItemStack.EMPTY)));
        this.uniform.offhandSlot.set(this.copyItem(this.interpItem(this.offhandSlot, tick, ItemStack.EMPTY)));
        this.uniform.rightOffhand.set(this.interpBoolean(this.rightOffhand, tick, false));
        this.uniform.health.set(this.interpDouble(this.health, tick, 20D));
        this.uniform.healthContainer.set(this.interpDouble(this.healthContainer, tick, 20D));
        this.uniform.absorption.set(this.interpDouble(this.absorption, tick, 0D));
        this.uniform.absorptionContainer.set(this.interpDouble(this.absorptionContainer, tick, 0D));
        this.uniform.heartType.set(this.interpInt(this.heartType, tick, HotbarState.HEART_NORMAL));
        this.uniform.hardcore.set(this.interpHardcore(tick));
        this.uniform.heartRegeneration.set(this.interpBoolean(this.heartRegeneration, tick, false));
        this.uniform.armor.set(this.interpDouble(this.armor, tick, 0D));
        this.uniform.hunger.set(this.interpDouble(this.hunger, tick, 20D));
        this.uniform.hungerEffect.set(this.interpBoolean(this.hungerEffect, tick, false));
        this.uniform.air.set(this.interpDouble(this.air, tick, 300D));
        this.uniform.experience.set(this.interpDouble(this.experience, tick, 0D));
        this.uniform.experienceLevel.set(this.interpInt(this.experienceLevel, tick, 0));
        this.uniform.mountHealth.set(this.interpDouble(this.mountHealth, tick, 0D));
        this.uniform.mountHealthContainer.set(this.interpDouble(this.mountHealthContainer, tick, 0D));
        this.uniform.horseJump.set(this.interpDouble(this.horseJump, tick, 0D));
        this.uniform.showHorseJump.set(this.interpBoolean(this.showHorseJump, tick, false));
        this.uniform.attackCooldown.set(this.interpDouble(this.attackCooldown, tick, 0D));
        this.uniform.showAttackCooldown.set(this.interpBoolean(this.showAttackCooldown, tick, false));
        this.uniform.showHotbar.set(this.interpBoolean(this.showHotbar, tick, true));
        this.uniform.showHealth.set(this.interpBoolean(this.showHealth, tick, true));
        this.uniform.showArmor.set(this.interpBoolean(this.showArmor, tick, true));
        this.uniform.showHunger.set(this.interpBoolean(this.showHunger, tick, true));
        this.uniform.showAir.set(this.interpBoolean(this.showAir, tick, true));
        this.uniform.showExperience.set(this.interpBoolean(this.showExperience, tick, true));
        this.uniformSeeded.set(true);
    }

    /**
     * When enabling keyframe mode, fill any empty channels from uniform values
     * so scrubbing/playback can interpolate. Existing keyframes are preserved.
     */
    public void ensureChannelsSeeded(float tick)
    {
        this.ensureUniformSeeded(tick);

        this.seedVector4f(this.layout, this.uniform.layout.get());
        this.seedInt(this.selectedSlot, this.uniform.selectedSlot.get());
        this.seedItem(this.slot0, this.uniform.slot0.get());
        this.seedItem(this.slot1, this.uniform.slot1.get());
        this.seedItem(this.slot2, this.uniform.slot2.get());
        this.seedItem(this.slot3, this.uniform.slot3.get());
        this.seedItem(this.slot4, this.uniform.slot4.get());
        this.seedItem(this.slot5, this.uniform.slot5.get());
        this.seedItem(this.slot6, this.uniform.slot6.get());
        this.seedItem(this.slot7, this.uniform.slot7.get());
        this.seedItem(this.slot8, this.uniform.slot8.get());
        this.seedItem(this.offhandSlot, this.uniform.offhandSlot.get());
        this.seedBoolean(this.rightOffhand, this.uniform.rightOffhand.get());
        this.seedDouble(this.health, this.uniform.health.get());
        this.seedDouble(this.healthContainer, this.uniform.healthContainer.get());
        this.seedDouble(this.absorption, this.uniform.absorption.get());
        this.seedDouble(this.absorptionContainer, this.uniform.absorptionContainer.get());
        this.seedInt(this.heartType, this.uniform.heartType.get());
        this.seedBoolean(this.hardcore, this.uniform.hardcore.get());
        this.seedBoolean(this.heartRegeneration, this.uniform.heartRegeneration.get());
        this.seedDouble(this.armor, this.uniform.armor.get());
        this.seedDouble(this.hunger, this.uniform.hunger.get());
        this.seedBoolean(this.hungerEffect, this.uniform.hungerEffect.get());
        this.seedDouble(this.air, this.uniform.air.get());
        this.seedDouble(this.experience, this.uniform.experience.get());
        this.seedInt(this.experienceLevel, this.uniform.experienceLevel.get());
        this.seedDouble(this.mountHealth, this.uniform.mountHealth.get());
        this.seedDouble(this.mountHealthContainer, this.uniform.mountHealthContainer.get());
        this.seedDouble(this.horseJump, this.uniform.horseJump.get());
        this.seedBoolean(this.showHorseJump, this.uniform.showHorseJump.get());
        this.seedDouble(this.attackCooldown, this.uniform.attackCooldown.get());
        this.seedBoolean(this.showAttackCooldown, this.uniform.showAttackCooldown.get());
        this.seedBoolean(this.showHotbar, this.uniform.showHotbar.get());
        this.seedBoolean(this.showHealth, this.uniform.showHealth.get());
        this.seedBoolean(this.showArmor, this.uniform.showArmor.get());
        this.seedBoolean(this.showHunger, this.uniform.showHunger.get());
        this.seedBoolean(this.showAir, this.uniform.showAir.get());
        this.seedBoolean(this.showExperience, this.uniform.showExperience.get());
    }

    private void seedDouble(KeyframeChannel<Double> channel, double value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0, value);
        }
    }

    private void seedInt(KeyframeChannel<Integer> channel, int value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0, value);
        }
    }

    private void seedBoolean(KeyframeChannel<Boolean> channel, boolean value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0, value);
        }
    }

    private void seedItem(KeyframeChannel<ItemStack> channel, ItemStack value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0, this.copyItem(value));
        }
    }

    private void seedVector4f(KeyframeChannel<Vector4f> channel, Vector4f value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0, new Vector4f(value == null ? DEFAULT_LAYOUT : value));
        }
    }

    private double valueDouble(KeyframeChannel<Double> channel, ValueDouble uniform, float t, double fallback)
    {
        if (!this.useKeyframes.get())
        {
            return uniform.get();
        }

        if (channel.isEmpty())
        {
            return this.uniformSeeded.get() ? uniform.get() : fallback;
        }

        return this.interpDouble(channel, t, fallback);
    }

    private int valueInt(KeyframeChannel<Integer> channel, ValueInt uniform, float t, int fallback)
    {
        if (!this.useKeyframes.get())
        {
            return uniform.get();
        }

        if (channel.isEmpty())
        {
            return this.uniformSeeded.get() ? uniform.get() : fallback;
        }

        return this.interpInt(channel, t, fallback);
    }

    private boolean valueBoolean(KeyframeChannel<Boolean> channel, ValueBoolean uniform, float t, boolean fallback)
    {
        if (!this.useKeyframes.get())
        {
            return uniform.get();
        }

        if (channel.isEmpty())
        {
            return this.uniformSeeded.get() ? uniform.get() : fallback;
        }

        return this.interpBoolean(channel, t, fallback);
    }

    private ItemStack valueItem(KeyframeChannel<ItemStack> channel, ValueItemStack uniform, float t, ItemStack fallback)
    {
        if (!this.useKeyframes.get())
        {
            return uniform.get();
        }

        if (channel.isEmpty())
        {
            return this.uniformSeeded.get() ? uniform.get() : fallback;
        }

        return this.interpItem(channel, t, fallback);
    }

    private Vector4f valueVector4f(KeyframeChannel<Vector4f> channel, ValueVector4f uniform, float t, Vector4f fallback)
    {
        if (!this.useKeyframes.get())
        {
            return uniform.get();
        }

        if (channel.isEmpty())
        {
            return this.uniformSeeded.get() ? uniform.get() : fallback;
        }

        return this.interpVector4f(channel, t, fallback);
    }

    private boolean valueHardcore(float t)
    {
        if (!this.useKeyframes.get())
        {
            return this.uniform.hardcore.get();
        }

        if (this.hardcore.isEmpty())
        {
            return this.uniformSeeded.get() ? this.uniform.hardcore.get() : false;
        }

        return this.interpHardcore(t);
    }

    private double interpDouble(KeyframeChannel<Double> channel, float t, double fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t);
    }

    private int interpInt(KeyframeChannel<Integer> channel, float t, int fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        Integer value = channel.interpolate(t, fallback);

        return value == null ? fallback : value;
    }

    private boolean interpBoolean(KeyframeChannel<Boolean> channel, float t, boolean fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
    }

    private ItemStack interpItem(KeyframeChannel<ItemStack> channel, float t, ItemStack fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        ItemStack value = channel.interpolate(t, fallback);

        return value == null ? fallback : value;
    }

    private Vector4f interpVector4f(KeyframeChannel<Vector4f> channel, float t, Vector4f fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
    }

    private ItemStack copyItem(ItemStack stack)
    {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private float clampStat(double value)
    {
        return Math.max(0F, Math.min(20F, (float) value));
    }

    private float clampHealth(double value, float healthContainer)
    {
        return Math.max(0F, Math.min(healthContainer, (float) value));
    }

    private int clampHeartType(int value)
    {
        return Math.max(HotbarState.HEART_NORMAL, Math.min(HotbarState.HEART_FROZEN, value));
    }

    private float clampHealthContainer(double value)
    {
        return Math.max(0F, Math.min(MAX_HEALTH_CONTAINER, (float) value));
    }

    private float clampExperience(double value)
    {
        return Math.max(0F, Math.min(1F, (float) value));
    }

    private float clampAir(double value)
    {
        return Math.max(0F, Math.min(300F, (float) value));
    }

    private int clampExperienceLevel(int value)
    {
        return Math.max(0, Math.min(9999, value));
    }

    @Override
    public void fromData(BaseType data)
    {
        boolean hasUseKeyframes = data != null && data.isMap() && data.asMap().has("use_keyframes");

        if (data != null && data.isMap())
        {
            MapType map = data.asMap();
            MapType hardcoreData = map.getMap("hardcore", null);

            if (hardcoreData != null && !"boolean".equals(hardcoreData.getString("type")))
            {
                hardcoreData.putString("type", "boolean");
            }

            this.migrateLegacyLayout(map);
        }

        super.fromData(data);

        /* Older films did not store this flag — keep keyframe mode enabled for compatibility. */
        if (!hasUseKeyframes)
        {
            this.useKeyframes.set(true);
        }
    }

    private void migrateLegacyLayout(MapType map)
    {
        if (map.has("layout") || (!map.has("x") && !map.has("y") && !map.has("scale")))
        {
            return;
        }

        KeyframeChannel<Double> legacyX = this.readLegacyDoubleChannel(map.getMap("x", null));
        KeyframeChannel<Double> legacyY = this.readLegacyDoubleChannel(map.getMap("y", null));
        KeyframeChannel<Double> legacyScale = this.readLegacyDoubleChannel(map.getMap("scale", null));

        TreeSet<Float> ticks = new TreeSet<>();
        Map<Float, Keyframe<Double>> xByTick = this.collectByTick(legacyX, ticks);
        Map<Float, Keyframe<Double>> yByTick = this.collectByTick(legacyY, ticks);
        Map<Float, Keyframe<Double>> scaleByTick = this.collectByTick(legacyScale, ticks);

        if (ticks.isEmpty())
        {
            ticks.add(0F);
        }

        MapType layoutData = new MapType();
        ListType keyframes = new ListType();

        layoutData.putString("type", "vector4f");
        layoutData.put("keyframes", keyframes);

        for (float tick : ticks)
        {
            float x = legacyX.interpolate(tick, 0D).floatValue();
            float y = legacyY.interpolate(tick, 0D).floatValue();
            float scale = legacyScale.interpolate(tick, 1D).floatValue();
            Keyframe<Double> source = xByTick.get(tick);

            if (source == null)
            {
                source = yByTick.get(tick);
            }

            if (source == null)
            {
                source = scaleByTick.get(tick);
            }

            MapType keyframeData = source == null ? new MapType() : source.toData().asMap();
            ListType value = new ListType();

            value.addFloat(x);
            value.addFloat(y);
            value.addFloat(scale);
            value.addFloat(0F);

            keyframeData.putFloat("tick", tick);
            keyframeData.put("value", value);
            keyframes.add(keyframeData);
        }

        map.put("layout", layoutData);
    }

    private KeyframeChannel<Double> readLegacyDoubleChannel(MapType data)
    {
        KeyframeChannel<Double> channel = new KeyframeChannel<>("legacy", KeyframeFactories.DOUBLE);

        if (data != null)
        {
            channel.fromData(data);
        }

        return channel;
    }

    private Map<Float, Keyframe<Double>> collectByTick(KeyframeChannel<Double> channel, TreeSet<Float> ticks)
    {
        Map<Float, Keyframe<Double>> byTick = new HashMap<>();

        for (Keyframe<Double> keyframe : channel.getKeyframes())
        {
            ticks.add(keyframe.getTick());
            byTick.put(keyframe.getTick(), keyframe);
        }

        return byTick;
    }

    @SuppressWarnings("rawtypes")
    private boolean interpHardcore(float tick)
    {
        if (this.hardcore.getFactory() == KeyframeFactories.BOOLEAN)
        {
            return this.hardcore.interpolate(tick, false);
        }

        Object value = ((KeyframeChannel) this.hardcore).interpolate(tick, 0);

        if (value instanceof Number number)
        {
            return number.intValue() > 0;
        }

        if (value instanceof Boolean bool)
        {
            return bool;
        }

        return false;
    }

    @Override
    public boolean isPositionClip()
    {
        return false;
    }

    @Override
    protected Clip create()
    {
        return new HotbarClip();
    }
}
