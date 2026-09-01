package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;

import org.joml.Matrix4f;

import java.util.Set;

/**
 * Optional feathering between per-bone color masks on BOBJ / emoticon meshes. Boundary
 * triangles blend uniforms from nearby weighted bones instead of a hard dominant-bone split.
 */
public final class BobjBoneColorSoftening
{
    private static final float SECONDARY_INFLUENCE = 0.12F;

    private static final BOBJBone BLEND_BONE = new BOBJBone(-1, "", null, new Matrix4f());
    private static final float[] TRIANGLE_WEIGHTS = new float[256];
    private static final Color BLEND_DRAW_COLOR = new Color();

    private BobjBoneColorSoftening()
    {}

    public static boolean isEnabled()
    {
        return BBSSettings.shouldSoftenBobjBoneColorMasks();
    }

    public static boolean isSoftBoundaryTriangle(BOBJLoader.CompiledData data, int[] dominantBonePerTriangle, Set<Integer> colorOverrideBones, int triangle)
    {
        if (data == null || colorOverrideBones == null || colorOverrideBones.isEmpty())
        {
            return false;
        }

        int base = triangle * 3;
        int dominant0 = getDominantBoneForVertex(data, base);
        int dominant1 = getDominantBoneForVertex(data, base + 1);
        int dominant2 = getDominantBoneForVertex(data, base + 2);

        boolean hasCustomDominant = colorOverrideBones.contains(dominant0)
            || colorOverrideBones.contains(dominant1)
            || colorOverrideBones.contains(dominant2);

        if (!hasCustomDominant && !hasAnyCustomInfluenceOnTriangle(data, colorOverrideBones, base))
        {
            return false;
        }

        if (!hasCustomDominant)
        {
            return hasSecondaryCustomInfluence(data, colorOverrideBones, base, dominant0);
        }

        if (dominant0 != dominant1 || dominant0 != dominant2)
        {
            return true;
        }

        return hasSecondaryCustomInfluence(data, colorOverrideBones, base, dominant0);
    }

    public static void computeTriangleBoneWeights(BOBJLoader.CompiledData data, int triangle, float[] weightsOut)
    {
        int length = weightsOut.length;

        for (int i = 0; i < length; i++)
        {
            weightsOut[i] = 0F;
        }

        for (int vertex = 0; vertex < 3; vertex++)
        {
            int vertexIndex = triangle * 3 + vertex;
            int base = vertexIndex * 4;

            for (int slot = 0; slot < 4; slot++)
            {
                int boneIndex = data.boneIndexData[base + slot];
                float weight = data.weightData[base + slot];

                if (boneIndex >= 0 && boneIndex < length && weight > 0F)
                {
                    weightsOut[boneIndex] += weight / 3F;
                }
            }
        }
    }

    public static void applyBlendedGroupUniforms(BOBJArmature armature, float[] weights, Set<Integer> colorOverrideBones)
    {
        float total = 0F;

        for (int boneIndex : colorOverrideBones)
        {
            if (boneIndex >= 0 && boneIndex < weights.length)
            {
                total += weights[boneIndex];
            }
        }

        if (total <= 0.0001F)
        {
            return;
        }

        resetBlendBone();

        for (int boneIndex : colorOverrideBones)
        {
            if (boneIndex < 0 || boneIndex >= weights.length)
            {
                continue;
            }

            float weight = weights[boneIndex] / total;

            if (weight <= 0.0001F)
            {
                continue;
            }

            BOBJBone bone = findBone(armature, boneIndex);

            if (bone == null)
            {
                continue;
            }

            accumulateBoneState(BLEND_BONE, bone, weight);
        }

        finalizeBlendBone();
        BobjBoneDrawEffects.applyGroupUniforms(BLEND_BONE);
    }

    public static void computeBlendedDrawColor(
        BOBJArmature armature,
        float[] weights,
        Set<Integer> colorOverrideBones,
        float baseR,
        float baseG,
        float baseB,
        float baseA,
        Color output
    )
    {
        float total = 0F;

        for (int boneIndex : colorOverrideBones)
        {
            if (boneIndex >= 0 && boneIndex < weights.length)
            {
                total += weights[boneIndex];
            }
        }

        if (total <= 0.0001F)
        {
            output.set(baseR, baseG, baseB, baseA);

            return;
        }

        output.set(0F, 0F, 0F, 0F);

        for (int boneIndex : colorOverrideBones)
        {
            if (boneIndex < 0 || boneIndex >= weights.length)
            {
                continue;
            }

            float weight = weights[boneIndex] / total;

            if (weight <= 0.0001F)
            {
                continue;
            }

            BOBJBone bone = findBone(armature, boneIndex);

            if (bone == null)
            {
                continue;
            }

            BobjBoneDrawEffects.computeDrawColor(bone, baseR, baseG, baseB, baseA, BLEND_DRAW_COLOR);

            output.r += BLEND_DRAW_COLOR.r * weight;
            output.g += BLEND_DRAW_COLOR.g * weight;
            output.b += BLEND_DRAW_COLOR.b * weight;
            output.a += BLEND_DRAW_COLOR.a * weight;
        }
    }

