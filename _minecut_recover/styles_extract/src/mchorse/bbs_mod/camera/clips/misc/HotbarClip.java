package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.class_1799;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class HotbarClip extends CameraClip
{
    private static final float MAX_HEALTH_CONTAINER = 1200F; /* 60 rows * 10 hearts * 2 HP */

    public final KeyframeChannel<Integer> selectedSlot = new KeyframeChannel<>("selected_slot", KeyframeFactories.INTEGER);
    public final KeyframeChannel<class_1799> slot0 = new KeyframeChannel<>("slot_0", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> slot1 = new KeyframeChannel<>("slot_1", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> slot2 = new KeyframeChannel<>("slot_2", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> slot3 = new KeyframeChannel<>("slot_3", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> slot4 = new KeyframeChannel<>("slot_4", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> slot5 = new KeyframeChannel<>("slot_5", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> slot6 = new KeyframeChannel<>("slot_6", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> slot7 = new KeyframeChannel<>("slot_7", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> slot8 = new KeyframeChannel<>("slot_8", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<class_1799> offhandSlot = new KeyframeChannel<>("offhand_slot", KeyframeFactories.ITEM_STACK);
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

    public final KeyframeChannel[] channels;
    public HotbarClip()
    {
        this.channels = new KeyframeChannel[] {
            this.layout,
            this.selectedSlot,
            this.slot0, this.slot1, this.slot2, this.slot3, this.slot4, this.slot5, this.slot6, this.slot7, this.slot8, this.offhandSlot,
            this.health, this.healthContainer, this.absorption, this.absorptionContainer, this.heartType, this.hardcore, this.heartRegeneration, this.armor, this.hunger, this.hungerEffect, this.air, this.experience, this.experienceLevel,
        };

        for (KeyframeChannel channel : this.channels)
        {
            this.add(channel);
        }

        this.selectedSlot.insert(0, 0);
        this.health.insert(0, 20D);
        this.healthContainer.insert(0, 20D);
        this.absorption.insert(0, 0D);
        this.absorptionContainer.insert(0, 0D);
        this.heartType.insert(0, HotbarState.HEART_NORMAL);
        this.hardcore.insert(0, false);
        this.heartRegeneration.insert(0, false);
        this.armor.insert(0, 0D);
        this.hunger.insert(0, 20D);
        this.hungerEffect.insert(0, false);
        this.air.insert(0, 300D);
        this.experience.insert(0, 0D);
        this.experienceLevel.insert(0, 0);
        this.layout.insert(0, new Vector4f(0F, 0F, 1F, 0F));
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

        state.selectedSlot = Math.max(0, Math.min(8, this.selectedSlot.interpolate(t)));
        state.items[0] = this.copyItem(this.slot0.interpolate(t));
        state.items[1] = this.copyItem(this.slot1.interpolate(t));
        state.items[2] = this.copyItem(this.slot2.interpolate(t));
        state.items[3] = this.copyItem(this.slot3.interpolate(t));
        state.items[4] = this.copyItem(this.slot4.interpolate(t));
        state.items[5] = this.copyItem(this.slot5.interpolate(t));
        state.items[6] = this.copyItem(this.slot6.interpolate(t));
        state.items[7] = this.copyItem(this.slot7.interpolate(t));
        state.items[8] = this.copyItem(this.slot8.interpolate(t));
        state.offhandItem = this.copyItem(this.offhandSlot.interpolate(t));
        state.healthContainer = this.clampHealthContainer(this.healthContainer.interpolate(t));
        state.health = this.clampHealth(this.health.interpolate(t), state.healthContainer);
        state.absorptionContainer = this.clampHealthContainer(this.absorptionContainer.interpolate(t));
        state.absorption = this.clampHealth(this.absorption.interpolate(t), state.absorptionContainer);
        state.heartType = this.clampHeartType(this.heartType.interpolate(t));
        state.hardcore = this.interpolateHardcore(t);
        state.heartRegeneration = this.heartRegeneration.interpolate(t, false);
        state.armor = this.clampStat(this.armor.interpolate(t));
        state.hunger = this.clampStat(this.hunger.interpolate(t));
        state.hungerEffect = this.hungerEffect.interpolate(t, false);
        state.air = this.clampAir(this.air.interpolate(t));
        state.experience = this.clampExperience(this.experience.interpolate(t));
        state.experienceLevel = this.clampExperienceLevel(this.experienceLevel.interpolate(t));
        Vector4f layout = this.layout.interpolate(t, new Vector4f(0F, 0F, 1F, 0F));
        state.x = layout.x;
        state.y = layout.y;
        state.scale = Math.max(0.05F, layout.z);
        state.alpha = alpha;
        state.renderOrder = context.applied;

        getHotbars(context).add(state);
    }

    private class_1799 copyItem(class_1799 stack)
    {
        return stack == null ? class_1799.field_8037 : stack.method_7972();
    }

    private float clampStat(Double value)
    {
        return Math.max(0F, Math.min(20F, value.floatValue()));
    }

    private float clampHealth(Double value, float healthContainer)
    {
        return Math.max(0F, Math.min(healthContainer, value.floatValue()));
    }

    private int clampHeartType(Integer value)
    {
        return Math.max(HotbarState.HEART_NORMAL, Math.min(HotbarState.HEART_FROZEN, value));
    }

    private float clampHealthContainer(Double value)
    {
        return Math.max(0F, Math.min(MAX_HEALTH_CONTAINER, value.floatValue()));
    }

    private float clampExperience(Double value)
    {
        return Math.max(0F, Math.min(1F, value.floatValue()));
    }

    private float clampAir(Double value)
    {
        return Math.max(0F, Math.min(300F, value.floatValue()));
    }

    private int clampExperienceLevel(Integer value)
    {
        return Math.max(0, Math.min(9999, value));
    }

    @Override
    public void fromData(BaseType data)
    {
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
    private boolean interpolateHardcore(float tick)
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
