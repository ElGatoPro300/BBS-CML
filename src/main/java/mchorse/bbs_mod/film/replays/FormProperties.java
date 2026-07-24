package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.ColorKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.resources.LinkUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class FormProperties extends ValueGroup
{
    public final Map<String, KeyframeChannel> properties = new HashMap<>();

    public FormProperties(String id)
    {
        super(id);
    }

    public void shift(float tick)
    {
        for (KeyframeChannel<?> value : this.properties.values())
        {
            for (Keyframe<?> keyframe : value.getKeyframes())
            {
                keyframe.setTick(keyframe.getTick() + tick);
            }
        }
    }

    public KeyframeChannel getOrCreate(Form form, String key)
    {
        BaseValue value = this.get(key);

        if (value instanceof KeyframeChannel channel)
        {
            return channel;
        }

        int colon = key.indexOf(':');

        if (colon != -1)
        {
            String propertyId = key.substring(0, colon);
            BaseValue property = FormUtils.getProperty(form, propertyId);

            if (property instanceof ValuePose)
            {
                KeyframeChannel channel = new KeyframeChannel(key, KeyframeFactories.TRANSFORM);

                channel.setModel(true);
                this.properties.put(key, channel);
                this.add(channel);

                return channel;
            }
        }

        BaseValue property = FormUtils.getProperty(form, key);

        return property != null ? this.create(property) : null;
    }

    public KeyframeChannel create(BaseValue property)
    {
        if (property instanceof BaseKeyframeFactoryValue<?> keyframeFactoryValue)
        {
            String key = FormUtils.getPropertyPath(property);
            boolean allowed = property.isVisible() || FormUtils.isRenderPropertyPath(key);

            if (allowed)
            {
                KeyframeChannel channel = new KeyframeChannel(key, keyframeFactoryValue.getFactory());

                channel.setModel(true);
                this.properties.put(key, channel);
                this.add(channel);

                return channel;
            }
        }

        return null;
    }

    public void applyProperties(Form form, float tick)
    {
        this.applyProperties(form, tick, 1F);
    }

    public void applyProperties(Form form, float tick, float blend)
    {
        if (form == null)
        {
            return;
        }

        /* First pass: apply standard properties */
        for (KeyframeChannel value : this.properties.values())
        {
            if (value.getId().indexOf(':') == -1)
            {
                this.applyProperty(tick, form, value, blend);
            }
        }

        /* Second pass: apply limb tracks (which override standard properties) */
        for (KeyframeChannel value : this.properties.values())
        {
            if (value.getId().indexOf(':') != -1)
            {
                this.applyProperty(tick, form, value, blend);
            }
        }
    }

    private void applyProperty(float tick, Form form, KeyframeChannel value, float blend)
    {
        String id = value.getId();
        int colon = id.indexOf(':');

        if (colon != -1)
        {
            String propertyId = id.substring(0, colon);
            String boneName = id.substring(colon + 1);
            BaseValueBasic property = FormUtils.getProperty(form, propertyId);

            if (property instanceof ValuePose valuePose)
            {
                KeyframeSegment segment = value.find(tick);

                if (segment != null)
                {
                    Transform transform = (Transform) segment.createInterpolated();
                    Pose pose = valuePose.getRuntimeValue();

                    if (pose == null)
                    {
                        pose = new Pose();
                        valuePose.setRuntimeValue(pose);
                    }

                    PoseTransform poseTransform = pose.get(boneName);

                    if (blend < 1F)
                    {
                        poseTransform.translate.add(transform.translate.x * blend, transform.translate.y * blend, transform.translate.z * blend);
                        poseTransform.scale.mul(1F + (transform.scale.x - 1F) * blend, 1F + (transform.scale.y - 1F) * blend, 1F + (transform.scale.z - 1F) * blend);
                        poseTransform.rotate.add(transform.rotate.x * blend, transform.rotate.y * blend, transform.rotate.z * blend);
                        poseTransform.rotate2.add(transform.rotate2.x * blend, transform.rotate2.y * blend, transform.rotate2.z * blend);
                        poseTransform.pivot.add(transform.pivot.x * blend, transform.pivot.y * blend, transform.pivot.z * blend);
                    }
                    else
                    {
                        poseTransform.translate.add(transform.translate);
                        poseTransform.scale.mul(transform.scale);
                        poseTransform.rotate.add(transform.rotate);
                        poseTransform.rotate2.add(transform.rotate2);
                        poseTransform.pivot.add(transform.pivot);
                    }

                    PoseTransform sourcePose = null;

                    if (transform instanceof PoseTransform transformPose)
                    {
                        sourcePose = transformPose;
                    }
                    else
                    {
                        Object closestValue = segment.getClosest().getValue();

                        if (closestValue instanceof PoseTransform closestPose)
                        {
                            sourcePose = closestPose;
                        }
                    }

                    if (sourcePose != null)
                    {
                        if (blend < 1F)
                        {
                            poseTransform.fix = Lerps.lerp(poseTransform.fix, sourcePose.fix, blend);
                            poseTransform.color.r = Lerps.lerp(poseTransform.color.r, sourcePose.color.r, blend);
                            poseTransform.color.g = Lerps.lerp(poseTransform.color.g, sourcePose.color.g, blend);
                            poseTransform.color.b = Lerps.lerp(poseTransform.color.b, sourcePose.color.b, blend);
                            poseTransform.color.a = Lerps.lerp(poseTransform.color.a, sourcePose.color.a, blend);
                            poseTransform.paintColor.r = Lerps.lerp(poseTransform.paintColor.r, sourcePose.paintColor.r, blend);
                            poseTransform.paintColor.g = Lerps.lerp(poseTransform.paintColor.g, sourcePose.paintColor.g, blend);
                            poseTransform.paintColor.b = Lerps.lerp(poseTransform.paintColor.b, sourcePose.paintColor.b, blend);
                            poseTransform.paintColor.a = Lerps.lerp(poseTransform.paintColor.a, sourcePose.paintColor.a, blend);
                            poseTransform.glowingColor.r = Lerps.lerp(poseTransform.glowingColor.r, sourcePose.glowingColor.r, blend);
                            poseTransform.glowingColor.g = Lerps.lerp(poseTransform.glowingColor.g, sourcePose.glowingColor.g, blend);
                            poseTransform.glowingColor.b = Lerps.lerp(poseTransform.glowingColor.b, sourcePose.glowingColor.b, blend);
                            poseTransform.glowingColor.a = 1F;
                            poseTransform.glowIntensity = Lerps.lerp(poseTransform.glowIntensity, sourcePose.glowIntensity, blend);
                            poseTransform.glowRadius = Lerps.lerp(poseTransform.glowRadius, sourcePose.glowRadius, blend);
                            poseTransform.lighting = Lerps.lerp(poseTransform.lighting, sourcePose.lighting, blend);
                            poseTransform.shaderShadow = PaintSettings.resolveAutoShaderShadowForPoseAlpha(poseTransform.paintColor.a);
                        }
                        else
                        {
                            poseTransform.fix = sourcePose.fix;
                            poseTransform.color.copy(sourcePose.color);
                            poseTransform.paintColor.copy(sourcePose.paintColor);
                            poseTransform.glowingColor.copy(sourcePose.glowingColor);
                            poseTransform.glowingColor.a = 1F;
                            poseTransform.glowIntensity = sourcePose.glowIntensity;
                            poseTransform.glowRadius = sourcePose.glowRadius;
                            poseTransform.lighting = sourcePose.lighting;
                            poseTransform.shaderShadow = PaintSettings.resolveAutoShaderShadowForPoseAlpha(poseTransform.paintColor.a);
                        }

                        this.applyPoseBoneTexture(poseTransform, segment, transform);
                    }
                }

                return;
            }
        }

        BaseValueBasic property = FormUtils.getProperty(form, id);

        if (property == null)
        {
            return;
        }

        if (FormUtils.isRenderPropertyPath(id) && value.getFactory() == KeyframeFactories.BOOLEAN)
        {
            @SuppressWarnings("unchecked")
            KeyframeChannel<Boolean> render = (KeyframeChannel<Boolean>) value;

            this.applyRenderProperty(tick, property, render);

            return;
        }

        KeyframeSegment segment = value.find(tick);

        if (segment != null)
        {
            if ("texture".equals(id))
            {
                this.applyTextureBlend(form, segment);
            }

            if (id.startsWith("illusion"))
            {
                this.applyIllusionTextureBlend(form, segment);
            }

            if ("opacity".equals(id))
            {
                form.noshadingOpacity.setRuntimeValue(segment.getClosest().isNoshadingOpacity());
            }
            else if ("color".equals(id) && segment.getClosest().isNoshadingOpacity())
            {
                /* Legacy films stored noshading on Color keyframes before Opacity presets. */
                form.noshadingOpacity.setRuntimeValue(true);
            }

            if (blend < 1F)
            {
                IKeyframeFactory factory = value.getFactory();
                Object v = factory.copy(property.get());
                Object a = factory.copy(segment.createInterpolated());
                Object interpolated = factory.interpolate(v, v, a, a, Interpolations.LINEAR, MathUtils.clamp(blend, 0F, 1F));

                property.setRuntimeValue(coerceRuntimeValue(property, factory.copy(interpolated)));
            }
            else
            {
                property.setRuntimeValue(coerceRuntimeValue(property, segment.createInterpolated()));
            }

            /* Color track keyframes are often RGBA-only; keep morph Blend Color grading unless
             * the track itself keyframes brightness/contrast/hue/saturation. */
            if ("color".equals(id))
            {
                this.mergeColorAdjustmentsFromForm(property, value);
            }
        }
        else
        {
            if ("texture".equals(id))
            {
                form.textureBlend = null;
            }

            if (id.startsWith("illusion"))
            {
                form.illusionTextureBlend = null;
            }

            if ("opacity".equals(id))
            {
                form.noshadingOpacity.setRuntimeValue(null);
            }

            property.setRuntimeValue(null);
        }
    }

    private void applyRenderProperty(float tick, BaseValueBasic property, KeyframeChannel<Boolean> channel)
    {
        if (channel.isEmpty())
        {
            property.setRuntimeValue(null);

            return;
        }

        Keyframe<Boolean> first = channel.get(0);

        if (first != null && tick < first.getTick())
        {
            property.setRuntimeValue(null);

            return;
        }

        property.setRuntimeValue(channel.interpolate(tick, true));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object coerceRuntimeValue(BaseValueBasic property, Object value)
    {
        if (property instanceof ValueBoolean)
        {
            if (value instanceof Boolean)
            {
                return value;
            }

            if (value instanceof Number)
            {
                return ((Number) value).floatValue() > 0.001F;
            }

            return value != null;
        }

        return value;
    }

    /**
     * Film Color tracks often store only blend RGB/intensity. Morph-level brightness /
     * contrast / hue / saturation (and their transforms) must still apply unless the
     * Color track itself keyframes them.
     */
    @SuppressWarnings("rawtypes")
    private void mergeColorAdjustmentsFromForm(BaseValueBasic property, KeyframeChannel channel)
    {
        if (!(property instanceof ValueColor valueColor))
        {
            return;
        }

        Object runtimeObj = valueColor.getRuntimeValue();

        if (!(runtimeObj instanceof Color runtime))
        {
            return;
        }

        Color base = valueColor.getOriginalValue();

        if (base == null)
        {
            return;
        }

        if (!this.colorChannelHasAdjustments(channel) && base.hasColorAdjustments())
        {
            runtime.brightness = base.brightness;
            runtime.contrast = base.contrast;
            runtime.hue = base.hue;
            runtime.saturation = base.saturation;
        }

        if (!this.colorChannelHasGradeTransforms(channel) && base.hasActiveGradeTransform())
        {
            runtime.brightnessTransform = base.brightnessTransform == null ? new EffectTransform() : base.brightnessTransform.copy();
            runtime.contrastTransform = base.contrastTransform == null ? new EffectTransform() : base.contrastTransform.copy();
            runtime.hueTransform = base.hueTransform == null ? new EffectTransform() : base.hueTransform.copy();
            runtime.saturationTransform = base.saturationTransform == null ? new EffectTransform() : base.saturationTransform.copy();
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean colorChannelHasAdjustments(KeyframeChannel channel)
    {
        if (channel == null)
        {
            return false;
        }

        for (Object kfObj : channel.getKeyframes())
        {
            Keyframe<?> keyframe = (Keyframe<?>) kfObj;
            Object value = keyframe.getValue();

            if (value instanceof Color color && color.hasColorAdjustments())
            {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("rawtypes")
    private boolean colorChannelHasGradeTransforms(KeyframeChannel channel)
    {
        if (channel == null)
        {
            return false;
        }

        for (Object kfObj : channel.getKeyframes())
        {
            Keyframe<?> keyframe = (Keyframe<?>) kfObj;
            Object value = keyframe.getValue();

            if (value instanceof Color color && color.hasActiveGradeTransform())
            {
                return true;
            }
        }

        return false;
    }

    private void applyTextureBlend(Form form, KeyframeSegment segment)
    {
        if (segment.a.isBend() && !segment.isSame())
        {
            float blendFactor = getSegmentBlendFactor(segment);
            Link from = LinkUtils.copy((Link) segment.a.getValue());
            Link to = LinkUtils.copy((Link) segment.b.getValue());

            if (form.textureBlend == null)
            {
                form.textureBlend = new TextureBlend();
            }

            form.textureBlend.from = from;
            form.textureBlend.to = to;
            form.textureBlend.blend = blendFactor;
        }
        else
        {
            form.textureBlend = null;
        }
    }

    private void applyIllusionTextureBlend(Form form, KeyframeSegment segment)
    {
        if (segment.a.isBend() && !segment.isSame())
        {
            Illusion fromIllusion = (Illusion) segment.a.getValue();
            Illusion toIllusion = (Illusion) segment.b.getValue();
            Link from = this.getIllusionPrimaryTexture(fromIllusion);
            Link to = this.getIllusionPrimaryTexture(toIllusion);

            if (from != null && to != null && !from.equals(to))
            {
                float blendFactor = getSegmentBlendFactor(segment);

                if (form.illusionTextureBlend == null)
                {
                    form.illusionTextureBlend = new TextureBlend();
                }

                form.illusionTextureBlend.from = LinkUtils.copy(from);
                form.illusionTextureBlend.to = LinkUtils.copy(to);
                form.illusionTextureBlend.blend = blendFactor;

                return;
            }
        }

        form.illusionTextureBlend = null;
    }

    private Link getIllusionPrimaryTexture(Illusion illusion)
    {
        if (illusion == null || illusion.textures.isEmpty())
        {
            return null;
        }

        return illusion.textures.get(0);
    }

    private static float getSegmentBlendFactor(KeyframeSegment segment)
    {
        return (float) segment.a.getInterpolation().interpolate(0D, 1D, segment.x);
    }

    private static PoseTransform getKeyframeBoneTransform(Keyframe<?> keyframe, String boneName)
    {
        if (keyframe == null)
        {
            return null;
        }

        Object value = keyframe.getValue();

        if (value instanceof PoseTransform poseTransform)
        {
            return poseTransform;
        }

        if (value instanceof Pose pose)
        {
            return pose.get(boneName);
        }

        return null;
    }

    /**
     * Applies bone texture crossfade for pose overlay limb tracks, matching the
     * form-level texture timeline blend (from fades out, to fades in).
     */
    private void applyPoseBoneTexture(PoseTransform target, KeyframeSegment segment, Transform transform)
    {
        if (segment.a.isBend() && !segment.isSame()
            && segment.a.getValue() instanceof PoseTransform aPose
            && segment.b.getValue() instanceof PoseTransform bPose
            && aPose.texture != null && bPose.texture != null
            && !aPose.texture.equals(bPose.texture))
        {
            target.texture = LinkUtils.copy(aPose.texture);
            target.textureBlendTo = LinkUtils.copy(bPose.texture);
            target.textureBlend = getSegmentBlendFactor(segment);

            return;
        }

        if (transform instanceof PoseTransform pose)
        {
            PoseTransform pick = pose;

            if (segment.a.getValue() instanceof PoseTransform aPose)
            {
                pick = aPose;

                if (segment.x >= 1F && segment.b.getValue() instanceof PoseTransform bPose)
                {
                    pick = bPose;
                }
            }

            target.texture = pick.texture != null ? LinkUtils.copy(pick.texture) : null;
            target.textureBlendTo = pose.textureBlendTo != null ? LinkUtils.copy(pose.textureBlendTo) : null;
            target.textureBlend = pose.textureBlend;
        }
    }

    public void resetProperties(Form form)
    {
        if (form == null)
        {
            return;
        }

        form.textureBlend = null;
        form.illusionTextureBlend = null;

        for (KeyframeChannel value : this.properties.values())
        {
            String id = value.getId();
            int colon = id.indexOf(':');

            if (colon != -1)
            {
                String propertyId = id.substring(0, colon);
                BaseValueBasic property = FormUtils.getProperty(form, propertyId);

                if (property instanceof ValuePose valuePose)
                {
                    valuePose.setRuntimeValue(null);
                }
            }

            BaseValueBasic property = FormUtils.getProperty(form, id);

            if (property == null)
            {
                continue;
            }

            property.setRuntimeValue(null);
        }
    }

    public void cleanUp()
    {
        Iterator<KeyframeChannel> it = this.properties.values().iterator();

        while (it.hasNext())
        {
            KeyframeChannel next = it.next();

            if (next.isEmpty())
            {
                it.remove();
                this.remove(next);
            }
        }
    }

    /**
     * Paint is edited from the Color inspector but stored on a hidden {@code "paint"}
     * channel. When Color keyframes at {@code ticks} are removed, drop matching paint
     * companions so leftover paint does not keep rendering.
     */
    public void removeCompanionPaintAtTicks(Form form, Collection<Float> ticks)
    {
        if (ticks == null || ticks.isEmpty())
        {
            return;
        }

        this.removeKeyframesAtTicks("paint", ticks);
        this.removeKeyframesAtTicks("paint_color", ticks);

        KeyframeChannel color = this.properties.get("color");

        if (color == null || color.isEmpty())
        {
            this.clearPaintChannels(form);
        }
        else
        {
            this.clearPaintRuntimeIfChannelsEmpty(form);
        }
    }

    /**
     * Move hidden paint companions by the same delta as Color keyframes.
     */
    public void moveCompanionPaintBy(float diff, Collection<Float> fromTicks)
    {
        if (Math.abs(diff) < 0.0001F || fromTicks == null || fromTicks.isEmpty())
        {
            return;
        }

        this.moveKeyframesBy("paint", diff, fromTicks);
        this.moveKeyframesBy("paint_color", diff, fromTicks);
    }

    private void clearPaintChannels(Form form)
    {
        KeyframeChannel paint = this.properties.get("paint");

        if (paint != null)
        {
            paint.removeAll();
        }

        KeyframeChannel legacy = this.properties.get("paint_color");

        if (legacy != null)
        {
            legacy.removeAll();
        }

        this.clearPaintRuntime(form);
    }

    private void clearPaintRuntimeIfChannelsEmpty(Form form)
    {
        KeyframeChannel paint = this.properties.get("paint");
        KeyframeChannel legacy = this.properties.get("paint_color");
        boolean paintEmpty = paint == null || paint.isEmpty();
        boolean legacyEmpty = legacy == null || legacy.isEmpty();

        if (paintEmpty && legacyEmpty)
        {
            this.clearPaintRuntime(form);
        }
    }

    private void clearPaintRuntime(Form form)
    {
        if (form == null)
        {
            return;
        }

        form.paintSettings.setRuntimeValue(null);
        form.paintColor.setRuntimeValue(null);
    }

    private void removeKeyframesAtTicks(String channelId, Collection<Float> ticks)
    {
        KeyframeChannel channel = this.properties.get(channelId);

        if (channel == null || channel.isEmpty())
        {
            return;
        }

        List keyframes = channel.getKeyframes();
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe keyframe = (Keyframe) keyframes.get(i);

            for (Float tick : ticks)
            {
                if (tick != null && Math.abs(keyframe.getTick() - tick) < 0.001F)
                {
                    indices.add(i);

                    break;
                }
            }
        }

        indices.sort(Comparator.reverseOrder());

        for (Integer index : indices)
        {
            channel.remove(index);
        }
    }

    private void moveKeyframesBy(String channelId, float diff, Collection<Float> fromTicks)
    {
        KeyframeChannel channel = this.properties.get(channelId);

        if (channel == null || channel.isEmpty())
        {
            return;
        }

        List keyframes = channel.getKeyframes();
        List<Keyframe> moving = new ArrayList<>();

        for (Object kfObj : keyframes)
        {
            Keyframe keyframe = (Keyframe) kfObj;

            for (Float tick : fromTicks)
            {
                if (tick != null && Math.abs(keyframe.getTick() - tick) < 0.001F)
                {
                    moving.add(keyframe);

                    break;
                }
            }
        }

        for (Keyframe keyframe : moving)
        {
            keyframe.setTick(keyframe.getTick() + diff, false);
        }

        channel.sort();
    }

    @Override
    public BaseType toData()
    {
        MapType data = (MapType) super.toData();

        if (BBSSettings.isSaveAsCompatible())
        {
            this.rewriteForLegacyCompat(data);
        }

        return data;
    }

    /**
     * Older FormProperties loaders crash (NPE / ClassCast) on unknown keyframe {@code type}
     * strings and on Color values stored as maps. Rewrite the saved payload to legacy shapes
     * older builds already understand, while keeping modern channels only in memory.
     */
    private void rewriteForLegacyCompat(MapType data)
    {
        this.dualWritePaintToLegacy(data);
        this.dualWriteGlowToLegacy(data);
        this.dualWriteStructureLightToLegacy(data);
        this.dualWriteOpacityIntoColor(data);
        this.flattenColorKeyframeValuesToInt(data);
        this.stripUnsafeKeyframeTypes(data);
    }

    private void dualWritePaintToLegacy(MapType data)
    {
        KeyframeChannel<?> paintAny = this.properties.get("paint");

        if (paintAny == null || paintAny.isEmpty() || paintAny.getFactory() != KeyframeFactories.PAINT_SETTINGS)
        {
            data.remove("paint");
            data.remove("paint_settings");

            return;
        }

        @SuppressWarnings("unchecked")
        KeyframeChannel<PaintSettings> paint = (KeyframeChannel<PaintSettings>) paintAny;
        KeyframeChannel<Color> paintColor = new KeyframeChannel<>("paint_color", KeyframeFactories.COLOR);

        paintColor.setModel(true);

        for (Keyframe<PaintSettings> kf : paint.getKeyframes())
        {
            PaintSettings settings = kf.getValue();

            if (settings == null)
            {
                continue;
            }

            Color color = new Color(settings.r, settings.g, settings.b, settings.intensity);
            int index = paintColor.insert(kf.getTick(), color);
            Keyframe<Color> out = paintColor.get(index);

            if (out != null)
            {
                out.getInterpolation().copy(kf.getInterpolation());
            }
        }

        data.put("paint_color", paintColor.toData());
        data.remove("paint");
        data.remove("paint_settings");
    }

    private void dualWriteGlowToLegacy(MapType data)
    {
        List<String> glowKeys = new ArrayList<>();

        for (Map.Entry<String, KeyframeChannel> entry : this.properties.entrySet())
        {
            KeyframeChannel<?> channel = entry.getValue();

            if (channel != null && channel.getFactory() == KeyframeFactories.GLOW_SETTINGS)
            {
                glowKeys.add(entry.getKey());
            }
        }

        for (String glowKey : glowKeys)
        {
            this.dualWriteOneGlowChannel(data, glowKey);
        }

        /* Drop any leftover modern glow keys stripUnsafe would otherwise delete forever. */
        List<String> leftover = new ArrayList<>();

        for (String key : data.keys())
        {
            if (this.isGlowChannelKey(key))
            {
                leftover.add(key);
            }
        }

        for (String key : leftover)
        {
            data.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    private void dualWriteOneGlowChannel(MapType data, String glowKey)
    {
        KeyframeChannel<?> glowAny = this.properties.get(glowKey);

        if (glowAny == null || glowAny.isEmpty() || glowAny.getFactory() != KeyframeFactories.GLOW_SETTINGS)
        {
            data.remove(glowKey);

            return;
        }

        String prefix = this.glowChannelPrefix(glowKey);

        if (prefix == null)
        {
            data.remove(glowKey);

            return;
        }

        KeyframeChannel<GlowSettings> glow = (KeyframeChannel<GlowSettings>) glowAny;
        String colorKey = prefix + "glowing_color";
        String intensityKey = prefix + "glow_intensity";
        KeyframeChannel<Color> glowingColor = new KeyframeChannel<>(colorKey, KeyframeFactories.COLOR);
        KeyframeChannel<Float> glowIntensity = new KeyframeChannel<>(intensityKey, KeyframeFactories.FLOAT);

        glowingColor.setModel(true);
        glowIntensity.setModel(true);

        for (Keyframe<GlowSettings> kf : glow.getKeyframes())
        {
            GlowSettings settings = kf.getValue();

            if (settings == null)
            {
                continue;
            }

            Color color = new Color(settings.r, settings.g, settings.b, 1F);
            int colorIndex = glowingColor.insert(kf.getTick(), color);
            int intensityIndex = glowIntensity.insert(kf.getTick(), settings.intensity);
            Keyframe<Color> colorKf = glowingColor.get(colorIndex);
            Keyframe<Float> intensityKf = glowIntensity.get(intensityIndex);

            if (colorKf != null)
            {
                colorKf.getInterpolation().copy(kf.getInterpolation());
            }

            if (intensityKf != null)
            {
                intensityKf.getInterpolation().copy(kf.getInterpolation());
            }
        }

        data.put(colorKey, glowingColor.toData());
        data.put(intensityKey, glowIntensity.toData());
        data.remove(glowKey);
        data.remove(prefix + "glow_settings");
    }

    private boolean isGlowChannelKey(String key)
    {
        return key != null && (key.equals("glow") || key.equals("glow_settings")
            || key.endsWith("/glow") || key.endsWith("/glow_settings"));
    }

    private String glowChannelPrefix(String key)
    {
        if (key == null)
        {
            return null;
        }

        if (key.equals("glow") || key.equals("glow_settings"))
        {
            return "";
        }

        if (key.endsWith("/glow"))
        {
            return key.substring(0, key.length() - "glow".length());
        }

        if (key.endsWith("/glow_settings"))
        {
            return key.substring(0, key.length() - "glow_settings".length());
        }

        return null;
    }

    private void dualWriteStructureLightToLegacy(MapType data)
    {
        KeyframeChannel<?> lightAny = this.properties.get("structure_light");

        if (lightAny == null || lightAny.isEmpty() || lightAny.getFactory() != KeyframeFactories.STRUCTURE_LIGHT_SETTINGS)
        {
            data.remove("structure_light");

            return;
        }

        @SuppressWarnings("unchecked")
        KeyframeChannel<StructureLightSettings> light = (KeyframeChannel<StructureLightSettings>) lightAny;
        KeyframeChannel<Boolean> emit = new KeyframeChannel<>("emit_light", KeyframeFactories.BOOLEAN);
        KeyframeChannel<Float> intensity = new KeyframeChannel<>("light_intensity", KeyframeFactories.FLOAT);

        emit.setModel(true);
        intensity.setModel(true);

        for (Keyframe<StructureLightSettings> kf : light.getKeyframes())
        {
            StructureLightSettings settings = kf.getValue();

            if (settings == null)
            {
                continue;
            }

            int emitIndex = emit.insert(kf.getTick(), settings.enabled);
            int intensityIndex = intensity.insert(kf.getTick(), (float) settings.intensity);
            Keyframe<Boolean> emitKf = emit.get(emitIndex);
            Keyframe<Float> intensityKf = intensity.get(intensityIndex);

            if (emitKf != null)
            {
                emitKf.getInterpolation().copy(kf.getInterpolation());
            }

            if (intensityKf != null)
            {
                intensityKf.getInterpolation().copy(kf.getInterpolation());
            }
        }

        data.put("emit_light", emit.toData());
        data.put("light_intensity", intensity.toData());
        data.remove("structure_light");
    }

    /**
     * Older builds only fade via {@code color.a}. Mirror Opacity into Color as an Int ARGB
     * value (Int-only Color factories ClassCast on Map values).
     */
    private void dualWriteOpacityIntoColor(MapType data)
    {
        KeyframeChannel<?> opacityAny = this.properties.get("opacity");

        if (opacityAny == null || opacityAny.isEmpty() || opacityAny.getFactory() != KeyframeFactories.FLOAT)
        {
            return;
        }

        @SuppressWarnings("unchecked")
        KeyframeChannel<Float> opacity = (KeyframeChannel<Float>) opacityAny;
        KeyframeChannel<?> colorAny = this.properties.get("color");
        @SuppressWarnings("unchecked")
        KeyframeChannel<Color> colorChannel = colorAny != null && colorAny.getFactory() == KeyframeFactories.COLOR
            ? (KeyframeChannel<Color>) colorAny
            : null;

        MapType colorData = data.getMap("color");

        if (colorData.isEmpty())
        {
            colorData = new MapType();
            colorData.putString("type", "color");
            colorData.put("keyframes", new ListType());
            data.put("color", colorData);
        }

        ListType keyframes = colorData.getList("keyframes");

        if (!colorData.has("keyframes"))
        {
            colorData.put("keyframes", keyframes);
        }

        colorData.putString("type", "color");

        for (Keyframe<Float> opacityKf : opacity.getKeyframes())
        {
            Float opacityValue = opacityKf.getValue();

            if (opacityValue == null)
            {
                continue;
            }

            float tick = opacityKf.getTick();
            float opacityA = MathUtils.clamp(opacityValue, 0F, 1F);
            Color source = this.resolveColorAt(colorChannel, tick);
            MapType keyframeMap = this.findOrCreateColorKeyframe(keyframes, tick, opacityKf);

            /* Keep modern Color (grade / blend intensity / transforms) beside the legacy Int. */
            this.writeCompatibleColorValue(keyframeMap, source, opacityA);

            if (opacityKf.isNoshadingOpacity())
            {
                keyframeMap.putBool("noshading_opacity", true);
            }
        }
    }

    /**
     * Writes legacy Int {@code value} (opacity in ARGB alpha) plus {@code value_bbs} so this
     * build can reload Color Grade and Blend intensity after save_as_compatible.
     */
    private void writeCompatibleColorValue(MapType keyframeMap, Color source, float opacityA)
    {
        Color modern = source == null ? new Color(1F, 1F, 1F, 0F) : source.copy();
        BaseType modernData = KeyframeFactories.COLOR.toData(modern);
        MapType modernMap;

        if (modernData instanceof MapType map)
        {
            modernMap = map;
        }
        else
        {
            modernMap = new MapType();
            modernMap.putInt("color", modern.getARGBColor());
        }

        modernMap.putFloat(ColorKeyframeFactory.BLEND_A, modern.a);
        keyframeMap.put("value_bbs", modernMap);
        keyframeMap.put("value", new IntType(Colors.setA(modern.getRGBColor(), opacityA)));
    }

    private void flattenColorKeyframeValuesToInt(MapType data)
    {
        for (String key : data.keys())
        {
            if (key.equals("color") || key.endsWith("/color")
                || key.equals("paint_color") || key.endsWith("/paint_color")
                || key.equals("glowing_color") || key.endsWith("/glowing_color"))
            {
                this.flattenColorChannelValuesToInt(data.getMap(key));
            }
        }
    }

    /**
     * Older builds ClassCast on Map color values. Keep the rich map in {@code value_bbs}
     * so this build round-trips Color Grade / transforms / blend_a.
     */
    private void flattenColorChannelValuesToInt(MapType colorData)
    {
        if (colorData == null || colorData.isEmpty() || !colorData.has("keyframes"))
        {
            return;
        }

        ListType keyframes = colorData.getList("keyframes");

        for (int i = 0; i < keyframes.size(); i++)
        {
            BaseType raw = keyframes.get(i);

            if (raw == null || !raw.isMap())
            {
                continue;
            }

            MapType keyframeMap = raw.asMap();
            BaseType value = keyframeMap.get("value");

            if (value != null && value.isMap())
            {
                MapType valueMap = value.asMap();

                if (!keyframeMap.has("value_bbs"))
                {
                    keyframeMap.put("value_bbs", valueMap.copy());
                }

                if (valueMap.has("color"))
                {
                    keyframeMap.put("value", new IntType(valueMap.getInt("color")));
                }
            }
        }
    }

    private void stripUnsafeKeyframeTypes(MapType data)
    {
        ArrayList<String> remove = new ArrayList<>();

        for (String key : data.keys())
        {
            MapType channel = data.getMap(key);

            if (channel.isEmpty())
            {
                continue;
            }

            String type = channel.getString("type");

            if (!isLegacySafeKeyframeType(type))
            {
                remove.add(key);
            }
        }

        for (String key : remove)
        {
            data.remove(key);
        }
    }

    private static boolean isLegacySafeKeyframeType(String type)
    {
        if (type == null || type.isEmpty())
        {
            return false;
        }

        return switch (type)
        {
            case "color", "transform", "pose", "boolean", "string",
                 "float", "double", "integer", "link", "vector4f",
                 "anchor", "block_state", "item_stack", "actions_config",
                 "shape_keys" -> true;
            default -> false;
        };
    }

    private Color resolveColorAt(KeyframeChannel<Color> colorChannel, float tick)
    {
        if (colorChannel == null || colorChannel.isEmpty())
        {
            return new Color(1F, 1F, 1F, 0F);
        }

        KeyframeSegment segment = colorChannel.find(tick);

        if (segment != null)
        {
            Object interpolated = segment.createInterpolated();

            if (interpolated instanceof Color color)
            {
                return color.copy();
            }
        }

        return new Color(1F, 1F, 1F, 0F);
    }

    private MapType findOrCreateColorKeyframe(ListType keyframes, float tick, Keyframe<Float> opacityKf)
    {
        for (int i = 0; i < keyframes.size(); i++)
        {
            BaseType raw = keyframes.get(i);

            if (raw == null || !raw.isMap())
            {
                continue;
            }

            MapType keyframeMap = raw.asMap();

            if (keyframeMap.has("tick") && Math.abs(keyframeMap.getFloat("tick") - tick) < 0.001F)
            {
                return keyframeMap;
            }
        }

        Keyframe<Color> created = new Keyframe<>("", KeyframeFactories.COLOR, tick, new Color(1F, 1F, 1F, 0F));

        created.getInterpolation().copy(opacityKf.getInterpolation());
        created.setNoshadingOpacity(opacityKf.isNoshadingOpacity());

        MapType keyframeMap = (MapType) created.toData();

        keyframes.add(keyframeMap);

        return keyframeMap;
    }

    @Override
    public void fromData(BaseType data)
    {
        /* FormProperties stores dynamic channels; rebuild from serialized data to avoid stale channels. */
        this.properties.clear();
        this.removeAll();

        if (!data.isMap())
        {
            return;
        }

        MapType map = data.asMap();

        for (String key : map.keys())
        {
            MapType mapType = map.getMap(key);

            if (mapType.isEmpty())
            {
                continue;
            }

            try
            {
                String type = mapType.getString("type");

                /* Skip unknown factories early — older builds NPE here; stay resilient too. */
                if (type != null && !type.isEmpty() && !KeyframeFactories.FACTORIES.containsKey(type))
                {
                    continue;
                }

                KeyframeChannel property = new KeyframeChannel(key, null);

                property.setModel(true);
                property.fromData(mapType);

            /* Patch 1.1.1 changes to lighting property */
            if (key.endsWith("lighting") && property.getFactory() == KeyframeFactories.BOOLEAN)
            {
                KeyframeChannel newProperty = new KeyframeChannel(key, KeyframeFactories.FLOAT);

                newProperty.setModel(true);

                for (Object keyframe : property.getKeyframes())
                {
                    Keyframe kf = (Keyframe) keyframe;
                    Boolean v = (Boolean) kf.getValue();

                    newProperty.insert(kf.getTick(), v ? 1F : 0F);
                }

                property = newProperty;
            }

            /* shaderShadow was briefly a float; migrate float keyframes back to boolean */
            if (key.equals("shaderShadow") && property.getFactory() == KeyframeFactories.FLOAT)
            {
                KeyframeChannel newProperty = new KeyframeChannel(key, KeyframeFactories.BOOLEAN);

                newProperty.setModel(true);

                for (Object keyframe : property.getKeyframes())
                {
                    Keyframe kf = (Keyframe) keyframe;
                    Object raw = kf.getValue();
                    float f = raw instanceof Number ? ((Number) raw).floatValue() : 0F;

                    newProperty.insert(kf.getTick(), f > 0.001F);
                }

                property = newProperty;
            }

            if (property.getFactory() != null)
            {
                this.properties.put(key, property);
                this.add(property);
            }
            }
            catch (Throwable ignored)
            {
                /* Skip corrupt / unknown property channels so the rest of the film still loads. */
            }
        }

        /* Migration: synthesize structure_light from legacy emit_light and light_intensity channels */
        try
        {
            KeyframeChannel<?> emit = this.properties.get("emit_light");
            KeyframeChannel<?> intensity = this.properties.get("light_intensity");

            if (emit != null || intensity != null)
            {
                KeyframeChannel<?> mergedAny = this.properties.get("structure_light");
                @SuppressWarnings("unchecked")
                KeyframeChannel<StructureLightSettings> merged = mergedAny != null
                    ? (KeyframeChannel<StructureLightSettings>) mergedAny
                    : new KeyframeChannel<>("structure_light", KeyframeFactories.STRUCTURE_LIGHT_SETTINGS);

                if (mergedAny == null)
                {
                    merged.setModel(true);
                    this.properties.put("structure_light", merged);
                    this.add(merged);
                }

                TreeSet<Float> ticks = new TreeSet<>();
                if (emit != null) for (Object kfObj : emit.getKeyframes()) { ticks.add(((Keyframe<?>) kfObj).getTick()); }
                if (intensity != null) for (Object kfObj : intensity.getKeyframes()) { ticks.add(((Keyframe<?>) kfObj).getTick()); }

                for (float t : ticks)
                {
                    boolean enabled = false;
                    int value = 0;

                    if (emit != null)
                    {
                        KeyframeSegment seg = emit.find(t);
                        if (seg != null)
                        {
                            Object v = seg.createInterpolated();
                            if (v instanceof Boolean b) enabled = b;
                            else if (v instanceof Number n) enabled = n.floatValue() >= 0.5F;
                        }
                    }

                    if (intensity != null)
                    {
                        KeyframeSegment seg = intensity.find(t);
                        if (seg != null)
                        {
                            Object v = seg.createInterpolated();
                            if (v instanceof Number n) value = Math.round(n.floatValue());
                        }
                    }

                    StructureLightSettings payload = new StructureLightSettings(
                        enabled,
                        Math.max(0, Math.min(15, value))
                    );

                    merged.insert(t, payload);
                }
            }
        }
        catch (Throwable ignored) {}

        /* Migration: rename glow_settings -> glow, merge glowing_color / glow_intensity
         * (including body-part paths like 0/1/glowing_color). */
        try
        {
            this.migrateGlowChannels();
        }
        catch (Throwable ignored) {}

        /* Migration: paint_color -> paint (PaintSettings) */
        try
        {
            KeyframeChannel<?> paintColorChannel = this.properties.remove("paint_color");

            if (paintColorChannel != null)
            {
                KeyframeChannel<?> mergedAny = this.properties.get("paint");
                @SuppressWarnings("unchecked")
                KeyframeChannel<PaintSettings> merged = mergedAny != null
                    ? (KeyframeChannel<PaintSettings>) mergedAny
                    : new KeyframeChannel<>("paint", KeyframeFactories.PAINT_SETTINGS);

                if (mergedAny == null)
                {
                    merged.setModel(true);
                    this.properties.put("paint", merged);
                    this.add(merged);
                }

                for (Object kfObj : paintColorChannel.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;
                    float t = kf.getTick();
                    PaintSettings settings = this.getPaintSettingsAt(merged, t);
                    Object v = kf.getValue();

                    if (v instanceof Color color)
                    {
                        settings.r = color.r;
                        settings.g = color.g;
                        settings.b = color.b;
                        settings.intensity = color.a;
                    }

                    merged.insert(t, settings);
                }

                this.remove(paintColorChannel);
            }
        }
        catch (Throwable ignored) {}

        /* Migration: color.a -> opacity channel (Blend Color keeps RGB on color) */
        try
        {
            KeyframeChannel<?> opacityAny = this.properties.get("opacity");
            KeyframeChannel<?> colorAny = this.properties.get("color");

            if (opacityAny == null && colorAny != null && colorAny.getFactory() == KeyframeFactories.COLOR)
            {
                @SuppressWarnings("unchecked")
                KeyframeChannel<Color> colorChannel = (KeyframeChannel<Color>) colorAny;
                KeyframeChannel<Float> opacity = new KeyframeChannel<>("opacity", KeyframeFactories.FLOAT);

                opacity.setModel(true);

                boolean migrated = false;

                for (Object kfObj : colorChannel.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;
                    Object v = kf.getValue();

                    if (v instanceof Color color)
                    {
                        float a = color.a;

                        /* color.a ≈ 0 is Blend Color intensity off, not legacy opacity.
                         * Migrating it created opacity=0 keyframes on every film reload (Alt+F4 save). */
                        if (a > 0.001F && a < 0.999F)
                        {
                            opacity.insert(kf.getTick(), a);
                            color.a = 1F;
                            migrated = true;
                        }
                    }
                }

                if (migrated)
                {
                    this.properties.put("opacity", opacity);
                    this.add(opacity);
                }
            }
            else if (opacityAny != null && opacityAny.getFactory() == KeyframeFactories.FLOAT && !opacityAny.isEmpty())
            {
                /* Repair films poisoned by the old a≈0 → opacity=0 migration. */
                @SuppressWarnings("unchecked")
                KeyframeChannel<Float> opacityChannel = (KeyframeChannel<Float>) opacityAny;
                boolean allZero = true;

                for (Object kfObj : opacityChannel.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;
                    Object v = kf.getValue();

                    if (!(v instanceof Float) || ((Float) v) > 0.001F)
                    {
                        allZero = false;

                        break;
                    }
                }

                if (allZero)
                {
                    this.properties.remove("opacity");
                    this.remove(opacityChannel);
                }
                else if (colorAny != null && colorAny.getFactory() == KeyframeFactories.COLOR)
                {
                    /* Compatible saves put opacity into Int color ARGB alpha. Only clear
                     * color.a when it still matches opacity at that tick (legacy Int path).
                     * Modern value_bbs + blend_a already restored real Blend intensity. */
                    @SuppressWarnings("unchecked")
                    KeyframeChannel<Color> colorChannel = (KeyframeChannel<Color>) colorAny;

                    for (Object kfObj : colorChannel.getKeyframes())
                    {
                        Keyframe<?> kf = (Keyframe<?>) kfObj;
                        Object v = kf.getValue();

                        if (!(v instanceof Color color))
                        {
                            continue;
                        }

                        KeyframeSegment opacitySeg = opacityChannel.find(kf.getTick());

                        if (opacitySeg == null)
                        {
                            continue;
                        }

                        Object opacityObj = opacitySeg.createInterpolated();

                        if (opacityObj instanceof Float opacityA && Math.abs(color.a - MathUtils.clamp(opacityA, 0F, 1F)) < 0.02F)
                        {
                            color.a = 1F;
                        }
                    }
                }
            }
        }
        catch (Throwable ignored) {}

        /* Migration: copy legacy visible render-gating keyframes into render while keeping visible */
        try
        {
            KeyframeChannel<?> legacyVisible = this.properties.get("visible");
            KeyframeChannel<?> renderChannel = this.properties.get("render");

            if (renderChannel == null && legacyVisible != null && legacyVisible.getFactory() == KeyframeFactories.BOOLEAN && !legacyVisible.isEmpty())
            {
                KeyframeChannel<Boolean> render = new KeyframeChannel<>("render", KeyframeFactories.BOOLEAN);

                render.setModel(true);

                for (Object kfObj : legacyVisible.getKeyframes())
                {
                    Keyframe<?> kf = (Keyframe<?>) kfObj;

                    render.insert(kf.getTick(), (Boolean) kf.getValue());
                }

                this.properties.put("render", render);
                this.add(render);
            }
        }
        catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    private void migrateGlowChannels()
    {
        List<String> keys = new ArrayList<>(this.properties.keySet());

        for (String key : keys)
        {
            if (!key.equals("glow_settings") && !key.endsWith("/glow_settings"))
            {
                continue;
            }

            String prefix = this.glowChannelPrefix(key);
            KeyframeChannel<?> renamed = this.properties.remove(key);

            if (prefix == null || renamed == null)
            {
                continue;
            }

            String glowKey = prefix + "glow";
            KeyframeChannel<GlowSettings> merged = this.ensureGlowChannel(glowKey);

            for (Object kfObj : renamed.getKeyframes())
            {
                Keyframe<?> kf = (Keyframe<?>) kfObj;
                Object v = kf.getValue();

                if (v instanceof GlowSettings settings)
                {
                    merged.insert(kf.getTick(), settings.copy());
                }
            }

            this.remove(renamed);
        }

        keys = new ArrayList<>(this.properties.keySet());

        for (String key : keys)
        {
            if (!key.equals("glowing_color") && !key.endsWith("/glowing_color"))
            {
                continue;
            }

            String prefix = key.equals("glowing_color") ? "" : key.substring(0, key.length() - "glowing_color".length());
            KeyframeChannel<?> glowingColorChannel = this.properties.remove(key);

            if (glowingColorChannel == null)
            {
                continue;
            }

            String glowKey = prefix + "glow";
            KeyframeChannel<GlowSettings> merged = this.ensureGlowChannel(glowKey);

            for (Object kfObj : glowingColorChannel.getKeyframes())
            {
                Keyframe<?> kf = (Keyframe<?>) kfObj;
                float t = kf.getTick();
                GlowSettings settings = this.getGlowSettingsAt(merged, t);
                Object v = kf.getValue();

                if (v instanceof Color color)
                {
                    settings.r = color.r;
                    settings.g = color.g;
                    settings.b = color.b;
                }

                merged.insert(t, settings);
            }

            this.remove(glowingColorChannel);
        }

        keys = new ArrayList<>(this.properties.keySet());

        for (String key : keys)
        {
            if (!key.equals("glow_intensity") && !key.endsWith("/glow_intensity"))
            {
                continue;
            }

            String prefix = key.equals("glow_intensity") ? "" : key.substring(0, key.length() - "glow_intensity".length());
            KeyframeChannel<?> legacyGlow = this.properties.remove(key);

            if (legacyGlow == null)
            {
                continue;
            }

            String glowKey = prefix + "glow";
            KeyframeChannel<GlowSettings> merged = this.ensureGlowChannel(glowKey);

            for (Object kfObj : legacyGlow.getKeyframes())
            {
                Keyframe<?> kf = (Keyframe<?>) kfObj;
                float t = kf.getTick();
                float intensity = 0F;
                Object v = kf.getValue();

                if (v instanceof Number n)
                {
                    intensity = n.floatValue();
                }

                GlowSettings settings = this.getGlowSettingsAt(merged, t);

                settings.intensity = intensity;
                merged.insert(t, settings);
            }

            this.remove(legacyGlow);
        }
    }

    @SuppressWarnings("unchecked")
    private KeyframeChannel<GlowSettings> ensureGlowChannel(String glowKey)
    {
        KeyframeChannel<?> existing = this.properties.get(glowKey);

        if (existing != null && existing.getFactory() == KeyframeFactories.GLOW_SETTINGS)
        {
            return (KeyframeChannel<GlowSettings>) existing;
        }

        KeyframeChannel<GlowSettings> merged = new KeyframeChannel<>(glowKey, KeyframeFactories.GLOW_SETTINGS);

        merged.setModel(true);
        this.properties.put(glowKey, merged);
        this.add(merged);

        return merged;
    }

    private PaintSettings getPaintSettingsAt(KeyframeChannel<PaintSettings> channel, float tick)
    {
        KeyframeSegment segment = channel.find(tick);
        PaintSettings settings;

        if (segment != null)
        {
            Object interpolated = segment.createInterpolated();

            settings = interpolated instanceof PaintSettings paint ? paint.copy() : new PaintSettings();
        }
        else
        {
            settings = new PaintSettings();
        }

        return settings;
    }

    private GlowSettings getGlowSettingsAt(KeyframeChannel<GlowSettings> channel, float tick)
    {
        KeyframeSegment segment = channel.find(tick);
        GlowSettings settings;

        if (segment != null)
        {
            Object interpolated = segment.createInterpolated();

            settings = interpolated instanceof GlowSettings glow ? glow.copy() : new GlowSettings();
        }
        else
        {
            settings = new GlowSettings();
        }

        return settings;
    }

    @Override
    protected boolean canPersist(BaseValue value)
    {
        if (value instanceof KeyframeChannel<?> channel)
        {
            return !channel.isEmpty();
        }

        return super.canPersist(value);
    }
}