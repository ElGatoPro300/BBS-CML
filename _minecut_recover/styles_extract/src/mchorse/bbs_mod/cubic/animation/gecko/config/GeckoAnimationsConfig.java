package mchorse.bbs_mod.cubic.animation.gecko.config;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GeckoAnimationsConfig implements IMapSerializable
{
    public boolean enabled;
    public boolean emitEvents = true;
    public float transitionSpeed = 0.35F;
    public float previewWheelSpeed = 1F;
    public final Map<String, String> stateAnimations = new HashMap<>();
    public final Map<String, GeckoLimbAnimationConfig> limbs = new HashMap<>();

    public GeckoAnimationsConfig()
    {
        this.stateAnimations.put("idle", "idle");
        this.stateAnimations.put("walk", "walk");
        this.stateAnimations.put("run", "run");
        this.stateAnimations.put("jump", "jump");
        this.stateAnimations.put("fall", "fall");
        this.stateAnimations.put("attack", "attack");
        this.stateAnimations.put("swim", "swim");
        this.stateAnimations.put("fly", "fly");
        this.stateAnimations.put("wheel", "wheel");
    }

    public boolean isDefault()
    {
        return !this.enabled || this.limbs.isEmpty();
    }

    public void copy(GeckoAnimationsConfig config)
    {
        this.enabled = config.enabled;
        this.emitEvents = config.emitEvents;
        this.transitionSpeed = config.transitionSpeed;
        this.previewWheelSpeed = config.previewWheelSpeed;
        this.stateAnimations.clear();
        this.stateAnimations.putAll(config.stateAnimations);
        this.limbs.clear();

        for (Map.Entry<String, GeckoLimbAnimationConfig> entry : config.limbs.entrySet())
        {
            this.limbs.put(entry.getKey(), entry.getValue().copy());
        }
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("enabled", this.enabled);
        data.putBool("emit_events", this.emitEvents);
        data.putFloat("transition_speed", this.transitionSpeed);
        data.putFloat("preview_wheel_speed", this.previewWheelSpeed);

        if (!this.stateAnimations.isEmpty())
        {
            MapType stateData = new MapType();

            for (Map.Entry<String, String> entry : this.stateAnimations.entrySet())
            {
                stateData.putString(entry.getKey(), entry.getValue());
            }

            data.put("states", stateData);
        }

        MapType limbData = new MapType();

        for (Map.Entry<String, GeckoLimbAnimationConfig> entry : this.limbs.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }

            MapType map = new MapType();
            entry.getValue().toData(map);
            limbData.put(entry.getKey(), map);
        }

        if (!limbData.isEmpty())
        {
            data.put("limbs", limbData);
        }
    }

    @Override
    public void fromData(MapType data)
    {
        this.enabled = data.getBool("enabled");
        this.emitEvents = data.getBool("emit_events", true);
        this.transitionSpeed = data.getFloat("transition_speed", 0.35F);
        this.previewWheelSpeed = data.getFloat("preview_wheel_speed", 1F);
        this.limbs.clear();
        this.stateAnimations.clear();
        this.stateAnimations.put("idle", "idle");
        this.stateAnimations.put("walk", "walk");
        this.stateAnimations.put("run", "run");
        this.stateAnimations.put("jump", "jump");
        this.stateAnimations.put("fall", "fall");
        this.stateAnimations.put("attack", "attack");
        this.stateAnimations.put("swim", "swim");
        this.stateAnimations.put("fly", "fly");
        this.stateAnimations.put("wheel", "wheel");

        if (data.has("states", BaseType.TYPE_MAP))
        {
            for (Map.Entry<String, BaseType> entry : data.getMap("states"))
            {
                if (entry.getValue().isString())
                {
                    this.stateAnimations.put(entry.getKey(), entry.getValue().asString());
                }
            }
        }

        if (!data.has("limbs", BaseType.TYPE_MAP))
        {
            return;
        }

        for (Map.Entry<String, BaseType> entry : data.getMap("limbs"))
        {
            if (!entry.getValue().isMap())
            {
                continue;
            }

            GeckoLimbAnimationConfig config = new GeckoLimbAnimationConfig();

            config.fromData(entry.getValue().asMap());
            this.limbs.put(entry.getKey(), config);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof GeckoAnimationsConfig config))
        {
            return false;
        }

        return this.enabled == config.enabled
            && this.emitEvents == config.emitEvents
            && Float.compare(this.transitionSpeed, config.transitionSpeed) == 0
            && Float.compare(this.previewWheelSpeed, config.previewWheelSpeed) == 0
            && Objects.equals(this.stateAnimations, config.stateAnimations)
            && Objects.equals(this.limbs, config.limbs);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.enabled, this.emitEvents, this.transitionSpeed, this.previewWheelSpeed, this.stateAnimations, this.limbs);
    }
}