    public static int computeBlendedDrawLight(BOBJArmature armature, float[] weights, Set<Integer> colorOverrideBones, int light)
    {
        float total = 0F;
        float weightedLight = 0F;

        for (int boneIndex : colorOverrideBones)
        {
            if (boneIndex < 0 || boneIndex >= weights.length)
            {
                continue;
            }

            float weight = weights[boneIndex];

            if (weight <= 0.0001F)
            {
                continue;
            }

            BOBJBone bone = findBone(armature, boneIndex);

            if (bone == null)
            {
                continue;
            }

            total += weight;
            weightedLight += BobjBoneDrawEffects.computeDrawLight(bone, light, null) * weight;
        }

        if (total <= 0.0001F)
        {
            return light;
        }

        return Math.round(weightedLight / total);
    }

    public static float[] borrowTriangleWeights(int maxBoneCount)
    {
        if (TRIANGLE_WEIGHTS.length < maxBoneCount)
        {
            return new float[maxBoneCount];
        }

        return TRIANGLE_WEIGHTS;
    }

    private static void resetBlendBone()
    {
        BLEND_BONE.lighting = 0F;
        BLEND_BONE.color.set(0F, 0F, 0F, 0F);
        BLEND_BONE.color.brightness = 0F;
        BLEND_BONE.color.contrast = 0F;
        BLEND_BONE.color.hue = 0F;
        BLEND_BONE.color.saturation = 0F;
        BLEND_BONE.color.transform = new EffectTransform();
        BLEND_BONE.color.brightnessTransform = new EffectTransform();
        BLEND_BONE.color.contrastTransform = new EffectTransform();
        BLEND_BONE.color.hueTransform = new EffectTransform();
        BLEND_BONE.color.saturationTransform = new EffectTransform();
        BLEND_BONE.paintColor.set(0F, 0F, 0F, 0F);
        BLEND_BONE.paintColor.transform = new EffectTransform();
        BLEND_BONE.glowingColor.set(0F, 0F, 0F, 0F);
        BLEND_BONE.glowingColor.transform = new EffectTransform();
        BLEND_BONE.glowIntensity = 0F;
        BLEND_BONE.glowRadius = 0F;
        resetTransform(BLEND_BONE.color.transform);
        resetTransform(BLEND_BONE.color.brightnessTransform);
        resetTransform(BLEND_BONE.color.contrastTransform);
        resetTransform(BLEND_BONE.color.hueTransform);
        resetTransform(BLEND_BONE.color.saturationTransform);
        resetTransform(BLEND_BONE.paintColor.transform);
        resetTransform(BLEND_BONE.glowingColor.transform);
    }

    private static void resetTransform(EffectTransform transform)
    {
        transform.offsetX = 0F;
        transform.offsetY = 0F;
        transform.offsetZ = 0F;
        transform.scaleX = 0F;
        transform.scaleY = 0F;
        transform.scaleZ = 0F;
        transform.rotateX = 0F;
        transform.rotateY = 0F;
        transform.rotateZ = 0F;
        transform.pivotX = 0F;
        transform.pivotY = 0F;
        transform.pivotZ = 0F;
    }

    private static void finalizeBlendBone()
    {
        BLEND_BONE.color.transform.scaleX += 1F;
        BLEND_BONE.color.transform.scaleY += 1F;
        BLEND_BONE.color.transform.scaleZ += 1F;
        BLEND_BONE.paintColor.transform.scaleX += 1F;
        BLEND_BONE.paintColor.transform.scaleY += 1F;
        BLEND_BONE.paintColor.transform.scaleZ += 1F;
        BLEND_BONE.glowingColor.transform.scaleX += 1F;
        BLEND_BONE.glowingColor.transform.scaleY += 1F;
        BLEND_BONE.glowingColor.transform.scaleZ += 1F;
        BLEND_BONE.color.brightnessTransform.scaleX += 1F;
        BLEND_BONE.color.brightnessTransform.scaleY += 1F;
        BLEND_BONE.color.brightnessTransform.scaleZ += 1F;
        BLEND_BONE.color.contrastTransform.scaleX += 1F;
        BLEND_BONE.color.contrastTransform.scaleY += 1F;
        BLEND_BONE.color.contrastTransform.scaleZ += 1F;
        BLEND_BONE.color.hueTransform.scaleX += 1F;
        BLEND_BONE.color.hueTransform.scaleY += 1F;
        BLEND_BONE.color.hueTransform.scaleZ += 1F;
        BLEND_BONE.color.saturationTransform.scaleX += 1F;
        BLEND_BONE.color.saturationTransform.scaleY += 1F;
        BLEND_BONE.color.saturationTransform.scaleZ += 1F;
    }

