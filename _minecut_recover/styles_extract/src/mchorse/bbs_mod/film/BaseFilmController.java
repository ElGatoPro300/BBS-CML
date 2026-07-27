package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.MorphFireRenderer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.forms.forms.utils.LookAt;
import mchorse.bbs_mod.forms.forms.utils.LookAtBone;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.ShadowSettings;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.forms.values.ValueIllusion;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.mixin.client.ClientPlayerEntityAccessor;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoMatrixUtils;
import mchorse.bbs_mod.ui.utils.gizmo.TransformOrientation;
import mchorse.bbs_mod.utils.AABB;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.class_1297;
import net.minecraft.class_1313;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_1944;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2388;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_265;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_3419;
import net.minecraft.class_4050;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_746;
import net.minecraft.class_765;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

public abstract class BaseFilmController
{
    /* Temporal smoothing state for "real" illusions' ground following (entity identity + illusion index -> lift) */
    private static final Map<Long, IllusionLift> ILLUSION_LIFTS = new HashMap<>();

    public final Film film;

    protected IntObjectMap<IEntity> entities = new IntObjectHashMap<>();
    protected Map<String, Replay> replayMap = new HashMap<>();

    public boolean paused;
    public int exception = -1;

    private List<FormRenderDepth.Occluder> currentRenderDepthOccluders = List.of();

    /* Rendering helpers */

    public static void renderEntity(FilmControllerContext context)
    {
        IntObjectMap<IEntity> entities = context.entities;
        IEntity entity = context.entity;
        class_4184 camera = context.camera;
        class_4587 stack = context.stack;
        float transition = context.transition;

        Form form = entity.getForm();

        if (form == null || !form.render.get())
        {
            return;
        }

        applyGroupPaintGlow(form, context.groupPaint, context.groupGlow);

        Vector3d position = Vectors.TEMP_3D.set(
            Lerps.lerp(entity.getPrevX(), entity.getX(), transition),
            Lerps.lerp(entity.getPrevY(), entity.getY(), transition),
            Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition)
        );

        double cx = camera.method_19326().field_1352;
        double cy = camera.method_19326().field_1351;
        double cz = camera.method_19326().field_1350;

        boolean relative = context.replay != null && context.relative;

        if (relative)
        {
            if (context.map != null)
            {
                cx = context.replay.keyframes.x.interpolate(0F) + context.replay.relativeOffset.get().x;
                cy = context.replay.keyframes.y.interpolate(0F) + context.replay.relativeOffset.get().y;
                cz = context.replay.keyframes.z.interpolate(0F) + context.replay.relativeOffset.get().z;
            }
            else
            {
                cx = position.x + context.replay.relativeOffset.get().x;
                cy = position.y + context.replay.relativeOffset.get().y;
                cz = position.z + context.replay.relativeOffset.get().z;
            }

            if (context.isShadowPass)
            {
                cx += camera.method_19326().field_1352;
                cy += camera.method_19326().field_1351;
                cz += camera.method_19326().field_1350;
            }
        }

        Matrix4f target = null;
        Matrix4f defaultMatrix = getMatrixForRenderWithRotation(entity, cx, cy, cz, transition);
        float opacity = 1F;

        if (!relative)
        {
            Pair<Matrix4f, Float> pair = getTotalMatrix(entities, form.anchor.get(), defaultMatrix, cx, cy, cz, transition, 0);

            target = pair.a;
            opacity = pair.b;
        }

        if (target != null)
        {
            Vector3f v = target.getTranslation(new Vector3f());
            Vector3f v2 = defaultMatrix.getTranslation(new Vector3f());

            position.x += v.x - v2.x;
            position.y += v.y - v2.y;
            position.z += v.z - v2.z;
        }
        else
        {
            target = defaultMatrix;
        }

        if (!relative)
        {
            applyLookAt(context, form, position, target);
            InverseKinematicsApplier.apply(context, form);
        }

        if (context.localGroupTransform != null)
        {
            target.mul(context.localGroupTransform);
        }

        class_2338 pos = class_2338.method_49637(position.x, position.y + 0.5D, position.z);
        class_1937 world = entity.getWorld();

        if (world == null)
        {
            world = class_310.method_1551().field_1687;
        }

        if (world == null)
        {
            return;
        }

