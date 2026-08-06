package mchorse.bbs_mod.utils.keyframes;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/**
 * Double keyframe channel that can dual-write integer (legacy) + precise double
 * data so older BBS builds keep loading the channel while newer builds restore
 * full float precision from {@link #PRECISE_KEYFRAMES}.
 */
public class CompatibleDoubleKeyframeChannel extends KeyframeChannel<Double>
{
    public static final String PRECISE_KEYFRAMES = "keyframes_precise";

    public CompatibleDoubleKeyframeChannel(String id)
    {
        super(id, KeyframeFactories.DOUBLE);
    }

    @Override
    public BaseType toData()
    {
        if (!BBSSettings.isSaveAsCompatible())
        {
            return super.toData();
        }

        MapType data = new MapType();
        ListType integerKeyframes = new ListType();
        ListType preciseKeyframes = new ListType();

        for (Keyframe<Double> keyframe : this.getKeyframes())
        {
            BaseType precise = keyframe.toData();

            preciseKeyframes.add(precise);

            if (precise != null && precise.isMap())
            {
                MapType integerData = (MapType) precise.copy();
                Double value = keyframe.getValue();
                int rounded = value == null ? 0 : (int) Math.round(value);

                integerData.putInt("value", rounded);
                integerKeyframes.add(integerData);
            }
        }

        /* Primary payload stays integer-typed so older builds that only know
         * integer lineHeight/maxWidth channels do not ClassCastException. */
        data.putString("type", "integer");
        data.put("keyframes", integerKeyframes);
        data.put(PRECISE_KEYFRAMES, preciseKeyframes);

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data != null && data.isMap())
        {
            MapType map = data.asMap();

            if (map.has(PRECISE_KEYFRAMES))
            {
                MapType precise = new MapType();

                precise.putString("type", "double");
                precise.put("keyframes", map.getList(PRECISE_KEYFRAMES));
                super.fromData(precise);

                return;
            }
        }

        super.fromData(data);
    }
}
