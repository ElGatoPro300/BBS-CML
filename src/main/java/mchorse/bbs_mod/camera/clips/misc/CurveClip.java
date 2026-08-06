package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValueChannels;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.HashMap;
import java.util.Map;

public class CurveClip extends CameraClip
{
    public static final String SHADER_CURVES_PREFIX = "curve.";
    public static final String CHROMA_SKY_ID = "chroma_sky";
    public static final String CHROMA_SKY_MARKER = "chroma_sky";

    public final ValueChannels channels = new ValueChannels("channels");
    public final KeyframeChannel<ChromaSkyCurveSettings> chromaSky = new KeyframeChannel<>(CHROMA_SKY_ID, KeyframeFactories.CHROMA_SKY_SETTINGS);

    public static Map<String, Double> getValues(ClipContext context)
    {
        return context.clipData.get("curve_data", HashMap::new);
    }

    public static ChromaSkyCurveSettings getChromaSkySettings(ClipContext context)
    {
        return context.clipData.get("curve_chroma_sky", ChromaSkyCurveSettings::new);
    }

    public CurveClip()
    {
        this.add(this.channels);
        this.add(this.chromaSky);
        this.channels.addChannel("sun_rotation");
        this.channels.addChannel("sun_path_rotation");
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        Map<String, Double> values = getValues(context);

        for (KeyframeChannel<Double> channel : this.channels.getChannels())
        {
            if (!channel.isEmpty())
            {
                values.put(channel.getId(), channel.interpolate(context.relativeTick + context.transition));
            }
        }

        if (!this.chromaSky.isEmpty())
        {
            ChromaSkyCurveSettings chromaSkySettings = this.chromaSky.interpolate(context.relativeTick + context.transition);
            ChromaSkyCurveSettings settings = getChromaSkySettings(context);

            values.put(CHROMA_SKY_MARKER, 1D);
            settings.enabled = chromaSkySettings.enabled;
            settings.color.copy(chromaSkySettings.color);
            settings.terrain = chromaSkySettings.terrain;
            settings.clouds = chromaSkySettings.clouds;
            settings.billboard = chromaSkySettings.billboard;
        }
    }

    @Override
    public boolean isPositionClip()
    {
        return false;
    }

    @Override
    protected Clip create()
    {
        return new CurveClip();
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isMap())
        {
            MapType map = data.asMap();

            if (map.has("key") && map.has("channel"))
            {
                ValueString key = new ValueString("key", "sun_rotation");

                key.fromData(map.get("key"));

                KeyframeChannel<Double> channel = this.channels.addChannel(key.get());

                channel.fromData(map.get("channel"));
            }
        }

        super.fromData(data);
    }
}