        int sky = world.method_8314(class_1944.field_9284, pos);
        int torch = world.method_8314(class_1944.field_9282, pos);
        int light = class_765.method_23687(torch, sky);
        int overlay = class_4608.method_23625(class_4608.method_23210(0F), class_4608.method_23212(entity.getHurtTimer() > 0));

        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.ENTITY, entity, stack, light, overlay, transition)
            .camera(camera)
            .stencilMap(context.map)
            .color(context.color)
            .renderDepthFrame(context.renderDepthFrame);

        formContext.relative = relative;
        formContext.isShadowPass = context.isShadowPass;
        formContext.viewMatrix = context.viewMatrix;

        stack.method_22903();

        try
        {
            if (relative)
            {
                if (!context.isShadowPass)
                {
                    stack.method_23760().method_23761().identity();
                    stack.method_23760().method_23762().identity();
                }

                if (context.map == null)
                {
                    stack.method_22907(camera.method_23767());
                }
            }

            MatrixStackUtils.multiply(stack, target);

            ModelFormRenderer lookAtRenderer = relative ? null : applyLookAtPose(context, form, position);

            if (context.isShadowPass)
            {
                if (context.shadowOpacity <= 0.001F || (context.shadowRadiusX <= 0F && context.shadowRadiusZ <= 0F))
                {
                    return;
                }

                /* Form Opacity is applied once in the form renderer (applyFormOpacity). Do not
                 * multiply it here or caster alpha becomes opacity² and ground shadows fade too fast. */
                if (form.getFormOpacity() <= 0.001F)
                {
                    return;
                }

                float shadowAlpha = Colors.getA(formContext.color) * context.shadowOpacity;

                if (shadowAlpha <= 0.001F)
                {
                    return;
                }

                /* Replay shadowOpacity only — Color-track effects must not crush caster alpha
                 * (Iris would drop the ground shadow). Opacity 0 already returned above. */
                formContext.color(Colors.setA(formContext.color, MathUtils.clamp(shadowAlpha, 0F, 1F)));

                if (context.shadowOffsetX != 0F || context.shadowOffsetY != 0F || context.shadowOffsetZ != 0F)
                {
                    stack.method_46416(context.shadowOffsetX, context.shadowOffsetY, context.shadowOffsetZ);
                }

                /* Independent X/Z scale from default radius 0.5 — stretch wide or long under Iris. */
                float scaleX = Math.max(0.001F, context.shadowRadiusX / 0.5F);
                float scaleZ = Math.max(0.001F, context.shadowRadiusZ / 0.5F);

                if (Math.abs(scaleX - 1F) > 0.001F || Math.abs(scaleZ - 1F) > 0.001F)
                {
                    stack.method_22905(scaleX, 1F, scaleZ);
                }
            }

            FormUtilsClient.render(form, formContext);

            if (!context.isShadowPass && context.map == null && entity.getFireTicks() > 0)
            {
                MorphFireRenderer.render(stack, context.consumers, entity, form, transition, camera, relative);
            }

            if (context.map == null)
            {
                renderIllusions(context, form, formContext, stack);
            }

            if (lookAtRenderer != null)
            {
                lookAtRenderer.setLookAtPose(null);
            }

            if (UIBaseMenu.renderAxes)
            {
                if (context.bone != null && context.orientation == TransformOrientation.PARENT)
                {
                    Form root = FormUtils.getRoot(form);
                    MatrixCache map = FormUtilsClient.getRenderer(root).collectMatrices(entity, transition);
                    MatrixCacheEntry entry = map.get(context.bone);

                    Matrix4f matrix = entry.origin();

                    if (matrix == null)
                    {
                        matrix = entry.matrix();
                    }

                    if (matrix != null)
                    {
                        stack.method_22903();
                        MatrixStackUtils.multiply(stack, matrix);

                        if (context.map == null)
                        {
                            BaseFilmController.renderGizmo(stack, null);
                        }
                        else
                        {
                            BaseFilmController.renderGizmo(stack, context.map);
                        }

                        RenderSystem.enableDepthTest();
                        stack.method_22909();
                    }
                }
                if (context.bone != null) renderAxes(context.bone, context.orientation, context.map, form, entity, transition, stack);
                if (context.bone2 != null && context.map == null) renderAxes(context.bone2, context.orientation2, context.map, form, entity, transition, stack);
            }
        }
        finally
        {
            stack.method_22909();
        }

        /* Vanilla blob shadows only without Iris shaders — Comp/BSL use the shadow map.
         * Blob opacity is the Shadow track only; form Opacity must not fade the ground circle.
         * Size X/Z are independent (matrix scale); vanilla API only has one radius. */
        if (!relative && context.map == null && opacity > 0F
            && (context.shadowRadiusX > 0F || context.shadowRadiusZ > 0F) && form.render.get()
            && !context.isShadowPass && !mchorse.bbs_mod.utils.iris.IrisUtils.isShaderPackEnabled())
        {
            float shadowOpacity = MathUtils.clamp(opacity * context.shadowOpacity, 0F, 1F);

            if (shadowOpacity > 0F)
            {
                double sx = position.x + context.shadowOffsetX;
                double sy = position.y + context.shadowOffsetY;
                double sz = position.z + context.shadowOffsetZ;

                stack.method_22903();
                stack.method_22904(sx - cx, sy - cy, sz - cz);

                ModelBlockEntityRenderer.renderShadow(context.consumers, stack, transition, sx, sy, sz, 0F, 0F, 0F, context.shadowRadiusX, context.shadowRadiusZ, shadowOpacity);

                stack.method_22909();
            }
        }

        if (!relative && !context.nameTag.isEmpty())
        {
            stack.method_22903();
            stack.method_22904(position.x - cx, position.y - cy, position.z - cz);

            renderNameTag(entity, class_2561.method_43470(StringUtils.processColoredText(context.nameTag)), stack, context.consumers, class_765.field_32767);

            stack.method_22909();
        }

        RenderSystem.enableDepthTest();
    }

    /**
     * Renders purely visual duplicates of the form that spread away from it in the
     * picked directions. They reuse the same form renderer (no extra entities), the
     * gaps between them shrink with each rank, and their opacity fades with distance
     * (optionally inverted).
     */
    private static void renderIllusions(FilmControllerContext context, Form form, FormRenderingContext formContext, class_4587 stack)
    {
        if (context.isShadowPass)
        {
            return;
        }

        List<Illusion> layers = collectIllusionLayers(form);
        boolean hasIllusions = false;

        for (Illusion layer : layers)
        {
            if (layer != null && layer.count > 0)
            {
                hasIllusions = true;

                break;
            }
        }

        if (!hasIllusions)
        {
            return;
        }

        int baseColor = formContext.color;
        int baseLight = formContext.light;
        AABB hitbox = context.entity.getPickingHitbox();
        float height = (float) hitbox.h;

        for (int layer = 0; layer < layers.size(); layer++)
        {
            Illusion layerIllusion = layers.get(layer);

            if (layerIllusion == null || layerIllusion.count <= 0)
            {
                continue;
            }

            Transform layerTransform = createIllusionTransform(form, layerIllusion);

            renderIllusionLayer(context, form, formContext, stack, layerIllusion, layerTransform, hitbox, height, layer, baseColor, baseLight);
        }

        formContext.textureOverride = null;
        formContext.textureBlendOverride = null;
        formContext.color(baseColor);
        formContext.light = baseLight;
        form.glowSettings.setRuntimeValue(null);
    }

    private static List<Illusion> collectIllusionLayers(Form form)
    {
        List<Illusion> layers = new ArrayList<>();

        layers.add(form.illusion.get());
        layers.add(form.illusionOverlay.get());

        for (ValueIllusion overlay : form.additionalIllusions)
        {
            layers.add(overlay.get());
        }

        return layers;
    }

    private static Transform createIllusionTransform(Form form, Illusion illusion)
    {
        Transform transform = new Transform();

        transform.copy(illusion.transform);

        /* Legacy form-level illusion transform tracks (deprecated, kept for old projects) */
        applyIllusionTransformOverlay(transform, form.illusionTransform.get());
        applyIllusionTransformOverlay(transform, form.illusionTransformOverlay.get());

        for (ValueTransform overlay : form.additionalIllusionTransforms)
        {
            applyIllusionTransformOverlay(transform, overlay.get());
        }

        return transform;
    }

    private static void applyIllusionTransformOverlay(Transform transform, Transform overlay)
    {
        transform.translate.add(overlay.translate);
        transform.scale.add(overlay.scale).sub(1F, 1F, 1F);
        transform.rotate.add(overlay.rotate);
        transform.rotate2.add(overlay.rotate2);
        transform.pivot.add(overlay.pivot);
    }

    private static void renderIllusionLayer(FilmControllerContext context, Form form, FormRenderingContext formContext, class_4587 stack, Illusion illusion, Transform illusionTransform, AABB hitbox, float height, int layerIndex, int baseColor, int baseLight)
    {
        List<Vector3f> directions = getIllusionDirections(illusion.directions);
        float strength = Math.max(illusion.opacity, 0F);
        int count = illusion.count;
        int dirCount = directions.size();
        int maxRank = (count + dirCount - 1) / dirCount;
        int textureCount = illusion.textures.size();
        boolean delayed = illusion.delay > 0F && context.replay != null && !Float.isNaN(context.propertyTick);
        int liftKeyBase = layerIndex * 10000;

        for (int i = 0; i < count; i++)
        {
            Vector3f dir = directions.get(i % dirCount);
            int rank = i / dirCount + 1;
            float distance = getIllusionDistance(illusion, hitbox, dir, rank, maxRank);
            float fadeT = maxRank <= 0 ? 1F : (rank - 0.5F) / maxRank;
            float alpha;

            fadeT = MathUtils.clamp(fadeT, 0F, 1F);

            if (illusion.opacityUniform)
            {
                alpha = 1F - strength;
            }
            else
            {
                alpha = illusion.invert ? 1F - strength * (1F - fadeT) : 1F - strength * fadeT;
            }

            alpha = MathUtils.clamp(alpha, 0F, 1F);

            if (alpha <= 0F)
            {
                continue;
            }

            if (delayed)
            {
                float delayedTick = Math.max(context.propertyTick - illusion.delay * (i + 1), 0F);

                context.replay.properties.resetProperties(form);
                context.replay.properties.applyProperties(form, delayedTick);
            }

            float lift = 0F;

            if (illusion.real && !formContext.relative)
            {
                lift = getIllusionLift(context.entity, dir, distance, liftKeyBase + i, formContext.transition);
            }

            Link savedTextureOverride = formContext.textureOverride;
            TextureBlend savedTextureBlendOverride = formContext.textureBlendOverride;

            if (form.illusionTextureBlend != null)
            {
                formContext.textureBlendOverride = form.illusionTextureBlend;
                formContext.textureOverride = null;
            }
            else if (textureCount > 0)
            {
                int index = illusion.randomTextures
                    ? (int) Math.floorMod((i + 1L) * 2654435761L + layerIndex, textureCount)
                    : i % textureCount;

                formContext.textureOverride = illusion.textures.get(index);
                formContext.textureBlendOverride = null;
            }

            Transform partial = null;

            if (!illusionTransform.isDefault())
            {
                float factor = getIllusionTransformFactor(i, count, illusion.gradual, illusion.gradualInvert);

                if (factor > 0F)
                {
                    partial = new Transform();
                    partial.lerp(illusionTransform, factor);
                }
            }

            applyIllusionGlow(form, illusion, i, count);
            float distortFactor = getIllusionDistortFactor(illusion, i, count);
            float x = dir.x * distance;
            float y = dir.y * distance + lift;
            float z = dir.z * distance;
            float mainAlpha = alpha * (1F - distortFactor);

            if (mainAlpha > 0F)
            {
                int a = Math.round(((baseColor >>> 24) & 0xFF) * mainAlpha);

                stack.method_22903();

                try
                {
                    stack.method_46416(x, y, z);

                    if (partial != null)
                    {
                        MatrixStackUtils.multiply(stack, partial.createMatrix());
                    }

                    formContext.color((a << 24) | (baseColor & Colors.RGB));
                    FormUtilsClient.render(form, formContext);
                }
                finally
                {
                    stack.method_22909();
                }
            }

            if (distortFactor > 0F)
            {
                float streakAlpha = alpha * (1F - distortFactor);
                int a = Math.round(((baseColor >>> 24) & 0xFF) * Math.min(streakAlpha + 0.2F * (1F - distortFactor), 1F));

                renderIllusionStreaks(form, formContext, stack, x, y, z, partial, (a << 24) | (baseColor & Colors.RGB), distortFactor, liftKeyBase + i, height);
            }

            formContext.textureOverride = savedTextureOverride;
            formContext.textureBlendOverride = savedTextureBlendOverride;
            formContext.light = baseLight;
            form.glowSettings.setRuntimeValue(null);
        }

        if (delayed)
        {
            context.replay.properties.resetProperties(form);
            context.replay.properties.applyProperties(form, context.propertyTick);
        }
    }

    private static float getIllusionDistance(Illusion illusion, AABB hitbox, Vector3f dir, int rank, int maxRank)
    {
        if (illusion.uniform)
        {
            /* Equal gaps between the illusions */
            return illusion.spacing * rank + illusion.offset;
        }

        /* Gaps shrink linearly with rank: the first gap equals spread, the last one spread / maxRank */
        return illusion.spread * (rank * maxRank - rank * (rank - 1) / 2F) / maxRank + illusion.offset;
    }

    /**
     * Transform gradient across illusion copies. The main model stays at 0; the first
     * illusion gets 1 / count of the transform and the last one gets the full value.
     */
    private static float getIllusionTransformFactor(int index, int count, boolean gradual, boolean invert)
    {
        if (!gradual || count <= 1)
        {
            return 1F;
        }

        float factor = (index + 1F) / count;

        if (invert)
        {
            factor = (count - index) / (float) count;
        }

        return factor;
    }

    private static float getIllusionGradientWeight(int index, int count, boolean uniform, boolean invert)
    {
        if (uniform || count <= 1)
        {
            return 1F;
        }

        /* Keep both ends in range so the first and last copies always receive some effect. */
        float weight = (count - index) / (float) count;

        if (invert)
        {
            weight = (index + 1F) / count;
        }

        return weight;
    }

    /**
     * Glow gradient across illusion copies. The main model stays at 0; the first
     * illusion starts low (~1 / (count + 1)) and the last one reaches full strength.
     */
    private static float getIllusionGlowWeight(int index, int count, boolean uniform, boolean invert)
    {
        if (uniform || count <= 1)
        {
            return 1F;
        }

        float minWeight = 1F / count;
        float weight = (index + 1F) / count;

        if (invert)
        {
            weight = (count - index) / (float) count;
        }

        return minWeight + (1F - minWeight) * weight;
    }

    private static float getIllusionDistortFactor(Illusion illusion, int index, int count)
    {
        if (illusion.distort <= 0F)
        {
            return 0F;
        }

        float weight = getIllusionGradientWeight(index, count, illusion.distortUniform, illusion.distortInvert);

        return MathUtils.clamp(illusion.distort * weight, 0F, 1F);
    }

    /**
     * Applies the illusion glow through the standard glow shader path (same as the
     * main model). Intensity ramps from the first illusion to the last by default.
     */
    private static void applyIllusionGlow(Form form, Illusion illusion, int index, int count)
    {
        if (illusion.glow == 0F)
        {
            return;
        }

        GlowSettings base = form.glowSettings.get();
        GlowSettings override = base.copy();
        float weight = getIllusionGlowWeight(index, count, illusion.glowUniform, illusion.glowInvert);

        override.intensity = illusion.glow * weight;
        form.glowSettings.setRuntimeValue(override);
    }

    /**
     * Renders the disintegration streaks of an illusion: squashed, stretched and
     * jittered copies of the model that look like the horizontal slices it falls
     * apart into. The randomness is stable per illusion and re-rolls a few times a
     * second for a glitchy feel.
     */
    private static void renderIllusionStreaks(Form form, FormRenderingContext formContext, class_4587 stack, float x, float y, float z, Transform partial, int argb, float distortFactor, int index, float height)
    {
        if (((argb >>> 24) & 0xFF) <= 0)
        {
            return;
        }

        Random random = new Random(index * 49297L);
        int streaks = 2 + Math.round(distortFactor * 5F);

        formContext.color(argb);

        for (int s = 0; s < streaks; s++)
        {
            float yPos = (0.1F + 0.8F * random.nextFloat()) * Math.max(height, 0.5F);
            float jx = (random.nextFloat() - 0.5F) * (0.3F + distortFactor);
            float jz = (random.nextFloat() - 0.5F) * (0.3F + distortFactor);
            float squash = 0.03F + random.nextFloat() * 0.09F;
            float stretch = 1F + random.nextFloat() * (0.5F + distortFactor);

            stack.method_22903();

            try
            {
                stack.method_46416(x + jx, y + yPos * (1F - squash), z + jz);

                if (partial != null)
                {
                    MatrixStackUtils.multiply(stack, partial.createMatrix());
                }

                stack.method_22905(stretch, squash, stretch);
                FormUtilsClient.render(form, formContext);
            }
            finally
            {
                stack.method_22909();
            }
        }
    }

    private static List<Vector3f> getIllusionDirections(int mask)
    {
        List<Vector3f> directions = new ArrayList<>();

        if (mask == 0)
        {
            mask = Illusion.FRONT | Illusion.LEFT | Illusion.RIGHT | Illusion.BACK;
        }

        if ((mask & Illusion.FRONT) != 0) directions.add(new Vector3f(0F, 0F, 1F));
        if ((mask & Illusion.LEFT) != 0) directions.add(new Vector3f(1F, 0F, 0F));
        if ((mask & Illusion.RIGHT) != 0) directions.add(new Vector3f(-1F, 0F, 0F));
        if ((mask & Illusion.BACK) != 0) directions.add(new Vector3f(0F, 0F, -1F));
        if ((mask & Illusion.UP) != 0) directions.add(new Vector3f(0F, 1F, 0F));
        if ((mask & Illusion.DOWN) != 0) directions.add(new Vector3f(0F, -1F, 0F));

        return directions;
    }

    /**
     * How much a "real" illusion has to be moved vertically so it stands on top of
     * the terrain at its spot (the illusion's local offset is rotated by the
     * entity's body yaw to find its world position first). It can both climb onto
     * blocks in its way (up to 3 blocks) and drop down when the ground is lower,
     * and the movement is smoothed over time so it looks like a natural little hop
     * instead of an instant snap.
     */
    private static float getIllusionLift(IEntity entity, Vector3f dir, float distance, int index, float transition)
    {
        class_1937 world = entity.getWorld();

        if (world == null)
        {
            return 0F;
        }

        double yaw = MathUtils.toRad(Lerps.lerp(entity.getPrevBodyYaw(), entity.getBodyYaw(), transition));
        double lx = dir.x * distance;
        double lz = dir.z * distance;
        double x = Lerps.lerp(entity.getPrevX(), entity.getX(), transition) + lx * Math.cos(yaw) - lz * Math.sin(yaw);
        double y = Lerps.lerp(entity.getPrevY(), entity.getY(), transition) + dir.y * distance;
        double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition) + lx * Math.sin(yaw) + lz * Math.cos(yaw);
        float target = getIllusionGroundDelta(world, x, y, z);

        long key = ((long) System.identityHashCode(entity) << 20) | (index & 0xFFFFF);
        long now = System.currentTimeMillis();
        IllusionLift lift = ILLUSION_LIFTS.get(key);

        if (lift == null)
        {
            if (ILLUSION_LIFTS.size() > 16384)
            {
                ILLUSION_LIFTS.clear();
            }

            lift = new IllusionLift();
            lift.value = target;
            lift.time = now;
            ILLUSION_LIFTS.put(key, lift);

            return target;
        }

        float dt = MathUtils.clamp((now - lift.time) / 1000F, 0F, 0.25F);

        lift.value = Lerps.lerp(lift.value, target, 1F - (float) Math.exp(-12F * dt));
        lift.time = now;

        return lift.value;
    }

    /**
     * The vertical offset between the given world position and the terrain surface
     * at that spot: positive when there are blocks in the way (climb on top of
     * them), negative when the ground is lower (drop down onto it), 0 when there's
     * no ground within range.
     */
    private static float getIllusionGroundDelta(class_1937 world, double x, double y, double z)
    {
        for (int i = 0; i <= 6; i++)
        {
            class_2338 blockPos = class_2338.method_49637(x, y + 3D - i, z);
            class_265 shape = world.method_8320(blockPos).method_26220(world, blockPos);

            if (shape.method_1110())
            {
                continue;
            }

            double top = blockPos.method_10264() + shape.method_1105(class_2350.class_2351.field_11052);

            return MathUtils.clamp((float) (top - y), -3F, 3F);
        }

        return 0F;
    }

    /**
     * Applies the translation part of the "Look at" constraint: when the translate
     * option is enabled, the form follows the displacement of the strongest locked
     * bone's target, scaled by that bone's lock strength.
     */
    /**
     * World-space point of a replay's attachment, used by look-at and inverse kinematics.
     */
    public static Vector3d resolveReplayAttachmentPoint(FilmControllerContext context, int replayIndex, String attachment)
    {
        if (context == null || replayIndex < 0)
        {
            return null;
        }

        IEntity targetEntity = context.entities.get(replayIndex);

        if (targetEntity == null)
        {
            return null;
        }

        return getLookAtTargetPoint(targetEntity, attachment, context.transition);
    }

    /**
     * World-space orientation of a replay's attachment, used by inverse kinematics
     * angle targets.
     */
    public static Quaternionf resolveReplayAttachmentRotation(FilmControllerContext context, int replayIndex, String attachment)
    {
        if (context == null || replayIndex < 0)
        {
            return null;
        }

        IEntity targetEntity = context.entities.get(replayIndex);

        if (targetEntity == null)
        {
            return null;
        }

        return getLookAtTargetRotation(targetEntity, attachment, context.transition);
    }

    private static void applyLookAt(FilmControllerContext context, Form form, Vector3d position, Matrix4f target)
    {
        LookAt lookAt = form.lookAt.get();

        if (lookAt == null || !lookAt.translate || context.film == null)
        {
            return;
        }

        LookAtBone strongest = null;

        for (LookAtBone bone : lookAt.bones.values())
        {
            if (bone.isActive() && (strongest == null || bone.blend > strongest.blend))
            {
                strongest = bone;
            }
        }

        if (strongest == null)
        {
            return;
        }

        IEntity targetEntity = context.entities.get(strongest.replay);

        if (targetEntity == null || targetEntity == context.entity)
        {
            return;
        }

        Replay targetReplay = CollectionUtils.getSafe(context.film.replays.getList(), strongest.replay);

        if (targetReplay == null)
        {
            return;
        }

        float transition = context.transition;
        float blend = MathUtils.clamp(strongest.blend, 0F, 1F);
        float restorePropertyTick = getLookAtRestorePropertyTick(context, targetReplay, transition);

        Vector3d pointNow = getLookAtTargetPoint(targetEntity, strongest.attachment, transition);
        Vector3d pointBase = getLookAtTargetPointAtPropertyTick(targetReplay, targetEntity, strongest.attachment, 0F, transition, restorePropertyTick);
        double dx = (pointNow.x - pointBase.x) * blend;
        double dy = (pointNow.y - pointBase.y) * blend;
        double dz = (pointNow.z - pointBase.z) * blend;

        position.add(dx, dy, dz);

        Vector3f translation = target.getTranslation(new Vector3f());

        target.setTranslation(translation.x + (float) dx, translation.y + (float) dy, translation.z + (float) dz);
    }

    /**
     * Computes the extra yaw/pitch (in the model's local space) that would make the
     * entity fully face the look at target, or null when the direction is degenerate.
     */
    private static Vector2f getLookAtRotation(IEntity entity, IEntity targetEntity, String attachment, Vector3d position, float transition)
    {
        Vector3d targetPoint = getLookAtTargetPoint(targetEntity, attachment, transition);
        double dirX = targetPoint.x - position.x;
        double dirY = targetPoint.y - position.y;
        double dirZ = targetPoint.z - position.z;
        double horizontal = Math.sqrt(dirX * dirX + dirZ * dirZ);

        if (horizontal * horizontal + dirY * dirY < 0.0001D)
        {
            return null;
        }

        /* Entities face (-sin(yaw), 0, cos(yaw)), and the matrix contains rotateY(-bodyYaw),
         * so the desired matrix rotation that faces the target is atan2(dirX, dirZ) */
        float desiredYaw = (float) Math.atan2(dirX, dirZ);
        float currentYaw = MathUtils.toRad(-Lerps.lerp(entity.getPrevBodyYaw(), entity.getBodyYaw(), transition));
        float deltaYaw = desiredYaw - currentYaw;

        /* Wrap into -PI..PI so the blended rotation takes the shortest path */
        deltaYaw = (float) Math.atan2(Math.sin(deltaYaw), Math.cos(deltaYaw));

        float pitch = (float) Math.atan2(dirY, horizontal);

        return new Vector2f(deltaYaw, pitch);
    }

    /**
     * Property tick at which the target replay's form properties should be restored
     * after temporarily sampling another tick for look at translate follow.
     */
    private static float getLookAtRestorePropertyTick(FilmControllerContext context, Replay targetReplay, float transition)
    {
        if (context.filmTick >= 0)
        {
            return targetReplay.getTick(context.filmTick) + transition;
        }

        if (targetReplay == context.replay && !Float.isNaN(context.propertyTick))
        {
            return context.propertyTick;
        }

        return Float.NaN;
    }

    /**
     * Visual offset matrix for a look at target: a picked attachment bone, the form
     * root (including transform overlays), or the anchor attachment as fallback.
     */
    private static Matrix4f getLookAtVisualMatrix(MatrixCache map, Form targetForm, String attachment)
    {
        Matrix4f visualMatrix = null;

        if (attachment != null && !attachment.isEmpty())
        {
            MatrixCacheEntry entry = map.get(attachment.replace("#origin", ""));

            if (entry != null)
            {
                visualMatrix = entry.origin() != null ? entry.origin() : entry.matrix();
            }
        }
        else
        {
            MatrixCacheEntry entry = map.get("");

            if (entry != null)
            {
                visualMatrix = entry.origin() != null ? entry.origin() : entry.matrix();
            }

            if (visualMatrix == null)
            {
                Anchor anchor = targetForm.anchor.get();

                if (anchor != null && !anchor.attachment.isEmpty())
                {
                    entry = map.get(anchor.attachment.replace("#origin", ""));

                    if (entry != null)
                    {
                        visualMatrix = entry.origin() != null ? entry.origin() : entry.matrix();
                    }
                }
            }
        }

        return visualMatrix;
    }

    /**
     * Entity matrix from replay position keyframes at the given property tick, without
     * using the entity's live coordinates.
     */
    private static Matrix4f getMatrixForReplayKeyframes(Replay replay, float propertyTick, float transition)
    {
        double x = replay.keyframes.x.interpolate(propertyTick);
        double y = replay.keyframes.y.interpolate(propertyTick);
        double z = replay.keyframes.z.interpolate(propertyTick);
        double prevX = replay.keyframes.x.interpolate(propertyTick - 1F);
        double prevY = replay.keyframes.y.interpolate(propertyTick - 1F);
        double prevZ = replay.keyframes.z.interpolate(propertyTick - 1F);
        float bodyYaw = replay.keyframes.bodyYaw.interpolate(propertyTick).floatValue();
        float prevBodyYaw = replay.keyframes.bodyYaw.interpolate(propertyTick - 1F).floatValue();
        Matrix4f matrix = new Matrix4f();

        matrix.translate(
            (float) Lerps.lerp(prevX, x, transition),
            (float) Lerps.lerp(prevY, y, transition),
            (float) Lerps.lerp(prevZ, z, transition)
        );
        float yaw = (float) Lerps.lerpYaw(prevBodyYaw, bodyYaw, transition);

        matrix.rotateY(MathUtils.toRad(-yaw));

        return matrix;
    }

    /**
     * World position of the look at target. When an attachment bone is picked, the
     * bone's matrix is used (which reacts to the target's pose animation), otherwise
     * the target form's full visual transform is taken into account.
     */
    private static Vector3d getLookAtTargetPoint(IEntity targetEntity, String attachment, float transition)
    {
        Matrix4f matrix = getMatrixForRenderWithRotation(targetEntity, 0D, 0D, 0D, transition);
        Form targetForm = targetEntity.getForm();

        if (targetForm != null)
        {
            MatrixCache map = FormUtilsClient.getRenderer(targetForm).collectMatrices(targetEntity, transition);
            Matrix4f visualMatrix = getLookAtVisualMatrix(map, targetForm, attachment);

            if (visualMatrix != null)
            {
                matrix.mul(visualMatrix);
            }
        }

        Vector3f translation = matrix.getTranslation(new Vector3f());

        return new Vector3d(translation);
    }

    private static Quaternionf getLookAtTargetRotation(IEntity targetEntity, String attachment, float transition)
    {
        Matrix4f matrix = getMatrixForRenderWithRotation(targetEntity, 0D, 0D, 0D, transition);
        Form targetForm = targetEntity.getForm();

        if (targetForm != null)
        {
            MatrixCache map = FormUtilsClient.getRenderer(targetForm).collectMatrices(targetEntity, transition);
            Matrix4f visualMatrix = getLookAtVisualMatrix(map, targetForm, attachment);

            if (visualMatrix != null)
            {
                matrix.mul(visualMatrix);
            }
        }

        return matrix.getNormalizedRotation(new Quaternionf());
    }

    /**
     * Same as {@link #getLookAtTargetPoint} but samples the target replay at a specific
     * property tick (for example tick 0 as the translate follow baseline). Restores the
     * target form's properties afterward when {@code restorePropertyTick} is not NaN.
     */
    private static Vector3d getLookAtTargetPointAtPropertyTick(Replay replay, IEntity targetEntity, String attachment, float propertyTick, float transition, float restorePropertyTick)
    {
        Form form = targetEntity.getForm();
        Matrix4f matrix = getMatrixForReplayKeyframes(replay, propertyTick, transition);

        if (form != null)
        {
            replay.properties.resetProperties(form);
            replay.properties.applyProperties(form, propertyTick);

            MatrixCache map = FormUtilsClient.getRenderer(form).collectMatrices(targetEntity, transition);
            Matrix4f visualMatrix = getLookAtVisualMatrix(map, form, attachment);

            if (visualMatrix != null)
            {
                matrix.mul(visualMatrix);
            }

            if (!Float.isNaN(restorePropertyTick))
            {
                replay.properties.resetProperties(form);
                replay.properties.applyProperties(form, restorePropertyTick);
            }
        }

        Vector3f translation = matrix.getTranslation(new Vector3f());

        return new Vector3d(translation);
    }

    /**
     * Sets a temporary look at pose on the form's renderer. Every locked bone gets
     * rotated toward its own target (replay and optionally attachment), scaled by
     * its own lock strength. The returned renderer must be cleared with
     * setLookAtPose(null) after rendering.
     */
    private static ModelFormRenderer applyLookAtPose(FilmControllerContext context, Form form, Vector3d position)
    {
        LookAt lookAt = form.lookAt.get();

        if (lookAt == null || !lookAt.isActive())
        {
            return null;
        }

        if (!(FormUtilsClient.getRenderer(form) instanceof ModelFormRenderer renderer))
        {
            return null;
        }

        Pose pose = new Pose();

        for (Map.Entry<String, LookAtBone> entry : lookAt.bones.entrySet())
        {
            LookAtBone bone = entry.getValue();

            if (!bone.isActive())
            {
                continue;
            }

            IEntity targetEntity = context.entities.get(bone.replay);

            if (targetEntity == null || targetEntity == context.entity)
            {
                continue;
            }

            Vector2f rotation = getLookAtRotation(context.entity, targetEntity, bone.attachment, position, context.transition);

            if (rotation == null)
            {
                continue;
            }

            float blend = MathUtils.clamp(bone.blend, 0F, 1F);
            PoseTransform poseTransform = pose.get(entry.getKey());

            poseTransform.rotate.y = rotation.x * blend;
            poseTransform.rotate.x = rotation.y * blend;
        }

        if (pose.isEmpty())
        {
            return null;
        }

        renderer.setLookAtPose(pose);

        return renderer;
    }

    private static void renderGizmo(class_4587 stack, StencilMap stencilMap)
    {
        if (stencilMap == null)
        {
            /* Visual is drawn later in the panel UI pass (Gizmo#renderInterface). */
            Gizmo.INSTANCE.captureVisual(stack);
        }
        else
        {
            /* Same as model editor: handles must win at the cursor even when behind mesh. */
            RenderSystem.disableDepthTest();
            Gizmo.INSTANCE.renderStencil(stack, stencilMap);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    private static void renderAxes(String bone, TransformOrientation space, StencilMap stencilMap, Form form, IEntity entity, float transition, class_4587 stack)
    {
        Form root = FormUtils.getRoot(form);
        MatrixCache map = FormUtilsClient.getRenderer(root).collectMatrices(entity, transition);
        MatrixCacheEntry entry = map.get(bone);

        if (entry == null)
        {
            return;
        }

        Matrix4f matrix;
        Form rootForm = FormUtils.getRoot(form);
        boolean bobj = rootForm instanceof ModelForm modelForm && ModelFormRenderer.isBobjModel(modelForm);

        matrix = GizmoMatrixUtils.resolveFilmPoseBoneMatrix(entry, space, bobj);

        if (matrix != null)
        {
            Gizmo.INSTANCE.setActiveOrientation(space);
            stack.method_22903();
            MatrixStackUtils.multiply(stack, matrix);

            if (stencilMap == null)
            {
                BaseFilmController.renderGizmo(stack, null);
            }
            else
            {
                BaseFilmController.renderGizmo(stack, stencilMap);
            }

            RenderSystem.enableDepthTest();
            stack.method_22909();
        }
    }

    public static Pair<Matrix4f, Float> getTotalMatrix(IntObjectMap<IEntity> entities, Anchor value, Matrix4f defaultMatrix, double cx, double cy, double cz, float transition, int i)
    {
        /* Stupid recursion stop, I don't think anyone would need more than that */
        if (i > 5)
        {
            return new Pair<>(defaultMatrix, 1F);
        }

        boolean same = value.previous == null || Objects.equals(value, value.previous);
        boolean only = value.x <= 0F && value.previous != null;
        Pair<Matrix4f, Float> result = new Pair<>(null, 1F);

        if (same || only)
        {
            Matrix4f matrix = getEntityMatrix(entities, cx, cy, cz, same ? value : value.previous, defaultMatrix, transition, i);

            matrix = applyAnchorTransform(matrix, same ? value : value.previous);

            if (matrix != defaultMatrix)
            {
                result.a = matrix;
                result.b = 0F;
            }
        }
        else
        {
            Matrix4f matrix = getEntityMatrix(entities, cx, cy, cz, value, defaultMatrix, transition, i);
            Matrix4f lastMatrix = getEntityMatrix(entities, cx, cy, cz, value.previous, defaultMatrix, transition, i);

            matrix = applyAnchorTransform(matrix, value);
            lastMatrix = applyAnchorTransform(lastMatrix, value.previous);

            result.a = value.x >= 1F ? matrix : Matrices.lerp(lastMatrix, matrix, value.x);

            if (value.isFadeOut()) result.b = value.x;
            else if (value.isFadeIn()) result.b = 1F - value.x;
            else result.b = 0F;
        }

        return result;
    }

    private static Matrix4f applyAnchorTransform(Matrix4f matrix, Anchor anchor)
    {
        if (matrix == null || anchor == null || anchor.transform.isDefault())
        {
            return matrix;
        }

        return matrix.mul(anchor.transform.createMatrix());
    }

    public static Matrix4f getEntityMatrix(IntObjectMap<IEntity> entities, double cameraX, double cameraY, double cameraZ, Anchor anchor, Matrix4f defaultMatrix, float transition, int i)
    {
        IEntity entity = entities.get(anchor.replay);

        if (entity != null)
        {
            Matrix4f basic = getMatrixForRenderWithRotation(entity, cameraX, cameraY, cameraZ, transition);

            Form form = entity.getForm();

            if (form != null)
            {
                Pair<Matrix4f, Float> totalMatrix = getTotalMatrix(entities, form.anchor.get(), basic, cameraX, cameraY, cameraZ, transition, i + 1);

                if (totalMatrix.a != null)
                {
                    basic = totalMatrix.a;
                }

                MatrixCache map = FormUtilsClient.getRenderer(form).collectMatrices(entity, transition);
                boolean forceOrigin = anchor.attachment != null && anchor.attachment.endsWith("#origin");
                String core = anchor.attachment == null ? null : anchor.attachment.replace("#origin", "");
                
                MatrixCacheEntry entry = map.get(core);
                Matrix4f matrix = null;

                if (entry != null)
                {
                    if (forceOrigin)
                    {
                        matrix = entry.origin();
                    }
                    else if (anchor.translate)
                    {
                        matrix = entry.origin();
                        if (matrix == null)
                        {
                            matrix = entry.matrix();
                        }
                    }
                    else
                    {
                        matrix = entry.matrix();
                        if (matrix == null)
                        {
                            matrix = entry.origin();
                        }
                    }
                }

                if (matrix != null)
                {
                    basic.mul(matrix);

                    if (anchor.scale)
                    {
                        Matrix3f mat = new Matrix3f();
                        Vector3f v = new Vector3f();
                        basic.get3x3(mat);

                        mat.getColumn(0, v); v.normalize(); mat.setColumn(0, v);
                        mat.getColumn(1, v); v.normalize(); mat.setColumn(1, v);
                        mat.getColumn(2, v); v.normalize(); mat.setColumn(2, v);

                        basic.set3x3(mat);
                    }

                    if (anchor.translate)
                    {
                        Vector3f t = new Vector3f();
                        basic.getTranslation(t);
                        basic.set(defaultMatrix);
                        basic.setTranslation(t);
                    }
                }
            }

            return basic;
        }

        return defaultMatrix;
    }

    public static Matrix4f getMatrixForRenderWithRotation(IEntity entity, double cameraX, double cameraY, double cameraZ, float tickDelta)
    {
        double x = Lerps.lerp(entity.getPrevX(), entity.getX(), tickDelta) - cameraX;
        double y = Lerps.lerp(entity.getPrevY(), entity.getY(), tickDelta) - cameraY;
        double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), tickDelta) - cameraZ;

        Matrix4f matrix = new Matrix4f();

        float bodyYaw = (float) Lerps.lerpYaw(entity.getPrevBodyYaw(), entity.getBodyYaw(), tickDelta);

        matrix.translate((float) x, (float) y, (float) z);
        matrix.rotateY(MathUtils.toRad(-bodyYaw));

        return matrix;
    }

    private static void renderNameTag(IEntity entity, class_2561 text, class_4587 matrices, class_4597 vertexConsumers, int light)
    {
        boolean sneaking = !entity.isSneaking();
        float hitboxH = (float) entity.getPickingHitbox().h + (entity.isSneaking() ? 0.25F : 0.5F);


        matrices.method_22903();
        matrices.method_46416(0F, hitboxH, 0F);
        matrices.method_22907(class_310.method_1551().method_1561().method_24197());
        matrices.method_22905(0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = matrices.method_23760().method_23761();
        class_327 textRenderer = class_310.method_1551().field_1772;

        float opacity = class_310.method_1551().field_1690.method_19343(0.25F);
        int background = (int) (opacity * 255F) << 24;
        float h = (float) (-textRenderer.method_27525(text) / 2);

        int maxLight = class_765.field_32767;

            RenderSystem.enableBlend();
            RenderSystem.disableCull();

            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                RenderSystem.disableDepthTest();
            });

            textRenderer.method_30882(text, h, 0, 0x00FFFFFF, false, matrix4f, consumers, class_327.class_6415.field_33993, background, maxLight);
            consumers.draw();

            textRenderer.method_30882(text, h, 0, -1, false, matrix4f, consumers, class_327.class_6415.field_33993, 0, maxLight);
            consumers.draw();

            CustomVertexConsumerProvider.clearRunnables();
            RenderSystem.enableDepthTest();

            RenderSystem.enableCull();
            RenderSystem.disableBlend();

        matrices.method_22909();
    }

    /* Film controller */

    public BaseFilmController(Film film)
    {
        this.film = film;
    }

    public IntObjectMap<IEntity> getEntities()
    {
        return this.entities;
    }

    public void togglePause()
    {
        this.paused = !this.paused;
    }

    public void createEntities()
    {
        this.entities.clear();
        this.replayMap.clear();

        if (this.film == null)
        {
            return;
        }

        int i = 0;

        for (Replay replay : this.film.replays.getList())
        {
            MobCemPoseCapture.syncReplay(replay);
            this.replayMap.put(replay.uuid.get(), replay);
            this.replayMap.put(replay.getId(), replay);

            if (this.isReplayEnabled(replay))
            {
                class_1937 world = class_310.method_1551().field_1687;
                IEntity entity = new StubEntity(world);

                entity.setForm(FormUtils.copy(replay.form.get()));
                replay.keyframes.apply(0, entity);
                entity.setPrevX(entity.getX());
                entity.setPrevY(entity.getY());
                entity.setPrevZ(entity.getZ());

                entity.setPrevYaw(entity.getYaw());
                entity.setPrevHeadYaw(entity.getHeadYaw());
                entity.setPrevPitch(entity.getPitch());
                entity.setPrevBodyYaw(entity.getBodyYaw());

                this.entities.put(i, entity);
            }

            i += 1;
        }
    }

    public abstract Map<String, Integer> getActors();

    public abstract int getTick();

    public boolean hasFinished()
    {
        return false;
    }

    public void update()
    {
        this.updateEntities(this.getTick());
    }

    protected void updateEntities(int ticks)
    {
        List<Replay> replays = this.film.replays.getList();

        MorphMountSync.assignMountTargets(this.entities, replays, ticks);

        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            Replay replay = CollectionUtils.getSafe(replays, i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.UPDATE))
            {
                continue;
            }

            if (replay != null)
            {
                int replayTick = replay.getTick(ticks);

                this.applyReplay(replay, replayTick, entity);
            }
        }

        MorphMountSync.syncMountedState(this.entities, replays, ticks);

        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            Replay replay = CollectionUtils.getSafe(replays, i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.UPDATE))
            {
                continue;
            }

            if (replay != null)
            {
                int replayTick = replay.getTick(ticks);

                this.updateEntityAndForm(entity, replayTick);

                boolean spawned = false;
                boolean mounted = entity.getMountTarget() != null;

                Map<String, Integer> actors = this.getActors();

                if (actors != null)
                {
                    Integer entityId = actors.get(replay.getId());

                    if (entityId != null)
                    {
                        class_1297 anEntity = class_310.method_1551().field_1687.method_8469(entityId);

                        if (anEntity instanceof ActorEntity actor)
                        {
                            /* IEntity already has mount rotation applied by MorphMountSync */
                            actor.method_36456(entity.getYaw());
                            actor.method_5847(entity.getHeadYaw());
                            actor.method_5636(entity.getBodyYaw());
                            actor.method_36457(entity.getPitch());
                            replay.applyClientActions(replayTick, new MCEntity(anEntity), this.film);

                            spawned = true;
                        }
                        else if (anEntity instanceof class_1657 player)
                        {
                            if (!mounted)
                            {
                                double x = replay.keyframes.x.interpolate(replayTick);
                                double y = replay.keyframes.y.interpolate(replayTick);
                                double z = replay.keyframes.z.interpolate(replayTick);
                                double prevX = replay.keyframes.x.interpolate(replayTick - 1);
                                double prevY = replay.keyframes.y.interpolate(replayTick - 1);
                                double prevZ = replay.keyframes.z.interpolate(replayTick - 1);

                                player.method_18800(x - prevX, y - prevY, z - prevZ);

                                this.spawnSprintParticles(replay, replayTick, player);
                            }
                            else
                            {
                                player.method_18800(0D, 0D, 0D);
                            }

                            spawned = true;
                        }
                    }
                }

                if (!spawned && !mounted)
                {
                    class_1937 world = class_310.method_1551().field_1687;
                    Form form = replay.form.get();
                    double width = form != null ? form.hitboxWidth.get() : 0.6D;

                    this.spawnReplayStepSound(replay, replayTick, world);
                    this.spawnSprintParticles(replay, replayTick, world, width);
                }
            }
        }
    }

    public void updateEndWorld()
    {
        int ticks = this.getTick();

        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            List<Replay> replays = this.film.replays.getList();
            Replay replay = CollectionUtils.getSafe(replays, i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.UPDATE))
            {
                continue;
            }

            if (replay != null)
            {
                int replayTick = replay.getTick(ticks);

                Map<String, Integer> actors = this.getActors();

                if (actors != null)
                {
                    Integer entityId = actors.get(replay.getId());

                    if (entityId != null)
                    {
                        class_1297 anEntity = class_310.method_1551().field_1687.method_8469(entityId);

                        if (anEntity instanceof class_1657 player)
                        {
                            double x = replay.keyframes.x.interpolate(replayTick);
                            double y = replay.keyframes.y.interpolate(replayTick);
                            double z = replay.keyframes.z.interpolate(replayTick);
                            boolean sneaking = replay.keyframes.sneaking.interpolate(replayTick) > 0;
                            boolean sprinting = replay.keyframes.sprinting.interpolate(replayTick) > 0;
                            boolean swimming = replay.keyframes.swimming.interpolate(replayTick) > 0;
                            boolean flying = replay.keyframes.flying.interpolate(replayTick) > 0;
                            boolean fallFlying = replay.keyframes.fallFlying.interpolate(replayTick) > 0;
                            boolean crawling = replay.keyframes.crawling.interpolate(replayTick) > 0;
                            boolean climbing = replay.keyframes.climbing.interpolate(replayTick) > 0;
                            boolean blocking = replay.keyframes.blocking.interpolate(replayTick) > 0;
                            boolean sleeping = replay.keyframes.sleeping.interpolate(replayTick) > 0;
                            boolean riptide = replay.keyframes.riptide.interpolate(replayTick) > 0;
                            boolean grounded = replay.keyframes.grounded.interpolate(replayTick) > 0;

                            class_243 pos = player.method_19538();

                            if (BBSSettings.editorReplayStepSound == null || BBSSettings.editorReplayStepSound.get())
                            {
                                player.method_24830(grounded);
                                player.method_5784(class_1313.field_6308, new class_243(x - pos.field_1352, y - pos.field_1351, z - pos.field_1350));
                            }

                            player.method_5814(x, y, z);

                            player.method_5660(sneaking);
                            player.method_5728(sprinting);
                            player.method_5796(swimming);
                            player.method_31549().field_7479 = flying;
                            player.method_5729(7, fallFlying);
                            player.method_5729(4, riptide);

                            if (crawling)
                            {
                                player.method_18380(class_4050.field_18079);
                            }
                            else if (sleeping)
                            {
                                player.method_18380(class_4050.field_18078);
                            }

                            if (blocking)
                            {
                                player.method_6085(1, true);
                            }

                            player.method_24830(grounded);

                            if (player instanceof ClientPlayerEntityAccessor accessor)
                            {
                                accessor.bbs$setIsSneakingPose(sneaking);
                            }

                            if (player instanceof class_746 playerEntity)
                            {
                                playerEntity.field_3913.field_3903 = sneaking;
                            }

                            player.field_6017 = replay.keyframes.fall.interpolate(replayTick).floatValue();
                        }
                    }
                }
            }
        }
    }

    protected void updateEntityAndForm(IEntity entity, int tick)
    {
        entity.setAge(tick);
        entity.update();

        if (entity.getForm() != null)
        {
            entity.getForm().update(entity);
        }
    }

    protected void applyReplay(Replay replay, int ticks, IEntity entity)
    {
        replay.keyframes.apply(ticks, entity);
        replay.applyClientActions(ticks, entity, this.film);
    }

      private void spawnSprintParticles(Replay replay, int ticks, class_1297 entity)
    {
        if (entity == null)
        {
            return;
        }

        this.spawnSprintParticles(replay, ticks, entity.method_37908(), entity.method_17681());
    }

    private void spawnSprintParticles(Replay replay, int ticks, class_1937 world, double width)
    {
        if (!BBSSettings.editorReplaySprintParticles.get() || replay == null || world == null)
        {
            return;
        }

        if (!this.isReplayVisible(replay, ticks))
        {
            return;
        }

        if (replay.keyframes.sprinting.interpolate(ticks) <= 0D)
        {
            return;
        }

        if (replay.keyframes.grounded.interpolate(ticks) <= 0D)
        {
            return;
        }

        double vX = replay.keyframes.vX.interpolate(ticks);
        double vZ = replay.keyframes.vZ.interpolate(ticks);

        if ((vX * vX + vZ * vZ) < 0.001D)
        {
            return;
        }

        double xPos = replay.keyframes.x.interpolate(ticks);
        double yPos = replay.keyframes.y.interpolate(ticks);
        double zPos = replay.keyframes.z.interpolate(ticks);

        class_2338 pos = class_2338.method_49637(xPos, yPos - 0.2D, zPos);

        if (world.method_22347(pos))
        {
            return;
        }

        double x = xPos + (world.field_9229.method_43058() - 0.5D) * width;
        double y = yPos + 0.1D;
        double z = zPos + (world.field_9229.method_43058() - 0.5D) * width;

        world.method_8406(new class_2388(class_2398.field_11217, world.method_8320(pos)), x, y, z, 0D, 0.1D, 0D);
    }

    private void spawnReplayStepSound(Replay replay, int ticks, class_1937 world)
    {
        if (BBSSettings.editorReplayStepSound == null || !BBSSettings.editorReplayStepSound.get() || replay == null || world == null)
        {
            return;
        }

        if (this.paused)
        {
            return;
        }

        if (!this.isReplayVisible(replay, ticks))
        {
            return;
        }

        if (replay.keyframes.grounded.interpolate(ticks) <= 0D)
        {
            return;
        }

        /* Reduce spam and approximate vanilla stepping cadence. */
        if ((ticks & 7) != 0)
        {
            return;
        }

        double vX = replay.keyframes.vX.interpolate(ticks);
        double vZ = replay.keyframes.vZ.interpolate(ticks);

        if ((vX * vX + vZ * vZ) < 0.01D)
        {
            return;
        }

        double xPos = replay.keyframes.x.interpolate(ticks);
        double yPos = replay.keyframes.y.interpolate(ticks);
        double zPos = replay.keyframes.z.interpolate(ticks);
        class_2338 pos = class_2338.method_49637(xPos, yPos - 0.2D, zPos);

        if (world.method_22347(pos))
        {
            return;
        }

        var soundGroup = world.method_8320(pos).method_26231();

        world.method_8486(
            xPos,
            yPos,
            zPos,
            soundGroup.method_10594(),
            class_3419.field_15248,
            soundGroup.method_10597() * 0.15F,
            soundGroup.method_10599(),
            false
        );
    }

    public boolean isReplayVisible(Replay replay, int ticks)
    {
        if (!this.isReplayEnabled(replay))
        {
            return false;
        }

        if (!this.isReplayVisibleAt(replay, ticks))
        {
            return false;
        }

        if (!replay.group.get().isEmpty())
        {
            String[] groups = replay.group.get().split("/");

            for (String uuid : groups)
            {
                Replay groupReplay = this.replayMap.get(uuid);

                if (groupReplay != null)
                {
                    if (!this.isReplayEnabled(groupReplay))
                    {
                        return false;
                    }

                    int groupTick = groupReplay.getTick(this.getTick());

                    if (!this.isReplayVisibleAt(groupReplay, groupTick))
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isReplayEnabled(Replay replay)
    {
        if (replay == null || !replay.enabled.get())
        {
            return false;
        }

        if (!replay.group.get().isEmpty())
        {
            String[] groups = replay.group.get().split("/");

            for (String uuid : groups)
            {
                Replay groupReplay = this.replayMap.get(uuid);

                if (groupReplay != null && !groupReplay.enabled.get())
                {
                    return false;
                }
            }
        }

        return true;
    }

    protected boolean isReplayVisibleAt(Replay replay, float tick)
    {
        BaseValue renderValue = replay.properties.get("render");

        if (renderValue instanceof KeyframeChannel)
        {
            @SuppressWarnings("unchecked")
            KeyframeChannel<Boolean> render = (KeyframeChannel<Boolean>) renderValue;

            if (render.isEmpty())
            {
                return true;
            }

            Keyframe<Boolean> first = render.get(0);

            if (first != null && tick < first.getTick())
            {
                return true;
            }

            Boolean value = render.interpolate(tick, true);

            return value == null || value;
        }

        return true;
    }


    public void startRenderFrame(float transition)
    {
        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            Replay replay = this.film.replays.getList().get(i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.PROPERTIES))
            {
                continue;
            }

            float delta = this.getTransition(entity, transition);
            int tick = replay.getTick(this.getTick());

            /* Apply property */
            Form form1 = entity.getForm();
            replay.properties.resetProperties(form1);
            replay.properties.applyProperties(form1, tick + delta);

            if (MobCemPoseCapture.isActive(replay))
            {
                MobCemPoseCapture.applyPlaybackPose(replay, form1, entity, tick + delta);
            }

            Map<String, Integer> actors = this.getActors();

            if (actors != null)
            {
                Integer entityId = actors.get(replay.getId());

                if (entityId != null)
                {
                    class_1297 anEntity = class_310.method_1551().field_1687.method_8469(entityId);

                    if (anEntity instanceof ActorEntity actor)
                    {
                        Form form = actor.getForm();
                        replay.properties.resetProperties(form);
                        replay.properties.applyProperties(form, tick + delta);
                    }
                    else if (anEntity instanceof class_1657 player)
                    {
                        Morph morph = Morph.getMorph(player);

                        if (morph != null)
                        {
                            Form form = morph.getForm();
                            replay.properties.resetProperties(form);
                            replay.properties.applyProperties(form, tick + delta);
                        }

                        float yawHead = replay.keyframes.headYaw.interpolate(tick + delta).floatValue();
                        float yawBody = replay.keyframes.bodyYaw.interpolate(tick + delta).floatValue();
                        float pitch = replay.keyframes.pitch.interpolate(tick + delta).floatValue();

                        player.method_36456(yawHead);
                        player.method_5847(yawHead);
                        player.method_36457(pitch);
                        player.method_5636(yawBody);
                        player.field_5982 = yawHead;
                        player.field_6259 = yawHead;
                        player.field_6004 = pitch;
                        player.field_6220 = yawBody;
                    }
                }
            }
        }
    }

    protected float getTransition(IEntity entity, float transition)
    {
        return this.paused ? 0F : transition;
    }

    protected boolean canUpdate(int i, Replay replay, IEntity entity, UpdateMode updateMode)
    {
        if (this.paused && (updateMode == UpdateMode.UPDATE))
        {
            return false;
        }

        return i != this.exception;
    }

    public void render(WorldRenderContext context)
    {
        RenderSystem.enableDepthTest();

        /* Render depth layers: lower depth draws first; within the same depth, farther
         * entities draw first so transparency composites correctly. Semi-transparent
         * forms in front fade out entities behind them whose render depth is lower
         * than the frontmost transparent occluder's depth; equal or higher depths stay
         * fully visible through that layer. */
        List<Map.Entry<Integer, IEntity>> sorted = new ArrayList<>(this.entities.entrySet());
        class_4184 camera = context.camera();
        float transition = context.tickCounter().method_60637(false);

        sorted.sort(Comparator
            .comparingDouble(this::getEntityRenderDepth)
            .thenComparing((Map.Entry<Integer, IEntity> a, Map.Entry<Integer, IEntity> b) ->
                Double.compare(
                    this.getEntityCameraDistanceSq(b.getValue(), camera, transition),
                    this.getEntityCameraDistanceSq(a.getValue(), camera, transition)
                )
            )
            .thenComparing(Map.Entry::getKey)
        );

        List<FormRenderDepth.Occluder> renderDepthOccluders = FormRenderDepth.collectOccluders(this.entities, camera, transition, (index) ->
        {
            Replay replay = CollectionUtils.getSafe(this.film.replays.getList(), index);

            return replay == null ? null : replay.form.get();
        });

        this.currentRenderDepthOccluders = renderDepthOccluders;

        for (Map.Entry<Integer, IEntity> entry : sorted)
        {
            int i = entry.getKey();
            IEntity entity = entry.getValue();
            Replay replay = this.film.replays.getList().get(i);

            if (!this.canUpdate(i, replay, entity, UpdateMode.RENDER))
            {
                continue;
            }

            this.renderEntity(context, replay, entity, i);
        }

        this.currentRenderDepthOccluders = List.of();
    }

    /**
     * Effective render depth used for draw-order sorting. The animated value comes from the
     * entity's form (keyframes are applied to it in {@link #startRenderFrame(float)}), while
     * the on/off toggle is read from the replay's source form so flipping it in the editor
     * takes effect immediately without recreating entities.
     */
    private double getEntityRenderDepth(Map.Entry<Integer, IEntity> entry)
    {
        Double depth = this.getEnabledRenderDepth(entry.getKey(), entry.getValue());

        return depth == null ? 0D : depth;
    }

    /** Render depth of an entity, or null when its form is missing or the feature is toggled off. */
    private Double getEnabledRenderDepth(int index, IEntity entity)
    {
        Form form = entity.getForm();

        if (form == null)
        {
            return null;
        }

        Replay replay = CollectionUtils.getSafe(this.film.replays.getList(), index);
        Form sourceForm = replay == null ? null : replay.form.get();
        boolean enabled = sourceForm != null ? sourceForm.renderDepthEnabled.get() : form.renderDepthEnabled.get();

        return enabled ? (double) form.renderDepth.get() : null;
    }

    /**
     * Fade factor (0..1) for render-depth layering. When a semi-transparent form with
     * render depth D is in front of this entity, entities with depth &lt; D fade out
     * completely; entities with depth &gt;= D stay fully visible through that layer.
     */
    protected float getRenderDepthFade(int index, IEntity entity, class_4184 camera, float transition)
    {
        Double depth = this.getEnabledRenderDepth(index, entity);

        if (depth == null)
        {
            return 1F;
        }

        double entityDistanceSq = this.getEntityCameraDistanceSq(entity, camera, transition);
        Double maxFrontTransparentDepth = null;

        for (Map.Entry<Integer, IEntity> entry : this.entities.entrySet())
        {
            if (entry.getKey() == index)
            {
                continue;
            }

            IEntity other = entry.getValue();

            if (other == null || other.getForm() == null || !this.isSemiTransparent(other.getForm()))
            {
                continue;
            }

            if (this.getEntityCameraDistanceSq(other, camera, transition) >= entityDistanceSq - 0.0001D)
            {
                continue;
            }

            Double otherDepth = this.getEnabledRenderDepth(entry.getKey(), other);

            if (otherDepth == null)
            {
                continue;
            }

            if (maxFrontTransparentDepth == null || otherDepth > maxFrontTransparentDepth)
            {
                maxFrontTransparentDepth = otherDepth;
            }
        }

        if (maxFrontTransparentDepth == null || depth >= maxFrontTransparentDepth)
        {
            return 1F;
        }

        return 0F;
    }

    private double getEntityCameraDistanceSq(IEntity entity, class_4184 camera, float transition)
    {
        double x = Lerps.lerp(entity.getPrevX(), entity.getX(), transition);
        double y = Lerps.lerp(entity.getPrevY(), entity.getY(), transition);
        double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition);
        double dx = x - camera.method_19326().field_1352;
        double dy = y - camera.method_19326().field_1351;
        double dz = z - camera.method_19326().field_1350;

        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isSemiTransparent(Form form)
    {
        return FormRenderDepth.isSemiTransparent(form);
    }

    protected void renderEntity(WorldRenderContext context, Replay replay, IEntity entity, int index)
    {
        if (!replay.actor.get())
        {
            int replayTick = replay.getTick(this.getTick());

            if (!this.isReplayVisible(replay, replayTick))
            {
                return;
            }

            FilmControllerContext filmContext = getFilmControllerContext(context, replay, entity);
            FormRenderDepth.Frame renderDepthFrame = new FormRenderDepth.Frame(this.currentRenderDepthOccluders, replay.form.get());

            filmContext.transition = getTransition(entity, context.tickCounter().method_60637(false));
            filmContext.renderDepthFrame(renderDepthFrame);

            filmContext.stack.method_22903();

            try
            {
                if (!this.applyGroupProperties(replay, filmContext))
                {
                    return;
                }

                renderEntity(filmContext);
            }
            finally
            {
                filmContext.stack.method_22909();
            }
        }
    }

    protected Replay getGroupPivot(String groupUuid)
    {
        for (Replay replay : this.film.replays.getList())
        {
            if (replay.group.get().contains(groupUuid))
            {
                return replay;
            }
        }

        return null;
    }

    protected boolean applyGroupProperties(Replay replay, FilmControllerContext context)
    {
        if (replay.group.get().isEmpty())
        {
            return true;
        }

        String[] groups = replay.group.get().split("/");
        int finalColor = Colors.WHITE;
        Matrix4f globalTranslate = new Matrix4f().identity();
        Matrix4f localTransform = new Matrix4f().identity();
        PaintSettings groupPaint = null;
        GlowSettings groupGlow = null;

        for (String uuid : groups)
        {
            Replay groupReplay = this.replayMap.get(uuid);

            if (groupReplay != null)
            {
                if (!groupReplay.enabled.get())
                {
                    return false;
                }

                double tick = groupReplay.getTick(this.getTick()) + context.transition;

                if (!this.isReplayVisibleAt(groupReplay, (float) tick))
                {
                    return false;
                }

                BaseValue colorValue = groupReplay.properties.get("color");

                if (colorValue instanceof KeyframeChannel)
                {
                    KeyframeChannel<Color> color = (KeyframeChannel<Color>) colorValue;

                    if (!color.isEmpty())
                    {
                        int groupColor = color.interpolate((float) tick).getARGBColor();
                        finalColor = this.mulColors(finalColor, groupColor);
                    }
                }

                Transform groupTransform = this.getGroupTransform(groupReplay, (float) tick);

                if (!groupTransform.isDefault())
                {
                    globalTranslate.translate(groupTransform.translate.x, groupTransform.translate.y, groupTransform.translate.z);

                    Matrix4f local = new Matrix4f();

                    if (groupTransform.pivot.x != 0F || groupTransform.pivot.y != 0F || groupTransform.pivot.z != 0F)
                    {
                        local.translate(groupTransform.pivot);
                    }

                    local.rotateZ(groupTransform.rotate.z);
                    local.rotateY(groupTransform.rotate.y);
                    local.rotateX(groupTransform.rotate.x);
                    local.rotateZ(groupTransform.rotate2.z);
                    local.rotateY(groupTransform.rotate2.y);
                    local.rotateX(groupTransform.rotate2.x);
                    local.scale(groupTransform.scale);

                    if (groupTransform.pivot.x != 0F || groupTransform.pivot.y != 0F || groupTransform.pivot.z != 0F)
                    {
                        local.translate(-groupTransform.pivot.x, -groupTransform.pivot.y, -groupTransform.pivot.z);
                    }

                    localTransform.mul(local);
                }

                PaintSettings paint = this.getGroupPaintSettings(groupReplay, (float) tick);

                if (paint != null)
                {
                    groupPaint = groupPaint == null ? paint : this.mergePaintSettings(groupPaint, paint);
                }

                GlowSettings glow = this.getGroupGlowSettings(groupReplay, (float) tick);

                if (glow != null)
                {
                    groupGlow = groupGlow == null ? glow : this.mergeGlowSettings(groupGlow, glow);
                }
            }
        }

        if (finalColor != Colors.WHITE)
        {
            context.color(this.mulColors(context.color, finalColor));
        }

        if (!globalTranslate.equals(new Matrix4f().identity()))
        {
            context.stack.method_23760().method_23761().mul(globalTranslate);
        }

        if (!localTransform.equals(new Matrix4f().identity()))
        {
            context.localGroupTransform = localTransform;
        }

        context.groupPaint = groupPaint;
        context.groupGlow = groupGlow;

        return true;
    }

    private Transform getGroupTransform(Replay groupReplay, float tick)
    {
        Transform transform = new Transform();

        BaseValue transformValue = groupReplay.properties.get("transform");

        if (transformValue instanceof KeyframeChannel)
        {
            KeyframeChannel<Transform> channel = (KeyframeChannel<Transform>) transformValue;

            if (!channel.isEmpty())
            {
                transform.copy(channel.interpolate(tick));
            }
        }

        this.applyGroupTransformOverlay(transform, groupReplay, "transform_overlay", tick);

        for (int i = 0; i < BBSSettings.recordingPoseTransformOverlays.get(); i++)
        {
            this.applyGroupTransformOverlay(transform, groupReplay, "transform_overlay" + i, tick);
        }

        return transform;
    }

    private void applyGroupTransformOverlay(Transform transform, Replay groupReplay, String key, float tick)
    {
        BaseValue overlayValue = groupReplay.properties.get(key);

        if (overlayValue instanceof KeyframeChannel)
        {
            KeyframeChannel<Transform> channel = (KeyframeChannel<Transform>) overlayValue;

            if (!channel.isEmpty())
            {
                Transform overlay = channel.interpolate(tick);

                transform.translate.add(overlay.translate);
                transform.scale.add(overlay.scale).sub(1F, 1F, 1F);
                transform.rotate.add(overlay.rotate);
                transform.rotate2.add(overlay.rotate2);
                transform.pivot.add(overlay.pivot);
            }
        }
    }

    private PaintSettings getGroupPaintSettings(Replay groupReplay, float tick)
    {
        BaseValue paintValue = groupReplay.properties.get("paint");

        if (paintValue instanceof KeyframeChannel)
        {
            KeyframeChannel<PaintSettings> channel = (KeyframeChannel<PaintSettings>) paintValue;

            if (!channel.isEmpty())
            {
                PaintSettings settings = channel.interpolate(tick);

                return settings == null ? null : settings.copy();
            }
        }

        return null;
    }

    private GlowSettings getGroupGlowSettings(Replay groupReplay, float tick)
    {
        BaseValue glowValue = groupReplay.properties.get("glow");

        if (glowValue instanceof KeyframeChannel)
        {
            KeyframeChannel<GlowSettings> channel = (KeyframeChannel<GlowSettings>) glowValue;

            if (!channel.isEmpty())
            {
                GlowSettings settings = channel.interpolate(tick);

                return settings == null ? null : settings.copy();
            }
        }

        return null;
    }

    private PaintSettings mergePaintSettings(PaintSettings base, PaintSettings overlay)
    {
        PaintSettings merged = base.copy();

        merged.r *= overlay.r;
        merged.g *= overlay.g;
        merged.b *= overlay.b;
        merged.intensity += overlay.intensity;
        merged.sync = merged.sync || overlay.sync;
        merged.shaderShadow = PaintSettings.resolveAutoShaderShadow(merged.intensity);

        return merged;
    }

    private GlowSettings mergeGlowSettings(GlowSettings base, GlowSettings overlay)
    {
        GlowSettings merged = base.copy();

        merged.r *= overlay.r;
        merged.g *= overlay.g;
        merged.b *= overlay.b;
        merged.intensity += overlay.intensity;
        merged.sync = merged.sync || overlay.sync;
        merged.paintOnly = merged.paintOnly || overlay.paintOnly;
        merged.radius = Math.max(merged.radius, overlay.radius);
        merged.width = Math.max(merged.width, overlay.width);
        merged.height = Math.max(merged.height, overlay.height);

        return merged;
    }

    private static void applyGroupPaintGlow(Form form, PaintSettings groupPaint, GlowSettings groupGlow)
    {
        if (groupPaint != null)
        {
            PaintSettings current = form.paintSettings.get().copy();

            current.r *= groupPaint.r;
            current.g *= groupPaint.g;
            current.b *= groupPaint.b;
            current.intensity += groupPaint.intensity;
            current.sync = current.sync || groupPaint.sync;
            current.shaderShadow = PaintSettings.resolveAutoShaderShadow(current.intensity);
            form.paintSettings.setRuntimeValue(current);
            /* Keep casting; paint.shaderShadow float is the Complementary flag only. */
            form.shaderShadow.setRuntimeValue(null);
        }

        if (groupGlow != null)
        {
            GlowSettings current = form.glowSettings.get().copy();

            current.r *= groupGlow.r;
            current.g *= groupGlow.g;
            current.b *= groupGlow.b;
            current.intensity += groupGlow.intensity;
            current.sync = current.sync || groupGlow.sync;
            current.paintOnly = current.paintOnly || groupGlow.paintOnly;
            current.radius = Math.max(current.radius, groupGlow.radius);
            current.width = Math.max(current.width, groupGlow.width);
            current.height = Math.max(current.height, groupGlow.height);
            form.glowSettings.setRuntimeValue(current);
        }
    }

    private int mulColors(int c1, int c2)
    {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = (c1) & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = (c2) & 0xFF;

        int a = (a1 * a2) / 255;
        int r = (r1 * r2) / 255;
        int g = (g1 * g2) / 255;
        int b = (b1 * b2) / 255;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    protected FilmControllerContext getFilmControllerContext(WorldRenderContext context, Replay replay, IEntity entity)
    {
        float tick = replay.getTick(this.getTick()) + this.getTransition(entity, context.tickCounter().method_60637(false));
        ShadowSettings shadow = resolveShadowSettings(replay, tick);

        return FilmControllerContext.instance
            .setup(this.entities, entity, replay, context)
            .film(this.film)
            .propertyTick(tick)
            .filmTick(this.getTick())
            .shadow(replay.shadow.get(), shadow)
            .nameTag(replay.nameTag.get())
            .relative(replay.relative.get());
    }

    /**
     * Interpolated replay shadow settings at {@code tick} (includes keyframes).
     * Always returns a fresh copy — factory interpolation reuses a shared instance.
     */
    public static ShadowSettings resolveShadowSettings(Replay replay, float tick)
    {
        ShadowSettings settings = new ShadowSettings(replay.shadowOpacity.get(), replay.shadowSize.get(), replay.shadowSizeZ.get());

        settings.offsetX = replay.shadowOffsetX.get();
        settings.offsetY = replay.shadowOffsetY.get();
        settings.offsetZ = replay.shadowOffsetZ.get();

        if (!replay.keyframes.shadow.isEmpty())
        {
            ShadowSettings interpolated = replay.keyframes.shadow.interpolate(tick);

            if (interpolated != null)
            {
                settings = interpolated.copy();
            }
        }

        settings.widthX = Math.max(0F, settings.widthX);
        settings.widthZ = Math.max(0F, settings.widthZ);
        settings.opacity = MathUtils.clamp(settings.opacity, 0F, 1F);

        return settings;
    }

    public static float resolveShadowSize(Replay replay, float tick)
    {
        return resolveShadowSettings(replay, tick).widthX;
    }

    public static float resolveShadowSizeZ(Replay replay, float tick)
    {
        return resolveShadowSettings(replay, tick).widthZ;
    }

    public static float resolveShadowOpacity(Replay replay, float tick)
    {
        return resolveShadowSettings(replay, tick).opacity;
    }

    public void shutdown()
    {}

    public static enum UpdateMode
    {
        UPDATE, RENDER, PROPERTIES;
    }

    private static class IllusionLift
    {
        public float value;
        public long time;
    }
}
