package mchorse.bbs_mod.cubic.animation;

import mchorse.bbs_mod.bobj.BOBJAction;
import mchorse.bbs_mod.bobj.BOBJChannel;
import mchorse.bbs_mod.bobj.BOBJGroup;
import mchorse.bbs_mod.bobj.BOBJKeyframe;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.gecko.routes.GeckoLimbRole;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.math.Constant;
import mchorse.bbs_mod.math.molang.MolangParser;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.math.molang.expressions.MolangValue;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProceduralDefaults
{
    private static final List<String> RIDING_ANIMATIONS = Arrays.asList("riding", "riding_idle");

    private static Animations cachedActionAnimations;

    private ProceduralDefaults()
    {}

    public static void ensureForModelInstance(ModelInstance instance, AssetProvider provider, MolangParser parser)
    {
        if (instance == null || !instance.procedural || instance.model == null)
        {
            return;
        }

        ensureRidingPose(instance);
        ensureSneakingPose(instance);
        ensureRidingAnimations(instance, provider, parser);
    }

    public static void ensureForConfig(ModelConfig config, IModel model)
    {
        if (config == null || !config.procedural.get() || model == null)
        {
            return;
        }

        if (config.ridingPose.get().isEmpty())
        {
            config.ridingPose.set(ProceduralPoseDefaults.applyRidingPoseToModel(model, ProceduralPoseDefaults.createStandardRidingPose()));
        }

        if (config.sneakingPose.get().isEmpty())
        {
            config.sneakingPose.set(ProceduralPoseDefaults.applyMappedPoseToModel(model, ProceduralPoseDefaults.createStandardSneakingPose()));
        }
    }

    public static void ensureRidingPose(ModelInstance instance)
    {
        if (instance.ridingPose.isEmpty() || !ProceduralPoseDefaults.hasRidingLimbTransforms(instance.ridingPose, instance.model))
        {
            instance.ridingPose = ProceduralPoseDefaults.applyRidingPoseToModel(instance.model, ProceduralPoseDefaults.createStandardRidingPose());
        }
    }

    public static void ensureSneakingPose(ModelInstance instance)
    {
        if (instance.sneakingPose.isEmpty())
        {
            instance.sneakingPose = ProceduralPoseDefaults.applyMappedPoseToModel(instance.model, ProceduralPoseDefaults.createStandardSneakingPose());
        }
    }

    public static void ensureRidingAnimations(ModelInstance instance, AssetProvider provider, MolangParser parser)
    {
        if (instance.animations == null || instance.model == null)
        {
            return;
        }

        Animations source = getActionAnimations(provider, parser);

        if (source == null)
        {
            return;
        }

        Map<GeckoLimbRole, String> roleBones = ProceduralPoseDefaults.buildRoleBoneMap(instance.model);

        for (String animationId : RIDING_ANIMATIONS)
        {
            if (instance.animations.get(animationId) != null)
            {
                continue;
            }

            Animation sourceAnimation = source.get(animationId);

            if (sourceAnimation == null)
            {
                continue;
            }

            boolean cubicModel = instance.model instanceof Model;
            Animation remapped = remapAnimation(sourceAnimation, roleBones, parser, cubicModel);

            if (!remapped.parts.isEmpty())
            {
                instance.animations.add(remapped);
            }
        }
    }

    private static Animations getActionAnimations(AssetProvider provider, MolangParser parser)
    {
        if (cachedActionAnimations != null)
        {
            return cachedActionAnimations;
        }

        Animations animations = new Animations(parser);

        try (InputStream stream = provider.getAsset(Link.assets("actions.bobj")))
        {
            BOBJLoader.BOBJData bobjData = BOBJLoader.readData(stream);

            for (Map.Entry<String, BOBJAction> entry : bobjData.actions.entrySet())
            {
                if (!RIDING_ANIMATIONS.contains(entry.getKey()))
                {
                    continue;
                }

                Animation animation = new Animation(entry.getKey(), parser);

                fillAnimation(animation, entry.getValue(), parser);
                animations.add(animation);
            }

            cachedActionAnimations = animations;
        }
        catch (Exception e)
        {
            System.err.println("Failed to load procedural riding animations from actions.bobj!");
            e.printStackTrace();
        }

        return cachedActionAnimations;
    }

    private static Animation remapAnimation(Animation source, Map<GeckoLimbRole, String> roleBones, MolangParser parser, boolean cubicModel)
    {
        Animation animation = new Animation(source.id, parser);

        animation.setLength(source.getLength());

        for (Map.Entry<String, AnimationPart> entry : source.parts.entrySet())
        {
            GeckoLimbRole role = ProceduralPoseDefaults.resolveSourceRole(entry.getKey());

            if (role == GeckoLimbRole.OTHER)
            {
                continue;
            }

            String targetBone = roleBones.get(role);

            if (targetBone == null)
            {
                continue;
            }

            AnimationPart part = copyAnimationPart(entry.getValue(), parser, cubicModel);
            String childBone = ProceduralPoseDefaults.lowChildBoneForRole(role);
            AnimationPart childPart = childBone == null ? null : source.parts.get(childBone);

            if (childPart != null)
            {
                mergeChildRotations(part, childPart, cubicModel);
            }

            animation.parts.put(targetBone, part);
        }

        return animation;
    }

    private static AnimationPart copyAnimationPart(AnimationPart source, MolangParser parser, boolean cubicModel)
    {
        AnimationPart copy = new AnimationPart(parser);
        float locationScale = cubicModel ? 16F : 1F;
        float rotationScale = cubicModel ? MathUtils.toDeg(1F) : 1F;

        /* Animation channels use a null keyframe factory; KeyframeChannel.copyKeyframes() NPEs on Molang values */
        copyChannelKeyframes(copy.x, source.x, locationScale);
        copyChannelKeyframes(copy.y, source.y, locationScale);
        copyChannelKeyframes(copy.z, source.z, locationScale);
        copyChannelKeyframes(copy.sx, source.sx, 1F);
        copyChannelKeyframes(copy.sy, source.sy, 1F);
        copyChannelKeyframes(copy.sz, source.sz, 1F);
        copyChannelKeyframes(copy.rx, source.rx, rotationScale);
        copyChannelKeyframes(copy.ry, source.ry, rotationScale);
        copyChannelKeyframes(copy.rz, source.rz, rotationScale);

        return copy;
    }

    private static void mergeChildRotations(AnimationPart target, AnimationPart child, boolean cubicModel)
    {
        float rotationScale = cubicModel ? MathUtils.toDeg(1F) : 1F;

        addRotationOffset(target.rx, child.rx, rotationScale);
        addRotationOffset(target.ry, child.ry, rotationScale);
        addRotationOffset(target.rz, child.rz, rotationScale);
    }

    private static void addRotationOffset(KeyframeChannel<MolangExpression> target, KeyframeChannel<MolangExpression> child, float scale)
    {
        double offset = getConstantChannelValue(child, scale);

        if (offset == 0D)
        {
            return;
        }

        for (Keyframe<MolangExpression> keyframe : target.getKeyframes())
        {
            double value = getConstantExpressionValue(keyframe.getValue());

            if (Double.isNaN(value))
            {
                continue;
            }

            keyframe.setValue(replaceConstantValue(keyframe.getValue(), value + offset));
        }
    }

    private static double getConstantChannelValue(KeyframeChannel<MolangExpression> channel, float scale)
    {
        if (channel.getKeyframes().isEmpty())
        {
            return 0D;
        }

        double value = getConstantExpressionValue(channel.getKeyframes().get(0).getValue());

        return Double.isNaN(value) ? 0D : value * scale;
    }

    private static double getConstantExpressionValue(MolangExpression expression)
    {
        if (expression instanceof MolangValue molangValue && molangValue.expression instanceof Constant constant && constant.isNumber())
        {
            return constant.doubleValue();
        }

        return Double.NaN;
    }

    private static void copyChannelKeyframes(KeyframeChannel<MolangExpression> target, KeyframeChannel<MolangExpression> source, float scale)
    {
        for (Keyframe<MolangExpression> keyframe : source.getKeyframes())
        {
            MolangExpression value = scaleKeyframeValue(keyframe.getValue(), scale);
            int index = target.insert(keyframe.getTick(), value);
            Keyframe<MolangExpression> copy = target.get(index);

            copy.getInterpolation().setInterp(keyframe.getInterpolation().getInterp());
            copy.lx = keyframe.lx;
            copy.ly = keyframe.ly * scale;
            copy.rx = keyframe.rx;
            copy.ry = keyframe.ry * scale;
            copy.setDuration(keyframe.getDuration());
            copy.setShape(keyframe.getShape());
            copy.setColor(keyframe.getColor());
        }

        target.sort();
    }

    private static MolangExpression scaleKeyframeValue(MolangExpression expression, float scale)
    {
        if (scale == 1F || expression == null)
        {
            return expression;
        }

        double value = getConstantExpressionValue(expression);

        if (Double.isNaN(value))
        {
            return expression;
        }

        return replaceConstantValue(expression, value * scale);
    }

    private static MolangExpression replaceConstantValue(MolangExpression expression, double value)
    {
        if (expression instanceof MolangValue molangValue)
        {
            return new MolangValue(molangValue.context, new Constant(value));
        }

        return expression;
    }

    private static void fillAnimation(Animation animation, BOBJAction value, MolangParser parser)
    {
        for (Map.Entry<String, BOBJGroup> entry : value.groups.entrySet())
        {
            AnimationPart part = new AnimationPart(parser);

            for (BOBJChannel channel : entry.getValue().channels)
            {
                if (channel.path.equals("location"))
                {
                    if (channel.index == 0)
                    {
                        copyKeyframes(parser, part.x, channel);
                    }
                    else if (channel.index == 1)
                    {
                        copyKeyframes(parser, part.y, channel);
                    }
                    else if (channel.index == 2)
                    {
                        copyKeyframes(parser, part.z, channel);
                    }
                }
                else if (channel.path.equals("scale"))
                {
                    if (channel.index == 0)
                    {
                        copyKeyframes(parser, part.sx, channel);
                    }
                    else if (channel.index == 1)
                    {
                        copyKeyframes(parser, part.sy, channel);
                    }
                    else if (channel.index == 2)
                    {
                        copyKeyframes(parser, part.sz, channel);
                    }
                }
                else
                {
                    if (channel.index == 0)
                    {
                        copyKeyframes(parser, part.rx, channel);
                    }
                    else if (channel.index == 1)
                    {
                        copyKeyframes(parser, part.ry, channel);
                    }
                    else if (channel.index == 2)
                    {
                        copyKeyframes(parser, part.rz, channel);
                    }
                }
            }

            animation.parts.put(entry.getKey(), part);
        }

        animation.setLength(value.getDuration() / 20F);
    }

    private static void copyKeyframes(MolangParser parser, KeyframeChannel<MolangExpression> keyframeChannel, BOBJChannel channel)
    {
        for (int i = 0, c = channel.keyframes.size(); i < c; i++)
        {
            BOBJKeyframe a = channel.keyframes.get(i);
            BOBJKeyframe b = a;

            if (i - 1 >= 0)
            {
                b = channel.keyframes.get(i - 1);
            }

            MolangValue value = new MolangValue(parser, new Constant(a.value));
            int index = keyframeChannel.insert(a.frame, value);
            Keyframe<MolangExpression> keyframe = keyframeChannel.get(index);

            keyframe.getInterpolation().setInterp(b.interpolation.interp);
            keyframe.lx = a.frame - a.leftX;
            keyframe.ly = a.leftY - a.value;
            keyframe.rx = a.rightX - a.frame;
            keyframe.ry = a.rightY - a.value;
        }

        keyframeChannel.sort();
    }
}
