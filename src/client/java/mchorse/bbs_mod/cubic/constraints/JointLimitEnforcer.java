package mchorse.bbs_mod.cubic.constraints;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Applies {@link JointLimitConfig} constraints to model bones by clamping
 * their euler rotations to the configured per-axis min/max range.
 *
 * <p>Supports both Cubic ({@link Model}) and BOBJ ({@link BOBJModel}) skeletons.
 * Results are cached per MapType instance to avoid redundant deserialization.
 */
public final class JointLimitEnforcer
{
    private static final WeakHashMap<MapType, Map<String, JointLimitConfig.JointLimit>> CACHE = new WeakHashMap<>();

    private JointLimitEnforcer()
    {
    }

    public static void clearCache()
    {
        CACHE.clear();
    }

    /**
     * Retrieves the joint limits for the given instance, deserializing and
     * caching if needed.
     */
    public static Map<String, JointLimitConfig.JointLimit> getJoints(ModelInstance instance)
    {
        MapType map = null;

        if (instance != null && instance.form instanceof ModelForm form && form.constraints.get() instanceof MapType m)
        {
            map = m;
        }
        else if (instance != null && instance.jointLimits != null)
        {
            map = instance.jointLimits;
        }

        if (map != null)
        {
            Map<String, JointLimitConfig.JointLimit> cached = CACHE.get(map);

            if (cached != null)
            {
                return cached;
            }

            JointLimitConfig config = JointLimitSerializer.deserialize(map);

            Map<String, JointLimitConfig.JointLimit> joints = config == null || config.joints() == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(config.joints()));

            CACHE.put(map, joints);

            return joints;
        }

        return Collections.emptyMap();
    }

    /**
     * Applies joint limits to all constrained bones in the model instance.
     * Call this after FK animation and before rendering.
     */
    public static void enforce(ModelInstance instance)
    {
        if (instance == null || instance.model == null)
        {
            return;
        }

        Map<String, JointLimitConfig.JointLimit> joints = getJoints(instance);

        if (joints.isEmpty())
        {
            return;
        }

        if (instance.model instanceof Model cubicModel)
        {
            enforceCubic(cubicModel, joints);
        }
        else if (instance.model instanceof BOBJModel bobjModel)
        {
            enforceBobj(bobjModel, joints);
        }
    }

    private static void enforceCubic(Model model, Map<String, JointLimitConfig.JointLimit> joints)
    {
        for (ModelGroup group : model.getAllGroups())
        {
            if (group == null)
            {
                continue;
            }

            JointLimitConfig.JointLimit limit = joints.get(group.id);

            if (limit == null || !limit.active())
            {
                continue;
            }

            float lx = limit.minX();
            float ly = limit.minY();
            float lz = limit.minZ();
            float ux = limit.maxX();
            float uy = limit.maxY();
            float uz = limit.maxZ();

            /* Ensure min <= max */
            if (lx > ux) { float t = lx; lx = ux; ux = t; }
            if (ly > uy) { float t = ly; ly = uy; uy = t; }
            if (lz > uz) { float t = lz; lz = uz; uz = t; }

            group.current.rotate.x = MathUtils.clamp(group.current.rotate.x, lx, ux);
            group.current.rotate.y = MathUtils.clamp(group.current.rotate.y, ly, uy);
            group.current.rotate.z = MathUtils.clamp(group.current.rotate.z, lz, uz);

            /* Clamping finalizes in euler — drop any composed quaternion orientation */
            group.orient = null;
        }
    }

    private static void enforceBobj(BOBJModel model, Map<String, JointLimitConfig.JointLimit> joints)
    {
        for (BOBJBone bone : model.getArmature().orderedBones)
        {
            if (bone == null)
            {
                continue;
            }

            JointLimitConfig.JointLimit limit = joints.get(bone.name);

            if (limit == null || !limit.active())
            {
                continue;
            }

            /* BOBJ rotations are in radians, limits are in degrees */
            float lx = (float) Math.toRadians(limit.minX());
            float ly = (float) Math.toRadians(limit.minY());
            float lz = (float) Math.toRadians(limit.minZ());
            float ux = (float) Math.toRadians(limit.maxX());
            float uy = (float) Math.toRadians(limit.maxY());
            float uz = (float) Math.toRadians(limit.maxZ());

            if (lx > ux) { float t = lx; lx = ux; ux = t; }
            if (ly > uy) { float t = ly; ly = uy; uy = t; }
            if (lz > uz) { float t = lz; lz = uz; uz = t; }

            bone.transform.rotate.x = MathUtils.clamp(bone.transform.rotate.x, lx, ux);
            bone.transform.rotate.y = MathUtils.clamp(bone.transform.rotate.y, ly, uy);
            bone.transform.rotate.z = MathUtils.clamp(bone.transform.rotate.z, lz, uz);
            bone.orient = null;
        }
    }
}
