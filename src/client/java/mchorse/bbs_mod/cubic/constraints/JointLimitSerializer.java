package mchorse.bbs_mod.cubic.constraints;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads and writes {@link JointLimitConfig} from/to the mod's MapType
 * serialization format.
 *
 * <p>Format:
 * <pre>
 * {
 *   "joints": {
 *     "bone_name": {
 *       "active": true,
 *       "lower": [minX, minY, minZ],
 *       "upper": [maxX, maxY, maxZ]
 *     }
 *   }
 * }
 * </pre>
 */
public final class JointLimitSerializer
{
    private static final String KEY_JOINTS = "joints";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_LOWER = "lower";
    private static final String KEY_UPPER = "upper";

    private static final float DEFAULT_LOWER = -180F;
    private static final float DEFAULT_UPPER = 180F;

    private JointLimitSerializer()
    {
    }

    public static JointLimitConfig deserialize(MapType root)
    {
        if (root == null || !root.has(KEY_JOINTS, BaseType.TYPE_MAP))
        {
            return null;
        }

        MapType jointsMap = root.getMap(KEY_JOINTS);
        Map<String, JointLimitConfig.JointLimit> result = new HashMap<>();

        for (String boneName : jointsMap.keys())
        {
            if (!jointsMap.has(boneName, BaseType.TYPE_MAP))
            {
                continue;
            }

            MapType entry = jointsMap.getMap(boneName);
            boolean active = entry.getBool(KEY_ACTIVE, true);

            if (!active)
            {
                continue;
            }

            float minX = DEFAULT_LOWER;
            float minY = DEFAULT_LOWER;
            float minZ = DEFAULT_LOWER;
            float maxX = DEFAULT_UPPER;
            float maxY = DEFAULT_UPPER;
            float maxZ = DEFAULT_UPPER;

            if (entry.has(KEY_LOWER, BaseType.TYPE_LIST))
            {
                ListType lower = entry.getList(KEY_LOWER);

                minX = readFloat(lower, 0, DEFAULT_LOWER);
                minY = readFloat(lower, 1, DEFAULT_LOWER);
                minZ = readFloat(lower, 2, DEFAULT_LOWER);
            }

            if (entry.has(KEY_UPPER, BaseType.TYPE_LIST))
            {
                ListType upper = entry.getList(KEY_UPPER);

                maxX = readFloat(upper, 0, DEFAULT_UPPER);
                maxY = readFloat(upper, 1, DEFAULT_UPPER);
                maxZ = readFloat(upper, 2, DEFAULT_UPPER);
            }

            result.put(boneName, new JointLimitConfig.JointLimit(true, minX, minY, minZ, maxX, maxY, maxZ));
        }

        return result.isEmpty() ? null : new JointLimitConfig(result);
    }

    public static MapType serialize(JointLimitConfig config)
    {
        MapType root = new MapType();
        MapType jointsMap = new MapType();

        if (config != null && config.joints() != null)
        {
            for (Map.Entry<String, JointLimitConfig.JointLimit> entry : config.joints().entrySet())
            {
                String boneName = entry.getKey();
                JointLimitConfig.JointLimit limit = entry.getValue();

                if (boneName == null || boneName.isEmpty() || limit == null || !limit.active())
                {
                    continue;
                }

                MapType boneMap = new MapType();
                boneMap.putBool(KEY_ACTIVE, true);

                ListType lower = new ListType();
                lower.addFloat(limit.minX());
                lower.addFloat(limit.minY());
                lower.addFloat(limit.minZ());

                ListType upper = new ListType();
                upper.addFloat(limit.maxX());
                upper.addFloat(limit.maxY());
                upper.addFloat(limit.maxZ());

                boneMap.put(KEY_LOWER, lower);
                boneMap.put(KEY_UPPER, upper);

                jointsMap.put(boneName, boneMap);
            }
        }

        root.put(KEY_JOINTS, jointsMap);

        return root;
    }

    private static float readFloat(ListType list, int index, float fallback)
    {
        BaseType element = list == null ? null : list.get(index);

        if (BaseType.isNumeric(element))
        {
            return element.asNumeric().floatValue();
        }

        return fallback;
    }
}
