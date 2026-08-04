package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.IModelVAO;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.forms.renderers.utils.BlockEffectOverlayUniforms;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.StructureVirtualBlockRenderView;
import mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;

import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * StructureForm Renderer
 *
 * Implements NBT loading and basic rendering by iterating blocks.
 * To minimize files, the NBT loader is integrated here.
 */
public class StructureFormRenderer extends FormRenderer<StructureForm>
{
    private final List<BlockEntry> blocks = new ArrayList<>();

    private String lastFile = null;

    private BlockPos size = BlockPos.ORIGIN;
    private BlockPos boundsMin = null;
    private BlockPos boundsMax = null;

    private VirtualBlockRenderView.Entry[] entriesCache = null;
    private StructureVirtualBlockRenderView cachedView = null;

    public StructureFormRenderer(StructureForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        /* Ensure current UI batch is flushed before drawing 3D */
        context.batcher.getContext().drawDeferredElements();

        this.ensureLoaded();

        /* 1.21.11 render: context.batcher.getContext().getMatrices() now returns a 2D Matrix3x2fStack for
         * GUI transforms, not a MatrixStack (see .port_1.21.11_notes.md #4); build our own stack instead. */
        MatrixStack matrices = new MatrixStack();
        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);

        /* To draw 3D content inside UI, use standard depth test and restore it at the end to avoid affecting other panels. */
        GlStateManager._depthFunc(GL11.GL_LEQUAL);

        /* Autoscale: adjust so the structure fits in the cell without clipping */
        float cellW = x2 - x1;
        float cellH = y2 - y1;
        float baseScale = cellH / 2.5F; /* same as in ModelFormRenderer#getUIMatrix */
        float targetPixels = Math.min(cellW, cellH) * 0.9F; /* 10% margin */

