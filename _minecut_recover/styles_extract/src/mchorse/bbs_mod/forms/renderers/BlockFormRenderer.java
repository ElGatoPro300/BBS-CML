package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.BlockEffectOverlayUniforms;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.class_1058;
import net.minecraft.class_1088;
import net.minecraft.class_1723;
import net.minecraft.class_1921;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_238;
import net.minecraft.class_2464;
import net.minecraft.class_2586;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2682;
import net.minecraft.class_2960;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_3726;
import net.minecraft.class_4583;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4608;
import net.minecraft.class_4696;
import net.minecraft.class_761;
import net.minecraft.class_765;
import net.minecraft.class_824;
import net.minecraft.class_827;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Function;

public class BlockFormRenderer extends FormRenderer<BlockForm>
{
    public static final Color color = new Color();

    public BlockFormRenderer(BlockForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.getContext().method_51452();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        class_4587 matrices = context.batcher.getContext().method_51448();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.method_22903();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.method_22905(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        Color storedFormColor = this.form.getFormColor();
        Color rawFormColor = storedFormColor.copyWithBlendIntensity();
        Color formColor = rawFormColor.copy();
        boolean colorTransformWanted = FormColorBlend.wantsColorTintOverlay(storedFormColor);
        boolean colorGradeWanted = storedFormColor.hasColorAdjustments();
        Color set = Color.white();

        if (FormColorBlend.shouldBakeFormColor(storedFormColor))
        {
            set.mul(rawFormColor);
        }

        this.form.applyFormOpacity(set);
        this.form.applyFormOpacity(formColor);

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(set, glowSettings, legacyGlow);
        }

        Color resolvedPaint = FormColorBlend.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positivePaint = FormColorBlend.hasPositivePaint(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean blockEntityVisual = this.isBlockEntityVisual();

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        consumers.setSubstitute(this.getBlockMainConsumer(set, resolvedPaint));
        consumers.setUI(true);
        this.renderRepeatedBlocks(null, matrices, consumers, class_765.field_32769, class_4608.field_21444, false, true, false, false);

        consumers.draw();

        if (positivePaint && !blockEntityVisual)
        {
            this.submitDeferredBlockPaintOverlay(null, matrices, resolvedPaint, set.a, class_4608.field_21444, this.form.paintSettings.get().transform, glowSettings, legacyGlow, glowIntensity, true);
        }

        if (colorTransformWanted && !blockEntityVisual)
        {
            Color overlayTint = colorGradeWanted ? storedFormColor.copyWithBlendIntensityOnly() : formColor;

            this.form.applyFormOpacity(overlayTint);
            this.renderBlockColorTintOverlay(null, matrices, overlayTint, set.a, class_4608.field_21444, true, storedFormColor);
        }

        if (glowIntensity > 0F && !glowSettings.resolvePaintOnly() && !blockEntityVisual)
        {
            this.renderGlowOverlay(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, class_4608.field_21444, true);
        }

        consumers.setUI(false);
        consumers.setSubstitute(null);

        class_308.method_24210();

        matrices.method_22909();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        context.stack.method_22903();

        try
        {
            if (context.isPicking())
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                    RenderSystem.setShaderTexture(0, class_1723.field_21668);
                    /* Unit pick cubes need both faces; culling clipped the volume to a flat slab. */
                    RenderSystem.disableCull();
                });

                light = 0;
                /* Form opacity / blend intensity must not discard pick pixels (picker_models a < 0.1). */
                consumers.setSubstitute(BBSRendering.getColorConsumer(new Color(1F, 1F, 1F, 1F)));
            }
            else
            {
                CustomVertexConsumerProvider.hijackVertexFormat((l) ->
                {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                });
            }

            Color storedFormColor = this.form.getFormColor();
            Color rawFormColor = storedFormColor.copyWithBlendIntensity();
            Color formColor = rawFormColor.copy();
            boolean colorTransformWanted = FormColorBlend.wantsColorTintOverlay(storedFormColor);
            boolean colorGradeWanted = storedFormColor.hasColorAdjustments();

