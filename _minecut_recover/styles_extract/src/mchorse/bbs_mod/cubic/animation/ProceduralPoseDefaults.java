package mchorse.bbs_mod.cubic.animation;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.animation.gecko.routes.GeckoAnimationRouteRegistry;
import mchorse.bbs_mod.cubic.animation.gecko.routes.GeckoLimbRole;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;

import java.util.EnumMap;
import java.util.Map;

public class ProceduralPoseDefaults
{
    private static final GeckoAnimationRouteRegistry ROUTES = new GeckoAnimationRouteRegistry();

    private ProceduralPoseDefaults()
    {}

    /**
     * Minecraft player riding sit pose (arms forward, legs bent to sides, anchor lowered).
     */
    public static Pose createStandardRidingPose()
    {
        Pose pose = new Pose();

        pose.get("left_arm").rotate.set(0.5235988F, 0F, 0F);
        pose.get("right_arm").rotate.set(0.5235988F, 0F, 0F);

        pose.get("left_leg").rotate.set(1.5707964F, 0.43633232F, 0F);
        pose.get("right_leg").rotate.set(1.5707964F, -0.43633235F, 0F);

        pose.get("anchor").translate.set(0F, -10F, 0F);

        return pose;
    }

    /**
     * Default sneaking pose from the steve player template.
     */
    public static Pose createStandardSneakingPose()
    {
        Pose pose = new Pose();

        pose.get("head").translate.set(0F, -4.2F, 0F);

        pose.get("left_arm").rotate.set(-0.39999452F, 0F, 0F);
        pose.get("left_arm").translate.set(0F, -3.2F, 0F);

        pose.get("right_arm").rotate.set(-0.39999452F, 0F, 0F);
        pose.get("right_arm").translate.set(0F, -3.2F, 0F);

        pose.get("torso").rotate.set(-0.50000197F, 0F, 0F);
        pose.get("torso").translate.set(0F, -3.2F, 0F);

        pose.get("left_leg").translate.set(0F, -0.2F, 4F);
        pose.get("right_leg").translate.set(0F, -0.2F, 4F);

        return pose;
    }

    /**
     * Vanilla Minecraft shield blocking pose (offhand shield).
     */
    public static Pose createStandardBlockingPose()
    {
        Pose pose = new Pose();

        pose.get("left_arm").rotate.set(0.7853982F, -0.4363323F, -0.2617994F);

        return pose;
    }

    /**
     * Vanilla Minecraft sleeping pose (horizontal lying pose).
     */
    public static Pose createStandardSleepingPose()
    {
        Pose pose = new Pose();

        pose.get("torso").rotate.set(1.5707964F, 0F, 0F);
        pose.get("head").rotate.set(0F, 0F, 0F);
        pose.get("left_arm").rotate.set(1.5707964F, 0F, 0F);
        pose.get("right_arm").rotate.set(1.5707964F, 0F, 0F);
        pose.get("left_leg").rotate.set(1.5707964F, 0F, 0F);
        pose.get("right_leg").rotate.set(1.5707964F, 0F, 0F);

        pose.get("anchor").translate.set(0F, -18F, 0F);

        return pose;
    }

    /**
     * Vanilla Minecraft crawling pose (face-down ground posture).
     */
    public static Pose createStandardCrawlingPose()
    {
        Pose pose = new Pose();

        pose.get("torso").rotate.set(1.5707964F, 0F, 0F);
        pose.get("head").translate.set(0F, -12F, 0F);
        pose.get("left_arm").rotate.set(3.1415927F, 0F, 0F);
        pose.get("right_arm").rotate.set(3.1415927F, 0F, 0F);

        pose.get("anchor").translate.set(0F, -18F, 0F);

        return pose;
    }

    /**
     * Vanilla Minecraft swimming pose (horizontal water posture).
     */
    public static Pose createStandardSwimmingPose()
    {
        Pose pose = new Pose();

        pose.get("torso").rotate.set(1.5707964F, 0F, 0F);
        pose.get("left_arm").rotate.set(3.1415927F, 0F, 0F);
        pose.get("right_arm").rotate.set(3.1415927F, 0F, 0F);

        pose.get("anchor").translate.set(0F, -14F, 0F);

        return pose;
    }

    /**
     * Vanilla Minecraft elytra gliding pose.
     */
    public static Pose createStandardGlidingPose()
    {
        Pose pose = new Pose();

        pose.get("torso").rotate.set(1.5707964F, 0F, 0F);
        pose.get("left_arm").rotate.set(3.1415927F, 0F, 0F);
        pose.get("right_arm").rotate.set(3.1415927F, 0F, 0F);

        pose.get("anchor").translate.set(0F, -14F, 0F);

        return pose;
    }