        int wUnits;
        int hUnits;
        int dUnits;
        int maxUnits;
        float auto;
        float finalScale;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            wUnits = Math.max(1, this.boundsMax.getX() - this.boundsMin.getX() + 1);
            hUnits = Math.max(1, this.boundsMax.getY() - this.boundsMin.getY() + 1);
            dUnits = Math.max(1, this.boundsMax.getZ() - this.boundsMin.getZ() + 1);
        }
        else
        {
            wUnits = Math.max(1, this.size.getX());
            hUnits = Math.max(1, this.size.getY());
            dUnits = Math.max(1, this.size.getZ());
        }

        maxUnits = Math.max(wUnits, Math.max(hUnits, dUnits));
        auto = maxUnits > 0 ? targetPixels / (baseScale * maxUnits) : 1F;

        /* Do not exceed user defined scale; only reduce if necessary */
        finalScale = this.form.uiScale.get() * Math.min(1F, auto);
        float structScaleUI = Math.max(Math.max(this.form.scaleX.get(), this.form.scaleY.get()), this.form.scaleZ.get());
        finalScale *= structScaleUI;
        matrices.scale(finalScale, finalScale, finalScale);

        matrices.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        matrices.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        /* 1.21.11 render: DiffuseLighting.disableGuiDepthLighting()/RenderSystem.setupLevelDiffuseLighting()
         * are gone; DiffuseLighting is now an instance obtained from the game renderer (see
         * .port_1.21.11_notes.md #3). */
        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

        /* 1.21.11 render: LightmapTextureManager.enable()/disable() and OverlayTexture.setupOverlayColor()/
         * teardownOverlayColor() are gone - the lightmap/overlay textures are now bound declaratively per
         * RenderLayer (via RenderSetup.useLightmap()/useOverlay(), already set on the vanilla block
         * RenderLayers used below), not toggled through a global enable/disable call before drawing. */
        VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            FormRenderingContext uiContext = new FormRenderingContext()
                .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        Color storedFormColor = this.form.color.get();
        Color rawFormColor = storedFormColor.copyBakingColorGrade();
        Color formColor = rawFormColor.copy();
        boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor);
        Color tint = Color.white();

        if (FormColorEffects.shouldBakeFormColor(storedFormColor))
        {
            tint.mul(rawFormColor);
        }

        this.form.applyFormOpacity(tint);
        this.form.applyFormOpacity(formColor);

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F)
        {
            FormColorEffects.blendFormGlowBrighten(tint, glowSettings, legacyGlow);
        }

        boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
        boolean deferColorTintToOverlay = colorTransformWanted && irisWorldPaintDeferral;
        Color resolvedPaint = FormColorEffects.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positivePaint = FormColorEffects.hasPositivePaint(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positiveGlow = glowIntensity > 0F;
        Function<VertexConsumer, VertexConsumer> mainRecolor = this.getMainConsumer(tint, resolvedPaint);

        if (!optimize)
        {
            /* BufferBuilder mode: better lighting, worse performance */
            boolean shaders = this.isShadersActive();
            VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            try
            {
                immediate.draw();
            }
            catch (Throwable ignored)
            {}
        }

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.LEVEL);

        matrices.pop();

        /* Restore depth state expected by UI system */
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        this.ensureLoaded();

        context.stack.push();

        try
        {


            boolean optimize = true;
            boolean picking = context.isPicking();

            IModelVAO vao = this.getStructureVao();

            StructureLightSettings sl = this.form.structureLight.getRuntimeValue();
            boolean currentEmitLight = (sl != null) ? sl.enabled : this.form.emitLight.get();
            int currentLightIntensity = (sl != null) ? sl.intensity : this.form.lightIntensity.get();

            if (currentEmitLight != this.lastEmitLight || currentLightIntensity != this.lastLightIntensity)
            {
                this.vaoDirty = true;
                this.lastEmitLight = currentEmitLight;
                this.lastLightIntensity = currentLightIntensity;
            }

            if (optimize && (vao == null || this.vaoDirty))
            {
                this.buildStructureVAO();
                vao = this.getStructureVao();
            }

            Color storedFormColor3D = this.form.color.get();
            Color rawFormColor3D = storedFormColor3D.copyBakingColorGrade();
            Color formColor3D = rawFormColor3D.copy();
            boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor3D);
            Color mainTint3D = new Color().set(context.color);

            if (FormColorEffects.shouldBakeFormColor(storedFormColor3D))
            {
                mainTint3D.mul(rawFormColor3D);
            }

            this.form.applyFormOpacity(mainTint3D);
            this.form.applyFormOpacity(formColor3D);

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            FormColorEffects.applyShadowPassColorFix(mainTint3D, storedFormColor3D, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);
            this.applyBlockEntityOnlyShaderShadow(mainTint3D, shadowPass);

            if (mainTint3D.a <= 0.001F && !shadowPass && !picking)
            {
                return;
            }

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F)
        {
            FormColorEffects.blendFormGlowBrighten(mainTint3D, glowSettings, legacyGlow);
        }

        boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
        boolean deferColorTintToOverlay = colorTransformWanted && irisWorldPaintDeferral && !shadowPass;
        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        Color resolvedPaint = FormColorEffects.resolvePaintColor(paintSettings, legacyPaint);
        boolean positivePaint = !picking && !shadowPass && FormColorEffects.hasPositivePaint(paintSettings, legacyPaint);
        boolean positiveGlow = !picking && !shadowPass && glowIntensity > 0F;
        /* Color tint overlay must not write into the shadow map (same solid-main-pass behavior as Block). */
        boolean applyColorTint = colorTransformWanted && !picking && !shadowPass;
        Function<VertexConsumer, VertexConsumer> mainRecolor = this.getMainConsumer(mainTint3D, resolvedPaint);
        boolean shaders = this.isShadersActive();

        if (!optimize)
        {
            /* If picking, render with VAO (picking) and picking shader to get full silhouette */
            if (picking)
            {
                IModelVAO pickingVao = this.getStructureVaoPicking();

                if (pickingVao == null || this.vaoPickingDirty)
                {
                    this.buildStructureVAOPicking();
                    pickingVao = this.getStructureVaoPicking();
                }

                Color tintPicking = this.form.color.get();
                int light = 0;
                GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

                gameRenderer.getLightmapTextureManager().enable();
                gameRenderer.getOverlayTexture().setupOverlayColor();

                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

                ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), pickingVao, context.stack, mainTint3D.r, mainTint3D.g, mainTint3D.b, mainTint3D.a, light, context.overlay);

                gameRenderer.getLightmapTextureManager().disable();
                gameRenderer.getOverlayTexture().teardownOverlayColor();

                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
            }
            else
            {
                /* BufferBuilder mode: use vanilla/culling pipeline with better lighting */
                int light = context.light;
                VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

                /* Align state handling with VAO path to avoid state leaks affecting the first model rendered after. */
                gameRenderer.getLightmapTextureManager().enable();
                gameRenderer.getOverlayTexture().setupOverlayColor();
                /* Ensure block atlas is active when starting the pass */
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

                try
                {
                    this.renderStructureCulledWorld(context, context.stack, consumers, light, context.overlay, shaders, mainRecolor, false, false);

                    if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }

                    if (positivePaint)
                    {
                        EffectTransform paintTransform = paintSettings.transform;

                        this.renderStructurePaintOverlay(context, context.stack, resolvedPaint, mainTint3D.a, context.overlay, false, shaders, paintTransform, glowSettings, legacyGlow, glowIntensity);
                    }

                    if (positiveGlow)
                    {
                        this.renderStructureGlowOverlay(context, context.stack, glowSettings, legacyGlow, glowIntensity, mainTint3D.a, context.overlay, false, shaders);
                    }

                    if (applyColorTint)
                    {
                        if (irisWorldPaintDeferral)
                        {
                            this.submitDeferredStructureColorTintOverlay(context, formColor3D, mainTint3D.a, context.overlay, false, shaders);
                        }
                        else
                        {
                            this.renderStructureColorTintOverlay(context, context.stack, formColor3D, mainTint3D.a, context.overlay, false, shaders, false);
                        }
                    }
                }
                catch (Throwable ignored)
                {}

                /* Restore state after BufferBuilder pass to avoid contaminating next render (models, UI, etc.) */
                gameRenderer.getLightmapTextureManager().disable();
                gameRenderer.getOverlayTexture().teardownOverlayColor();

                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
            }
        }
        else if (vao != null)
        {
            int light = context.isPicking() ? 0 : context.light;
            GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

            gameRenderer.getLightmapTextureManager().enable();
            gameRenderer.getOverlayTexture().setupOverlayColor();

            if (context.isPicking())
            {
                /* 1.21.11 render: structure picking loses pixel accuracy, same as BlockFormRenderer/
                 * LabelFormRenderer (see .port_1.21.11_notes.md #5/#6) - each block quad now draws through
                 * whichever vanilla RenderLayer its own model declares, and there is no more "hijack the
                 * bound shader" trick to force it through our picker pipeline instead. The picking index is
                 * still recorded for whatever else consults it. */
                this.setupTarget(context, null);
            }

            int light = context.isPicking() ? 0 : context.light;

            /* 1.21.11 render: LightmapTextureManager.enable()/disable() and OverlayTexture.setupOverlayColor()/
             * teardownOverlayColor() are gone - the lightmap/overlay textures are now bound declaratively per
             * RenderLayer (via RenderSetup.useLightmap()/useOverlay(), already set on the vanilla block
             * RenderLayers used below), not toggled through a global enable/disable call before drawing. */
            VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            try
            {
                this.renderStructureCulledWorld(context, context.stack, consumers, light, context.overlay);

                if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                {
                    immediate.draw();
                }
            }
            catch (Throwable ignored)
            {}

            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GL11.GL_LEQUAL);

            CustomVertexConsumerProvider.clearRunnables();
        }
        }
        finally
        {
            context.stack.pop();
        }
    }

    private static class RenderInfo
    {
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        public VirtualBlockRenderView view;
        public BlockPos anchor;
    }

    private RenderInfo calculateRenderInfo(FormRenderingContext context, boolean forceMaxSkyLight)
    {
        RenderInfo info = new RenderInfo();
        float cx;
        float cy;
        float cz;
        float parityXAuto = 0F;
        float parityZAuto = 0F;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            cx = (this.boundsMin.getX() + this.boundsMax.getX()) / 2F;
            cz = (this.boundsMin.getZ() + this.boundsMax.getZ()) / 2F;
            /* Keep it on the ground: use the minimum Y as base */
            cy = this.boundsMin.getY();
        }
        else
        {
            /* Fallback if no bounds calculated */
            cx = this.size.getX() / 2F;
            cy = 0F;
            cz = this.size.getZ() / 2F;
        }

        if (this.boundsMin != null && this.boundsMax != null)
        {
            int widthX = this.boundsMax.getX() - this.boundsMin.getX() + 1;
            int widthZ = this.boundsMax.getZ() - this.boundsMin.getZ() + 1;

            parityXAuto = (widthX % 2 == 1) ? -0.5F : 0F;
            parityZAuto = (widthZ % 2 == 1) ? -0.5F : 0F;
        }

        info.pivotX = cx - parityXAuto;
        info.pivotY = cy;
        info.pivotZ = cz - parityZAuto;

        if (this.entriesCache == null || this.entriesCache.length != this.blocks.size())
        {
            this.entriesCache = new VirtualBlockRenderView.Entry[this.blocks.size()];

            for (int i = 0; i < this.blocks.size(); i++)
            {
                BlockEntry be = this.blocks.get(i);
                this.entriesCache[i] = new VirtualBlockRenderView.Entry(be.state, be.pos);
            }
        }

        StructureLightSettings slRuntime = this.form.structureLight.getRuntimeValue();
        boolean lightsEnabled;
        int lightIntensity;

        /* Resolve unified structure light settings with legacy fallback */
        if (slRuntime != null)
        {
            lightsEnabled = slRuntime.enabled;
            lightIntensity = slRuntime.intensity;
        }
        else
        {
            lightsEnabled = this.form.emitLight.get();
            lightIntensity = this.form.lightIntensity.get();
        }

        if (this.cachedView == null)
        {
            this.cachedView = new StructureVirtualBlockRenderView(Arrays.asList(this.entriesCache));
        }

        info.view = this.cachedView
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(lightsEnabled)
            .setLightIntensity(lightIntensity);

        if (lightsEnabled)
        {
            this.cachedView.setVirtualMode(true, lightIntensity)
                .setIgnoreWorldBlockLight(false);
        }
        else
        {
            this.cachedView.setVirtualMode(false, 0)
                .setIgnoreWorldBlockLight(true);
        }

        /* World anchor: for items/UI use player position (more stable) */
        /* to avoid anchoring at (0,0,0) and getting low world light. */
        boolean isItemContext = (context.type == FormRenderType.ITEM
            || context.type == FormRenderType.ITEM_FP
            || context.type == FormRenderType.ITEM_TP
            || context.type == FormRenderType.ITEM_INVENTORY);

        if (isItemContext || context.entity == null)
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            info.anchor = (mc.player != null) ? mc.player.getBlockPos() : BlockPos.ORIGIN;
        }
        else
        {
            info.anchor = new BlockPos(
                (int) Math.floor(context.entity.getX()),
                (int) Math.floor(context.entity.getY()),
                (int) Math.floor(context.entity.getZ())
            );
        }

        /* Define base offset from center/parity so BlockRenderView */
        /* can translate light/color queries to real world coordinates. */
        int baseDx = (int) Math.floor(-info.pivotX);
        int baseDy = (int) Math.floor(-info.pivotY);
        int baseDz = (int) Math.floor(-info.pivotZ);

        info.view.setWorldAnchor(info.anchor, baseDx, baseDy, baseDz)
            /* In UI/thumbnail/inventory item, force max sky light to avoid darkening.
               EXCEPT during VAO capture, where we want real virtual lighting baked. */
            .setForceMaxSkyLight(context.ui
                || context.type == FormRenderType.PREVIEW
                || context.type == FormRenderType.ITEM_INVENTORY || forceMaxSkyLight);

        return info;
    }

    /**
     * Render with culling using virtual BlockRenderView to leverage vanilla logic. Keeps the same
     * centering and parity as renderStructure.
     *
     * <p>1.21.11 render: draws every block through the entity-compatible {@code RenderLayer} variant
     * ({@link BlockRenderLayers#getEntityBlockLayer}/{@link BlockRenderLayers#getMovingBlockLayer}) so
     * this detached preview goes through the same {@code VertexConsumerProvider} path as entities/other
     * forms. The old chunk-building {@code BlockRenderLayer} enum returned by
     * {@link BlockRenderLayers#getBlockLayer} is no longer usable here: it now feeds the deferred
     * chunk-batching pipeline, not {@code VertexConsumerProvider} (see .port_1.21.11_notes.md #5), so the
     * previous distinction between a "shaders" entity path and a plain chunk-layer path is gone -
     * everything renders through the entity path. Block-entity decorations (chests, signs, skulls, etc.)
     * are intentionally NOT rendered here anymore: {@code BlockEntityRenderer#render} now requires a
     * {@code CameraRenderState} that only exists inside the main world render loop (same limitation
     * documented in {@code BlockFormRenderer#renderBlockEntity}), which this detached structure preview
     * has no access to. The base block models still render fine; only the extra BE decoration layer is
     * skipped.</p>
     */
    private void renderStructureCulledWorld(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);
        float globalAlpha = this.form.color.get().a;

        for (BlockEntry entry : this.blocks)
        {
            RenderLayer layer;
            VertexConsumer vc;
            Color tint;
            Function<VertexConsumer, VertexConsumer> recolor;

            stack.push();

            try
            {
                stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

                /* Animated blocks (portals, fire, fluids) get the "moving" texture variant so their
                 * animation keeps advancing; everything else uses the plain entity-block layer. */
                layer = this.isAnimatedTexture(entry.state)
                    ? BlockRenderLayers.getMovingBlockLayer(entry.state)
                    : BlockRenderLayers.getEntityBlockLayer(entry.state);

                /* If there is global opacity (<1), force translucent layer for all blocks */
                /* of the structure, so alpha is applied even to solid/cutout geometry. */
                if (globalAlpha < 0.999F)
                {
                    layer = RenderLayers.entityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
                }

                vc = consumers.getBuffer(layer);
                /* Wrap the consumer with tint/opacity to ensure coloration */
                /* also when using entity buffers (shader compatibility). */
                tint = this.form.color.get();
                recolor = BBSRendering.getColorConsumer(tint);


                if (recolor != null)
                {
                    vc = recolor.apply(vc);
                }

                if (!entry.state.getFluidState().isEmpty())
                {
                    VertexConsumer fluidVc = consumers.getBuffer(layer);

                    if (recolor != null)
                    {
                        fluidVc = recolor.apply(fluidVc);
                    }

                    fluidVc = new TransformingVertexConsumer(fluidVc, stack.peek(), entry.pos, true);
                    MinecraftClient.getInstance().getBlockRenderManager().renderFluid(entry.pos, info.view, fluidVc, entry.state, entry.state.getFluidState());
                }

                if (entry.state.getRenderType() != BlockRenderType.INVISIBLE)
                {
                    this.renderBlockModel(entry.state, entry.pos, info.view, stack, vc);
                }
            }

            finally
            {
                stack.pop();
            }
        }

        /* Important: if Sodium/Iris is active, the recolor wrapper uses */
        /* global static state (RecolorVertexConsumer.newColor). Ensure */
        /* it is reset after this pass so UI doesn't inherit the tint. */
        RecolorVertexConsumer.newColor = null;
    }

    /**
     * Renders a single block's baked model quads. 1.21.11 changed
     * {@code BlockRenderManager#renderBlock} to take the already-resolved {@code List<BlockModelPart>}
     * instead of a {@code Random} directly; the model lookup and per-position random seeding that used
     * to happen inside that call now happens here, mirroring vanilla's own chunk/entity renderers.
     */
    private void renderBlockModel(BlockState state, BlockPos pos, VirtualBlockRenderView view, MatrixStack stack, VertexConsumer vc)
    {
        Random random = Random.create();

        random.setSeed(state.getRenderingSeed(pos));

        BlockStateModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        List<BlockModelPart> parts = model.getParts(random);

        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(state, pos, view, stack, vc, true, random);
    }

    private void renderAnimatedBlocks(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, Function<VertexConsumer, VertexConsumer> recolor) {
            RenderInfo info = this.calculateRenderInfo(context, false);
            boolean shadersEnabled;
            RenderLayer layer;
            float globalAlphaAnim;
            VertexConsumer vc;

            for (BlockEntry entry : this.animatedBlocks)
            {
            if (!this.isAnimatedTexture(entry.state))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            /* Layer selection: in shaders use entity variant so the pack processes the animation */
            shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            layer = shadersEnabled
                ? RenderLayers.getEntityBlockLayer(entry.state, true)
                : RenderLayer.getTranslucentMovingBlock();

            /* If global alpha exists, prefer translucent entity layer in shaders to ensure smooth fade */
            globalAlphaAnim = this.form.getFormOpacity();

            if (globalAlphaAnim < 0.999F)
            {
                layer = shadersEnabled
                    ? TexturedRenderLayers.getEntityTranslucentCull()
                    : RenderLayer.getTranslucentMovingBlock();
            }

            /* Apply global alpha as recolor */
            vc = consumers.getBuffer(layer);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            if (this.form.renderFluid.get() && !entry.state.getFluidState().isEmpty())
            {
                boolean shaders = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                RenderLayer fluidLayer = shaders
                    ? RenderLayers.getEntityBlockLayer(entry.state, false)
                    : RenderLayers.getFluidLayer(entry.state.getFluidState());
                VertexConsumer fluidVc = consumers.getBuffer(fluidLayer);
                if (recolor != null)
                {
                    fluidVc = recolor.apply(fluidVc);
                }
                fluidVc = new TransformingVertexConsumer(fluidVc, stack.peek(), entry.pos, shaders);
                MinecraftClient.getInstance().getBlockRenderManager().renderFluid(entry.pos, info.view, fluidVc, entry.state, entry.state.getFluidState());
            }
            if (entry.state.getRenderType() != BlockRenderType.INVISIBLE)
            {
                MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, info.view, stack, vc, true, Random.create());
            }
            stack.pop();
        }

        /* Reset global color state (Sodium/Iris) after animated pass */
        RecolorVertexConsumer.newColor = null;
    }

    /**
     * Glass/ice and other translucent blocks: drawn separately with depth writes off so
     * models and deferred paint overlays behind the structure stay visible.
     */
    private void renderTranslucentBlocksVanilla(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, Function<VertexConsumer, VertexConsumer> recolor)
    {
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        this.syncFancyGraphicsFromOptions();
        RenderSystem.depthMask(false);

        try
        {
            RenderInfo info = this.calculateRenderInfo(context, false);

            for (BlockEntry entry : this.translucentBlocks)
            {
                boolean shadersEnabled;
                RenderLayer layer;
                float globalAlpha;
                VertexConsumer vc;

                if (!this.isTranslucentBlock(entry.state))
                {
                    continue;
                }

                stack.push();
                stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

                shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                layer = shadersEnabled
                    ? RenderLayers.getEntityBlockLayer(entry.state, false)
                    : RenderLayers.getBlockLayer(entry.state);

                globalAlpha = this.form.getFormOpacity();

                if (globalAlpha < 0.999F)
                {
                    layer = shadersEnabled
                        ? TexturedRenderLayers.getEntityTranslucentCull()
                        : RenderLayer.getTranslucent();
                }

                vc = consumers.getBuffer(layer);

                if (recolor != null)
                {
                    vc = recolor.apply(vc);
                }

                if (entry.state.getRenderType() != BlockRenderType.INVISIBLE)
                {
                    MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, info.view, stack, vc, true, Random.create());
                }

                stack.pop();
            }
        }
        finally
        {
            RenderSystem.depthMask(savedDepthMask);
            RecolorVertexConsumer.newColor = null;
        }
    }

    /** Renders blocks that require biome tint (leaves, grass, vines, lily pad) using vanilla layers. */
    private void renderBiomeTintedBlocksVanilla(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, Function<VertexConsumer, VertexConsumer> recolor)
    {
        /* Ensure correct blending state for translucent layers */
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        /* Ensure block atlas is active */
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        /* Structure foliage always Fancy — never sync down to Fast for this pass. */
        try
        {
            RenderLayers.setFancyGraphicsOrBetter(true);
        }
        catch (Throwable ignored)
        {}

        RenderInfo info = this.calculateRenderInfo(context, false);

        for (BlockEntry entry : this.biomeTintedBlocks)
        {
            boolean shadersEnabledTint;
            RenderLayer layer;
            float globalAlpha;
            VertexConsumer vc;

            if (!this.isBiomeTinted(entry.state))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            /* Leaves: Fancy cutout_mipped. Under Iris live entity buffers they look Fast with the
             * opacity patch — skip here (except shadow); deferred post-composite draws Fancy. */
            if (entry.state.getBlock() instanceof LeavesBlock)
            {
                boolean irisLive = BBSRendering.isIrisShadersEnabled()
                    && BBSRendering.isRenderingWorld()
                    && !context.isShadowPass
                    && !BBSRendering.isIrisShadowPass();

                if (irisLive)
                {
                    stack.pop();
                    continue;
                }

                this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);
                stack.pop();
                continue;
            }

            shadersEnabledTint = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            layer = this.resolveStructureBlockLayer(entry.state, shadersEnabledTint);

            /* If there is global opacity (<1), force translucent layer so alpha */
            /* applies to materials originally cutout/cull and they don't "disappear". */
            globalAlpha = this.form.getFormOpacity();

            if (globalAlpha < 0.999F)
            {
                layer = shadersEnabledTint ? TexturedRenderLayers.getEntityTranslucentCull() : RenderLayer.getTranslucent();
            }

            vc = consumers.getBuffer(layer);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, info.view, stack, vc, true, Random.create());
            stack.pop();
        }

        /* Restore state */
        RenderSystem.disableBlend();
        /* Reset global color state (Sodium/Iris) to avoid UI tinting */
        RecolorVertexConsumer.newColor = null;
    }

    private Color resolveStructureBlendColor()
    {
        Color storedFormColor = this.form.color.get();
        Color rawFormColor = storedFormColor.copyBakingColorGrade();
        Color tint = Color.white();

        if (FormColorEffects.shouldBakeFormColor(storedFormColor))
        {
            tint.mul(rawFormColor);
        }

        this.form.applyFormOpacity(tint);

        return tint;
    }

    /**
     * After Iris composite, redraw structure leaves as Fancy {@code cutout_mipped} with
     * vanilla biome foliage tint (same green as world trees). A BBS leaf VAO drops
     * per-vertex colors and looked gray under packs.
     */
    private void submitDeferredStructureLeavesFancy(FormRenderingContext context, int light, int overlay)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());
        Function<VertexConsumer, VertexConsumer> recolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            MatrixStack overlayStack = new MatrixStack();
            VertexConsumerProvider.Immediate consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            try
            {
                RenderLayers.setFancyGraphicsOrBetter(true);
            }
            catch (Throwable ignored)
            {}

            /* Vanilla post-composite defaults to depthMask(false) for BE tint overlays;
             * leaves need depth write so overlapping Fancy faces sort correctly. */
            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            try
            {
                this.renderStructureLeavesOnly(context, overlayStack, consumers, recolor);
                consumers.draw(RenderLayer.getCutoutMipped());
            }
            catch (Throwable ignored)
            {
                try
                {
                    consumers.draw();
                }
                catch (Throwable ignoredDraw)
                {}
            }
        });
    }

    /** Draws only leaf blocks as Fancy cutout (no-shader / non-Iris live path). */
    private void renderStructureLeavesOnly(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, Function<VertexConsumer, VertexConsumer> recolor)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);

        try
        {
            RenderLayers.setFancyGraphicsOrBetter(true);
        }
        catch (Throwable ignored)
        {}

        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

        for (BlockEntry entry : this.biomeTintedBlocks)
        {
            if (!(entry.state.getBlock() instanceof LeavesBlock))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);
            this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);
            stack.pop();
        }
    }

    /**
     * Bake blend / paint / grade into a ColorModulator tint for chests, beds, etc.
     * Under Iris the main-pass tint is ignored; {@link #submitDeferredStructureBlockEntityTint}
     * redraws BEs after composite where ColorModulator works again.
     */
    private Color resolveStructureBlockEntityColor()
    {
        Color tint = FormColorEffects.resolveBlockEntityTint(this.form.color.get(), this.form.paintSettings.get(), this.form.paintColor.get());

        this.form.applyFormOpacity(tint);

        return tint;
    }

    /**
     * True when every loaded block is a block-entity provider (chests/beds/signs/…).
     * Those meshes write heavy Iris shadow map coverage and trigger the cursor speck unless softened.
     */
    private boolean isEntirelyBlockEntities()
    {
        return this.hasBlockEntityLayer
            && !this.blockEntitiesList.isEmpty()
            && this.blockEntitiesList.size() >= this.blocks.size();
    }

    /**
     * Soften shadow-map alpha for BE-only structures: clears Complementary cursor fringe while
     * keeping a visible cast ({@link PaintSettings#SHADER_SHADOW_BLOCK_ENTITY}).
     */
    private void applyBlockEntityOnlyShaderShadow(Color color, boolean shadowPass)
    {
        if (color == null || !shadowPass || !this.isEntirelyBlockEntities())
        {
            return;
        }

        color.a = PaintSettings.SHADER_SHADOW_BLOCK_ENTITY;
    }

    private boolean needsDeferredBlockEntityTint(boolean positivePaint, boolean applyColorTint, Color storedFormColor)
    {
        /* Only redraw when the baked tint actually changes pixels. A white redraw on top of
         * the Iris-lit BE is pure z-fighting (see bed/chest flicker with paint=0). */
        Color beTint = this.resolveStructureBlockEntityColor();

        return beTint.r < 0.999F || beTint.g < 0.999F || beTint.b < 0.999F;
    }

    /**
     * After Iris composite, vanilla ColorModulator works again — redraw chests/beds/etc.
     * with the baked blend/paint/grade tint so BE-only structures actually change color.
     */
    private void submitDeferredStructureBlockEntityTint(FormRenderingContext context, int overlay)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            MatrixStack overlayStack = new MatrixStack();
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            try
            {
                this.renderBlockEntitiesOnly(context, overlayStack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, true);
            }
            catch (Throwable ignored)
            {}
        });
    }

    private Function<VertexConsumer, VertexConsumer> getMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void prepareVaoPaintForMainPass(Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            ModelVAORenderer.setPaint(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);
        }
        else
        {
            this.clearVaoPaint();
        }
    }

    private void clearVaoPaint()
    {
        ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
    }

    private void prepareVaoGlowForMainPass(GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        if (glowIntensity < 0F)
        {
            Color glowColor = new Color();

            glowSettings.resolveColor(legacyGlow, glowColor);
            ModelVAORenderer.setGlow(glowSettings, glowColor.r, glowColor.g, glowColor.b, legacyGlow);
        }
        else
        {
            this.clearVaoGlow();
        }
    }

    private void clearVaoGlow()
    {
        GlowSettings glowOff = new GlowSettings();

        glowOff.intensity = 0F;
        ModelVAORenderer.setGlow(glowOff, 0F, 0F, 0F, null);
    }

    private void resolveStructureMaskSize(Vector3f dest)
    {
        if (this.boundsMin != null && this.boundsMax != null)
        {
            dest.set(
                Math.max(1, this.boundsMax.getX() - this.boundsMin.getX() + 1),
                Math.max(1, this.boundsMax.getY() - this.boundsMin.getY() + 1),
                Math.max(1, this.boundsMax.getZ() - this.boundsMin.getZ() + 1)
            );

            return;
        }

        dest.set(
            Math.max(1, this.size.getX()),
            Math.max(1, this.size.getY()),
            Math.max(1, this.size.getZ())
        );
    }

    private void resolveStructureMaskHalf(EffectTransform transform, Vector3f dest)
    {
        Vector3f size = new Vector3f();

        this.resolveStructureMaskSize(size);
        EffectTransformMath.resolveStructureMaskHalfExtents(transform, dest, size.x, size.y, size.z);
    }

    private void prepareVaoColorTintForMainPass(MatrixStack stack, Color formColor, boolean active)
    {
        /* Color / Color Grade for structures use the same block overlay path as BlockFormRenderer
         * (scene regrade + Shape masks). Do not bake tint/grade into the VAO main pass. */
        if (!active)
        {
            return;
        }

        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        Vector3f colorMaskHalf = new Vector3f();

        this.resolveStructureMaskHalf(formColor.transform, colorMaskHalf);
        ModelVAORenderer.setColorEffectTransform(formRootInverse, formColor.transform, colorMaskHalf);
        ModelVAORenderer.setFormColorTint(formColor.r, formColor.g, formColor.b, formColor.a);
    }

    private void clearVaoColorTint()
    {
        ModelVAORenderer.clearColorEffectTransform();
        ModelVAORenderer.clearFormColorTint();
        ModelVAORenderer.clearFormColorGrade();
        ModelVAORenderer.clearGradeEffectTransforms();
    }

    private void renderStructureGlowOverlay(FormRenderingContext context, MatrixStack stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean optimize, boolean useEntityLayers)
    {
        this.renderStructureGlowOverlayPass(context, stack, glowSettings, legacyGlow, glowIntensity, alpha, overlay, optimize, useEntityLayers);
    }

    private void renderStructureGlowOverlayPass(FormRenderingContext context, MatrixStack stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean optimize, boolean useEntityLayers)
    {
        int layers = FormColorEffects.resolveGlowOverlayLayers(glowIntensity);

        if (optimize)
        {
            this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, () ->
            {
                CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

                this.renderStructureCulledWorld(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, useEntityLayers, null, true, true);
            });

            if (this.hasBiomeTintedLayer)
            {
                this.renderStructureLayerGlowOverlay(context, stack, glowSettings, legacyGlow, glowIntensity, alpha, overlay, useEntityLayers, StructurePaintLayer.BIOME, layers);
            }

            if (this.hasAnimatedLayer)
            {
                this.renderStructureLayerGlowOverlay(context, stack, glowSettings, legacyGlow, glowIntensity, alpha, overlay, useEntityLayers, StructurePaintLayer.ANIMATED, layers);
            }

            if (this.hasTranslucentLayer)
            {
                this.renderStructureLayerGlowOverlay(context, stack, glowSettings, legacyGlow, glowIntensity, alpha, overlay, useEntityLayers, StructurePaintLayer.TRANSLUCENT, layers);
            }
        }
        else
        {
            this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, () ->
            {
                CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

                this.renderStructureCulledWorld(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, useEntityLayers, null, true, false);
            });
        }
    }

    private void renderStructureLayerGlowOverlay(FormRenderingContext context, MatrixStack stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean useEntityLayers, StructurePaintLayer layer, int layers)
    {
        this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            if (layer == StructurePaintLayer.BIOME)
            {
                this.renderBiomeTintedBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
            else if (layer == StructurePaintLayer.ANIMATED)
            {
                this.renderAnimatedBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
            else
            {
                this.renderTranslucentBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
        });
    }

    private void runStructureBlocksGlowOverlay(GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, int layers, Runnable draw)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        Color glowColor = FormColorEffects.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);
        RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

        try
        {
            consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));
            draw.run();
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.depthFunc(savedDepthFunc);

            if (savedPolygonOffsetFill)
            {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            }
            else
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.defaultBlendFunc();
        }
    }

    private void renderStructurePaintOverlay(FormRenderingContext context, MatrixStack stack, Color resolvedPaint, float alpha, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        this.renderStructurePaintOverlayPass(context, stack, paintOverlay, overlay, optimize, useEntityLayers, transform, glowSettings, legacyGlow, glowIntensity, alpha);
    }

    private void submitDeferredStructurePaintOverlay(FormRenderingContext context, Color resolvedPaint, float alpha, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderStructurePaintOverlayPass(context, overlayStack, paintOverlay, overlay, optimize, useEntityLayers, transform, glowSettings, legacyGlow, glowIntensity, alpha);
        });
    }

    private void renderStructurePaintOverlayPass(FormRenderingContext context, MatrixStack stack, Color paintOverlay, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        if (optimize)
        {
            IModelVAO vao = this.getStructureVao();

            if (vao != null)
            {
                /* White tint — paint strength must not depend on form color tint (same as Block/Item). */
                this.renderStructureVaoPaintOverlay(vao, stack, Color.white(), paintOverlay, light, overlay, transform);
            }

            if (this.hasBiomeTintedLayer)
            {
                this.renderStructureLayerPaintOverlay(context, stack, paintOverlay, overlay, useEntityLayers, StructurePaintLayer.BIOME, transform, glowSettings, legacyGlow, glowIntensity, alpha);
            }

            if (this.hasAnimatedLayer)
            {
                this.renderStructureLayerPaintOverlay(context, stack, paintOverlay, overlay, useEntityLayers, StructurePaintLayer.ANIMATED, transform, glowSettings, legacyGlow, glowIntensity, alpha);
            }

            if (this.hasTranslucentLayer)
            {
                this.renderStructureLayerPaintOverlay(context, stack, paintOverlay, overlay, useEntityLayers, StructurePaintLayer.TRANSLUCENT, transform, glowSettings, legacyGlow, glowIntensity, alpha);
            }

            /* Block entities bake paint in the main pass — overlay shaders are incompatible. */
        }
        else
        {
            this.renderStructureCulledBlocksPaintOverlay(context, stack, paintOverlay, overlay, useEntityLayers, transform, glowSettings, legacyGlow, glowIntensity, alpha);
        }
    }

    private void renderStructureVaoPaintOverlay(IModelVAO vao, MatrixStack stack, Color tint, Color paintOverlay, int light, int overlay, EffectTransform transform)
    {
        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        Vector3f paintMaskHalf = new Vector3f();

        /* Structures: UI scale 1 covers full AABB for box / circle / triangle. */
        this.resolveStructureMaskHalf(transform, paintMaskHalf);

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        try
        {
            this.clearVaoColorTint();
            ModelVAORenderer.beginPaintOverlayPass(false);
            /* Stronger bias than the default paint pass — large coplanar structure faces z-fight otherwise. */
            GL11.glPolygonOffset(-1F, -2F);
            ModelVAORenderer.setPaint(paintOverlay.r, paintOverlay.g, paintOverlay.b, paintOverlay.a);
            ModelVAORenderer.setPaintEffectTransform(formRootInverse, transform, paintMaskHalf, true);
            RenderSystem.setShader(BBSShaders::getModel);
            RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(false);
            ModelVAORenderer.render(BBSShaders.getModel(), vao, stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
        }
        finally
        {
            RenderSystem.depthMask(true);
            ModelVAORenderer.clearPaintEffectTransform();
            ModelVAORenderer.endPaintOverlayPass();
            this.clearVaoPaint();
            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }

    private void renderStructureLayerPaintOverlay(FormRenderingContext context, MatrixStack stack, Color paintOverlay, int overlay, boolean useEntityLayers, StructurePaintLayer layer, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        this.runStructureBlocksPaintOverlay(paintOverlay, stack, transform, glowSettings, legacyGlow, glowIntensity, alpha, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            if (layer == StructurePaintLayer.BIOME)
            {
                this.renderBiomeTintedBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
            else if (layer == StructurePaintLayer.ANIMATED)
            {
                this.renderAnimatedBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
            else
            {
                this.renderTranslucentBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
        });
    }

    private void renderStructureCulledBlocksPaintOverlay(FormRenderingContext context, MatrixStack stack, Color paintOverlay, int overlay, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        this.runStructureBlocksPaintOverlay(paintOverlay, stack, transform, glowSettings, legacyGlow, glowIntensity, alpha, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            this.renderStructureCulledWorld(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, useEntityLayers, null, true, false);
        });
    }

    private void runStructureBlocksPaintOverlay(Color paintOverlay, MatrixStack stack, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, Runnable draw)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        CustomVertexConsumerProvider.clearRunnables();

        Vector3f structureSize = new Vector3f();

        this.resolveStructureMaskSize(structureSize);
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configurePaintOverlayRenderStateStructure(formRootInverse, transform, true, glowSettings, legacyGlow, glowIntensity, alpha, structureSize.x, structureSize.y, structureSize.z));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        /* Pull paint toward camera so coplanar structure faces do not z-fight. */
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -2F);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));

        try
        {
            draw.run();
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
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void renderStructureColorTintOverlay(FormRenderingContext context, MatrixStack stack, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers, boolean includeVao)
    {
        this.renderStructureColorTintOverlayPass(context, stack, formColor, alpha, overlay, optimize, useEntityLayers, includeVao);
    }

    private void submitDeferredStructureColorTintOverlay(FormRenderingContext context, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());
        Color formColorSnapshot = formColor.copy();

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderStructureColorTintOverlayPass(context, overlayStack, formColorSnapshot, alpha, overlay, optimize, useEntityLayers, false);
        });
    }

    private void submitDeferredStructureVaoColorTintOverlay(FormRenderingContext context, Color formColor, float alpha, int overlay)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());
        Color formColorSnapshot = formColor.copy();
        Color tint = Color.white();

        tint.a = alpha;

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            IModelVAO vao = this.getStructureVao();

            if (vao != null)
            {
                this.renderStructureVaoColorTintOverlay(vao, overlayStack, tint, formColorSnapshot, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay);
            }
        });
    }

    private void renderStructureColorTintOverlayPass(FormRenderingContext context, MatrixStack stack, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers, boolean includeVao)
    {
        Color tintUniform = this.resolveStructureColorTintUniform(formColor);

        /* Same pipeline as BlockFormRenderer: redraw geometry with block_color_tint_overlay
         * (lit scene regrade + per-channel Shape/Transform). VAO ColorTint multiply is not used. */
        if (optimize)
        {
            this.runStructureBlocksColorTintOverlay(tintUniform, stack, this.form.color.get(), () ->
            {
                CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

                this.renderStructureCulledWorld(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, useEntityLayers, null, true, true);
            });

            if (this.hasBiomeTintedLayer)
            {
                this.renderStructureLayerColorTintOverlay(context, stack, tintUniform, overlay, useEntityLayers, StructurePaintLayer.BIOME);
            }

            if (this.hasAnimatedLayer)
            {
                this.renderStructureLayerColorTintOverlay(context, stack, tintUniform, overlay, useEntityLayers, StructurePaintLayer.ANIMATED);
            }

            if (this.hasTranslucentLayer)
            {
                this.renderStructureLayerColorTintOverlay(context, stack, tintUniform, overlay, useEntityLayers, StructurePaintLayer.TRANSLUCENT);
            }

            /* Block entities bake Color Grade in the main pass — overlay shaders are incompatible. */
        }
        else
        {
            this.renderStructureCulledBlocksColorTintOverlay(context, stack, tintUniform, overlay, useEntityLayers);
        }
    }

    /**
     * When Color Grade is active, FormColorTint must stay blend-only; grade is uploaded
     * separately so hue/sat regrade lit pixels (same as Block).
     */
    private Color resolveStructureColorTintUniform(Color formColor)
    {
        Color stored = this.form.color.get();

        if (stored != null && stored.hasColorAdjustments())
        {
            Color tint = stored.copyDeferringColorGrade();

            if (formColor != null && formColor.transform != null)
            {
                tint.transform = formColor.transform.copy();
            }
            else if (stored.transform != null)
            {
                tint.transform = stored.transform.copy();
            }

            this.form.applyFormOpacity(tint);

            return tint;
        }

        return formColor;
    }

    private void renderStructureVaoColorTintOverlay(IModelVAO vao, MatrixStack stack, Color tint, Color formColor, int light, int overlay)
    {
        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        Vector3f colorMaskHalf = new Vector3f();

        this.resolveStructureMaskHalf(formColor.transform, colorMaskHalf);

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        try
        {
            ModelVAORenderer.beginColorTintOverlayPass();
            ModelVAORenderer.setColorEffectTransform(formRootInverse, formColor.transform, colorMaskHalf);
            ModelVAORenderer.setFormColorTint(formColor.r, formColor.g, formColor.b, formColor.a);

            Color gradeSource = this.form.color.get();

            if (gradeSource != null && gradeSource.hasColorAdjustments())
            {
                ModelVAORenderer.setFormColorGrade(gradeSource.brightness, gradeSource.contrast, gradeSource.hue, gradeSource.saturation);
            }

            RenderSystem.setShader(BBSShaders::getModel);
            RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            ModelVAORenderer.render(BBSShaders.getModel(), vao, stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
        }
        finally
        {
            ModelVAORenderer.clearColorEffectTransform();
            ModelVAORenderer.clearFormColorTint();
            ModelVAORenderer.clearFormColorGrade();
            ModelVAORenderer.endColorTintOverlayPass();
            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }

    private void renderStructureLayerColorTintOverlay(FormRenderingContext context, MatrixStack stack, Color formColor, int overlay, boolean useEntityLayers, StructurePaintLayer layer)
    {
        this.runStructureBlocksColorTintOverlay(formColor, stack, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            if (layer == StructurePaintLayer.BIOME)
            {
                this.renderBiomeTintedBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
            else if (layer == StructurePaintLayer.ANIMATED)
            {
                this.renderAnimatedBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
            else
            {
                this.renderTranslucentBlocksVanilla(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, null);
            }
        });
    }

    private void renderStructureCulledBlocksColorTintOverlay(FormRenderingContext context, MatrixStack stack, Color formColor, int overlay, boolean useEntityLayers)
    {
        this.runStructureBlocksColorTintOverlay(formColor, stack, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            this.renderStructureCulledWorld(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, useEntityLayers, null, true, false);
        });
    }

    private void runStructureBlocksColorTintOverlay(Color formColor, MatrixStack stack, Runnable draw)
    {
        this.runStructureBlocksColorTintOverlay(formColor, stack, this.form.color.get(), draw);
    }

    private void runStructureBlocksColorTintOverlay(Color formColor, MatrixStack stack, Color gradeSource, Runnable draw)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();

        CustomVertexConsumerProvider.clearRunnables();

        Vector3f structureSize = new Vector3f();

        this.resolveStructureMaskSize(structureSize);
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configureColorTintOverlayRenderStateStructure(formRootInverse, formColor.transform, true, formColor, gradeSource, structureSize.x, structureSize.y, structureSize.z));

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());

        try
        {
            draw.run();
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

    /** Glass/ice/stained glass — translucent layer, excluding animated fluids already drawn separately. */
    private boolean isTranslucentBlock(BlockState state)
    {
        RenderLayer layer;

        if (state == null || this.isAnimatedTexture(state))
        {
            return false;
        }

        layer = RenderLayers.getBlockLayer(state);

        return layer == RenderLayer.getTranslucent()
            || layer == RenderLayer.getTranslucentMovingBlock()
            || layer == RenderLayer.getTripwire();
    }

    /** Determines if the block requires texture animation (portal/water/lava). */
    private boolean isAnimatedTexture(BlockState state)
    {
        FluidState fs;

        if (state == null)
        {
            return false;
        }

        /* Nether Portal */
        if (state.isOf(Blocks.NETHER_PORTAL))
        {
            return true;
        }

        /* Fire */
        if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE))
        {
            return true;
        }

        /* Fluids: water and lava (including flowing variants). */
        fs = state.getFluidState();

        if (fs != null)
        {
            if (fs.getFluid() == Fluids.WATER || fs.getFluid() == Fluids.FLOWING_WATER ||
                fs.getFluid() == Fluids.LAVA || fs.getFluid() == Fluids.FLOWING_LAVA)
            {
                return true;
            }
        }

        return false;
    }

    /** Heuristic: determines if the block uses biome tint (foliage/grass/vine/lily pad). */
    private boolean isBiomeTinted(BlockState state)
    {
        Block b;

        if (state == null)
        {
            return false;
        }

        b = state.getBlock();

        return (b instanceof LeavesBlock)
            || (b instanceof GrassBlock)
            || (b instanceof VineBlock)
            || (b instanceof LilyPadBlock)
            || (b instanceof RedstoneWireBlock)
            || (b instanceof StemBlock)
            || (b instanceof AttachedStemBlock)
            || state.isOf(Blocks.FERN)
            || state.isOf(Blocks.SUGAR_CANE)
            || state.isOf(Blocks.SHORT_GRASS)
            || state.isOf(Blocks.TALL_GRASS)
            || state.isOf(Blocks.LARGE_FERN);
    }

    /**
     * Renders only Block Entities (chests, beds, signs, skulls, etc.) over the structure already drawn via VAO.
     * Reuses the same centering/parity and world anchor calculation as the culled render.
     */
    private void renderBlockEntitiesOnly(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay)
    {
        this.renderBlockEntitiesOnly(context, stack, consumers, light, overlay, true);
    }

    /**
     * @param applyColorTint when false, keep the caller's vertex substitute (paint / color-tint overlays).
     */
    private void renderBlockEntitiesOnly(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, boolean applyColorTint)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);
        BlockEntityRenderDispatcher beDispatcher = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

        for (BlockEntry entry : this.blockEntitiesList)
        {
            Block block = entry.state.getBlock();

            stack.push();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            int dx = (int) Math.floor(entry.pos.getX() - info.pivotX);
            int dy = (int) Math.floor(entry.pos.getY() - info.pivotY);
            int dz = (int) Math.floor(entry.pos.getZ() - info.pivotZ);
            BlockPos worldPos = info.anchor.add(dx, dy, dz);

            BlockEntity be = ((BlockEntityProvider) block).createBlockEntity(worldPos, entry.state);

            if (be != null)
            {
                if (entry.nbt != null)
                {
                    be.readNbt(entry.nbt, MinecraftClient.getInstance().world.getRegistryManager());
                }
                BlockEntityRenderer<?> renderer;
                int skyLight;
                int blockLight;
                int beLight;

                if (MinecraftClient.getInstance().world != null)
                {
                    be.setWorld(MinecraftClient.getInstance().world);
                }

                renderer = beDispatcher.get(be);

                skyLight = info.view.getLightLevel(LightType.SKY, entry.pos);
                blockLight = info.view.getLightLevel(LightType.BLOCK, entry.pos);
                /* LightmapTextureManager.pack expects block light first then sky light. */
                beLight = LightmapTextureManager.pack(blockLight, skyLight);

                if (renderer != null)
                {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    BlockEntityRenderer raw = (BlockEntityRenderer) renderer;
                    CustomVertexConsumerProvider beProvider;

                    /* Apply tint outside Iris gbuffer. Iris ignores ColorModulator and
                     * setShaderColor during the entity pass also breaks pack shading. */
                    beProvider = FormUtilsClient.getProvider();

                    Color beTint = null;
                    boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

                    if (applyColorTint)
                    {
                        beTint = this.resolveStructureBlockEntityColor();
                        this.applyBlockEntityOnlyShaderShadow(beTint, shadowPass);
                        beProvider.setSubstitute(BBSRendering.getColorConsumer(beTint));
                    }
                    else if (shadowPass && this.isEntirelyBlockEntities())
                    {
                        /* Untinted Iris path still needs softened alpha so BE-only casts do not speck. */
                        beTint = new Color(1F, 1F, 1F, PaintSettings.SHADER_SHADOW_BLOCK_ENTITY);
                        beProvider.setSubstitute(BBSRendering.getColorConsumer(beTint));
                    }

                    try
                    {
                        if (beTint != null)
                        {
                            RenderSystem.setShaderColor(beTint.r, beTint.g, beTint.b, beTint.a);
                        }

                        raw.render(be, 0F, stack, beProvider, beLight, overlay);
                    }
                    finally
                    {
                        if (beTint != null)
                        {
                            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                        }

                        beProvider.draw();

                        if (applyColorTint)
                        {
                            beProvider.setSubstitute(null);
                            CustomVertexConsumerProvider.clearRunnables();
                        }
                    }
                }
            }

            stack.pop();
        }
    }

    /**
     * Detects if shaders are active (Iris). Avoids hard dependencies using reflection.
     */
    private boolean isShadersActive()
    {
        try
        {
            Class<?> apiClass = Class.forName("IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isShaderPackInUse").invoke(api);

            return result instanceof Boolean && (Boolean) result;
        }
        catch (Throwable ignored)
        {}

        return false;
    }

    private void ensureLoaded()
    {
        String file = this.form.structureFile.get();

        if (file == null || file.isEmpty())
        {
            /* Nothing selected; clear to avoid ghost render. */
            this.blocks.clear();
            this.size = BlockPos.ORIGIN;
            this.boundsMin = null;
            this.boundsMax = null;
            this.entriesCache = null;
            this.cachedView = null;
            this.lastFile = null;

            return;
        }

        if (file.equals(this.lastFile) && !this.blocks.isEmpty())
        {
            return;
        }

        File nbtFile = BBSMod.getProvider().getFile(Link.create(file));

        this.blocks.clear();
        this.size = BlockPos.ORIGIN;
        this.boundsMin = null;
        this.boundsMax = null;
        this.lastFile = file;
        this.entriesCache = null;
        this.cachedView = null;

        try
        {
            /* Equivalent of the old "fancy graphics" toggle for leaves cutout vs solid classification
             * (see .port_1.21.11_notes.md - BlockRenderLayers.setCutoutLeaves replaces the removed
             * RenderLayers.setFancyGraphicsOrBetter for this purpose). */
            GraphicsMode gm = MinecraftClient.getInstance().options.getPreset().getValue();

            BlockRenderLayers.setCutoutLeaves(gm != GraphicsMode.FAST);
        }
        catch (Throwable ignored)
        {}

        /* Try reading as external file if exists; otherwise use internal assets InputStream. */
        if (nbtFile != null && nbtFile.exists())
        {
            try
            {
                NbtCompound root = NbtIo.readCompressed(nbtFile.toPath(), NbtSizeTracker.ofUnlimitedBytes());

                this.parseStructure(root);

                return;
            }
            catch (IOException e)
            {}
        }

        /* If no File (internal assets), read via provider InputStream. */
        try (InputStream is = BBSMod.getProvider().getAsset(Link.create(file)))
        {
            try
            {
                NbtCompound root = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes());

                this.parseStructure(root);
            }
            catch (IOException e)
            {}
        }
        catch (Exception e)
        {}
    }

    /**
     * 1.21.11 render: {@code NbtCompound}/{@code NbtList} lost their {@code contains(String, byte type)}
     * and 2-arg {@code getXxx(String, byte type)} overloads (unrelated to the RenderPipeline migration,
     * but required for this file to compile against the current mappings); the single-arg getters now
     * return {@code Optional}, and {@code getXxxOrEmpty(...)}/{@code getXxx(key, default)} convenience
     * overloads replace the old contains()-then-get() pattern used throughout this parser.
     */
    private void parseStructure(NbtCompound root)
    {
        /* Size */
        int[] sz = root.getIntArray("size").orElse(null);

        if (sz != null && sz.length >= 3)
        {
            this.size = new BlockPos(sz[0], sz[1], sz[2]);
        }

        /* Palette -> state list */
        List<BlockState> paletteStates = new ArrayList<>();
        NbtList palette = root.getListOrEmpty("palette");

        for (int i = 0; i < palette.size(); i++)
        {
            NbtCompound entry = palette.getCompoundOrEmpty(i);
            BlockState state = this.readBlockState(entry);

            paletteStates.add(state);
        }

        /* Blocks */
        NbtList list = root.getListOrEmpty("blocks");

        if (!list.isEmpty())
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (int i = 0; i < list.size(); i++)
            {
                NbtCompound be = list.getCompoundOrEmpty(i);
                BlockPos pos = this.readBlockPos(be.getListOrEmpty("pos"));
                int stateIndex = be.getInt("state", -1);

                if (stateIndex >= 0 && stateIndex < paletteStates.size())
                {
                    BlockState state = paletteStates.get(stateIndex);

                    if (state == null || state.isAir())
                    {
                        continue;
                    }

                    NbtCompound nbt = be.getCompound("nbt").orElse(null);
                    BlockEntry blockEntry = new BlockEntry(state, pos, nbt);

                    this.blocks.add(blockEntry);

                    /* Update bounds */
                    if (pos.getX() < minX) minX = pos.getX();
                    if (pos.getY() < minY) minY = pos.getY();
                    if (pos.getZ() < minZ) minZ = pos.getZ();
                    if (pos.getX() > maxX) maxX = pos.getX();
                    if (pos.getY() > maxY) maxY = pos.getY();
                    if (pos.getZ() > maxZ) maxZ = pos.getZ();
                }
            }

            if (!this.blocks.isEmpty())
            {
                this.boundsMin = new BlockPos(minX, minY, minZ);
                this.boundsMax = new BlockPos(maxX, maxY, maxZ);
            }
        }
    }

    private BlockPos readBlockPos(NbtList list)
    {
        int x;
        int y;
        int z;

        if (list == null || list.size() < 3)
        {
            return BlockPos.ORIGIN;
        }

        x = list.getInt(0, 0);
        y = list.getInt(1, 0);
        z = list.getInt(2, 0);

        return new BlockPos(x, y, z);
    }

    private BlockState readBlockState(NbtCompound entry)
    {
        String name = entry.getString("Name", "");
        Block block;
        BlockState state;

        try
        {
            Identifier id = Identifier.of(name);

            block = Registries.BLOCK.get(id);

            if (block == null)
            {
                block = Blocks.AIR;
            }
        }
        catch (Exception e)
        {
            block = Blocks.AIR;
        }

        if ("minecraft:jigsaw".equals(name) || block == Blocks.JIGSAW)
        {
            return Blocks.AIR.getDefaultState();
        }

        state = block.getDefaultState();

        NbtCompound props = entry.getCompound("Properties").orElse(null);

        if (props != null)
        {
            for (String key : props.getKeys())
            {
                String value = props.getString(key, "");
                Property<?> property = block.getStateManager().getProperty(key);

                if (property != null)
                {
                    Optional<?> parsed = property.parse(value);

                    if (parsed.isPresent())
                    {
                        try
                        {
                            @SuppressWarnings({"rawtypes", "unchecked"})
                            Property raw = property;
                            @SuppressWarnings("unchecked")
                            Comparable c = (Comparable) parsed.get();

                            state = state.with(raw, c);
                        }
                        catch (Exception ignored)
                        {}
                    }
                }
            }
        }

        return state;
    }

    private static class BlockEntry
    {
        final BlockState state;
        final BlockPos pos;
        final NbtCompound nbt;

        BlockEntry(BlockState state, BlockPos pos, NbtCompound nbt)
        {
            this.state = state;
            this.pos = pos;
            this.nbt = nbt;
        }
    }

    private static class TransformingVertexConsumer implements VertexConsumer
    {
        private final VertexConsumer parent;
        private final Matrix4f positionMatrix;
        private final Matrix3f normalMatrix;
        private final BlockPos offset;
        private final boolean injectOverlay;

        public TransformingVertexConsumer(VertexConsumer parent, MatrixStack.Entry entry, BlockPos offset, boolean injectOverlay)
        {
            this.parent = parent;
            this.positionMatrix = new Matrix4f(entry.getPositionMatrix());
            this.normalMatrix = new Matrix3f(entry.getNormalMatrix());
            this.offset = offset;
            this.injectOverlay = injectOverlay;
        }

        @Override
        public VertexConsumer lineWidth(float width)
        {
            this.parent.lineWidth(width);
            return this;
        }

        @Override
        public VertexConsumer color(int argb)
        {
            this.parent.color(argb);
            return this;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z)
        {
            float nx = x - this.offset.getX();
            float ny = y - this.offset.getY();
            float nz = z - this.offset.getZ();

            float tx = this.positionMatrix.m00() * nx + this.positionMatrix.m10() * ny + this.positionMatrix.m20() * nz + this.positionMatrix.m30();
            float ty = this.positionMatrix.m01() * nx + this.positionMatrix.m11() * ny + this.positionMatrix.m21() * nz + this.positionMatrix.m31();
            float tz = this.positionMatrix.m02() * nx + this.positionMatrix.m12() * ny + this.positionMatrix.m22() * nz + this.positionMatrix.m32();

            this.parent.vertex(tx, ty, tz);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha)
        {
            this.parent.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v)
        {
            this.parent.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v)
        {
            this.parent.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v)
        {
            if (this.injectOverlay)
            {
                this.parent.overlay(0, 10);
            }
            this.parent.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z)
        {
            float tx = this.normalMatrix.m00() * x + this.normalMatrix.m10() * y + this.normalMatrix.m20() * z;
            float ty = this.normalMatrix.m01() * x + this.normalMatrix.m11() * y + this.normalMatrix.m21() * z;
            float tz = this.normalMatrix.m02() * x + this.normalMatrix.m12() * y + this.normalMatrix.m22() * z;

            this.parent.normal(tx, ty, tz);
            return this;
        }
    }

    private enum StructurePaintLayer
    {
        BIOME, ANIMATED, TRANSLUCENT
    }
}