            color.set(context.color);

            if (FormColorBlend.shouldBakeFormColor(storedFormColor))
            {
                color.mul(rawFormColor);
            }

            this.form.applyFormOpacity(color);
            this.form.applyFormOpacity(formColor);

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            FormColorBlend.applyShadowPassColorFix(color, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);

            if (color.a <= 0.001F && !shadowPass && !context.isPicking())
            {
                return;
            }

            GlowSettings glowSettings = this.form.glowSettings.get();
            Color legacyGlow = this.form.glowingColor.get();
            float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
            boolean positiveGlow = !context.isPicking() && !shadowPass && glowIntensity > 0F;

            if (glowIntensity < 0F)
            {
                FormColorBlend.blendFormGlowBrighten(color, glowSettings, legacyGlow);
            }

            PaintSettings paintSettings = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color resolvedPaint = FormColorBlend.resolvePaintColor(paintSettings, legacyPaint);
            boolean positivePaint = !context.isPicking() && !shadowPass && FormColorBlend.hasPositivePaint(paintSettings, legacyPaint);
            /* Chests/beds/signs use entity textures — block atlas paint/tint overlays corrupt them.
             * Bake blend/paint/grade into ColorModulator tint instead (Iris: deferred redraw). */
            boolean blockEntityVisual = this.isBlockEntityVisual();

            if (!context.isPicking())
            {
                consumers.setSubstitute(this.getBlockMainConsumer(color, resolvedPaint));
            }

            this.renderRepeatedBlocks(context, context.stack, consumers, light, context.overlay, context.isPicking(), false, false, false);

            consumers.draw();
            consumers.setSubstitute(null);

            if (positivePaint && !blockEntityVisual)
            {
                this.submitDeferredBlockPaintOverlay(context, context.stack, resolvedPaint, color.a, context.overlay, paintSettings.transform, glowSettings, legacyGlow, glowIntensity, false);
            }

            if (colorTransformWanted && !shadowPass && !context.isPicking() && !blockEntityVisual)
            {
                Color overlayTint = colorGradeWanted ? storedFormColor.copyWithBlendIntensityOnly() : formColor;

                this.form.applyFormOpacity(overlayTint);

                if (BBSRendering.isIrisWorldPaintDeferral())
                {
                    this.submitDeferredBlockColorTintOverlay(context, context.stack, overlayTint, color.a, context.overlay, false, storedFormColor);
                }
                else
                {
                    this.renderBlockColorTintOverlay(context, context.stack, overlayTint, color.a, context.overlay, false, storedFormColor);
                }
            }

            if (blockEntityVisual && !context.isPicking() && !shadowPass && this.needsDeferredBlockEntityTint())
            {
                this.submitDeferredBlockEntityTint(context, context.overlay);
            }

            if (positiveGlow && !glowSettings.resolvePaintOnly() && !blockEntityVisual)
            {
                this.renderGlowOverlay(context, context.stack, consumers, glowSettings, legacyGlow, glowIntensity, color.a, context.overlay, false);
            }
            else
            {
                CustomVertexConsumerProvider.clearRunnables();
            }