    private static void accumulateBoneState(BOBJBone target, BOBJBone source, float weight)
    {
        target.color.r += source.color.r * weight;
        target.color.g += source.color.g * weight;
        target.color.b += source.color.b * weight;
        target.color.a += source.color.a * weight;
        target.color.brightness += source.color.brightness * weight;
        target.color.contrast += source.color.contrast * weight;
        target.color.hue += source.color.hue * weight;
        target.color.saturation += source.color.saturation * weight;

        target.paintColor.r += source.paintColor.r * weight;
        target.paintColor.g += source.paintColor.g * weight;
        target.paintColor.b += source.paintColor.b * weight;
        target.paintColor.a += source.paintColor.a * weight;

        target.glowingColor.r += source.glowingColor.r * weight;
        target.glowingColor.g += source.glowingColor.g * weight;
        target.glowingColor.b += source.glowingColor.b * weight;

        target.glowIntensity += source.glowIntensity * weight;
        target.glowRadius += source.glowRadius * weight;
        target.lighting += source.lighting * weight;

        accumulateEffectTransform(target.color.transform, source.color.transform, weight);
        accumulateEffectTransform(target.paintColor.transform, source.paintColor.transform, weight);
        accumulateEffectTransform(target.glowingColor.transform, source.glowingColor.transform, weight);
        accumulateEffectTransform(target.color.brightnessTransform, source.color.brightnessTransform, weight);
        accumulateEffectTransform(target.color.contrastTransform, source.color.contrastTransform, weight);
        accumulateEffectTransform(target.color.hueTransform, source.color.hueTransform, weight);
        accumulateEffectTransform(target.color.saturationTransform, source.color.saturationTransform, weight);

        if (weight >= 0.5F)
        {
            target.color.transform.shape = source.color.transform.shape;
            target.paintColor.transform.shape = source.paintColor.transform.shape;
            target.glowingColor.transform.shape = source.glowingColor.transform.shape;
            target.color.brightnessTransform.shape = source.color.brightnessTransform.shape;
            target.color.contrastTransform.shape = source.color.contrastTransform.shape;
            target.color.hueTransform.shape = source.color.hueTransform.shape;
            target.color.saturationTransform.shape = source.color.saturationTransform.shape;
        }
    }

    private static void accumulateEffectTransform(EffectTransform target, EffectTransform source, float weight)
    {
        target.offsetX += source.offsetX * weight;
        target.offsetY += source.offsetY * weight;
        target.offsetZ += source.offsetZ * weight;
        target.scaleX += (source.scaleX - 1F) * weight;
        target.scaleY += (source.scaleY - 1F) * weight;
        target.scaleZ += (source.scaleZ - 1F) * weight;
        target.rotateX += source.rotateX * weight;
        target.rotateY += source.rotateY * weight;
        target.rotateZ += source.rotateZ * weight;
        target.pivotX += source.pivotX * weight;
        target.pivotY += source.pivotY * weight;
        target.pivotZ += source.pivotZ * weight;
    }

    private static boolean hasAnyCustomInfluenceOnTriangle(BOBJLoader.CompiledData data, Set<Integer> colorOverrideBones, int baseVertex)
    {
        for (int vertex = 0; vertex < 3; vertex++)
        {
            int vertexIndex = baseVertex + vertex;
            int base = vertexIndex * 4;

            for (int slot = 0; slot < 4; slot++)
            {
                int boneIndex = data.boneIndexData[base + slot];
                float weight = data.weightData[base + slot];

                if (boneIndex >= 0 && weight > 0.0001F && colorOverrideBones.contains(boneIndex))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasSecondaryCustomInfluence(BOBJLoader.CompiledData data, Set<Integer> colorOverrideBones, int baseVertex, int dominant)
    {
        for (int vertex = 0; vertex < 3; vertex++)
        {
            int vertexIndex = baseVertex + vertex;
            int base = vertexIndex * 4;

            for (int slot = 0; slot < 4; slot++)
            {
                int boneIndex = data.boneIndexData[base + slot];
                float weight = data.weightData[base + slot];

                if (boneIndex >= 0
                    && boneIndex != dominant
                    && weight > SECONDARY_INFLUENCE
                    && colorOverrideBones.contains(boneIndex))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static int getDominantBoneForVertex(BOBJLoader.CompiledData data, int vertex)
    {
        int base = vertex * 4;
        float max = -1F;
        int bone = -1;

        for (int slot = 0; slot < 4; slot++)
        {
            float weight = data.weightData[base + slot];
            int boneIndex = data.boneIndexData[base + slot];

            if (boneIndex >= 0 && weight > max)
            {
                max = weight;
                bone = boneIndex;
            }
        }

        return bone;
    }

    private static BOBJBone findBone(BOBJArmature armature, int index)
    {
        for (BOBJBone bone : armature.orderedBones)
        {
            if (bone.index == index)
            {
                return bone;
            }
        }

        return null;
    }
}
