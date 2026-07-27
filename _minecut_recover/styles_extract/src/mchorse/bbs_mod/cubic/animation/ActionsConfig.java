package mchorse.bbs_mod.cubic.animation;

import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoAnimationModuleConfig;
import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoAnimationsConfig;
import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class ActionsConfig implements IMapSerializable
{
    private static final String GECKO_JS_DATA_KEY = "gecko_animations_js";
    private static Map<String, ActionConfig> a = new HashMap<>();
    private static Map<String, ActionConfig> b = new HashMap<>();

    public Map<String, ActionConfig> actions = new HashMap<>();
    public GeckoAnimationsConfig geckoAnimations = new GeckoAnimationsConfig();
    public String geckoAnimationsJavascript = "";

    public static void removeDefaultActions(Map<String, ActionConfig> map)
    {
        Iterator<Map.Entry<String, ActionConfig>> it = map.entrySet().iterator();

        while (it.hasNext())
        {
            Map.Entry<String, ActionConfig> entry = it.next();
            String key = entry.getKey();
            ActionConfig config = entry.getValue();

            if (config.isDefault(key))
            {
                it.remove();
            }
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof ActionsConfig)
        {
            ActionsConfig config = (ActionsConfig) obj;

            a.clear();
            a.putAll(this.actions);
            b.clear();
            b.putAll(config.actions);

            removeDefaultActions(a);
            removeDefaultActions(b);

            return a.equals(b)
                && this.geckoAnimations.equals(config.geckoAnimations)
                && Objects.equals(this.geckoAnimationsJavascript, config.geckoAnimationsJavascript);
        }

        return false;
    }

    public void copy(ActionsConfig config)
    {
        this.actions.clear();
        this.geckoAnimations.copy(config.geckoAnimations);
        this.geckoAnimationsJavascript = config.geckoAnimationsJavascript;

        for (Map.Entry<String, ActionConfig> entry : config.actions.entrySet())
        {
            this.actions.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public ActionConfig getConfig(String key)
    {
        ActionConfig output = this.actions.get(key);

        return output == null ? new ActionConfig(key) : output;
    }

    @Override
    public void toData(MapType data)
    {
        if (!this.geckoAnimations.isDefault())
        {
            data.put(GeckoAnimationModuleConfig.DATA_KEY, this.geckoAnimations.toData());

            if (this.geckoAnimationsJavascript != null && !this.geckoAnimationsJavascript.isBlank())
            {
                data.putString(GECKO_JS_DATA_KEY, this.geckoAnimationsJavascript);
            }
        }

        for (Map.Entry<String, ActionConfig> entry : this.actions.entrySet())
        {
            if (entry.getValue().isDefault())
            {
                if (!entry.getValue().name.equals(entry.getKey()))
                {
                    data.putString(entry.getKey(), entry.getValue().name);
                }
            }
            else
            {
                data.put(entry.getKey(), entry.getValue().toData());
            }
        }
    }

    @Override
    public void fromData(MapType data)
    {
        this.actions.clear();
        this.geckoAnimations = new GeckoAnimationsConfig();

        if (data.has(GeckoAnimationModuleConfig.DATA_KEY, BaseType.TYPE_MAP))
        {
            this.geckoAnimations.fromData(data.getMap(GeckoAnimationModuleConfig.DATA_KEY));
            this.geckoAnimationsJavascript = data.getString(GECKO_JS_DATA_KEY);
        }
        else
        {
            this.geckoAnimationsJavascript = "";
        }

        for (Map.Entry<String, BaseType> entry : data)
        {
            if (entry.getKey().equals(GeckoAnimationModuleConfig.DATA_KEY) || entry.getKey().equals(GECKO_JS_DATA_KEY))
            {
                continue;
            }

            if (entry.getValue().isMap())
            {
                ActionConfig action = new ActionConfig();

                action.fromData(entry.getValue().asMap());
                this.actions.put(entry.getKey(), action);
            }
            else if (entry.getValue().isString())
            {
                ActionConfig action = new ActionConfig();

                action.name = entry.getValue().asString();

                this.actions.put(entry.getKey(), action);
            }
        }
    }
}