            RenderSystem.defaultBlendFunc();
        }
        finally
        {
            if (context.isPicking())
            {
                RenderSystem.enableCull();
                CustomVertexConsumerProvider.clearRunnables();
            }

            context.stack.method_22909();
        }

        RenderSystem.enableDepthTest();
    }

    private Function<class_4588, class_4588> getBlockMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void renderRepeatedBlocks(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay)
    {
        int repeatX = this.form.repeatX.get();
        int repeatY = this.form.repeatY.get();
        int repeatZ = this.form.repeatZ.get();
        int startX = BlockForm.repeatAxisStart(repeatX, this.form.repeatCenterX.get());
        int startY = BlockForm.repeatAxisStart(repeatY, this.form.repeatCenterY.get());
        int startZ = BlockForm.repeatAxisStart(repeatZ, this.form.repeatCenterZ.get());

        for (int y = 0; y < repeatY; y++)
        {
            for (int z = 0; z < repeatZ; z++)
            {
                for (int x = 0; x < repeatX; x++)
                {
                    stack.method_22903();
                    stack.method_46416(startX + x, startY + y, startZ + z);

                    int blockLight = light;

                    if (!glowOverlay && context != null)
                    {
                        blockLight = this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);
                    }

                    this.renderSingleBlock(stack, consumers, blockLight, overlay, picking, ui, glowOverlay, paintOverlay);
                    stack.method_22909();
                }
            }
        }
    }

    /**
     * Samples world skylight/blocklight at each repeated block's world position.
     * Uses the entity/world matrix instead of the camera-relative render matrix.
     */
    private int resolveBlockLight(FormRenderingContext context, int localX, int localY, int localZ, int fallback)
    {
        if (this.form.repeatX.get() == 1 && this.form.repeatY.get() == 1 && this.form.repeatZ.get() == 1)
        {
            return fallback;
        }

        class_1937 world = null;

        if (context.entity != null)
        {
            world = context.entity.getWorld();
        }

        if (world == null)
        {
            world = class_310.method_1551().field_1687;
        }

        if (world == null)
        {
            return fallback;
        }

        class_2338 blockPos = this.getRepeatBlockWorldPos(context, localX, localY, localZ);

        if (blockPos == null)
        {
            return fallback;
        }

        int sampled = class_761.method_23794(world, blockPos);
        float lf = 1F - MathUtils.clamp(this.form.lighting.get(), 0F, 1F);
        int u = sampled & '\uffff';
        int v = sampled >> 16 & '\uffff';

        u = (int) Lerps.lerp(u, class_765.field_32769, lf);

        return u | v << 16;
    }

    private class_2338 getRepeatBlockWorldPos(FormRenderingContext context, int localX, int localY, int localZ)
    {
        if (context.world != null)
        {
            class_4587 probe = new class_4587();

            probe.method_23760().method_23761().set(context.world.method_23760().method_23761());
            probe.method_46416(localX, localY, localZ);

            Vector3f translation = probe.method_23760().method_23761().getTranslation(new Vector3f());

            return class_2338.method_49637(translation.x, translation.y + 0.5D, translation.z);
        }

        if (context.entity == null)
        {
            return null;
        }

        Transform transform = this.createTransform();
        Vector3f offset = transform.createMatrix().transformPosition(new Vector3f(localX + 0.5F, localY, localZ + 0.5F), new Vector3f());
        float transition = context.getTransition();
        double x = Lerps.lerp(context.entity.getPrevX(), context.entity.getX(), transition) + offset.x;
        double y = Lerps.lerp(context.entity.getPrevY(), context.entity.getY(), transition) + offset.y;
        double z = Lerps.lerp(context.entity.getPrevZ(), context.entity.getZ(), transition) + offset.z;

        return class_2338.method_49637(x, y, z);
    }

    private void renderSingleBlock(class_4587 stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay)
    {
        stack.method_22903();
        stack.method_46416(-0.5F, 0F, -0.5F);

        /* UI preview uses fixed diffuse lights; world rendering relied on vanilla block lighting before repeat. */
        if (ui && !picking)
        {
            MatrixStackUtils.invertUiNormalY(stack);
        }

        /* Glass/ice etc. write depth in the entity pass and hide models behind the morph.
         * Terrain glass is drawn later in translucent; match that by not writing depth here. */
        boolean translucent = !picking && !paintOverlay && !glowOverlay && this.isTranslucentBlockState(this.form.blockState.get());
        boolean savedDepthMask = false;

        if (translucent)
        {
            savedDepthMask = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
            RenderSystem.depthMask(false);
        }

        try
        {
            class_2680 blockState = this.form.blockState.get();
            boolean pickVolume = picking && this.needsPickVolume(blockState);

            /* Signs/chests/beds/etc. have no solid mesh (or only thin BE parts). During Alt-pick
             * draw one solid unit cube only — outline shapes / BE meshes make noisy multi-hitboxes. */
            if (pickVolume)
            {
                this.renderPickVolume(stack, consumers, light, overlay);
            }
            else
            {
                class_310.method_1551().method_1541().method_3353(blockState, stack, consumers, light, overlay);

                /* Skip BE on paint / color-tint / glow overlay redraw — those shaders expect block atlas. */
                if (!picking && !glowOverlay && !paintOverlay)
                {
                    this.renderBlockEntity(stack, consumers, light, overlay, false);
                }

                int breakingLevel = this.form.breaking.get();

                if (!picking && !glowOverlay && !paintOverlay && breakingLevel > 0 && breakingLevel <= 10)
                {
                    class_1921 crackingLayer = class_1088.field_21772.get(breakingLevel - 1);
                    class_4588 delegateConsumer = consumers.getBuffer(crackingLayer);
                    class_4588 crackingConsumer = new class_4583(delegateConsumer, stack.method_23760(), 1.0F);
                    Function<class_4588, class_4588> previousSubstitute = consumers.getSubstitute();

                    consumers.setSubstitute((vertexConsumer) -> crackingConsumer);

                    try
                    {
                        class_310.method_1551().method_1541().method_3353(this.form.blockState.get(), stack, consumers, light, overlay);
                    }
                    finally
                    {
                        consumers.setSubstitute(previousSubstitute);
                    }
                }
            }
        }
        finally
        {
            if (translucent)
            {
                RenderSystem.depthMask(savedDepthMask);
            }
        }

        stack.method_22909();
    }

    private boolean isTranslucentBlockState(class_2680 state)
    {
        if (state == null)
        {
            return false;
        }

        class_1921 layer = class_4696.method_23679(state);

        return layer == class_1921.method_23583() || layer == class_1921.method_29997();
    }

    private boolean needsPickVolume(class_2680 state)
    {
        if (state == null)
        {
            return false;
        }

        /* Signs / hanging signs / chests / beds / … — animated or invisible mesh, or any BE. */
        if (state.method_26217() == class_2464.field_11455
            || state.method_26217() == class_2464.field_11456
            || state.method_26204() instanceof class_2343)
        {
            return true;
        }

        try
        {
            class_265 shape = state.method_26172(class_2682.field_12294, class_2338.field_10980, class_3726.method_16194());

            if (shape.method_1110())
            {
                return true;
            }

            class_238 box = shape.method_1107();

            /* Fences, panes, rods, chains, … — thin outline is nearly impossible to Alt-pick from the side. */
            return (box.field_1320 - box.field_1323) < 0.999D
                || (box.field_1325 - box.field_1322) < 0.999D
                || (box.field_1324 - box.field_1321) < 0.999D;
        }
        catch (Exception e)
        {
            return true;
        }
    }

    /**
     * One solid unit cube for Alt-pick stencil — clean single hitbox for signs/chests/beds/….
     * Stack is already translated to block local space (-0.5, 0, -0.5).
     * UVs must sample an opaque atlas texel; UV 0–1 spans the whole atlas and picker_models
     * discards transparent samples, which left only a noisy flat square (and looked like
     * extra offset hitboxes from the side).
     */
    private void renderPickVolume(class_4587 stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        RenderSystem.setShaderTexture(0, class_1723.field_21668);
        RenderSystem.disableCull();

        class_4588 buffer = consumers.getBuffer(class_1921.method_23572(class_1723.field_21668));
        class_4587.class_4665 entry = stack.method_23760();
        Matrix4f matrix = entry.method_23761();
        float[] uv = this.getOpaquePickUv();

        this.emitPickCube(buffer, entry, matrix, 0F, 0F, 0F, 1F, 1F, 1F, uv[0], uv[1], light, overlay);
    }

    private float[] getOpaquePickUv()
    {
        class_1058 sprite = class_310.method_1551().method_1554()
            .method_24153(class_1723.field_21668)
            .method_4608(class_2960.method_60655("minecraft", "block/white_concrete"));
        float u = (sprite.method_4594() + sprite.method_4577()) * 0.5F;
        float v = (sprite.method_4593() + sprite.method_4575()) * 0.5F;

        return new float[] {u, v};
    }

    private void emitPickCube(class_4588 buffer, class_4587.class_4665 entry, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, int light, int overlay)
    {
        /* Front faces */
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0F, 0F, -1F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, 0F, 0F, 1F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, 0F, -1F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0F, 1F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, -1F, 0F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1F, 0F, 0F, u, v, light, overlay);
        /* Back faces — entity solid layers may re-enable cull after hijack. */
        this.emitPickQuad(buffer, entry, matrix, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0F, 0F, 1F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0F, 0F, -1F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, y0, z0, 0F, 1F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0F, -1F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, 1F, 0F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, -1F, 0F, 0F, u, v, light, overlay);
    }

    private void emitPickQuad(class_4588 buffer, class_4587.class_4665 entry, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz, float u, float v, int light, int overlay)
    {
        buffer.method_22918(matrix, x0, y0, z0).method_22915(1F, 1F, 1F, 1F).method_22913(u, v).method_22922(overlay).method_60803(light).method_60831(entry, nx, ny, nz);
        buffer.method_22918(matrix, x1, y1, z1).method_22915(1F, 1F, 1F, 1F).method_22913(u, v).method_22922(overlay).method_60803(light).method_60831(entry, nx, ny, nz);
        buffer.method_22918(matrix, x2, y2, z2).method_22915(1F, 1F, 1F, 1F).method_22913(u, v).method_22922(overlay).method_60803(light).method_60831(entry, nx, ny, nz);
        buffer.method_22918(matrix, x3, y3, z3).method_22915(1F, 1F, 1F, 1F).method_22913(u, v).method_22922(overlay).method_60803(light).method_60831(entry, nx, ny, nz);
    }

    private boolean isBlockEntityVisual()
    {
        class_2680 state = this.form.blockState.get();

        if (state == null)
        {
            return false;
        }

        return state.method_26204() instanceof class_2343
            || state.method_26217() == class_2464.field_11456
            || state.method_26217() == class_2464.field_11455;
    }

    private Color resolveBlockEntityColor()
    {
        Color tint = FormColorBlend.resolveBlockEntityTint(this.form.getFormColor(), this.form.paintSettings.get(), this.form.paintColor.get());

        this.form.applyFormOpacity(tint);

        return tint;
    }

    private boolean needsDeferredBlockEntityTint()
    {
        if (!this.isBlockEntityVisual() || !BBSRendering.isIrisWorldPaintDeferral())
        {
            return false;
        }

        Color beTint = this.resolveBlockEntityColor();

        return beTint.r < 0.999F || beTint.g < 0.999F || beTint.b < 0.999F || beTint.a < 0.999F;
    }

    private void submitDeferredBlockEntityTint(FormRenderingContext context, int overlay)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.method_23760().method_23762());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            try
            {
                this.renderRepeatedBlockEntitiesTinted(context, overlayStack, consumers, class_765.field_32767, overlay);
                consumers.draw();
            }
            catch (Throwable ignored)
            {}
            finally
            {
                consumers.setSubstitute(null);
                RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            }
        });
    }

    private void renderRepeatedBlockEntitiesTinted(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        int repeatX = this.form.repeatX.get();
        int repeatY = this.form.repeatY.get();
        int repeatZ = this.form.repeatZ.get();
        int startX = BlockForm.repeatAxisStart(repeatX, this.form.repeatCenterX.get());
        int startY = BlockForm.repeatAxisStart(repeatY, this.form.repeatCenterY.get());
        int startZ = BlockForm.repeatAxisStart(repeatZ, this.form.repeatCenterZ.get());

        for (int y = 0; y < repeatY; y++)
        {
            for (int z = 0; z < repeatZ; z++)
            {
                for (int x = 0; x < repeatX; x++)
                {
                    stack.method_22903();
                    stack.method_46416(startX + x, startY + y, startZ + z);
                    stack.method_46416(-0.5F, 0F, -0.5F);

                    int blockLight = light;

                    if (context != null)
                    {
                        blockLight = this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);
                    }

                    this.renderBlockEntity(stack, consumers, blockLight, overlay, true);
                    stack.method_22909();
                }
            }
        }
    }

    private void renderBlockEntity(class_4587 stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean forceTint)
    {
        if (!(this.form.blockState.get().method_26204() instanceof class_2343 provider))
        {
            return;
        }

        class_310 client = class_310.method_1551();
        class_2586 blockEntity = provider.method_10123(class_2338.field_10980, this.form.blockState.get());

        if (blockEntity == null)
        {
            return;
        }

        if (client.field_1687 != null)
        {
            blockEntity.method_31662(client.field_1687);
        }

        class_824 dispatcher = client.method_31975();
        class_827<?> renderer = dispatcher.method_3550(blockEntity);

        if (renderer == null)
        {
            return;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        class_827 raw = (class_827) renderer;
        Function<class_4588, class_4588> previousSubstitute = consumers.getSubstitute();
        Color beTint = this.resolveBlockEntityColor();
        boolean applyTint = forceTint || !BBSRendering.isIrisWorldPaintDeferral();

        try
        {
            /* Iris gbuffer ignores ColorModulator — tinted redraw runs after composite.
             * Without Iris, bake blend/paint/grade into vertex tint (overlays break BE atlases). */
            if (applyTint)
            {
                consumers.setSubstitute(BBSRendering.getColorConsumer(beTint));
                RenderSystem.setShaderColor(beTint.r, beTint.g, beTint.b, beTint.a);
            }

            raw.method_3569(blockEntity, 0F, stack, consumers, light, overlay);
        }
        finally
        {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            consumers.setSubstitute(previousSubstitute);
        }
    }

    private void submitDeferredBlockColorTintOverlay(FormRenderingContext context, class_4587 stack, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(stack.method_23760().method_23762());
        Color formColorSnapshot = formColor.copy();
        Color gradeSnapshot = gradeSource == null ? null : gradeSource.copy();

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            this.renderBlockColorTintOverlay(context, overlayStack, formColorSnapshot, alpha, overlay, ui, gradeSnapshot);
        });
    }

    private void renderBlockColorTintOverlay(FormRenderingContext context, class_4587 stack, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

        this.renderColorTintOverlayPass(context, stack, consumers, formColor, alpha, overlay, ui, gradeSource);
    }

    private void renderColorTintOverlayPass(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();
        boolean gradeActive = gradeSource != null && gradeSource.hasColorAdjustments();

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configureColorTintOverlayRenderState(formRootInverse, formColor.transform, true, formColor, 0.5F, gradeSource));

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);

        /* Neutral vertices — lighting lives in the scene copy when grading. */
        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, class_765.field_32767, overlay, false, ui, false, true);
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void submitDeferredBlockPaintOverlay(FormRenderingContext context, class_4587 stack, Color resolvedPaint, float alpha, int overlay, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean ui)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(stack.method_23760().method_23762());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            this.renderPaintOverlayPass(null, overlayStack, overlayConsumers, paintOverlay, overlay, ui, transform, glowSettings, legacyGlow, glowIntensity, alpha);
        });
    }

    private void renderPaintOverlay(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform)
    {
        this.renderPaintOverlay(context, stack, consumers, resolvedPaint, alpha, overlay, ui, transform, null, null, 0F);
    }

    private void renderPaintOverlay(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, glowSettings, legacyGlow, glowIntensity, alpha);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, null, null, 0F, 1F);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configurePaintOverlayRenderState(formRootInverse, transform, true, glowSettings, legacyGlow, glowIntensity, alpha));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, class_765.field_32767, overlay, false, ui, false, true);
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void renderGlowOverlay(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui)
    {
        Color glowColor = FormColorBlend.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorBlend.resolveGlowOverlayShaderScale(glowIntensity);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

        consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, class_765.field_32767, overlay, false, ui, true, false);
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }
}
