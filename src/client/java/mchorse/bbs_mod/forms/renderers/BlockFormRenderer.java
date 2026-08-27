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
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.forms.renderers.utils.FormLightingRender;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.World;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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
        context.batcher.getContext().draw();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        MatrixStack matrices = context.batcher.getContext().getMatrices();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        Color storedFormColor = this.form.color.get();
        Color rawFormColor = storedFormColor.copyBakingColorGrade();
        Color formColor = rawFormColor.copy();
        boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor);
        boolean colorGradeWanted = storedFormColor.hasColorAdjustments();
        Color set = Color.white();

        if (FormColorEffects.shouldBakeFormColor(storedFormColor))
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
            FormColorEffects.blendFormGlowBrighten(set, glowSettings, legacyGlow);
        }

        Color resolvedPaint = FormColorEffects.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positivePaint = FormColorEffects.hasPositivePaint(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean blockEntityVisual = this.isBlockEntityVisual();

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        consumers.setSubstitute(this.getBlockMainConsumer(set, resolvedPaint));
        consumers.setUI(true);
        this.renderRepeatedBlocks(null, matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, false, true, false, false);

        consumers.draw();

        if (positivePaint && !blockEntityVisual)
        {
            this.submitDeferredBlockPaintOverlay(null, matrices, resolvedPaint, set.a, OverlayTexture.DEFAULT_UV, this.form.paintSettings.get().transform, glowSettings, legacyGlow, glowIntensity, true);
        }

        if (colorTransformWanted && !blockEntityVisual)
        {
            Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

            this.form.applyFormOpacity(overlayTint);
            this.renderBlockColorTintOverlay(null, matrices, overlayTint, set.a, OverlayTexture.DEFAULT_UV, true, storedFormColor);
        }

        if (glowIntensity > 0F && !glowSettings.resolvePaintOnly() && !blockEntityVisual)
        {
            this.renderGlowOverlay(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, OverlayTexture.DEFAULT_UV, true);
        }

        consumers.setUI(false);
        consumers.setSubstitute(null);

        DiffuseLighting.disableGuiDepthLighting();

        matrices.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        context.stack.push();

        try
        {
            if (context.isPicking())
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    RenderSystem.setShader(BBSShaders.getPickerModelsProgram());
                    RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
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

            Color storedFormColor = this.form.color.get();
            Color rawFormColor = storedFormColor.copyBakingColorGrade();
            Color formColor = rawFormColor.copy();
            boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor);
            boolean colorGradeWanted = storedFormColor.hasColorAdjustments();

            color.set(context.color);

            if (FormColorEffects.shouldBakeFormColor(storedFormColor))
            {
                color.mul(rawFormColor);
            }

            this.form.applyFormOpacity(color);
            this.form.applyFormOpacity(formColor);

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            FormColorEffects.applyShadowPassColorFix(color, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);

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
                FormColorEffects.blendFormGlowBrighten(color, glowSettings, legacyGlow);
            }

            PaintSettings paintSettings = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color resolvedPaint = FormColorEffects.resolvePaintColor(paintSettings, legacyPaint);
            boolean positivePaint = !context.isPicking() && !shadowPass && FormColorEffects.hasPositivePaint(paintSettings, legacyPaint);
            /* Chests/beds/signs use entity textures — block atlas paint/tint overlays corrupt them.
             * Bake blend/paint/grade into ColorModulator tint instead (Iris: deferred redraw). */
            boolean blockEntityVisual = this.isBlockEntityVisual();
            boolean noshadingDefer = !context.modelRenderer
                && !context.isPicking()
                && !shadowPass
                && BBSRendering.needsIrisNoshadingOpacityDeferral(color.a, this.form.noshadingOpacity.get());
            boolean softPostDeferred = !context.modelRenderer
                && !context.isPicking()
                && !shadowPass
                && ShaderOpacityPatch.shouldDelayUntilPostDeferred(color.a)
                && !noshadingDefer;

            if (softPostDeferred || noshadingDefer)
            {
                boolean irisCamera = BBSRendering.isIrisWorldModelPass() && !noshadingDefer;
                Matrix4f positionMatrix = irisCamera
                    ? new Matrix4f(context.stack.peek().getPositionMatrix())
                    : ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
                Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());
                Color colorSnapshot = color.copy();
                Color resolvedPaintSnapshot = resolvedPaint == null ? null : resolvedPaint.copy();
                int lightSnapshot = light;
                int overlaySnapshot = context.overlay;
                boolean depthWrite = ShaderOpacityPatch.shouldWriteDepthForOpacity(color.a);
                boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(color.a);
                double formSortKey = this.computeBlockFormSortKey(context.stack.peek().getPositionMatrix(), context);
                boolean positiveGlowSnapshot = positiveGlow && !glowSettings.resolvePaintOnly() && !blockEntityVisual;
                float glowIntensitySnapshot = glowIntensity;
                GlowSettings glowSettingsSnapshot = glowSettings;
                Color legacyGlowSnapshot = legacyGlow;

                Runnable deferredDraw = () ->
                {
                    MatrixStack overlayStack = new MatrixStack();

                    overlayStack.peek().getPositionMatrix().set(positionMatrix);
                    overlayStack.peek().getNormalMatrix().set(normalMatrix);

                    CustomVertexConsumerProvider deferredConsumers = FormUtilsClient.getProvider();

                    RenderSystem.enableDepthTest();
                    RenderSystem.depthMask(depthWrite);
                    ShaderOpacityPatch.reassertPostDeferredDepthState(depthWrite);
                    CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                    {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.depthMask(depthWrite);
                        ShaderOpacityPatch.reassertPostDeferredDepthState(depthWrite);
                    });

                    deferredConsumers.setSubstitute(this.getBlockMainConsumer(colorSnapshot, resolvedPaintSnapshot));

                    try
                    {
                        this.renderRepeatedBlocks(context, overlayStack, deferredConsumers, lightSnapshot, overlaySnapshot, false, false, false, false);
                        deferredConsumers.draw();
                    }
                    finally
                    {
                        deferredConsumers.setSubstitute(null);
                        CustomVertexConsumerProvider.clearRunnables();
                    }

                    if (positiveGlowSnapshot)
                    {
                        this.renderGlowOverlay(context, overlayStack, deferredConsumers, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot, colorSnapshot.a, overlaySnapshot, false);
                    }

                    /* Soft flush isolation — glow leaves additive blend / depthMask false. */
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                    RenderSystem.defaultBlendFunc();
                    CustomVertexConsumerProvider.clearRunnables();
                    RenderSystem.depthMask(depthWrite);
                    ShaderOpacityPatch.reassertPostDeferredDepthState(depthWrite);
                };

                if (noshadingDefer)
                {
                    ModelVAORenderer.submitDeferredTranslucentModel(deferredDraw, depthWrite);
                }
                else if (irisCamera)
                {
                    ShaderOpacityPatch.submitPostDeferredForm(0D, formSortKey, depthWrite, afterFluids, deferredDraw);
                }
                else
                {
                    ShaderOpacityPatch.submitPostDeferredBbsForm(0D, formSortKey, depthWrite, afterFluids, deferredDraw);
                }
            }
            else
            {
                if (!context.isPicking())
                {
                    consumers.setSubstitute(this.getBlockMainConsumer(color, resolvedPaint));
                }

                this.renderRepeatedBlocks(context, context.stack, consumers, light, context.overlay, context.isPicking(), false, false, false);

                consumers.draw();
                consumers.setSubstitute(null);
            }

            if (positivePaint && !blockEntityVisual)
            {
                this.submitDeferredBlockPaintOverlay(context, context.stack, resolvedPaint, color.a, context.overlay, paintSettings.transform, glowSettings, legacyGlow, glowIntensity, false);
            }

            if (colorTransformWanted && !shadowPass && !context.isPicking() && !blockEntityVisual)
            {
                Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

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

            if (!softPostDeferred && !noshadingDefer && positiveGlow && !glowSettings.resolvePaintOnly() && !blockEntityVisual)
            {
                this.renderGlowOverlay(context, context.stack, consumers, glowSettings, legacyGlow, glowIntensity, color.a, context.overlay, false);
            }
            else if (!softPostDeferred && !noshadingDefer)
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

            context.stack.pop();
        }

        RenderSystem.enableDepthTest();
    }

    private Function<VertexConsumer, VertexConsumer> getBlockMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void renderRepeatedBlocks(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay)
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
                    stack.push();
                    stack.translate(startX + x, startY + y, startZ + z);

                    int blockLight = light;

                    if (!glowOverlay && context != null)
                    {
                        blockLight = this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);
                    }

                    this.renderSingleBlock(stack, consumers, blockLight, overlay, picking, ui, glowOverlay, paintOverlay);
                    stack.pop();
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

        World world = null;

        if (context.entity != null)
        {
            world = context.entity.getWorld();
        }

        if (world == null)
        {
            world = MinecraftClient.getInstance().world;
        }

        if (world == null)
        {
            return fallback;
        }

        BlockPos blockPos = this.getRepeatBlockWorldPos(context, localX, localY, localZ);

        if (blockPos == null)
        {
            return fallback;
        }

        int sampled = WorldRenderer.getLightmapCoordinates(world, blockPos);

        return FormLightingRender.apply(sampled, this.form.lightingSettings, this.form.lighting.get());
    }

    private BlockPos getRepeatBlockWorldPos(FormRenderingContext context, int localX, int localY, int localZ)
    {
        if (context.world != null)
        {
            MatrixStack probe = new MatrixStack();

            probe.peek().getPositionMatrix().set(context.world.peek().getPositionMatrix());
            probe.translate(localX, localY, localZ);

            Vector3f translation = probe.peek().getPositionMatrix().getTranslation(new Vector3f());

            return BlockPos.ofFloored(translation.x, translation.y + 0.5D, translation.z);
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

        return BlockPos.ofFloored(x, y, z);
    }

    private void renderSingleBlock(MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay)
    {
        stack.push();
        stack.translate(-0.5F, 0F, -0.5F);

        /* UI preview uses fixed diffuse lights; world rendering relied on vanilla block lighting before repeat. */
        if (ui && !picking)
        {
            MatrixStackUtils.invertUiNormalY(stack);
        }

        /* Glass/ice etc. write depth in the entity pass and hide models behind the morph.
         * Terrain glass is drawn later in translucent; match that by not writing depth here.
         * Soft post-deferred already owns depth write — do not suppress it there. */
        boolean translucent = !picking && !paintOverlay && !glowOverlay && this.isTranslucentBlockState(this.form.blockState.get())
            && !ShaderOpacityPatch.isPostDeferredPhase();
        boolean savedDepthMask = false;

        if (translucent)
        {
            savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            RenderSystem.depthMask(false);
        }

        try
        {
            BlockState blockState = this.form.blockState.get();
            boolean pickVolume = picking && this.needsPickVolume(blockState);

            /* Signs/chests/beds/etc. have no solid mesh (or only thin BE parts). During Alt-pick
             * draw one solid unit cube only — outline shapes / BE meshes make noisy multi-hitboxes. */
            if (pickVolume)
            {
                this.renderPickVolume(stack, consumers, light, overlay);
            }
            else
            {
                MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(blockState, stack, consumers, light, overlay);

                /* Skip BE on paint / color-tint / glow overlay redraw — those shaders expect block atlas. */
                if (!picking && !glowOverlay && !paintOverlay)
                {
                    this.renderBlockEntity(stack, consumers, light, overlay, false);
                }

                int breakingLevel = this.form.breaking.get();

                if (!picking && !glowOverlay && !paintOverlay && breakingLevel > 0 && breakingLevel <= 10)
                {
                    RenderLayer crackingLayer = ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(breakingLevel - 1);
                    VertexConsumer delegateConsumer = consumers.getBuffer(crackingLayer);
                    VertexConsumer crackingConsumer = new OverlayVertexConsumer(delegateConsumer, stack.peek(), 1.0F);
                    Function<VertexConsumer, VertexConsumer> previousSubstitute = consumers.getSubstitute();

                    consumers.setSubstitute((vertexConsumer) -> crackingConsumer);

                    try
                    {
                        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(this.form.blockState.get(), stack, consumers, light, overlay);
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

        stack.pop();
    }

    private boolean isTranslucentBlockState(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        RenderLayer layer = RenderLayers.getBlockLayer(state);

        return layer == RenderLayer.getTranslucent() || layer == RenderLayer.getTripwire();
    }

    /**
     * Soft-opacity queue key for the block form origin (farther first).
     */
    private double computeBlockFormSortKey(Matrix4f drawMatrix, FormRenderingContext context)
    {
        Vector4f origin = new Vector4f(0F, 0F, 0F, 1F);
        Matrix4f viewSpace = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(drawMatrix));

        viewSpace.transform(origin);

        boolean filmLookAxis = context != null
            && context.type == FormRenderType.ENTITY
            && context.camera != null
            && !context.modelRenderer;

        if (filmLookAxis)
        {
            return -origin.z;
        }

        return origin.x * origin.x + origin.y * origin.y + origin.z * origin.z;
    }

    private boolean needsPickVolume(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        /* Signs / hanging signs / chests / beds / … — animated or invisible mesh, or any BE. */
        if (state.getRenderType() == BlockRenderType.INVISIBLE
            || state.getBlock() instanceof BlockEntityProvider)
        {
            return true;
        }

        try
        {
            VoxelShape shape = state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, ShapeContext.absent());

            if (shape.isEmpty())
            {
                return true;
            }

            Box box = shape.getBoundingBox();

            /* Fences, panes, rods, chains, … — thin outline is nearly impossible to Alt-pick from the side. */
            return (box.maxX - box.minX) < 0.999D
                || (box.maxY - box.minY) < 0.999D
                || (box.maxZ - box.minZ) < 0.999D;
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
    private void renderPickVolume(MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        RenderSystem.disableCull();

        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        MatrixStack.Entry entry = stack.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        float[] uv = this.getOpaquePickUv();

        this.emitPickCube(buffer, entry, matrix, 0F, 0F, 0F, 1F, 1F, 1F, uv[0], uv[1], light, overlay);
    }

    private float[] getOpaquePickUv()
    {
        Sprite sprite = MinecraftClient.getInstance().getBakedModelManager()
            .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
            .getSprite(Identifier.of("minecraft", "block/white_concrete"));
        float u = (sprite.getMinU() + sprite.getMaxU()) * 0.5F;
        float v = (sprite.getMinV() + sprite.getMaxV()) * 0.5F;

        return new float[] {u, v};
    }

    private void emitPickCube(VertexConsumer buffer, MatrixStack.Entry entry, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, int light, int overlay)
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

    private void emitPickQuad(VertexConsumer buffer, MatrixStack.Entry entry, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz, float u, float v, int light, int overlay)
    {
        buffer.vertex(matrix, x0, y0, z0).color(1F, 1F, 1F, 1F).texture(u, v).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        buffer.vertex(matrix, x1, y1, z1).color(1F, 1F, 1F, 1F).texture(u, v).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        buffer.vertex(matrix, x2, y2, z2).color(1F, 1F, 1F, 1F).texture(u, v).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        buffer.vertex(matrix, x3, y3, z3).color(1F, 1F, 1F, 1F).texture(u, v).overlay(overlay).light(light).normal(entry, nx, ny, nz);
    }

    private boolean isBlockEntityVisual()
    {
        BlockState state = this.form.blockState.get();

        if (state == null)
        {
            return false;
        }

        return state.getBlock() instanceof BlockEntityProvider
            || state.getRenderType() == BlockRenderType.INVISIBLE;
    }

    private Color resolveBlockEntityColor()
    {
        Color tint = FormColorEffects.resolveBlockEntityTint(this.form.color.get(), this.form.paintSettings.get(), this.form.paintColor.get());

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
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            try
            {
                this.renderRepeatedBlockEntitiesTinted(context, overlayStack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay);
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

    private void renderRepeatedBlockEntitiesTinted(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
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
                    stack.push();
                    stack.translate(startX + x, startY + y, startZ + z);
                    stack.translate(-0.5F, 0F, -0.5F);

                    int blockLight = light;

                    if (context != null)
                    {
                        blockLight = this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);
                    }

                    this.renderBlockEntity(stack, consumers, blockLight, overlay, true);
                    stack.pop();
                }
            }
        }
    }

    private void renderBlockEntity(MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean forceTint)
    {
        if (!(this.form.blockState.get().getBlock() instanceof BlockEntityProvider provider))
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        BlockEntity blockEntity = provider.createBlockEntity(BlockPos.ORIGIN, this.form.blockState.get());

        if (blockEntity == null)
        {
            return;
        }

        if (client.world != null)
        {
            blockEntity.setWorld(client.world);
        }

        BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();
        BlockEntityRenderer<?> renderer = dispatcher.get(blockEntity);

        if (renderer == null)
        {
            return;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        BlockEntityRenderer raw = (BlockEntityRenderer) renderer;
        Function<VertexConsumer, VertexConsumer> previousSubstitute = consumers.getSubstitute();
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

            raw.render(blockEntity, 0F, stack, consumers, light, overlay);
        }
        finally
        {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            consumers.setSubstitute(previousSubstitute);
        }
    }

    private void submitDeferredBlockColorTintOverlay(FormRenderingContext context, MatrixStack stack, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(stack.peek().getNormalMatrix());
        Color formColorSnapshot = formColor.copy();
        Color gradeSnapshot = gradeSource == null ? null : gradeSource.copy();

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderBlockColorTintOverlay(context, overlayStack, formColorSnapshot, alpha, overlay, ui, gradeSnapshot);
        });
    }

    private void renderBlockColorTintOverlay(FormRenderingContext context, MatrixStack stack, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

        this.renderColorTintOverlayPass(context, stack, consumers, formColor, alpha, overlay, ui, gradeSource);
    }

    private void renderColorTintOverlayPass(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configureColorTintOverlayRenderState(formRootInverse, formColor.transform, true, formColor, 0.5F, gradeSource));

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        /* Pull tint overlay toward camera so it does not z-fight the main block pass. */
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -2F);

        /* Neutral vertices — lighting lives in the scene copy when grading. */
        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, false, ui, false, true);
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.depthFunc(savedDepthFunc);
            GL11.glPolygonOffset(0F, 0F);

            if (savedPolygonOffsetFill)
            {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            }
            else
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void submitDeferredBlockPaintOverlay(FormRenderingContext context, MatrixStack stack, Color resolvedPaint, float alpha, int overlay, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean ui)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(stack.peek().getNormalMatrix());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderPaintOverlayPass(null, overlayStack, overlayConsumers, paintOverlay, overlay, ui, transform, glowSettings, legacyGlow, glowIntensity, alpha);
        });
    }

    private void renderPaintOverlay(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform)
    {
        this.renderPaintOverlay(context, stack, consumers, resolvedPaint, alpha, overlay, ui, transform, null, null, 0F);
    }

    private void renderPaintOverlay(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, glowSettings, legacyGlow, glowIntensity, alpha);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, null, null, 0F, 1F);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configurePaintOverlayRenderState(formRootInverse, transform, true, glowSettings, legacyGlow, glowIntensity, alpha));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, false, ui, false, true);
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

    private void renderGlowOverlay(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui)
    {
        Color glowColor = FormColorEffects.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

        CustomVertexConsumerProvider.hijackVertexFormat((l) ->
        {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        });

        consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, false, ui, true, false);
            consumers.draw();
        }
        finally
        {
            CustomVertexConsumerProvider.clearRunnables();
            consumers.setSubstitute(null);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }
}
