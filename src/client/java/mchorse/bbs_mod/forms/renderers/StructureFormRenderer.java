package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.cubic.render.vao.IModelVAO;
import mchorse.bbs_mod.cubic.render.vao.LightmapModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAOData;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.cubic.render.vao.StructureVAOCollector;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.forms.renderers.utils.BlockEffectOverlayUniforms;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.StructureData;
import mchorse.bbs_mod.forms.renderers.utils.StructureFormOverlayRenderer;
import mchorse.bbs_mod.forms.renderers.utils.StructureVaoManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import mchorse.bbs_mod.forms.renderers.utils.StructureVirtualBlockRenderView;
import mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.state.property.Property;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;

import net.irisshaders.iris.api.v0.IrisApi;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static class VaoHolder
    {
        public IModelVAO vao;
        public IModelVAO picking;
    }

    private static final Map<String, VaoHolder> VAO_CACHE = new HashMap<>();
    /* Bump when structure leaf Fancy capture/draw rules change. */
    private static final int LIGHTING_REVISION = 5;
    private static int cachedLightingRevision = -1;

    private final StructureData data = new StructureData();
    private final StructureFormOverlayRenderer overlayRenderer = new StructureFormOverlayRenderer();
    private final StructureVaoManager vaoManager = new StructureVaoManager();

    private final List<BlockEntry> blocks = new ArrayList<>();
    private final List<BlockEntry> animatedBlocks = new ArrayList<>();
    private final List<BlockEntry> biomeTintedBlocks = new ArrayList<>();
    private final List<BlockEntry> translucentBlocks = new ArrayList<>();
    private final List<BlockEntry> blockEntitiesList = new ArrayList<>();

    private String lastFile = null;

    private BlockPos size = BlockPos.ORIGIN;
    private BlockPos boundsMin = null;
    private BlockPos boundsMax = null;

    private boolean vaoDirty = true;
    private boolean capturingVAO = false;
    private boolean vaoPickingDirty = true;
    private boolean capturingIncludeSpecialBlocks = false;
    private boolean lastEmitLight = false;
    private int lastLightIntensity = 0;
    private boolean hasTranslucentLayer = false;
    private boolean hasCutoutLayer = false;
    private boolean hasAnimatedLayer = false;
    private boolean hasBiomeTintedLayer = false;
    private boolean hasLeavesLayer = false;
    private boolean hasBlockEntityLayer = false;
    private VirtualBlockRenderView.Entry[] entriesCache = null;
    private StructureVirtualBlockRenderView cachedView = null;

    private enum StructurePaintLayer
    {
        BIOME,
        ANIMATED,
        TRANSLUCENT
    }

    public static void clearAllCachedVaos()
    {
        for (VaoHolder holder : VAO_CACHE.values())
        {
            if (holder.vao instanceof ModelVAO)
            {
                ((ModelVAO) holder.vao).delete();
            }

            if (holder.vao instanceof LightmapModelVAO)
            {
                ((LightmapModelVAO) holder.vao).delete();
            }

            if (holder.picking instanceof ModelVAO)
            {
                ((ModelVAO) holder.picking).delete();
            }
        }

        VAO_CACHE.clear();
    }

    private static void ensureLightingRevision()
    {
        if (cachedLightingRevision != LIGHTING_REVISION)
        {
            StructureFormRenderer.clearAllCachedVaos();
            cachedLightingRevision = LIGHTING_REVISION;
        }
    }

    public StructureFormRenderer(StructureForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        // context.batcher.getContext().draw();

        StructureFormRenderer.ensureLightingRevision();
        this.ensureLoaded();

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

        int wUnits = 1;
        int hUnits = 1;
        int dUnits = 1;
        int maxUnits;

        float auto;
        float finalScale;

        boolean optimize = true;

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

        MatrixStackUtils.invertUiNormalY(matrices);

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        // RenderSystem.setupLevelDiffuseLighting(light0, light1);

        StructureLightSettings slUi = this.form.structureLight.getRuntimeValue();
        boolean currentEmitLightUi = (slUi != null) ? slUi.enabled : this.form.emitLight.get();
        int currentLightIntensityUi = (slUi != null) ? slUi.intensity : this.form.lightIntensity.get();

        if (currentEmitLightUi != this.lastEmitLight || currentLightIntensityUi != this.lastLightIntensity)
        {
            this.vaoDirty = true;
            this.lastEmitLight = currentEmitLightUi;
            this.lastLightIntensity = currentLightIntensityUi;
        }

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
                FormRenderingContext uiContext = new FormRenderingContext()
                    .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                this.renderStructureCulledWorld(uiContext, matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, shaders, mainRecolor, false, false);

                if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                {
                    immediate.draw();
                }


                if (positivePaint)
                {
                    EffectTransform paintTransform = this.form.paintSettings.get().transform;
                    this.overlayRenderer.renderStructurePaintOverlay(this.data, null, uiContext, matrices, resolvedPaint, tint.a, OverlayTexture.DEFAULT_UV, true, BBSRendering.isIrisShadersEnabled(), paintTransform, glowSettings, legacyGlow, glowIntensity, layer -> this.renderPaintLayer(layer, uiContext, matrices, OverlayTexture.DEFAULT_UV, null), (s) -> this.renderStructureCulledWorld(uiContext, s, FormUtilsClient.getProvider(), LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, BBSRendering.isIrisShadersEnabled(), null, true, false));
                }

                if (colorTransformWanted)
                {
                    this.overlayRenderer.renderStructureColorTintOverlay(this.data, this.form, uiContext, matrices, formColor, tint.a, OverlayTexture.DEFAULT_UV, true, BBSRendering.isIrisShadersEnabled(), deferColorTintToOverlay, layer -> this.renderPaintLayer(layer, uiContext, matrices, OverlayTexture.DEFAULT_UV, null), (s) -> this.renderStructureCulledWorld(uiContext, s, FormUtilsClient.getProvider(), LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, BBSRendering.isIrisShadersEnabled(), null, true, false));
                }
            }
            catch (Throwable ignored)
            {}
        }
        else
        {
            IModelVAO vao = this.getStructureVao();

            if (vao == null || this.vaoDirty)
            {
                this.buildStructureVAO();
                vao = this.getStructureVao();
            }

            if (vao != null)
            {
                // GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
                // ShaderProgram shader = BBSShaders.getModel();

                // gameRenderer.getLightmapTextureManager().enable();
                // gameRenderer.getOverlayTexture().setupOverlayColor();

                /* Revert to own model shader in vanilla to ensure VAO compatibility */
                // RenderSystem.setShader(shader);
                BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);

                boolean needBlendUI = tint.a < 0.999F || this.hasTranslucentLayer;

                if (needBlendUI)
                {
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(770, 771, 1, 0);
                }
                else
                {
                    GlStateManager._disableBlend();
                }

                GlStateManager._enableCull();

                this.prepareVaoPaintForMainPass(resolvedPaint);
                this.prepareVaoGlowForMainPass(glowSettings, legacyGlow, glowIntensity);
                /* Color / Color Grade: block overlay after draw (same as BlockFormRenderer). */

                try
                {
                    ModelVAORenderer.render(null, vao, matrices, tint.r, tint.g, tint.b, tint.a, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
                }
                finally
                {
                    this.clearVaoColorTint();
                    this.clearVaoPaint();
                    this.clearVaoGlow();
                }

                if (this.hasBlockEntityLayer)
                {
                    try
                    {
                        VertexConsumerProvider beConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                        FormRenderingContext beContext = new FormRenderingContext()
                            .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                        this.renderBlockEntitiesOnly(beContext, matrices, beConsumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

                        if (beConsumers instanceof VertexConsumerProvider.Immediate immediate)
                        {
                            immediate.draw();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (this.hasBiomeTintedLayer)
                {
                    try
                    {
                        boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                        VertexConsumerProvider consumersTint = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                        FormRenderingContext tintContext = new FormRenderingContext()
                            .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                        this.renderBiomeTintedBlocksVanilla(tintContext, matrices, consumersTint, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, mainRecolor);

                        if (consumersTint instanceof VertexConsumerProvider.Immediate immediate)
                        {
                            immediate.draw();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (this.hasAnimatedLayer)
                {
                    try
                    {
                        boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                        VertexConsumerProvider consumersAnim = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                        FormRenderingContext animContext = new FormRenderingContext()
                            .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                        this.renderAnimatedBlocksVanilla(animContext, matrices, consumersAnim, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, mainRecolor);

                        if (consumersAnim instanceof VertexConsumerProvider.Immediate immediate)
                        {
                            immediate.draw();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (this.hasTranslucentLayer)
                {
                    try
                    {
                        VertexConsumerProvider consumersGlass = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                        FormRenderingContext glassContext = new FormRenderingContext()
                            .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                        this.renderTranslucentBlocksVanilla(glassContext, matrices, consumersGlass, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, mainRecolor);

                        if (consumersGlass instanceof VertexConsumerProvider.Immediate immediate)
                        {
                            immediate.draw();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                // gameRenderer.getLightmapTextureManager().disable();
                // gameRenderer.getOverlayTexture().teardownOverlayColor();
                GlStateManager._disableBlend();

                FormRenderingContext uiContext = new FormRenderingContext()
                    .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                if (positivePaint)
                {
                    EffectTransform paintTransform = this.form.paintSettings.get().transform;

                    this.renderStructurePaintOverlay(uiContext, matrices, resolvedPaint, tint.a, OverlayTexture.DEFAULT_UV, true, this.isShadersActive(), paintTransform, glowSettings, legacyGlow, glowIntensity);
                }

                if (positiveGlow)
                {
                    this.overlayRenderer.renderStructureGlowOverlay(this.data, uiContext, matrices, glowSettings, legacyGlow, glowIntensity, tint.a, OverlayTexture.DEFAULT_UV, false, BBSRendering.isIrisShadersEnabled(), null, (s) -> this.renderStructureCulledWorld(uiContext, s, FormUtilsClient.getProvider(), LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, BBSRendering.isIrisShadersEnabled(), null, true, false));
                }
            }
        }

        // DiffuseLighting.disableGuiDepthLighting();

        matrices.pop();

        /* Restore depth state expected by UI system */
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        StructureFormRenderer.ensureLightingRevision();
        this.ensureLoaded();

        context.stack.push();

        try
        {
            /* Apply structure scale */
            context.stack.scale(this.form.scaleX.get(), this.form.scaleY.get(), this.form.scaleZ.get());


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

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            if (shadowPass)
            {
                mainTint3D.a *= storedFormColor3D.a;
            }
            else if (FormColorEffects.shouldBakeFormColor(storedFormColor3D))
            {
                mainTint3D.mul(rawFormColor3D);
            }

            this.form.applyFormOpacity(mainTint3D);
            this.form.applyFormOpacity(formColor3D);

            FormColorEffects.applyShadowPassColorFix(mainTint3D, storedFormColor3D, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);
            this.applyBlockEntityOnlyShaderShadow(mainTint3D, shadowPass);

            if (mainTint3D.a <= 0.001F && !shadowPass && !picking)
            {
                return;
            }

            GlowSettings glowSettings = this.form.glowSettings.get();
            Color legacyGlow = this.form.glowingColor.get();
            float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
            EffectTransform glowTransform = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
            boolean hasGlowTransform = glowTransform != null && glowTransform.isActive();
            boolean positiveGlow = !picking && !shadowPass && glowIntensity > 0F;
            boolean hasEmissiveGlow = positiveGlow && !glowSettings.resolvePaintOnly();

            boolean noshadingDefer = !context.modelRenderer
                && !shadowPass
                && BBSRendering.needsIrisNoshadingOpacityDeferral(mainTint3D.a, this.form.noshadingOpacity.get());
            boolean softPostDeferred = !context.modelRenderer
                && !shadowPass
                && ShaderOpacityPatch.shouldDelayUntilPostDeferred(mainTint3D.a)
                && !noshadingDefer;

            boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
            boolean deferColorTintToOverlay = colorTransformWanted && irisWorldPaintDeferral && !shadowPass;
            PaintSettings paintSettings = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color resolvedPaint = FormColorEffects.resolvePaintColor(paintSettings, legacyPaint);
            boolean positivePaint = !picking && !shadowPass && FormColorEffects.hasPositivePaint(paintSettings, legacyPaint);
            boolean applyColorTint = colorTransformWanted && !picking && !shadowPass;
            Function<VertexConsumer, VertexConsumer> mainRecolor = this.getMainConsumer(mainTint3D, resolvedPaint);
            Color vaoTint = mainTint3D.copy();
            Function<VertexConsumer, VertexConsumer> layerRecolor = mainRecolor;

            if (glowIntensity < 0F)
            {
                FormColorEffects.blendFormGlowBrighten(mainTint3D, glowSettings, legacyGlow);
                FormColorEffects.blendFormGlowBrighten(vaoTint, glowSettings, legacyGlow);
            }
            else if (irisWorldPaintDeferral && hasEmissiveGlow && !softPostDeferred && !noshadingDefer && !hasGlowTransform)
            {
                /* Must hit the Iris entity/gbuffer pass - post-composite BBS additive never blooms.
                 * Base emission on a neutral white base so form color tint does not distort bloom. */
                vaoTint = new Color(1F, 1F, 1F, mainTint3D.a);
                FormColorEffects.blendFormGlowBrighten(vaoTint, glowSettings, legacyGlow);
                layerRecolor = this.getMainConsumer(new Color(1F, 1F, 1F, mainTint3D.a), resolvedPaint);
            }

            boolean shaders = BBSRendering.isIrisShadersEnabled();

            if (vao != null)
            {
                int light = context.isPicking() ? 0 : context.light;
                GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

                if (context.isPicking())
                {
                    IModelVAO pickingVao = this.getPickingVao();

                    if (pickingVao == null || this.vaoPickingDirty)
                    {
                        this.buildStructureVAOPicking();
                        pickingVao = this.getPickingVao();
                    }

                    if (pickingVao != null)
                    {
                        ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), pickingVao, context.stack, 1F, 1F, 1F, 1F, light, context.overlay);
                    }
                }
                else if (softPostDeferred || noshadingDefer)
                {
                    boolean irisCamera = BBSRendering.isIrisWorldModelPass() && !noshadingDefer;
                    Matrix4f positionMatrix = irisCamera
                        ? new Matrix4f(context.stack.peek().getPositionMatrix())
                        : ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
                    Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());
                    Color mainTintSnapshot = mainTint3D.copy();
                    Color vaoTintSnapshot = vaoTint.copy();
                    Color formColor3DSnapshot = formColor3D.copy();
                    Color resolvedPaintSnapshot = resolvedPaint == null ? null : resolvedPaint.copy();
                    PaintSettings paintSettingsSnapshot = paintSettings == null ? null : paintSettings.copy();
                    int lightSnapshot = light;
                    int overlaySnapshot = context.overlay;
                    boolean depthWrite = ShaderOpacityPatch.shouldWriteDepthForOpacity(mainTint3D.a);
                    boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(mainTint3D.a);
                    double formSortKey = this.computeStructureFormSortKey(context.stack.peek().getPositionMatrix(), context);
                    boolean positiveGlowSnapshot = positiveGlow && !glowSettings.resolvePaintOnly();
                    float glowIntensitySnapshot = glowIntensity;
                    GlowSettings glowSettingsSnapshot = glowSettings;
                    Color legacyGlowSnapshot = legacyGlow;
                    boolean positivePaintSnapshot = positivePaint;
                    boolean applyColorTintSnapshot = applyColorTint;
                    boolean beTintSnapshot = !irisWorldPaintDeferral;
                    IModelVAO vaoSnapshot = vao;
                    boolean shadersSnapshot = shaders;
                    Function<VertexConsumer, VertexConsumer> mainRecolorSnapshot = this.getMainConsumer(mainTintSnapshot, resolvedPaintSnapshot);

                    Runnable deferredDraw = () ->
                    {
                        MatrixStack overlayStack = new MatrixStack();
                        GameRenderer deferredGameRenderer = MinecraftClient.getInstance().gameRenderer;

                        overlayStack.peek().getPositionMatrix().set(positionMatrix);
                        overlayStack.peek().getNormalMatrix().set(normalMatrix);

                        try
                        {
                            GlStateManager._enableDepthTest();
                            GlStateManager._depthFunc(GL11.GL_LEQUAL);
                            GlStateManager._enableBlend();
                            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

                            /* Soft Structure: per-block back-to-front color (depth-write off) so
                             * leaves behind soft trunks stay visible AND leaves in front composite
                             * after the trunk. VAO depth stamp afterward still occludes the world.
                             * Solid-VAO + special-layer splits cannot satisfy both at once. */
                            ShaderOpacityPatch.setFlushingDepthWrite(false);
                            GlStateManager._depthMask(false);
                            this.renderStructureSoftSortedColor(context, overlayStack, mainRecolorSnapshot, lightSnapshot, overlaySnapshot);

                            if (this.data.hasBlockEntityLayer())
                            {
                                this.renderBlockEntitiesPass(context, overlayStack, lightSnapshot, overlaySnapshot, beTintSnapshot);
                                ShaderOpacityPatch.setFlushingDepthWrite(false);
                                GlStateManager._depthMask(false);
                            }

                            if (depthWrite)
                            {
                                ShaderOpacityPatch.setFlushingDepthWrite(true);
                                GlStateManager._depthMask(true);
                                GlStateManager._colorMask(false, false, false, false);
                                GlStateManager._disableBlend();

                                try
                                {
                                    this.renderStructureSoftDepthStamp(overlayStack, vaoSnapshot, mainTintSnapshot, lightSnapshot, overlaySnapshot);
                                }
                                finally
                                {
                                    GlStateManager._enableBlend();
                                    GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                                    GlStateManager._colorMask(true, true, true, true);
                                }
                            }

                            if (positivePaintSnapshot)
                            {
                                EffectTransform paintTransform = paintSettingsSnapshot.transform;
                                this.overlayRenderer.renderStructurePaintOverlay(this.data, vaoSnapshot, context, overlayStack, resolvedPaintSnapshot, mainTintSnapshot.a, overlaySnapshot, true, shadersSnapshot, paintTransform, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot, layer -> this.renderPaintLayer(layer, context, overlayStack, overlaySnapshot, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), lightSnapshot, overlaySnapshot, shadersSnapshot, null, true, false));
                            }

                            if (applyColorTintSnapshot)
                            {
                                this.overlayRenderer.renderStructureColorTintOverlay(this.data, this.form, context, overlayStack, formColor3DSnapshot, mainTintSnapshot.a, overlaySnapshot, true, shadersSnapshot, false, layer -> this.renderPaintLayer(layer, context, overlayStack, overlaySnapshot, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), lightSnapshot, overlaySnapshot, shadersSnapshot, null, true, false));
                            }

                            if (positiveGlowSnapshot)
                            {
                                ShaderOpacityPatch.setFlushingDepthWrite(false);
                                GlStateManager._depthMask(false);
                                this.overlayRenderer.renderStructureGlowOverlay(this.data, context, overlayStack, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot, mainTintSnapshot.a, overlaySnapshot, false, shadersSnapshot, null, (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), lightSnapshot, overlaySnapshot, shadersSnapshot, null, true, false));
                            }

                            ShaderOpacityPatch.setFlushingDepthWrite(depthWrite);
                            GlStateManager._depthMask(depthWrite);
                            CustomVertexConsumerProvider.clearRunnables();
                        }
                        finally
                        {
                            GlStateManager._colorMask(true, true, true, true);
                            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
                            ShaderOpacityPatch.setFlushingDepthWrite(depthWrite);
                            GlStateManager._depthMask(depthWrite);
                        }
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
                    RenderPipeline shader = (BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld())
                        ? BBSShaders.getModel()
                        : BBSShaders.getModel();

                    // this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    // RenderSystem.setShader(BBSShaders.getPickerModelsProgram());
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(770, 771, 1, 0);
                    BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(770, 771, 1, 0);

                    this.prepareVaoPaintForMainPass(resolvedPaint);
                    this.prepareVaoGlowForMainPass(glowSettings, legacyGlow, glowIntensity);
                    /* Color / Color Grade: block overlay after draw (same as BlockFormRenderer). */

                    try
                    {
                        ModelVAORenderer.render(shader, vao, context.stack, vaoTint.r, vaoTint.g, vaoTint.b, vaoTint.a, light, context.overlay);
                    }
                    finally
                    {
                        this.overlayRenderer.clearVaoColorTint();
                        this.overlayRenderer.clearVaoPaint();
                        this.overlayRenderer.clearVaoGlow();
                    }

                    Color layerShaderTint = (BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld()) ? vaoTint : mainTint3D;

                    if (this.data.hasBlockEntityLayer())
                    {
                        VertexConsumerProvider beConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                        /* Under Iris, ColorModulator on BEs breaks pack shading and still ignores tint.
                         * Draw untinted here; tinted redraw runs after composite. */
                        boolean beTint = !irisWorldPaintDeferral;

                        this.renderBlockEntitiesPass(context, context.stack, light, context.overlay, beTint);

                        if (beConsumers instanceof VertexConsumerProvider.Immediate immediate)
                        {
                            immediate.draw();
                        }
                    }

                    if (this.data.hasBiomeTintedLayer())
                    {
                        this.renderLayerGroup(this.data.getBiomeTintedBlocks(), context, context.stack, light, context.overlay, layerRecolor, layerShaderTint, true);
                    }

                    if (this.data.hasAnimatedLayer())
                    {
                        this.renderLayerGroup(this.data.getAnimatedBlocks(), context, context.stack, light, context.overlay, layerRecolor, layerShaderTint, false);
                    }

                    if (this.data.hasTranslucentLayer())
                    {
                        this.renderLayerGroup(this.data.getTranslucentBlocks(), context, context.stack, light, context.overlay, layerRecolor, layerShaderTint, false);
                    }
                }

                if (!softPostDeferred && !noshadingDefer && applyColorTint)
                {
                    if (irisWorldPaintDeferral)
                    {
                        this.overlayRenderer.submitDeferredStructureColorTintOverlay(this.data, this.form, context, formColor3D, mainTint3D.a, context.overlay, true, shaders, layer -> this.renderPaintLayer(layer, context, context.stack, context.overlay, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                    }
                    else
                    {
                        this.overlayRenderer.renderStructureColorTintOverlay(this.data, this.form, context, context.stack, formColor3D, mainTint3D.a, context.overlay, true, shaders, false, layer -> this.renderPaintLayer(layer, context, context.stack, context.overlay, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                    }
                }

                if (!softPostDeferred && !noshadingDefer && positivePaint)
                {
                    EffectTransform paintTransform = paintSettings.transform;
                    this.overlayRenderer.submitDeferredStructurePaintOverlay(this.data, vao, context, resolvedPaint, mainTint3D.a, context.overlay, true, shaders, paintTransform, glowSettings, legacyGlow, glowIntensity, layer -> this.renderPaintLayer(layer, context, context.stack, context.overlay, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                }

                if (!softPostDeferred && !noshadingDefer && positiveGlow)
                {
                    if (irisWorldPaintDeferral)
                    {
                        this.overlayRenderer.submitDeferredStructureGlowOverlay(this.data, context, glowSettings, legacyGlow, glowIntensity, mainTint3D.a, context.overlay, false, shaders, hasGlowTransform ? glowTransform : null, null, (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                    }
                    else
                    {
                        this.overlayRenderer.renderStructureGlowOverlay(this.data, context, context.stack, glowSettings, legacyGlow, glowIntensity, mainTint3D.a, context.overlay, false, shaders, null, (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                    }
                }
            }

            // gameRenderer.getLightmapTextureManager().disable();
            // gameRenderer.getOverlayTexture().teardownOverlayColor();

            /* Restore state if VAO was used */
            GlStateManager._disableBlend();
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GL11.GL_LEQUAL);
        }
        finally
        {
            CustomVertexConsumerProvider.clearRunnables();
            context.stack.pop();
        }
    }

    /**
     * Soft Structure color: every block back-to-front with depth-write off.
     * Fixes the solid-vs-translucent tradeoff that VAO + layered passes cannot resolve.
     */
    private void renderStructureSoftSortedColor(FormRenderingContext context, MatrixStack stack, Function<VertexConsumer, VertexConsumer> recolor, int light, int overlay)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        StructureData.syncFancyGraphicsFromOptions();
        ShaderOpacityPatch.setFlushingDepthWrite(false);
        GlStateManager._depthMask(false);

        RenderInfo info = this.calculateRenderInfo(context, false);
        List<BlockEntry> sorted = new ArrayList<>(this.data.getBlocks());
        Matrix4f drawMatrix = stack.peek().getPositionMatrix();
        boolean filmLookAxis = context != null
            && context.type == FormRenderType.ENTITY
            && context.camera != null
            && !context.modelRenderer;

        sorted.sort(Comparator.comparingDouble((BlockEntry entry) -> this.computeStructureBlockSortKey(entry, info, drawMatrix, filmLookAxis)).reversed());

        VertexConsumerProvider.Immediate immediateConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        for (BlockEntry entry : sorted)
        {
            stack.push();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);
            this.renderStructureSoftBlock(entry, info, stack, immediateConsumers, recolor);
            stack.pop();
        }

        immediateConsumers.draw();
        GlStateManager._disableBlend();
        RecolorVertexConsumer.newColor = null;
        ShaderOpacityPatch.setFlushingDepthWrite(false);
    }

    private double computeStructureBlockSortKey(BlockEntry entry, RenderInfo info, Matrix4f drawMatrix, boolean filmLookAxis)
    {
        Vector4f center = new Vector4f(
            entry.pos.getX() - info.pivotX + 0.5F,
            entry.pos.getY() - info.pivotY + 0.5F,
            entry.pos.getZ() - info.pivotZ + 0.5F,
            1F
        );

        drawMatrix.transform(center);

        if (filmLookAxis)
        {
            return -center.z;
        }

        return center.x * center.x + center.y * center.y + center.z * center.z;
    }

    private void renderStructureSoftBlock(BlockEntry entry, RenderInfo info, MatrixStack stack, VertexConsumerProvider consumers, Function<VertexConsumer, VertexConsumer> recolor)
    {
        if (entry.state.getBlock() instanceof LeavesBlock)
        {
            this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);

            return;
        }

        boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
        RenderLayer layer = TexturedRenderLayers.getItemTranslucentCull();
        VertexConsumer vc = consumers.getBuffer(layer);

        if (recolor != null)
        {
            vc = recolor.apply(vc);
        }

        if (this.form.renderFluid.get() && !entry.state.getFluidState().isEmpty())
        {
            RenderLayer fluidLayer = shadersEnabled
                ? BlockRenderLayers.getEntityBlockLayer(entry.state)
                : BlockRenderLayers.getFluidLayer(entry.state.getFluidState());
            VertexConsumer fluidVc = consumers.getBuffer(fluidLayer);

            if (recolor != null)
            {
                fluidVc = recolor.apply(fluidVc);
            }

            fluidVc = new TransformingVertexConsumer(fluidVc, stack.peek(), entry.pos, shadersEnabled);
            MinecraftClient.getInstance().getBlockRenderManager().renderFluid(entry.pos, info.view, fluidVc, entry.state, entry.state.getFluidState());
        }

        if (entry.state.getRenderType() != BlockRenderType.INVISIBLE)
        {
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, info.view, stack, vc, true, Random.create());
        }
    }

    /**
     * Depth-only stamp of the solid structure VAO after soft color (no color write).
     * Keeps other forms/world occluded without letting soft color depth-kill itself.
     */
    private void renderStructureSoftDepthStamp(MatrixStack stack, IModelVAO vao, Color mainTint, int light, int overlay)
    {
        if (vao == null)
        {
            return;
        }

        RenderPipeline shader = BBSShaders.getModel();

        ModelVAORenderer.render(shader, vao, stack, mainTint.r, mainTint.g, mainTint.b, mainTint.a, light, overlay);
    }

    /**
     * Soft-opacity queue key for the structure form origin (farther first).
     */
    private double computeStructureFormSortKey(Matrix4f drawMatrix, FormRenderingContext context)
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

    private void checkLightState()
    {
        StructureLightSettings sl = this.form.structureLight.getRuntimeValue();
        boolean currentEmitLight = (sl != null) ? sl.enabled : this.form.emitLight.get();
        int currentLightIntensity = (sl != null) ? sl.intensity : this.form.lightIntensity.get();

        if (currentEmitLight != this.lastEmitLight || currentLightIntensity != this.lastLightIntensity)
        {
            this.vaoManager.setVaoDirty(true);
            this.lastEmitLight = currentEmitLight;
            this.lastLightIntensity = currentLightIntensity;
        }
    }

    private IModelVAO getVao()
    {
        IModelVAO vao = this.vaoManager.getStructureVao(this.data.getLastFile());

        if (vao == null || this.vaoManager.isVaoDirty())
        {
            this.vaoManager.buildStructureVAO(this.data.getLastFile(), () ->
            {
                Function<VertexConsumer, VertexConsumer> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());
                MatrixStack captureStack = new MatrixStack();
                FormRenderingContext captureContext = new FormRenderingContext()
                    .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                this.renderStructureCulledWorld(captureContext, captureStack, FormUtilsClient.getProvider(), LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, false, captureRecolor, false, false);
            });

            vao = this.vaoManager.getStructureVao(this.data.getLastFile());
        }

        return vao;
    }

    private IModelVAO getPickingVao()
    {
        IModelVAO pickingVao = this.vaoManager.getStructureVaoPicking(this.data.getLastFile());

        if (pickingVao == null || this.vaoManager.isVaoPickingDirty())
        {
            this.vaoManager.buildStructureVAOPicking(
                this.data.getLastFile(),
                this.data,
                () ->
                {
                    Function<VertexConsumer, VertexConsumer> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());
                    MatrixStack captureStack = new MatrixStack();
                    FormRenderingContext captureContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                    this.renderStructureCulledWorld(captureContext, captureStack, FormUtilsClient.getProvider(), LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, false, captureRecolor, false, false);
                },
                () ->
                {
                    MatrixStack captureStack = new MatrixStack();
                    FormRenderingContext captureContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                    this.renderBlockEntitiesOnly(captureContext, captureStack, FormUtilsClient.getProvider(), LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, false);
                },
                collector ->
                {
                    MatrixStack captureStack = new MatrixStack();
                    FormRenderingContext captureContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                    this.appendBlockEntityPickCubes(collector, captureContext);
                }
            );

            pickingVao = this.vaoManager.getStructureVaoPicking(this.data.getLastFile());
        }

        return pickingVao;
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
            .setForceMaxSkyLight(!this.capturingVAO && (context.ui
                || context.type == FormRenderType.PREVIEW
                || context.type == FormRenderType.ITEM_INVENTORY || forceMaxSkyLight));

        return info;
    }

    /**
     * Leaves (and similar) use solid vs cutout_mipped based on {@link RenderLayers}'
     * fancy flag. Keep that flag in sync with the client Graphics option whenever we
     * pick block layers — otherwise structure trees stay opaque (Fast) even when Fancy.
     */
    private void syncFancyGraphicsFromOptions()
    {
        try
        {
            BlockRenderLayers.setCutoutLeaves(this.isFancyGraphicsEnabled());
        }
        catch (Throwable ignored)
        {}
    }

    private boolean isFancyGraphicsEnabled()
    {
        try
        {
            return MinecraftClient.getInstance().options.getPreset().getValue() != GraphicsMode.FAST;
        }
        catch (Throwable ignored)
        {
            return true;
        }
    }

    /**
     * Structure morphs always draw leaves as Fancy cutout (see-through) so they match
     * world trees. Terrain {@link RenderLayers#getBlockLayer} returns Solid when the
     * Fancy flag is false / desynced — that is the opaque “Fast graphics” look.
     */
    private RenderLayer resolveStructureBlockLayer(BlockState state, boolean useEntityLayers)
    {
        if (state.getBlock() instanceof LeavesBlock)
        {
            return this.resolveStructureLeavesLayer(state, useEntityLayers);
        }

        return useEntityLayers
            ? BlockRenderLayers.getEntityBlockLayer(state)
            : BlockRenderLayers.getBlockLayer(state);
    }

    private RenderLayer resolveStructureLeavesLayer(BlockState state, boolean useEntityLayers)
    {
        boolean irisWorld = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();

        if (irisWorld || useEntityLayers)
        {
            return BlockRenderLayers.getEntityBlockLayer(state);
        }

        if (StructureData.isFancyGraphicsEnabled())
        {
            try
            {
                BlockRenderLayers.setCutoutLeaves(true);
            }
            catch (Throwable ignored)
            {}

            /* Always terrain cutout_mipped — Iris maps this to gbuffers_terrain_cutout
             * (alpha discard). entity_cutout / gbuffers_entities often looks Fast under packs. */
            return RenderLayers.cutout();
        }

        StructureData.syncFancyGraphicsFromOptions();

        return RenderLayers.solid();
    }

    /**
     * Fancy leaves: same layer as world trees ({@code cutout_mipped}). Do not use
     * {@code renderBlockAsEntity} under Iris — that picks entity_cutout and packs treat it opaque.
     */
    private void renderStructureLeaves(BlockState state, BlockPos pos, BlockRenderView view, MatrixStack stack, VertexConsumerProvider consumers, Function<VertexConsumer, VertexConsumer> recolor)
    {
        boolean fancy = StructureData.isFancyGraphicsEnabled();
        boolean irisWorld = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
        boolean softOpacity = this.wantsSoftStructureBlockLayers();
        RenderLayer layer;
        VertexConsumer vc;
        boolean cull;

        if (softOpacity)
        {
            layer = TexturedRenderLayers.getItemTranslucentCull();
            cull = !fancy;
        }
        else if (irisWorld)
        {
            layer = BlockRenderLayers.getEntityBlockLayer(state);
            cull = !fancy;
        }
        else if (fancy)
        {
            try
            {
                BlockRenderLayers.setCutoutLeaves(true);
            }
            catch (Throwable ignored)
            {
            }

            layer = RenderLayers.cutout();
            cull = false;
        }
        else
        {
            StructureData.syncFancyGraphicsFromOptions();
            layer = RenderLayers.solid();
            cull = true;
        }

        vc = consumers.getBuffer(layer);

        if (recolor != null)
        {
            vc = recolor.apply(vc);
        }

        /* cull=false: leaf-vs-leaf faces stay visible like Fancy chunk meshing. */
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(state, pos, view, stack, vc, false, Collections.emptyList());
    }

    private boolean wantsSoftStructureBlockLayers()
    {
        return this.form.getFormOpacity() < ShaderOpacityPatch.LIVE_DEPTH_WRITE_ALPHA
            || ShaderOpacityPatch.isPostDeferredPhase();
    }

    private void renderLayerGroup(List<BlockEntry> group, FormRenderingContext context, MatrixStack stack, int light, int overlay, Function<VertexConsumer, VertexConsumer> recolor, Color shaderTint, boolean forceDrawLeaves)
    {
        /* Ensure block atlas is active */
        BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);

        RenderInfo info = this.calculateRenderInfo(context, false);
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();

        CustomVertexConsumerProvider.clearRunnables();

        try
        {
            for (BlockEntry entry : group)
            {
                stack.push();
                stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

                if (entry.state.getBlock() instanceof LeavesBlock)
                {
                    this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);
                    stack.pop();
                    continue;
                }

                RenderLayer layer = this.resolveStructureBlockLayer(entry.state, shadersEnabled);

                if (this.wantsSoftStructureBlockLayers())
                {
                    /* Always entity translucent under soft opacity - terrain translucent vanishes
                     * when drawn from the soft post-deferred flush without shaders. */
                    layer = TexturedRenderLayers.getItemTranslucentCull();
                }

                VertexConsumer vc = consumers.getBuffer(layer);
                if (recolor != null)
                {
                    vc = recolor.apply(vc);
                     if (this.form.renderFluid.get() && !entry.state.getFluidState().isEmpty())
                {
                    RenderLayer fluidLayer = shadersEnabled
                        ? BlockRenderLayers.getEntityBlockLayer(entry.state)
                        : BlockRenderLayers.getFluidLayer(entry.state.getFluidState());
                    VertexConsumer fluidVc = consumers.getBuffer(fluidLayer);

                    if (recolor != null)
                    {
                        fluidVc = recolor.apply(fluidVc);
                    }

                    fluidVc = new TransformingVertexConsumer(fluidVc, stack.peek(), entry.pos, shadersEnabled);
                    MinecraftClient.getInstance().getBlockRenderManager().renderFluid(entry.pos, info.view, fluidVc, entry.state, entry.state.getFluidState());
                }
            }

                if (entry.state.getRenderType() != BlockRenderType.INVISIBLE)
                {
                    MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, info.view, stack, vc, true, Collections.emptyList());
                }

                stack.pop();
            }

            consumers.draw();
        }
        finally
        {
            GlStateManager._disableBlend();
            CustomVertexConsumerProvider.clearRunnables();
            RecolorVertexConsumer.newColor = null;
        }
    }

    /** Renders blocks that require biome tint (leaves, grass, vines, lily pad) using vanilla layers. */
    private void renderBiomeTintedBlocksVanilla(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, Function<VertexConsumer, VertexConsumer> recolor, StructurePaintLayer layer)
    {
        if (layer == StructurePaintLayer.BIOME)
        {
            this.renderLayerGroup(this.data.getBiomeTintedBlocks(), context, stack, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, recolor, null, true);
        }
        else if (layer == StructurePaintLayer.ANIMATED)
        {
            this.renderLayerGroup(this.data.getAnimatedBlocks(), context, stack, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, recolor, null, false);
        }
        else
        {
            this.renderLayerGroup(this.data.getTranslucentBlocks(), context, stack, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, recolor, null, false);
        }

        /* Restore state */
        GlStateManager._disableBlend();
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
                BlockRenderLayers.setCutoutLeaves(true);
            }
            catch (Throwable ignored)
            {}

            /* Vanilla post-composite defaults to depthMask(false) for BE tint overlays;
             * leaves need depth write so overlapping Fancy faces sort correctly. */
            GlStateManager._depthMask(true);
            BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(770, 771, 1, 0);

            try
            {
                this.renderStructureLeavesOnly(context, overlayStack, consumers, recolor);
                consumers.draw(RenderLayers.cutout());
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
            BlockRenderLayers.setCutoutLeaves(true);
        }
        catch (Throwable ignored)
        {}

        BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);

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
        Matrix4f exactMvm = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f exactStack = new Matrix4f(context.stack.peek().getPositionMatrix());
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            MatrixStack overlayStack = new MatrixStack();
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            overlayStack.peek().getPositionMatrix().set(exactStack);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(exactMvm);
            MatrixStackUtils.applyModelViewMatrix();

            try
            {
                this.renderBlockEntitiesOnly(context, overlayStack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, true);
                consumers.draw();
            }
            catch (Throwable ignored)
            {
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                MatrixStackUtils.applyModelViewMatrix();
                consumers.setSubstitute(null);
            }
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

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0);
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);
        // RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

        try
        {
            consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));
            draw.run();
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            GlStateManager._depthMask(savedDepthMask);
            GlStateManager._depthFunc(savedDepthFunc);

            if (savedPolygonOffsetFill)
            {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            }
            else
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
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

        // gameRenderer.getLightmapTextureManager().enable();
        // gameRenderer.getOverlayTexture().setupOverlayColor();

        try
        {
            this.clearVaoColorTint();
            ModelVAORenderer.beginPaintOverlayPass(false);
            /* Stronger bias than the default paint pass — large coplanar structure faces z-fight otherwise. */
            GL11.glPolygonOffset(-1F, -2F);
            ModelVAORenderer.setPaint(paintOverlay.r, paintOverlay.g, paintOverlay.b, paintOverlay.a);
            ModelVAORenderer.setPaintEffectTransform(formRootInverse, transform, paintMaskHalf, true);
            BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager._depthMask(false);
            ModelVAORenderer.render(null, vao, stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
        }
        finally
        {
            GlStateManager._depthMask(true);
            ModelVAORenderer.clearPaintEffectTransform();
            ModelVAORenderer.endPaintOverlayPass();
            this.clearVaoPaint();
            // gameRenderer.getLightmapTextureManager().disable();
            // gameRenderer.getOverlayTexture().teardownOverlayColor();
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

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(false);
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
            GlStateManager._depthMask(savedDepthMask);
            GlStateManager._depthFunc(savedDepthFunc);
            GL11.glPolygonOffset(0F, 0F);

            if (savedPolygonOffsetFill)
            {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            }
            else
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
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

        // gameRenderer.getLightmapTextureManager().enable();
        // gameRenderer.getOverlayTexture().setupOverlayColor();

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

            // RenderSystem.setShader(BBSShaders.getModel());
            BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            ModelVAORenderer.render(null, vao, stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
        }
        finally
        {
            ModelVAORenderer.clearColorEffectTransform();
            ModelVAORenderer.clearFormColorTint();
            ModelVAORenderer.clearFormColorGrade();
            ModelVAORenderer.endColorTintOverlayPass();
            // gameRenderer.getLightmapTextureManager().disable();
            // gameRenderer.getOverlayTexture().teardownOverlayColor();
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

        GlStateManager._enableBlend();
        GlStateManager._depthMask(false);

        /* Neutral vertices — lighting lives in the scene copy when grading. */
        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());

        try
        {
            draw.run();
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            GlStateManager._depthMask(true);
            // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    /** Glass/ice/stained glass — translucent layer, excluding animated fluids already drawn separately. */
    private boolean isTranslucentBlock(BlockState state)
    {
        if (state == null || this.isAnimatedTexture(state))
        {
            return false;
        }

        BlockRenderLayer layer = BlockRenderLayers.getBlockLayer(state);

        return layer == BlockRenderLayer.TRANSLUCENT
            || layer == BlockRenderLayer.TRIPWIRE;
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
        BlockEntityRenderManager beDispatcher = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

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
                    this.readBlockEntityNbt(be, entry.nbt);
                }
                @SuppressWarnings({"rawtypes", "unchecked"})
                BlockEntityRenderer renderer;
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
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        var renderState = renderer.createRenderState();
                        if (renderState != null)
                        {
                            renderer.updateRenderState(be, renderState, 0F, Vec3d.ZERO, null);
                            renderer.render(renderState, stack, null, null);
                        }
                    }
                    finally
                    {
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
            this.animatedBlocks.clear();
            this.biomeTintedBlocks.clear();
            this.translucentBlocks.clear();
            this.blockEntitiesList.clear();
            this.size = BlockPos.ORIGIN;
            this.boundsMin = null;
            this.boundsMax = null;
            this.vaoDirty = true;
            this.vaoPickingDirty = true;
            this.hasTranslucentLayer = false;
            this.hasCutoutLayer = false;
            this.hasAnimatedLayer = false;
            this.hasBiomeTintedLayer = false;
            this.hasLeavesLayer = false;
            this.hasBlockEntityLayer = false;
            this.entriesCache = null;
            this.cachedView = null;
            this.clearCachedVao();
            this.lastFile = null;

            return;
        }

        if (file.equals(this.lastFile) && !this.blocks.isEmpty())
        {
            return;
        }

        File nbtFile = BBSMod.getProvider().getFile(Link.create(file));

        this.blocks.clear();
        this.animatedBlocks.clear();
        this.biomeTintedBlocks.clear();
        this.translucentBlocks.clear();
        this.blockEntitiesList.clear();
        this.size = BlockPos.ORIGIN;
        this.boundsMin = null;
        this.boundsMax = null;
        this.clearCachedVao();
        this.lastFile = file;
        this.vaoDirty = true;
        this.vaoPickingDirty = true;
        this.hasTranslucentLayer = false;
        this.hasCutoutLayer = false;
        this.hasAnimatedLayer = false;
        this.hasBiomeTintedLayer = false;
        this.hasLeavesLayer = false;
        this.hasBlockEntityLayer = false;
        this.entriesCache = null;
        this.cachedView = null;

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

    private void buildStructureVAO()
    {
        /* Capture geometry in a VAO using vanilla pipeline but substituting the consumer. */
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();
        LightmapStructureVAOCollector lightWrapper = new LightmapStructureVAOCollector(collector);
        MatrixStack captureStack = new MatrixStack();
        FormRenderingContext captureContext;
        boolean useEntityLayers = false; /* capture with block layers */
        ModelVAOData data;

        /* Substitute any consumer with our collector. */
        provider.setSubstitute(vc -> lightWrapper);

        captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        try
        {
            this.syncFancyGraphicsFromOptions();
        }
        catch (Throwable ignored)
        {}

        /* Avoid rendering BlockEntities during capture to avoid mixing atlases. */
        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = false; /* for normal VAO, skip animated/biome. */

        try
        {
            Function<VertexConsumer, VertexConsumer> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());

            this.renderStructureCulledWorld(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, useEntityLayers, captureRecolor, false, false);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
        }

        provider.draw();
        provider.setSubstitute(null);

        data = collector.toData();

        if (this.lastFile != null)
        {
            VaoHolder holder = VAO_CACHE.computeIfAbsent(this.lastFile, k -> new VaoHolder());

            if (holder.vao instanceof ModelVAO)
            {
                ((ModelVAO) holder.vao).delete();
            }

            if (holder.vao instanceof LightmapModelVAO)
            {
                ((LightmapModelVAO) holder.vao).delete();
            }

            holder.vao = new LightmapModelVAO(data, lightWrapper.getLightmapData());
        }

        this.vaoDirty = false;
    }

    /**
     * Builds a picking VAO that includes animated and biome tinted blocks,
     * so selection silhouette covers the whole structure.
     */
    private void buildStructureVAOPicking()
    {
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();
        MatrixStack captureStack = new MatrixStack();
        FormRenderingContext captureContext;
        boolean useEntityLayers = false;
        ModelVAOData data;

        provider.setSubstitute(vc -> collector);

        captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        try
        {
            this.syncFancyGraphicsFromOptions();
        }
        catch (Throwable ignored)
        {}

        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = true; /* include animated and biome for picking. */

        try
        {
            Function<VertexConsumer, VertexConsumer> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());

            this.renderStructureCulledWorld(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, useEntityLayers, captureRecolor, false, false);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
        }

        /* BE meshes (chests/beds/…) are skipped during VAO capture and often have
         * BlockRenderType.INVISIBLE — without pick volumes, Alt-click cannot select
         * structures that are only block entities. */
        if (this.hasBlockEntityLayer && !this.blockEntitiesList.isEmpty())
        {
            try
            {
                provider.setSubstitute(vc -> collector);
                this.renderBlockEntitiesOnly(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, false);
            }
            catch (Throwable ignored)
            {}

            this.appendBlockEntityPickCubes(collector, captureContext);
        }

        provider.draw();
        provider.setSubstitute(null);

        data = collector.toData();

        if (this.lastFile != null)
        {
            VaoHolder holder = VAO_CACHE.computeIfAbsent(this.lastFile, k -> new VaoHolder());

            if (holder.picking instanceof ModelVAO)
            {
                ((ModelVAO) holder.picking).delete();
            }

            holder.picking = new ModelVAO(data);
        }

        this.vaoPickingDirty = false;
    }

    /**
     * Unit cubes at each block-entity cell so stencil picking always has a hit volume,
     * even when the BE renderer emits triangle strips the collector cannot triangulate.
     */
    private void appendBlockEntityPickCubes(StructureVAOCollector collector, FormRenderingContext context)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);

        for (BlockEntry entry : this.blockEntitiesList)
        {
            float x0 = entry.pos.getX() - info.pivotX;
            float y0 = entry.pos.getY() - info.pivotY;
            float z0 = entry.pos.getZ() - info.pivotZ;

            this.emitPickCube(collector, x0, y0, z0, x0 + 1F, y0 + 1F, z0 + 1F);
        }
    }

    private void emitPickCube(StructureVAOCollector collector, float x0, float y0, float z0, float x1, float y1, float z1)
    {
        /* -Z */
        this.emitPickQuad(collector, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0F, 0F, -1F);
        /* +Z */
        this.emitPickQuad(collector, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, 0F, 0F, 1F);
        /* -Y */
        this.emitPickQuad(collector, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, 0F, -1F, 0F);
        /* +Y */
        this.emitPickQuad(collector, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0F, 1F, 0F);
        /* -X */
        this.emitPickQuad(collector, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, -1F, 0F, 0F);
        /* +X */
        this.emitPickQuad(collector, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1F, 0F, 0F);
    }

    private void emitPickQuad(StructureVAOCollector collector, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz)
    {
        collector.vertex(x0, y0, z0).color(255, 255, 255, 255).texture(0F, 0F).overlay(0, 0).light(0, 0).normal(nx, ny, nz);
        collector.vertex(x1, y1, z1).color(255, 255, 255, 255).texture(1F, 0F).overlay(0, 0).light(0, 0).normal(nx, ny, nz);
        collector.vertex(x2, y2, z2).color(255, 255, 255, 255).texture(1F, 1F).overlay(0, 0).light(0, 0).normal(nx, ny, nz);
        collector.vertex(x3, y3, z3).color(255, 255, 255, 255).texture(0F, 1F).overlay(0, 0).light(0, 0).normal(nx, ny, nz);
    }

    private void readBlockEntityNbt(BlockEntity be, NbtCompound nbt)
    {
        if (be == null || nbt == null)
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        RegistryWrapper.WrapperLookup registries = client.world != null
            ? client.world.getRegistryManager()
            : BBSMod.getRegistryManager();

        if (registries != null)
        {
            be.read(NbtReadView.create(ErrorReporter.EMPTY, registries, nbt));
        }
    }

    private IModelVAO getStructureVao()
    {
        if (this.lastFile == null)
        {
            return null;
        }

        VaoHolder holder = VAO_CACHE.get(this.lastFile);

        return holder != null ? holder.vao : null;
    }

    private IModelVAO getStructureVaoPicking()
    {
        if (this.lastFile == null)
        {
            return null;
        }

        VaoHolder holder = VAO_CACHE.get(this.lastFile);

        return holder != null ? holder.picking : null;
    }

    private void clearCachedVao()
    {
        if (this.lastFile == null)
        {
            return;
        }

        VaoHolder holder = VAO_CACHE.remove(this.lastFile);

        if (holder != null)
        {
            if (holder.vao instanceof ModelVAO)
            {
                ((ModelVAO) holder.vao).delete();
            }

            if (holder.vao instanceof LightmapModelVAO)
            {
                ((LightmapModelVAO) holder.vao).delete();
            }

            if (holder.picking instanceof ModelVAO)
            {
                ((ModelVAO) holder.picking).delete();
            }
        }
    }

    private static class LightmapStructureVAOCollector implements VertexConsumer
    {
        private final StructureVAOCollector delegate;
        private int[] lightData = new int[8192];
        private int lightSize = 0;
        private final int[] quadLights = new int[4];
        private int quadIndex = 0;

        public LightmapStructureVAOCollector(StructureVAOCollector delegate)
        {
            this.delegate = delegate;
        }

        public int[] getLightmapData()
        {
            return Arrays.copyOf(this.lightData, this.lightSize);
        }

        @Override
        public VertexConsumer lineWidth(float width)
        {
            return this;
        }

        @Override
        public VertexConsumer color(int color)
        {
            this.delegate.color(color);
            return this;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z)
        {
            this.delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha)
        {
            this.delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v)
        {
            this.delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v)
        {
            this.delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v)
        {
            this.quadLights[this.quadIndex] = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
            this.delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z)
        {
            this.delegate.normal(x, y, z);

            this.quadIndex++;

            if (this.quadIndex == 4)
            {
                this.addLight(this.quadLights[0]);
                this.addLight(this.quadLights[1]);
                this.addLight(this.quadLights[2]);

                this.addLight(this.quadLights[0]);
                this.addLight(this.quadLights[2]);
                this.addLight(this.quadLights[3]);

                this.quadIndex = 0;
            }

            return this;
        }

        public void fixedColor(int red, int green, int blue, int alpha)
        {
        }

        public void unfixColor()
        {
        }

        private void addLight(int l)
        {
            if (this.lightSize >= this.lightData.length)
            {
                int[] n = new int[this.lightData.length * 2];
                System.arraycopy(this.lightData, 0, n, 0, this.lightSize);
                this.lightData = n;
            }

            this.lightData[this.lightSize++] = l;
        }
    }

    private void parseStructure(NbtCompound root)
    {
        /* Size */
        if (root.contains("size"))
        {
            int[] sz = root.getIntArray("size").orElse(new int[0]);

            if (sz.length >= 3)
            {
                this.size = new BlockPos(sz[0], sz[1], sz[2]);
            }
        }

        /* Palette -> state list */
        List<BlockState> paletteStates = new ArrayList<>();

        if (root.contains("palette"))
        {
            NbtList palette = root.getListOrEmpty("palette");

            for (int i = 0; i < palette.size(); i++)
            {
                NbtCompound entry = palette.getCompoundOrEmpty(i);
                BlockState state = this.readBlockState(entry);

                paletteStates.add(state);
            }
        }

        /* Blocks */
        if (root.contains("blocks"))
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            NbtList list = root.getListOrEmpty("blocks");

            this.syncFancyGraphicsFromOptions();

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

                    BlockRenderLayer baseLayer = BlockRenderLayers.getBlockLayer(state);

                    if (baseLayer == BlockRenderLayer.CUTOUT)
                    {
                        this.hasCutoutLayer = true;
                    }

                    if (this.isAnimatedTexture(state))
                    {
                        this.animatedBlocks.add(blockEntry);
                        this.hasAnimatedLayer = true;
                    }

                    if (this.isBiomeTinted(state))
                    {
                        this.biomeTintedBlocks.add(blockEntry);
                        this.hasBiomeTintedLayer = true;
                    }

                    if (state.getBlock() instanceof LeavesBlock)
                    {
                        this.hasLeavesLayer = true;
                    }

                    if (this.isTranslucentBlock(state))
                    {
                        this.translucentBlocks.add(blockEntry);
                        this.hasTranslucentLayer = true;
                    }

                    if (state.getBlock() instanceof BlockEntityProvider)
                    {
                        this.blockEntitiesList.add(blockEntry);
                        this.hasBlockEntityLayer = true;
                    }

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

        if (entry.contains("Properties"))
        {
            NbtCompound props = entry.getCompoundOrEmpty("Properties");

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
            return this;
        }

        @Override
        public VertexConsumer color(int color)
        {
            this.parent.color(color);
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
        public VertexConsumer vertex(org.joml.Matrix4fc matrix, float x, float y, float z)
        {
            Vector4f pos = new Vector4f(x, y, z, 1F);

            if (matrix != null)
            {
                matrix.transform(pos);
            }

            return this.vertex(pos.x, pos.y, pos.z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha)
        {
            this.parent.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer color(float red, float green, float blue, float alpha)
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
}