    public static Pose applyRidingPoseToModel(IModel model, Pose sourcePose)
    {
        Pose output = applyMappedPoseToModel(model, sourcePose);
        PoseTransform anchorSource = sourcePose.transforms.get("anchor");

        if (anchorSource == null || anchorSource.isDefault())
        {
            return output;
        }

        String anchorBone = model.getAnchor();

        if (anchorBone == null || anchorBone.isEmpty())
        {
            anchorBone = "anchor";
        }

        if (model.getAllGroupKeys().contains(anchorBone) || hasBOBJBone(model, anchorBone))
        {
            output.transforms.put(anchorBone, (PoseTransform) anchorSource.copy());
        }

        return output;
    }

    private static boolean hasBOBJBone(IModel model, String bone)
    {
        for (BOBJBone bobjBone : model.getAllBOBJBones())
        {
            if (bobjBone.name.equals(bone))
            {
                return true;
            }
        }

        return false;
    }

    public static Pose applyMappedPoseToModel(IModel model, Pose sourcePose)
    {
        Pose output = new Pose();
        Map<GeckoLimbRole, String> roleBones = buildRoleBoneMap(model);

        for (Map.Entry<GeckoLimbRole, String> entry : roleBones.entrySet())
        {
            String standardBone = standardBoneForRole(entry.getKey());

            if (standardBone == null)
            {
                continue;
            }

            PoseTransform source = sourcePose.transforms.get(standardBone);

            if (source == null || source.isDefault())
            {
                continue;
            }

            output.transforms.put(entry.getValue(), (PoseTransform) source.copy());
        }

        return output;
    }

    public static Map<GeckoLimbRole, String> buildRoleBoneMap(IModel model)
    {
        EnumMap<GeckoLimbRole, String> map = new EnumMap<>(GeckoLimbRole.class);

        for (String bone : model.getAllGroupKeys())
        {
            thisPutRoleBone(map, bone);
        }

        for (BOBJBone bone : model.getAllBOBJBones())
        {
            thisPutRoleBone(map, bone.name);
        }

        return map;
    }

    private static void thisPutRoleBone(Map<GeckoLimbRole, String> map, String bone)
    {
        GeckoLimbRole role = ROUTES.resolve(bone);

        if (role != GeckoLimbRole.OTHER && !map.containsKey(role))
        {
            map.put(role, bone);
        }
    }

    public static String standardBoneForRole(GeckoLimbRole role)
    {
        if (role == null)
        {
            return null;
        }

        switch (role)
        {
            case HEAD:
                return "head";
            case LEFT_ARM:
                return "left_arm";
            case RIGHT_ARM:
                return "right_arm";
            case TORSO:
                return "torso";
            case LEFT_LEG:
                return "left_leg";
            case RIGHT_LEG:
                return "right_leg";
            default:
                return null;
        }
    }

    public static String resolveSourceBoneName(String sourceBone)
    {
        if (sourceBone == null)
        {
            return null;
        }

        if (sourceBone.equals("low_body"))
        {
            return "torso";
        }

        if (sourceBone.startsWith("low_") || sourceBone.equals("anchor"))
        {
            return null;
        }

        return sourceBone;
    }

    public static GeckoLimbRole resolveSourceRole(String sourceBone)
    {
        String lookup = resolveSourceBoneName(sourceBone);

        if (lookup == null)
        {
            return GeckoLimbRole.OTHER;
        }

        return ROUTES.resolve(lookup);
    }

    public static String lowChildBoneForRole(GeckoLimbRole role)
    {
        if (role == null)
        {
            return null;
        }

        switch (role)
        {
            case LEFT_ARM:
                return "low_left_arm";
            case RIGHT_ARM:
                return "low_right_arm";
            case LEFT_LEG:
                return "low_left_leg";
            case RIGHT_LEG:
                return "low_leg_right";
            case TORSO:
                return "low_body";
            default:
                return null;
        }
    }

    public static boolean hasRidingLimbTransforms(Pose pose, IModel model)
    {
        if (pose == null || pose.isEmpty())
        {
            return false;
        }

        Map<GeckoLimbRole, String> roleBones = buildRoleBoneMap(model);
        GeckoLimbRole[] limbs = {
            GeckoLimbRole.LEFT_ARM,
            GeckoLimbRole.RIGHT_ARM,
            GeckoLimbRole.LEFT_LEG,
            GeckoLimbRole.RIGHT_LEG
        };

        for (GeckoLimbRole role : limbs)
        {
            String bone = roleBones.get(role);

            if (bone == null)
            {
                continue;
            }

            PoseTransform transform = pose.transforms.get(bone);

            if (transform != null && !transform.isDefault())
            {
                return true;
            }
        }

        return false;
    }
}
