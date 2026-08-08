package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.forms.values.ValueIllusion;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.utils.AABB;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Renders form illusions (visual duplicates) for any form draw path: films,
 * model blocks, morphs, and the form-editor preview.
 */
public final class FormIllusionRenderer
{
    private static final Map<Long, IllusionLift> ILLUSION_LIFTS = new HashMap<>();

    private FormIllusionRenderer()
    {}

    /**
     * Optional film-only delay: apply / restore replay properties per copy.
     */
    public static final class Extras
    {
        public float propertyTick = Float.NaN;
        public Consumer<Float> applyFormAtTick;
        public Runnable restoreFormTick;
    }

    public static void render(Form form, FormRenderingContext formContext)
    {
        render(form, formContext, null);
    }

    public static void render(Form form, FormRenderingContext formContext, Extras extras)
    {
        if (form == null || formContext == null || formContext.stack == null)
        {
            return;
        }

        if (formContext.isShadowPass || formContext.isPicking())
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
        AABB hitbox = resolveHitbox(form, formContext.entity);
        float height = (float) hitbox.h;
        MatrixStack stack = formContext.stack;

        for (int layer = 0; layer < layers.size(); layer++)
        {
            Illusion layerIllusion = layers.get(layer);

            if (layerIllusion == null || layerIllusion.count <= 0)
            {
                continue;
            }

            Transform layerTransform = createIllusionTransform(form, layerIllusion);

            renderIllusionLayer(form, formContext, stack, layerIllusion, layerTransform, hitbox, height, layer, baseColor, baseLight, extras);
        }

        formContext.textureOverride = null;
        formContext.textureBlendOverride = null;
        formContext.color(baseColor);
        formContext.light = baseLight;
        form.glowSettings.setRuntimeValue(null);
    }

    private static AABB resolveHitbox(Form form, IEntity entity)
    {
        if (entity != null)
        {
            AABB picking = entity.getPickingHitbox();

            if (picking != null && picking.w > 0D && picking.h > 0D)
            {
                return picking;
            }
        }

        float width = form != null && form.hitbox.get() ? Math.max(form.hitboxWidth.get(), 0.5F) : 0.6F;
        float height = form != null && form.hitbox.get() ? Math.max(form.hitboxHeight.get(), 0.5F) : 1.8F;

        return new AABB(0D, 0D, 0D, width, height, width);
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

    private static void renderIllusionLayer(Form form, FormRenderingContext formContext, MatrixStack stack, Illusion illusion, Transform illusionTransform, AABB hitbox, float height, int layerIndex, int baseColor, int baseLight, Extras extras)
    {
        List<Vector3f> directions = getIllusionDirections(illusion.directions);
        float strength = Math.max(illusion.opacity, 0F);
        int count = illusion.count;
        int dirCount = directions.size();
        int maxRank = (count + dirCount - 1) / dirCount;
        int textureCount = illusion.textures.size();
        boolean delayed = illusion.delay > 0F
            && extras != null
            && extras.applyFormAtTick != null
            && !Float.isNaN(extras.propertyTick);
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
                float delayedTick = Math.max(extras.propertyTick - illusion.delay * (i + 1), 0F);

                extras.applyFormAtTick.accept(delayedTick);
            }

            float lift = 0F;

            if (illusion.real && !formContext.relative && formContext.entity != null)
            {
                lift = getIllusionLift(formContext.entity, dir, distance, liftKeyBase + i, formContext.transition);
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
            int savedTrailInstance = formContext.trailInstance;

            /* Unique positive slot so TrailFormRenderer does not append illusion
             * samples into the primary trail history. */
            formContext.trailInstance = liftKeyBase + i + 1;

            try
            {
                if (mainAlpha > 0F)
                {
                    int a = Math.round(((baseColor >>> 24) & 0xFF) * mainAlpha);

                    stack.push();

                    try
                    {
                        stack.translate(x, y, z);

                        if (partial != null)
                        {
                            MatrixStackUtils.multiply(stack, partial.createMatrix());
                        }

                        formContext.color((a << 24) | (baseColor & Colors.RGB));
                        FormUtilsClient.render(form, formContext);
                    }
                    finally
                    {
                        stack.pop();
                    }
                }

                if (distortFactor > 0F)
                {
                    float streakAlpha = alpha * (1F - distortFactor);
                    int a = Math.round(((baseColor >>> 24) & 0xFF) * Math.min(streakAlpha + 0.2F * (1F - distortFactor), 1F));

                    renderIllusionStreaks(form, formContext, stack, x, y, z, partial, (a << 24) | (baseColor & Colors.RGB), distortFactor, liftKeyBase + i, height);
                }
            }
            finally
            {
                formContext.trailInstance = savedTrailInstance;
            }

            formContext.textureOverride = savedTextureOverride;
            formContext.textureBlendOverride = savedTextureBlendOverride;
            formContext.light = baseLight;
            form.glowSettings.setRuntimeValue(null);
        }

        if (delayed && extras.restoreFormTick != null)
        {
            extras.restoreFormTick.run();
        }
    }

    private static float getIllusionDistance(Illusion illusion, AABB hitbox, Vector3f dir, int rank, int maxRank)
    {
        if (illusion.uniform)
        {
            return illusion.spacing * rank + illusion.offset;
        }

        return illusion.spread * (rank * maxRank - rank * (rank - 1) / 2F) / maxRank + illusion.offset;
    }

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

        float weight = (count - index) / (float) count;

        if (invert)
        {
            weight = (index + 1F) / count;
        }

        return weight;
    }

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

    private static void renderIllusionStreaks(Form form, FormRenderingContext formContext, MatrixStack stack, float x, float y, float z, Transform partial, int argb, float distortFactor, int index, float height)
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

            stack.push();

            try
            {
                stack.translate(x + jx, y + yPos * (1F - squash), z + jz);

                if (partial != null)
                {
                    MatrixStackUtils.multiply(stack, partial.createMatrix());
                }

                stack.scale(stretch, squash, stretch);
                FormUtilsClient.render(form, formContext);
            }
            finally
            {
                stack.pop();
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

    private static float getIllusionLift(IEntity entity, Vector3f dir, float distance, int index, float transition)
    {
        World world = entity.getWorld();

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

    private static float getIllusionGroundDelta(World world, double x, double y, double z)
    {
        for (int i = 0; i <= 6; i++)
        {
            BlockPos blockPos = BlockPos.ofFloored(x, y + 3D - i, z);
            VoxelShape shape = world.getBlockState(blockPos).getCollisionShape(world, blockPos);

            if (shape.isEmpty())
            {
                continue;
            }

            double top = blockPos.getY() + shape.getMax(Direction.Axis.Y);

            return MathUtils.clamp((float) (top - y), -3F, 3F);
        }

        return 0F;
    }

    private static class IllusionLift
    {
        public float value;
        public long time;
    }
}
