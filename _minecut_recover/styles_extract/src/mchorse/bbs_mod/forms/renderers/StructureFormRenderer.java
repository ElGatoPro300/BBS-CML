package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.IModelVAO;
import mchorse.bbs_mod.cubic.render.vao.LightmapModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAOData;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.cubic.render.vao.StructureVAOCollector;
import mchorse.bbs_mod.film.FormRenderDepth;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.forms.renderers.utils.BlockEffectOverlayUniforms;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.StructureVirtualBlockRenderView;
import mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.class_1723;
import net.minecraft.class_1921;
import net.minecraft.class_1944;
import net.minecraft.class_2195;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_2372;
import net.minecraft.class_2397;
import net.minecraft.class_2457;
import net.minecraft.class_2464;
import net.minecraft.class_2487;
import net.minecraft.class_2499;
import net.minecraft.class_2505;
import net.minecraft.class_2507;
import net.minecraft.class_2513;
import net.minecraft.class_2520;
import net.minecraft.class_2541;
import net.minecraft.class_2553;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2769;
import net.minecraft.class_2960;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_3610;
import net.minecraft.class_3612;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_4696;
import net.minecraft.class_4722;
import net.minecraft.class_5365;
import net.minecraft.class_5819;
import net.minecraft.class_5944;
import net.minecraft.class_757;
import net.minecraft.class_765;
import net.minecraft.class_7923;
import net.minecraft.class_824;
import net.minecraft.class_827;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
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
    /* Glass / biome-tint baked once for large structures (avoids per-frame remesh). */
    /* Baked specials VAO: translucent (glass) only — grass/leaves bake into opaque with vertex colors. */
    public IModelVAO specials;
        /* Opaque mesh split into 16³ chunks for frustum culling. */
        public List<VaoChunk> chunks;
    }

    private static final class VaoChunk
    {
        final IModelVAO vao;
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;

        VaoChunk(IModelVAO vao, float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
        {
            this.vao = vao;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }

    /** Shared parsed NBT so many forms of the same file do not re-read/parse. */
    private static final class ParsedStructure
    {
        final List<BlockEntry> blocks;
        final List<BlockEntry> animatedBlocks;
        final List<BlockEntry> biomeTintedBlocks;
        final List<BlockEntry> translucentBlocks;
        final List<BlockEntry> blockEntitiesList;
        final class_2338 size;
        final class_2338 boundsMin;
        final class_2338 boundsMax;
        final boolean hasTranslucentLayer;
        final boolean hasCutoutLayer;
        final boolean hasAnimatedLayer;
        final boolean hasBiomeTintedLayer;
        final boolean hasLeavesLayer;
        final boolean hasBlockEntityLayer;

        ParsedStructure(
            List<BlockEntry> blocks,
            List<BlockEntry> animatedBlocks,
            List<BlockEntry> biomeTintedBlocks,
            List<BlockEntry> translucentBlocks,
            List<BlockEntry> blockEntitiesList,
            class_2338 size,
            class_2338 boundsMin,
            class_2338 boundsMax,
            boolean hasTranslucentLayer,
            boolean hasCutoutLayer,
            boolean hasAnimatedLayer,
            boolean hasBiomeTintedLayer,
            boolean hasLeavesLayer,
            boolean hasBlockEntityLayer)
        {
            this.blocks = List.copyOf(blocks);
            this.animatedBlocks = List.copyOf(animatedBlocks);
            this.biomeTintedBlocks = List.copyOf(biomeTintedBlocks);
            this.translucentBlocks = List.copyOf(translucentBlocks);
            this.blockEntitiesList = List.copyOf(blockEntitiesList);
            this.size = size;
            this.boundsMin = boundsMin;
            this.boundsMax = boundsMax;
            this.hasTranslucentLayer = hasTranslucentLayer;
            this.hasCutoutLayer = hasCutoutLayer;
            this.hasAnimatedLayer = hasAnimatedLayer;
            this.hasBiomeTintedLayer = hasBiomeTintedLayer;
            this.hasLeavesLayer = hasLeavesLayer;
            this.hasBlockEntityLayer = hasBlockEntityLayer;
        }
    }

    private static final Map<String, VaoHolder> VAO_CACHE = new HashMap<>();
    private static final Map<String, ParsedStructure> PARSED_CACHE = new HashMap<>();
    /* Bump when structure leaf Fancy capture/draw rules change. */
    private static final int LIGHTING_REVISION = 9;
    private static int cachedLightingRevision = -1;
    /* Bump when an on-disk .nbt is overwritten so open forms reload. */
    private static int STRUCTURE_DATA_REVISION = 0;
    /* Beyond this distance, only the baked VAO is drawn (skip expensive remesh/BE). */
    private static final double SPECIALS_MAX_DISTANCE_SQ = 72D * 72D;
    private static final double BLOCK_ENTITY_MAX_DISTANCE_SQ = 48D * 48D;
    /* Large structures: tighter LOD + baked specials + chunked opaque VAOs. */
    private static final int LARGE_STRUCTURE_BLOCKS = 2048;
    private static final int LARGE_STRUCTURE_SPECIALS = 512;
    private static final double LARGE_SPECIALS_MAX_DISTANCE_SQ = 40D * 40D;
    private static final double LARGE_BLOCK_ENTITY_MAX_DISTANCE_SQ = 24D * 24D;
    private static final double LARGE_ANIMATED_MAX_DISTANCE_SQ = 24D * 24D;
    private static final int CHUNK_SIZE = 16;

    private final List<BlockEntry> blocks = new ArrayList<>();
    private final List<BlockEntry> animatedBlocks = new ArrayList<>();
    private final List<BlockEntry> biomeTintedBlocks = new ArrayList<>();
    private final List<BlockEntry> translucentBlocks = new ArrayList<>();
    private final List<BlockEntry> blockEntitiesList = new ArrayList<>();
    private final Map<BlockEntry, class_2586> blockEntityCache = new IdentityHashMap<>();
    private final class_5819 blockRandom = class_5819.method_43047();

    private String lastFile = null;
    private int lastStructureDataRevision = -1;
    private String lastVaoCacheKey = null;

    private class_2338 size = class_2338.field_10980;
    private class_2338 boundsMin = null;
    private class_2338 boundsMax = null;

    private boolean vaoDirty = true;
    private boolean capturingVAO = false;
    private boolean vaoPickingDirty = true;
    private boolean capturingIncludeSpecialBlocks = false;
    private boolean capturingSpecialsOnly = false;
    private boolean capturingBakeBiome = false;
    private CaptureChunkBus captureChunkBus = null;
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
    private RenderInfo frameRenderInfo = null;
    private boolean frameRenderInfoValid = false;
    private boolean frameForceMaxSkyLight = false;

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
            StructureFormRenderer.deleteHolderVaos(holder);
        }

        VAO_CACHE.clear();
        PARSED_CACHE.clear();
        STRUCTURE_DATA_REVISION++;
    }

    private static void deleteHolderVaos(VaoHolder holder)
    {
        if (holder == null)
        {
            return;
        }

        StructureFormRenderer.deleteVao(holder.vao);
        StructureFormRenderer.deleteVao(holder.picking);
        StructureFormRenderer.deleteVao(holder.specials);

        if (holder.chunks != null)
        {
            for (VaoChunk chunk : holder.chunks)
            {
                StructureFormRenderer.deleteVao(chunk.vao);
            }

            holder.chunks.clear();
        }
    }

    private static void deleteVao(IModelVAO vao)
    {
        if (vao instanceof ModelVAO)
        {
            ((ModelVAO) vao).delete();
        }

        if (vao instanceof LightmapModelVAO)
        {
            ((LightmapModelVAO) vao).delete();
        }
    }

    /**
     * Drop parsed/VAO caches so open StructureForms reload after an on-disk overwrite.
     */
    public static void notifyStructureFileChanged()
    {
        StructureFormRenderer.clearAllCachedVaos();
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
        /* Ensure current UI batch is flushed before drawing 3D */
        context.batcher.getContext().method_51452();

        StructureFormRenderer.ensureLightingRevision();
        this.ensureLoaded();

        class_4587 matrices = context.batcher.getContext().method_51448();
        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.method_22903();
        MatrixStackUtils.multiply(matrices, uiMatrix);

        /* To draw 3D content inside UI, use standard depth test and restore it at the end to avoid affecting other panels. */
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

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
            wUnits = Math.max(1, this.boundsMax.method_10263() - this.boundsMin.method_10263() + 1);
            hUnits = Math.max(1, this.boundsMax.method_10264() - this.boundsMin.method_10264() + 1);
            dUnits = Math.max(1, this.boundsMax.method_10260() - this.boundsMin.method_10260() + 1);
        }
        else
        {
            wUnits = Math.max(1, this.size.method_10263());
            hUnits = Math.max(1, this.size.method_10264());
            dUnits = Math.max(1, this.size.method_10260());
        }

        maxUnits = Math.max(wUnits, Math.max(hUnits, dUnits));
        auto = maxUnits > 0 ? targetPixels / (baseScale * maxUnits) : 1F;

        /* Do not exceed user defined scale; only reduce if necessary */
        finalScale = this.form.uiScale.get() * Math.min(1F, auto);
        float structScaleUI = Math.max(Math.max(this.form.scaleX.get(), this.form.scaleY.get()), this.form.scaleZ.get());
        finalScale *= structScaleUI;
        matrices.method_22905(finalScale, finalScale, finalScale);

        MatrixStackUtils.invertUiNormalY(matrices);

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        StructureLightSettings slUi = this.form.structureLight.getRuntimeValue();
        boolean currentEmitLightUi = (slUi != null) ? slUi.enabled : this.form.emitLight.get();
        int currentLightIntensityUi = (slUi != null) ? slUi.intensity : this.form.lightIntensity.get();

        if (currentEmitLightUi != this.lastEmitLight || currentLightIntensityUi != this.lastLightIntensity)
        {
            this.vaoDirty = true;
            this.lastEmitLight = currentEmitLightUi;
            this.lastLightIntensity = currentLightIntensityUi;
        }

        Color storedFormColor = this.form.getFormColor();
        Color rawFormColor = storedFormColor.copyWithBlendIntensity();
        Color formColor = rawFormColor.copy();
        boolean colorTransformWanted = FormColorBlend.wantsColorTintOverlay(storedFormColor);
        Color tint = Color.white();

        if (FormColorBlend.shouldBakeFormColor(storedFormColor))
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
            FormColorBlend.blendFormGlowBrighten(tint, glowSettings, legacyGlow);
        }

        boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
        boolean deferColorTintToOverlay = colorTransformWanted && irisWorldPaintDeferral;
        Color resolvedPaint = FormColorBlend.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positivePaint = FormColorBlend.hasPositivePaint(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positiveGlow = glowIntensity > 0F;
        Function<class_4588, class_4588> mainRecolor = this.getMainConsumer(tint, resolvedPaint);

        if (!optimize)
        {
            /* BufferBuilder mode: better lighting, worse performance */
            boolean shaders = this.isShadersActive();
            class_4597 consumers = class_310.method_1551().method_22940().method_23000();

            try
            {
                FormRenderingContext uiContext = new FormRenderingContext()
                    .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);

                this.renderStructureCulledWorld(uiContext, matrices, consumers, class_765.field_32769, class_4608.field_21444, shaders, mainRecolor, false, false);

                if (consumers instanceof class_4597.class_4598 immediate)
                {
                    immediate.method_22993();
                }

                if (positivePaint)
                {
                    EffectTransform paintTransform = this.form.paintSettings.get().transform;

                    this.renderStructurePaintOverlay(uiContext, matrices, resolvedPaint, tint.a, class_4608.field_21444, false, shaders, paintTransform, glowSettings, legacyGlow, glowIntensity);
                }

                if (positiveGlow)
                {
                    this.renderStructureGlowOverlay(uiContext, matrices, glowSettings, legacyGlow, glowIntensity, tint.a, class_4608.field_21444, false, shaders);
                }

                if (colorTransformWanted)
                {
                    this.renderStructureColorTintOverlay(uiContext, matrices, formColor, tint.a, class_4608.field_21444, false, shaders, false);
                }
            }
            catch (Throwable ignored)
            {}
        }
        else
        {
            IModelVAO vao = this.getStructureVao();
            boolean hasChunked = this.hasChunkedStructureVao();

            if (this.vaoDirty || (vao == null && !hasChunked))
            {
                this.buildStructureVAO();
                vao = this.getStructureVao();
                hasChunked = this.hasChunkedStructureVao();
            }

            if (vao != null || hasChunked)
            {
                class_757 gameRenderer = class_310.method_1551().field_1773;
                class_5944 shader = BBSShaders.getModel();

                gameRenderer.method_22974().method_3316();
                gameRenderer.method_22975().method_23209();

                /* Revert to own model shader in vanilla to ensure VAO compatibility */
                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, class_1723.field_21668);

                boolean needBlendUI = tint.a < 0.999F || this.hasTranslucentLayer;

                if (needBlendUI)
                {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                }
                else
                {
                    RenderSystem.disableBlend();
                }

                RenderSystem.enableCull();

                this.prepareVaoPaintForMainPass(resolvedPaint);
                this.prepareVaoGlowForMainPass(glowSettings, legacyGlow, glowIntensity);
                this.prepareVaoColorGradeForMainPass(storedFormColor);

                if (FormColorBlend.wantsColorTransformMask(storedFormColor))
                {
                    Color tintForMask = storedFormColor.hasColorAdjustments()
                        ? storedFormColor.copyWithBlendIntensityOnly()
                        : formColor;

                    this.form.applyFormOpacity(tintForMask);
                    this.prepareVaoColorTintForMainPass(matrices, tintForMask, true);
                }

                try
                {
                    FormRenderingContext uiVaoContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);

                    uiVaoContext.ui = true;
                    this.drawOpaqueStructureVaos(shader, uiVaoContext, tint, class_765.field_32769, class_4608.field_21444);

                    IModelVAO specialsVao = this.getStructureSpecialsVao();

                    if (specialsVao != null)
                    {
                        this.drawBakedSpecialsVao(shader, uiVaoContext, specialsVao, tint, class_765.field_32769, class_4608.field_21444);
                    }
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
                        class_4597 beConsumers = class_310.method_1551().method_22940().method_23000();
                        FormRenderingContext beContext = new FormRenderingContext()
                            .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);

                        this.renderBlockEntitiesOnly(beContext, matrices, beConsumers, class_765.field_32769, class_4608.field_21444);

                        if (beConsumers instanceof class_4597.class_4598 immediate)
                        {
                            immediate.method_22993();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (this.hasBiomeTintedLayer && !this.isLargeStructure())
                {
                    try
                    {
                        boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                        class_4597 consumersTint = class_310.method_1551().method_22940().method_23000();
                        FormRenderingContext tintContext = new FormRenderingContext()
                            .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);

                        this.renderBiomeTintedBlocksVanilla(tintContext, matrices, consumersTint, class_765.field_32769, class_4608.field_21444, mainRecolor);

                        if (consumersTint instanceof class_4597.class_4598 immediate)
                        {
                            immediate.method_22993();
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
                        class_4597 consumersAnim = class_310.method_1551().method_22940().method_23000();
                        FormRenderingContext animContext = new FormRenderingContext()
                            .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);

                        this.renderAnimatedBlocksVanilla(animContext, matrices, consumersAnim, class_765.field_32769, class_4608.field_21444, mainRecolor);

                        if (consumersAnim instanceof class_4597.class_4598 immediate)
                        {
                            immediate.method_22993();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (this.hasTranslucentLayer && this.getStructureSpecialsVao() == null)
                {
                    try
                    {
                        class_4597 consumersGlass = class_310.method_1551().method_22940().method_23000();
                        FormRenderingContext glassContext = new FormRenderingContext()
                            .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);

                        this.renderTranslucentBlocksVanilla(glassContext, matrices, consumersGlass, class_765.field_32769, class_4608.field_21444, mainRecolor);

                        if (consumersGlass instanceof class_4597.class_4598 immediate)
                        {
                            immediate.method_22993();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                gameRenderer.method_22974().method_3315();
                gameRenderer.method_22975().method_23213();
                RenderSystem.disableBlend();

                if (positivePaint)
                {
                    FormRenderingContext uiContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);
                    EffectTransform paintTransform = this.form.paintSettings.get().transform;

                    this.renderStructurePaintOverlay(uiContext, matrices, resolvedPaint, tint.a, class_4608.field_21444, true, this.isShadersActive(), paintTransform, glowSettings, legacyGlow, glowIntensity);
                }

                if (positiveGlow)
                {
                    FormRenderingContext uiContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);

                    this.renderStructureGlowOverlay(uiContext, matrices, glowSettings, legacyGlow, glowIntensity, tint.a, class_4608.field_21444, true, this.isShadersActive());
                }

                if (colorTransformWanted && FormColorBlend.wantsColorTransformMask(storedFormColor))
                {
                    FormRenderingContext uiContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, class_765.field_32769, class_4608.field_21444, 0F);
                    Color overlayTint = Color.white();

                    overlayTint.a = tint.a;

                    if (this.hasChunkedStructureVao())
                    {
                        this.renderStructureChunkedVaoColorTintOverlay(uiContext, overlayTint, formColor, class_765.field_32767, class_4608.field_21444);
                    }
                    else
                    {
                        IModelVAO colorVao = this.getStructureVao();

                        if (colorVao != null)
                        {
                            this.renderStructureVaoColorTintOverlay(colorVao, matrices, overlayTint, formColor, class_765.field_32767, class_4608.field_21444);
                        }
                    }
                }
            }
        }

        class_308.method_24210();

        matrices.method_22909();

        /* Restore depth state expected by UI system */
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        StructureFormRenderer.ensureLightingRevision();
        this.ensureLoaded();
        this.frameRenderInfoValid = false;

        context.stack.method_22903();

        try
        {
            /* Apply structure scale */
            context.stack.method_22905(this.form.scaleX.get(), this.form.scaleY.get(), this.form.scaleZ.get());


            boolean optimize = true;
            boolean picking = context.isPicking();
            double cameraDistSq = this.getCameraDistanceSq(context);
            boolean large = this.isLargeStructure();
            double specialsMaxSq = large ? LARGE_SPECIALS_MAX_DISTANCE_SQ : SPECIALS_MAX_DISTANCE_SQ;
            double beMaxSq = large ? LARGE_BLOCK_ENTITY_MAX_DISTANCE_SQ : BLOCK_ENTITY_MAX_DISTANCE_SQ;
            IModelVAO specialsVao = this.getStructureSpecialsVao();
            boolean bakedSpecials = specialsVao != null;
            boolean drawSpecials = picking || context.ui || cameraDistSq <= specialsMaxSq;
            boolean drawAnimated = picking || context.ui || cameraDistSq <= (large ? LARGE_ANIMATED_MAX_DISTANCE_SQ : specialsMaxSq);
            boolean drawBlockEntities = picking || context.ui || cameraDistSq <= beMaxSq;
            /* Large builds: glass uses baked specials VAO; grass/leaves bake into opaque VAO. */
            boolean remeshBiome = drawSpecials && this.hasBiomeTintedLayer && !large;
            boolean remeshTranslucent = drawSpecials && (!bakedSpecials || picking || context.ui);
            boolean remeshAnimated = drawAnimated && this.hasAnimatedLayer;

            IModelVAO vao = this.getStructureVao();
            boolean hasChunkedVao = this.hasChunkedStructureVao();

            StructureLightSettings sl = this.form.structureLight.getRuntimeValue();
            boolean currentEmitLight = (sl != null) ? sl.enabled : this.form.emitLight.get();
            int currentLightIntensity = (sl != null) ? sl.intensity : this.form.lightIntensity.get();

            if (currentEmitLight != this.lastEmitLight || currentLightIntensity != this.lastLightIntensity)
            {
                this.vaoDirty = true;
                this.lastEmitLight = currentEmitLight;
                this.lastLightIntensity = currentLightIntensity;
            }

            if (optimize && (this.vaoDirty || (vao == null && !hasChunkedVao)))
            {
                this.buildStructureVAO();
                vao = this.getStructureVao();
                hasChunkedVao = this.hasChunkedStructureVao();
                specialsVao = this.getStructureSpecialsVao();
                bakedSpecials = specialsVao != null;
                remeshBiome = drawSpecials && this.hasBiomeTintedLayer && !large;
                remeshTranslucent = drawSpecials && (!bakedSpecials || picking || context.ui);
            }

            Color storedFormColor3D = this.form.getFormColor();
            Color rawFormColor3D = storedFormColor3D.copyWithBlendIntensity();
            Color formColor3D = rawFormColor3D.copy();
            boolean colorTransformWanted = FormColorBlend.wantsColorTintOverlay(storedFormColor3D);
            Color mainTint3D = new Color().set(context.color);

            if (FormColorBlend.shouldBakeFormColor(storedFormColor3D))
            {
                mainTint3D.mul(rawFormColor3D);
            }

            this.form.applyFormOpacity(mainTint3D);
            this.form.applyFormOpacity(formColor3D);

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            FormColorBlend.applyShadowPassColorFix(mainTint3D, storedFormColor3D, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);
            this.applyBlockEntityOnlyShaderShadow(mainTint3D, shadowPass);

            if (mainTint3D.a <= 0.001F && !shadowPass && !picking)
            {
                return;
            }

            /* Soft opacity must remesh every block the same way. Opaque VAO + separate grass
             * remesh made solids pop away while biome blocks faded. */
            boolean softOpacityActive = !shadowPass && !picking && mainTint3D.a < ShaderOpacityPatch.LIVE_DEPTH_WRITE_ALPHA;

            if (softOpacityActive)
            {
                remeshBiome = false;
                remeshTranslucent = false;
                remeshAnimated = false;
            }

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(mainTint3D, glowSettings, legacyGlow);
        }

        boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
        boolean formHasGrade = storedFormColor3D != null && storedFormColor3D.hasColorAdjustments();
        boolean spatialColorMask = FormColorBlend.wantsColorTransformMask(storedFormColor3D);
        /* No-shader: FormColorGrade on BBS VAO. Iris: live pack draw + ColorGradeOverlay (VAO only). */
        boolean uploadGradeToVao = formHasGrade && !irisWorldPaintDeferral && !shadowPass && !picking;
        boolean useColorGradeOverlay = formHasGrade && irisWorldPaintDeferral && !shadowPass && !picking;
        boolean forceBbsForColorEffects = (uploadGradeToVao || (colorTransformWanted && !irisWorldPaintDeferral)) && !shadowPass && !picking;
        boolean deferColorTintToOverlay = spatialColorMask && irisWorldPaintDeferral && !shadowPass && !picking;
        boolean applyColorTint = false;
        boolean applyVaoColorOverlay = deferColorTintToOverlay;
        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        Color resolvedPaint = FormColorBlend.resolvePaintColor(paintSettings, legacyPaint);
        boolean positivePaint = !picking && !shadowPass && FormColorBlend.hasPositivePaint(paintSettings, legacyPaint);
        boolean positiveGlow = !picking && !shadowPass && glowIntensity > 0F;
        Function<class_4588, class_4588> mainRecolor = this.getMainConsumer(mainTint3D, resolvedPaint);
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

                Color tintPicking = this.form.getFormColor();
                int light = 0;
                class_757 gameRenderer = class_310.method_1551().field_1773;

                gameRenderer.method_22974().method_3316();
                gameRenderer.method_22975().method_23209();

                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, class_1723.field_21668);

                ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), pickingVao, context.stack, mainTint3D.r, mainTint3D.g, mainTint3D.b, mainTint3D.a, light, context.overlay);

                gameRenderer.method_22974().method_3315();
                gameRenderer.method_22975().method_23213();

                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
            }
            else
            {
                /* BufferBuilder mode: use vanilla/culling pipeline with better lighting */
                int light = context.light;
                class_4597 consumers = class_310.method_1551().method_22940().method_23000();
                class_757 gameRenderer = class_310.method_1551().field_1773;

                /* Align state handling with VAO path to avoid state leaks affecting the first model rendered after. */
                gameRenderer.method_22974().method_3316();
                gameRenderer.method_22975().method_23209();
                /* Ensure block atlas is active when starting the pass */
                RenderSystem.setShaderTexture(0, class_1723.field_21668);

                try
                {
                    this.renderStructureCulledWorld(context, context.stack, consumers, light, context.overlay, shaders, mainRecolor, false, false);

                    if (consumers instanceof class_4597.class_4598 immediate)
                    {
                        immediate.method_22993();
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
                gameRenderer.method_22974().method_3315();
                gameRenderer.method_22975().method_23213();

                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
            }
        }
        else if (vao != null || hasChunkedVao)
        {
            int light = context.isPicking() ? 0 : context.light;
            class_757 gameRenderer = class_310.method_1551().field_1773;

            gameRenderer.method_22974().method_3316();
            gameRenderer.method_22975().method_23209();

            if (context.isPicking())
            {
                IModelVAO pickingVao = this.getStructureVaoPicking();

                if (pickingVao == null || this.vaoPickingDirty)
                {
                    this.buildStructureVAOPicking();
                    pickingVao = this.getStructureVaoPicking();
                }

                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, class_1723.field_21668);

                ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), pickingVao, context.stack, mainTint3D.r, mainTint3D.g, mainTint3D.b, mainTint3D.a, light, context.overlay);
            }
            else
            {
                float opacityAlpha = mainTint3D.a;
                /* Orbit/UI must stay live — post-deferred never redraws editor previews. */
                boolean softOpacityLive = context.ui;
                boolean delaySoftOpacity = softOpacityActive && !softOpacityLive
                    && ShaderOpacityPatch.shouldDelayUntilPostDeferred(opacityAlpha, false);

                if (delaySoftOpacity || softOpacityActive)
                {
                    /* One translucent remesh for every block (solids + grass + glass). Keeps
                     * opacity uniform — VAO solids previously vanished while remesh grass faded. */
                    boolean irisCamera = BBSRendering.isIrisWorldModelPass();
                    boolean useEntityLayers = shaders || irisCamera;
                    int lightSnapshot = light;
                    int overlaySnapshot = context.overlay;
                    Color tintSnapshot = mainTint3D.copy();
                    Color paintSnapshot = resolvedPaint.copy();
                    boolean depthWrite = ShaderOpacityPatch.shouldWriteDepthForOpacity(opacityAlpha);
                    Runnable softDraw = () ->
                    {
                        Function<class_4588, class_4588> softRecolor = this.getMainConsumer(tintSnapshot, paintSnapshot);
                        class_4597.class_4598 consumers = class_310.method_1551().method_22940().method_23000();

                        RenderSystem.setShaderTexture(0, class_1723.field_21668);
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.depthMask(depthWrite);

                        try
                        {
                            this.renderStructureCulledWorld(context, context.stack, consumers, lightSnapshot, overlaySnapshot, useEntityLayers, softRecolor, true, false);
                            consumers.method_22993();
                        }
                        finally
                        {
                            RecolorVertexConsumer.newColor = null;
                            RenderSystem.depthMask(true);
                        }
                    };

                    if (delaySoftOpacity)
                    {
                        Matrix4f positionMatrix = irisCamera
                            ? new Matrix4f(context.stack.method_23760().method_23761())
                            : ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.method_23760().method_23761()));
                        Matrix3f normalMatrix = new Matrix3f(context.stack.method_23760().method_23762());
                        IEntity entitySnapshot = context.entity;
                        FormRenderType typeSnapshot = context.type;
                        float transitionSnapshot = context.transition;
                        FormRenderDepth.Frame depthFrameSnapshot = context.renderDepthFrame;
                        double sortDepth = FormRenderDepth.resolveSortDepth(this.form, depthFrameSnapshot);
                        double distanceSq = this.getCameraDistanceSq(context);
                        boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(opacityAlpha);
                        Runnable deferredDraw = () ->
                        {
                            class_4587 overlayStack = new class_4587();

                            overlayStack.method_23760().method_23761().set(positionMatrix);
                            overlayStack.method_23760().method_23762().set(normalMatrix);

                            FormRenderingContext deferredContext = new FormRenderingContext()
                                .set(typeSnapshot, entitySnapshot, overlayStack, lightSnapshot, overlaySnapshot, transitionSnapshot);

                            deferredContext.renderDepthFrame = depthFrameSnapshot;

                            Function<class_4588, class_4588> softRecolor = this.getMainConsumer(tintSnapshot, paintSnapshot);
                            class_4597.class_4598 consumers = class_310.method_1551().method_22940().method_23000();

                            RenderSystem.setShaderTexture(0, class_1723.field_21668);
                            RenderSystem.enableBlend();
                            RenderSystem.defaultBlendFunc();
                            RenderSystem.depthMask(depthWrite);

                            try
                            {
                                this.renderStructureCulledWorld(deferredContext, overlayStack, consumers, lightSnapshot, overlaySnapshot, useEntityLayers, softRecolor, true, false);
                                consumers.method_22993();
                            }
                            finally
                            {
                                RecolorVertexConsumer.newColor = null;
                                RenderSystem.depthMask(true);
                            }
                        };

                        if (irisCamera)
                        {
                            ShaderOpacityPatch.submitPostDeferredForm(sortDepth, distanceSq, depthWrite, afterFluids, deferredDraw);
                        }
                        else
                        {
                            ShaderOpacityPatch.submitPostDeferredBbsForm(sortDepth, distanceSq, depthWrite, afterFluids, deferredDraw);
                        }
                    }
                    else
                    {
                        softDraw.run();
                    }
                }
                else
                {
                /* Fully opaque live VAO — Iris entity lighting when available. */
                boolean irisEntity = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld() && !forceBbsForColorEffects;
                class_5944 shader = irisEntity
                    ? class_757.method_34507()
                    : BBSShaders.getModel();

                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, class_1723.field_21668);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                this.prepareVaoPaintForMainPass(resolvedPaint);
                this.prepareVaoGlowForMainPass(glowSettings, legacyGlow, glowIntensity);

                if (uploadGradeToVao)
                {
                    this.prepareVaoColorGradeForMainPass(storedFormColor3D);
                }

                if (forceBbsForColorEffects && spatialColorMask)
                {
                    Color tintForMask = formHasGrade
                        ? storedFormColor3D.copyWithBlendIntensityOnly()
                        : formColor3D;

                    this.form.applyFormOpacity(tintForMask);
                    this.prepareVaoColorTintForMainPass(context.stack, tintForMask, true);
                }

                try
                {
                    this.drawOpaqueStructureVaos(shader, context, mainTint3D, light, context.overlay);

                    if (bakedSpecials && !picking)
                    {
                        this.drawBakedSpecialsVao(shader, context, specialsVao, mainTint3D, light, context.overlay);
                    }
                }
                finally
                {
                    this.clearVaoColorTint();
                    this.clearVaoPaint();
                    this.clearVaoGlow();
                }
                } /* end opaque live VAO */

                if (useColorGradeOverlay && !softOpacityActive)
                {
                    this.submitDeferredStructureColorGradeOverlay(context, storedFormColor3D, mainTint3D.a, context.overlay);
                }

                if (drawBlockEntities && this.hasBlockEntityLayer)
                {
                    try
                    {
                        class_4597 beConsumers = class_310.method_1551().method_22940().method_23000();
                        /* Under Iris, ColorModulator on BEs breaks pack shading and still ignores tint.
                         * Draw untinted here; tinted redraw runs after composite. */
                        boolean beTint = !irisWorldPaintDeferral;

                        this.renderBlockEntitiesOnly(context, context.stack, beConsumers, light, context.overlay, beTint);

                        if (beConsumers instanceof class_4597.class_4598 immediate)
                        {
                            immediate.method_22993();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (remeshBiome && this.hasBiomeTintedLayer)
                {
                    try
                    {
                        class_4597.class_4598 tintConsumers = class_310.method_1551().method_22940().method_23000();

                        this.renderBiomeTintedBlocksVanilla(context, context.stack, tintConsumers, light, context.overlay, mainRecolor);
                        tintConsumers.method_22993();
                    }
                    catch (Throwable ignored)
                    {}
                }

                /* Iris live entity buffers make structure leaves look Fast with the opacity patch.
                 * After composite, redraw leaves with Fancy cutout (same as no-shader).
                 * Skip when specials are already baked (large structure path). */
                if (remeshBiome && this.hasLeavesLayer && irisWorldPaintDeferral && !shadowPass && !picking)
                {
                    this.submitDeferredStructureLeavesFancy(context, light, context.overlay);
                }

                if (remeshAnimated)
                {
                    try
                    {
                        class_4597.class_4598 animConsumers = class_310.method_1551().method_22940().method_23000();

                        this.renderAnimatedBlocksVanilla(context, context.stack, animConsumers, light, context.overlay, mainRecolor);
                        animConsumers.method_22993();
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (remeshTranslucent && this.hasTranslucentLayer)
                {
                    try
                    {
                        class_4597.class_4598 glassConsumers = class_310.method_1551().method_22940().method_23000();

                        this.renderTranslucentBlocksVanilla(context, context.stack, glassConsumers, light, context.overlay, mainRecolor);
                        glassConsumers.method_22993();
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (drawSpecials && positivePaint)
                {
                    EffectTransform paintTransform = paintSettings.transform;

                    this.submitDeferredStructurePaintOverlay(context, resolvedPaint, mainTint3D.a, context.overlay, true, shaders, paintTransform, glowSettings, legacyGlow, glowIntensity);
                }

                if (drawSpecials && positiveGlow)
                {
                    this.renderStructureGlowOverlay(context, context.stack, glowSettings, legacyGlow, glowIntensity, mainTint3D.a, context.overlay, true, shaders);
                }

                if (drawSpecials && applyVaoColorOverlay)
                {
                    if (deferColorTintToOverlay)
                    {
                        this.submitDeferredStructureVaoColorTintOverlay(context, formColor3D, mainTint3D.a, context.overlay);
                    }
                    else
                    {
                        Color overlayTint = Color.white();

                        overlayTint.a = mainTint3D.a;

                        if (this.hasChunkedStructureVao())
                        {
                            this.renderStructureChunkedVaoColorTintOverlay(context, overlayTint, formColor3D, class_765.field_32767, context.overlay);
                        }
                        else
                        {
                            IModelVAO colorVao = this.getStructureVao();

                            if (colorVao != null)
                            {
                                this.renderStructureVaoColorTintOverlay(colorVao, context.stack, overlayTint, formColor3D, class_765.field_32767, context.overlay);
                            }
                        }
                    }
                }

                /* Iris gbuffer ignores ColorModulator / vertex tint on chests/beds. Redraw BEs
                 * after composite so blend / paint / grade actually show. */
                if (drawBlockEntities && irisWorldPaintDeferral && this.hasBlockEntityLayer && this.needsDeferredBlockEntityTint(positivePaint, colorTransformWanted, storedFormColor3D))
                {
                    this.submitDeferredStructureBlockEntityTint(context, context.overlay);
                }
            }

            gameRenderer.method_22974().method_3315();
            gameRenderer.method_22975().method_23213();

            /* Restore state if VAO was used */
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }

            CustomVertexConsumerProvider.clearRunnables();
        }
        finally
        {
            this.frameRenderInfoValid = false;
            context.stack.method_22909();
        }
    }

    private double getCameraDistanceSq(FormRenderingContext context)
    {
        if (context == null || context.ui || context.entity == null)
        {
            return 0D;
        }

        double ex = context.entity.getX();
        double ey = context.entity.getY();
        double ez = context.entity.getZ();
        double cx = context.camera.position.x;
        double cy = context.camera.position.y;
        double cz = context.camera.position.z;

        if (this.boundsMin == null || this.boundsMax == null)
        {
            double dx = ex - cx;
            double dy = ey - cy;
            double dz = ez - cz;

            return dx * dx + dy * dy + dz * dz;
        }

        /* Distance to nearest point on the structure AABB (not only the origin),
         * so huge builds do not stay on "close remesh" LOD forever. */
        float[] local = this.getLocalBounds();
        float sx = this.form.scaleX.get();
        float sy = this.form.scaleY.get();
        float sz = this.form.scaleZ.get();
        double minX = ex + local[0] * sx;
        double minY = ey + local[1] * sy;
        double minZ = ez + local[2] * sz;
        double maxX = ex + local[3] * sx;
        double maxY = ey + local[4] * sy;
        double maxZ = ez + local[5] * sz;
        double nx = mchorse.bbs_mod.utils.MathUtils.clamp(cx, minX, maxX);
        double ny = mchorse.bbs_mod.utils.MathUtils.clamp(cy, minY, maxY);
        double nz = mchorse.bbs_mod.utils.MathUtils.clamp(cz, minZ, maxZ);
        double dx = nx - cx;
        double dy = ny - cy;
        double dz = nz - cz;

        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isLargeStructure()
    {
        int specials = this.translucentBlocks.size() + this.biomeTintedBlocks.size() + this.animatedBlocks.size();

        return this.blocks.size() >= LARGE_STRUCTURE_BLOCKS || specials >= LARGE_STRUCTURE_SPECIALS;
    }

    /**
     * Local-space AABB after pivot (same space as VAO vertices / chunk bounds).
     * @return {minX,minY,minZ,maxX,maxY,maxZ}
     */
    private float[] getLocalBounds()
    {
        float[] out = new float[6];
        float[] pivot = this.getStructurePivot();

        if (this.boundsMin == null || this.boundsMax == null)
        {
            out[3] = out[4] = out[5] = 1F;

            return out;
        }

        out[0] = this.boundsMin.method_10263() - pivot[0];
        out[1] = this.boundsMin.method_10264() - pivot[1];
        out[2] = this.boundsMin.method_10260() - pivot[2];
        out[3] = this.boundsMax.method_10263() + 1 - pivot[0];
        out[4] = this.boundsMax.method_10264() + 1 - pivot[1];
        out[5] = this.boundsMax.method_10260() + 1 - pivot[2];

        return out;
    }

    private float[] getStructurePivot()
    {
        float[] pivot = new float[3];

        if (this.boundsMin == null || this.boundsMax == null)
        {
            return pivot;
        }

        float cx = (this.boundsMin.method_10263() + this.boundsMax.method_10263()) / 2F;
        float cy = this.boundsMin.method_10264();
        float cz = (this.boundsMin.method_10260() + this.boundsMax.method_10260()) / 2F;
        int widthX = this.boundsMax.method_10263() - this.boundsMin.method_10263() + 1;
        int widthZ = this.boundsMax.method_10260() - this.boundsMin.method_10260() + 1;
        float parityX = (widthX % 2 == 1) ? -0.5F : 0F;
        float parityZ = (widthZ % 2 == 1) ? -0.5F : 0F;

        pivot[0] = cx - parityX;
        pivot[1] = cy;
        pivot[2] = cz - parityZ;

        return pivot;
    }

    private static class RenderInfo
    {
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        public VirtualBlockRenderView view;
        public class_2338 anchor;
    }

    private RenderInfo calculateRenderInfo(FormRenderingContext context, boolean forceMaxSkyLight)
    {
        if (this.frameRenderInfoValid && this.frameRenderInfo != null && this.frameForceMaxSkyLight == forceMaxSkyLight)
        {
            return this.frameRenderInfo;
        }

        RenderInfo info = new RenderInfo();
        float cx;
        float cy;
        float cz;
        float parityXAuto = 0F;
        float parityZAuto = 0F;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            cx = (this.boundsMin.method_10263() + this.boundsMax.method_10263()) / 2F;
            cz = (this.boundsMin.method_10260() + this.boundsMax.method_10260()) / 2F;
            /* Keep it on the ground: use the minimum Y as base */
            cy = this.boundsMin.method_10264();
        }
        else
        {
            /* Fallback if no bounds calculated */
            cx = this.size.method_10263() / 2F;
            cy = 0F;
            cz = this.size.method_10260() / 2F;
        }

        if (this.boundsMin != null && this.boundsMax != null)
        {
            int widthX = this.boundsMax.method_10263() - this.boundsMin.method_10263() + 1;
            int widthZ = this.boundsMax.method_10260() - this.boundsMin.method_10260() + 1;

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
            class_310 mc = class_310.method_1551();

            info.anchor = (mc.field_1724 != null) ? mc.field_1724.method_24515() : class_2338.field_10980;
        }
        else
        {
            info.anchor = new class_2338(
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

        this.frameRenderInfo = info;
        this.frameForceMaxSkyLight = forceMaxSkyLight;
        this.frameRenderInfoValid = true;

        return info;
    }

    /**
     * Leaves (and similar) use solid vs cutout_mipped based on {@link class_4696}'
     * fancy flag. Keep that flag in sync with the client Graphics option whenever we
     * pick block layers — otherwise structure trees stay opaque (Fast) even when Fancy.
     */
    private void syncFancyGraphicsFromOptions()
    {
        try
        {
            class_4696.method_23682(this.isFancyGraphicsEnabled());
        }
        catch (Throwable ignored)
        {}
    }

    private boolean isFancyGraphicsEnabled()
    {
        try
        {
            return class_310.method_1551().field_1690.method_42534().method_41753() != class_5365.field_25427;
        }
        catch (Throwable ignored)
        {
            return true;
        }
    }

    /**
     * Structure morphs always draw leaves as Fancy cutout (see-through) so they match
     * world trees. Terrain {@link class_4696#method_23679} returns Solid when the
     * Fancy flag is false / desynced — that is the opaque “Fast graphics” look.
     */
    private class_1921 resolveStructureBlockLayer(class_2680 state, boolean useEntityLayers)
    {
        if (state.method_26204() instanceof class_2397)
        {
            try
            {
                class_4696.method_23682(true);
            }
            catch (Throwable ignored)
            {}

            /* Always terrain cutout_mipped — Iris maps this to gbuffers_terrain_cutout
             * (alpha discard). entity_cutout / gbuffers_entities often looks Fast under packs. */
            return class_1921.method_23579();
        }

        return useEntityLayers
            ? class_4696.method_23683(state, false)
            : class_4696.method_23679(state);
    }

    /**
     * Fancy leaves: same layer as world trees ({@code cutout_mipped}). Do not use
     * {@code renderBlockAsEntity} under Iris — that picks entity_cutout and packs treat it opaque.
     */
    private void renderStructureLeaves(class_2680 state, class_2338 pos, net.minecraft.class_1920 view, class_4587 stack, class_4597 consumers, Function<class_4588, class_4588> recolor)
    {
        try
        {
            class_4696.method_23682(true);
        }
        catch (Throwable ignored)
        {}

        class_1921 layer = class_1921.method_23579();
        class_4588 vc = consumers.getBuffer(layer);

        if (recolor != null)
        {
            vc = recolor.apply(vc);
        }

        /* cull=false: leaf-vs-leaf faces stay visible like Fancy chunk meshing. */
        class_310.method_1551().method_1541().method_3355(state, pos, view, stack, vc, false, this.blockRandom);
    }

    /**
     * Render with culling using virtual BlockRenderView to leverage vanilla logic.
     * Keeps the same centering and parity as renderStructure.
     */
    private void renderStructureCulledWorld(FormRenderingContext context, class_4587 stack, class_4597 consumers, int light, int overlay, boolean useEntityLayers, Function<class_4588, class_4588> recolor, boolean skipBlockEntities, boolean skipSpecialBlocks)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);
        float globalAlpha;

        this.syncFancyGraphicsFromOptions();

        for (BlockEntry entry : this.blocks)
        {
            class_1921 layer;
            class_4588 vc;
            class_2248 block;

            stack.method_22903();
            stack.method_46416(entry.pos.method_10263() - info.pivotX, entry.pos.method_10264() - info.pivotY, entry.pos.method_10260() - info.pivotZ);

            if (this.captureChunkBus != null)
            {
                this.captureChunkBus.setBlock(entry.pos);
            }

            /* During normal VAO capture, skip animated / glass. Biome grass/leaves bake into the
             * opaque VAO with per-vertex colors when capturingBakeBiome (large structures). */
            if (this.capturingVAO && !this.capturingIncludeSpecialBlocks
                && (this.isAnimatedTexture(entry.state) || this.isTranslucentBlock(entry.state)
                    || (this.isBiomeTinted(entry.state) && !this.capturingBakeBiome)))
            {
                stack.method_22909();
                continue;
            }

            if (this.capturingSpecialsOnly)
            {
                /* Specials bake: translucent glass only. Biome tint is baked into opaque for large. */
                if (!this.isTranslucentBlock(entry.state) || this.isAnimatedTexture(entry.state) || this.isBiomeTinted(entry.state))
                {
                    stack.method_22909();
                    continue;
                }
            }

            if (skipSpecialBlocks && (this.isAnimatedTexture(entry.state) || this.isBiomeTinted(entry.state) || this.isTranslucentBlock(entry.state)))
            {
                stack.method_22909();
                continue;
            }

            /* Use entity layer for blocks when rendering with the entity vertex provider
             * of WorldRenderer. Leaves always resolve to Fancy cutout (see resolveStructureBlockLayer). */
            layer = this.resolveStructureBlockLayer(entry.state, useEntityLayers);

            /* If there is global opacity (<1), force translucent layer for all blocks */
            /* of the structure, so alpha is applied even to solid/cutout geometry. */
            /* In shaders mode (useEntityLayers=true) use the translucent entity variant WITH CULL */
            /* to preserve culling and avoid double faces with packs. */
            globalAlpha = this.form.getFormOpacity();

            if (globalAlpha < 0.999F)
            {
                layer = useEntityLayers
                    ? class_4722.method_24076()
                    : class_1921.method_23583();
            }

            vc = consumers.getBuffer(layer);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            if (this.form.renderFluid.get() && !entry.state.method_26227().method_15769())
            {
                boolean shaders = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                class_1921 fluidLayer = shaders
                    ? class_4696.method_23683(entry.state, false)
                    : class_4696.method_23680(entry.state.method_26227());
                class_4588 fluidVc = consumers.getBuffer(fluidLayer);
                if (recolor != null)
                {
                    fluidVc = recolor.apply(fluidVc);
                }
                fluidVc = new TransformingVertexConsumer(fluidVc, stack.method_23760(), entry.pos, shaders);
                class_310.method_1551().method_1541().method_3352(entry.pos, info.view, fluidVc, entry.state, entry.state.method_26227());
            }
            if (entry.state.method_26217() != class_2464.field_11455)
            {
                if (entry.state.method_26204() instanceof class_2397)
                {
                    this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);
                }
                else
                {
                    class_310.method_1551().method_1541().method_3355(entry.state, entry.pos, info.view, stack, vc, true, this.blockRandom);
                }
            }

            /* Render blocks with entity (chests, beds, signs, skulls, etc.) */
            block = entry.state.method_26204();

            if (!this.capturingVAO && !skipBlockEntities && block instanceof class_2343)
            {
                /* Align BE position with the real location where it is drawn */
                int dx = (int) Math.floor(entry.pos.method_10263() - info.pivotX);
                int dy = (int) Math.floor(entry.pos.method_10264() - info.pivotY);
                int dz = (int) Math.floor(entry.pos.method_10260() - info.pivotZ);
                class_2338 worldPos = info.anchor.method_10069(dx, dy, dz);
                class_2586 be = ((class_2343) block).method_10123(worldPos, entry.state);

                if (be != null)
                {
                    if (entry.nbt != null)
                    {
                        be.method_11014(entry.nbt, class_310.method_1551().field_1687.method_30349());
                    }
                    /* Associate real world so renderer can query light and effects */
                    if (class_310.method_1551().field_1687 != null)
                    {
                        be.method_31662(class_310.method_1551().field_1687);
                    }

                    /* Diagnostic: check if renderer exists for this BE */
                    class_824 beDispatcher = class_310.method_1551().method_31975();
                    class_827<?> renderer = beDispatcher.method_3550(be);

                    /* Render BE directly with the renderer to avoid internal translations */
                    /* based on camera/world position that misalign drawing respecting local matrix. */
                    /* BE Light: use virtual view to incorporate artificial light */
                    /* from buffer, combining sky and block as in vanilla pipeline. */
                    int skyLight = info.view.method_8314(class_1944.field_9284, entry.pos);
                    int blockLight = info.view.method_8314(class_1944.field_9282, entry.pos);
                    /* LightmapTextureManager.pack expects block light first then sky light. */
                    int beLight = class_765.method_23687(blockLight, skyLight);

                    if (renderer != null)
                    {
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        class_827 raw = (class_827) renderer;
                        CustomVertexConsumerProvider beProvider;

                        /* Apply global tint/alpha and force translucent layer on cutout layers */
                        /* so Block Entities also respect opacity. */
                        beProvider = FormUtilsClient.getProvider();

                        Color beTint = this.resolveStructureBlockEntityColor();
                        boolean beShadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

                        this.applyBlockEntityOnlyShaderShadow(beTint, beShadowPass);
                        beProvider.setSubstitute(BBSRendering.getColorConsumer(beTint));

                        try
                        {
                            RenderSystem.setShaderColor(beTint.r, beTint.g, beTint.b, beTint.a);
                            raw.method_3569(be, 0F, stack, beProvider, beLight, overlay);
                        }
                        finally
                        {
                            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                            beProvider.draw();
                            beProvider.setSubstitute(null);
                            CustomVertexConsumerProvider.clearRunnables();
                        }
                    }
                }
            }

            stack.method_22909();
        }

        /* Important: if Sodium/Iris is active, the recolor wrapper uses */
        /* global static state (RecolorVertexConsumer.newColor). Ensure */
        /* it is reset after this pass so UI doesn't inherit the tint. */
        RecolorVertexConsumer.newColor = null;
    }

    /**
     * Specialized render: draws only blocks with animated textures (portal, water, lava)
     * using the vanilla TranslucentMovingBlock layer to get continuous animation.
     * Reuses the same centering/parity and virtual world view.
     */
    private void renderAnimatedBlocksVanilla(FormRenderingContext context, class_4587 stack, class_4597 consumers, int light, int overlay, Function<class_4588, class_4588> recolor)
    {
        /* Ensure block atlas is active */
        RenderSystem.setShaderTexture(0, class_1723.field_21668);
        this.syncFancyGraphicsFromOptions();

        RenderInfo info = this.calculateRenderInfo(context, false);

        for (BlockEntry entry : this.animatedBlocks)
        {
            boolean shadersEnabled;
            class_1921 layer;
            float globalAlphaAnim;
            class_4588 vc;

            if (!this.isAnimatedTexture(entry.state))
            {
                continue;
            }

            stack.method_22903();
            stack.method_46416(entry.pos.method_10263() - info.pivotX, entry.pos.method_10264() - info.pivotY, entry.pos.method_10260() - info.pivotZ);

            /* Layer selection: in shaders use entity variant so the pack processes the animation */
            shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            layer = shadersEnabled
                ? class_4696.method_23683(entry.state, true)
                : class_1921.method_29380();

            /* If global alpha exists, prefer translucent entity layer in shaders to ensure smooth fade */
            globalAlphaAnim = this.form.getFormOpacity();

            if (globalAlphaAnim < 0.999F)
            {
                layer = shadersEnabled
                    ? class_4722.method_24076()
                    : class_1921.method_29380();
            }

            /* Apply global alpha as recolor */
            vc = consumers.getBuffer(layer);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            if (this.form.renderFluid.get() && !entry.state.method_26227().method_15769())
            {
                boolean shaders = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                class_1921 fluidLayer = shaders
                    ? class_4696.method_23683(entry.state, false)
                    : class_4696.method_23680(entry.state.method_26227());
                class_4588 fluidVc = consumers.getBuffer(fluidLayer);
                if (recolor != null)
                {
                    fluidVc = recolor.apply(fluidVc);
                }
                fluidVc = new TransformingVertexConsumer(fluidVc, stack.method_23760(), entry.pos, shaders);
                class_310.method_1551().method_1541().method_3352(entry.pos, info.view, fluidVc, entry.state, entry.state.method_26227());
            }
            if (entry.state.method_26217() != class_2464.field_11455)
            {
                class_310.method_1551().method_1541().method_3355(entry.state, entry.pos, info.view, stack, vc, true, this.blockRandom);
            }
            stack.method_22909();
        }

        /* Reset global color state (Sodium/Iris) after animated pass */
        RecolorVertexConsumer.newColor = null;
    }

    /**
     * Glass/ice and other translucent blocks: drawn separately with depth writes off so
     * models and deferred paint overlays behind the structure stay visible.
     */
    private void renderTranslucentBlocksVanilla(FormRenderingContext context, class_4587 stack, class_4597 consumers, int light, int overlay, Function<class_4588, class_4588> recolor)
    {
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, class_1723.field_21668);
        this.syncFancyGraphicsFromOptions();
        RenderSystem.depthMask(false);

        try
        {
            RenderInfo info = this.calculateRenderInfo(context, false);

            for (BlockEntry entry : this.translucentBlocks)
            {
                boolean shadersEnabled;
                class_1921 layer;
                float globalAlpha;
                class_4588 vc;

                if (!this.isTranslucentBlock(entry.state))
                {
                    continue;
                }

                stack.method_22903();
                stack.method_46416(entry.pos.method_10263() - info.pivotX, entry.pos.method_10264() - info.pivotY, entry.pos.method_10260() - info.pivotZ);

                shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                layer = shadersEnabled
                    ? class_4696.method_23683(entry.state, false)
                    : class_4696.method_23679(entry.state);

                globalAlpha = this.form.getFormOpacity();

                if (globalAlpha < 0.999F)
                {
                    layer = shadersEnabled
                        ? class_4722.method_24076()
                        : class_1921.method_23583();
                }

                vc = consumers.getBuffer(layer);

                if (recolor != null)
                {
                    vc = recolor.apply(vc);
                }

                if (entry.state.method_26217() != class_2464.field_11455)
                {
                    class_310.method_1551().method_1541().method_3355(entry.state, entry.pos, info.view, stack, vc, true, this.blockRandom);
                }

                stack.method_22909();
            }
        }
        finally
        {
            RenderSystem.depthMask(savedDepthMask);
            RecolorVertexConsumer.newColor = null;
        }
    }

    /** Renders blocks that require biome tint (leaves, grass, vines, lily pad) using vanilla layers. */
    private void renderBiomeTintedBlocksVanilla(FormRenderingContext context, class_4587 stack, class_4597 consumers, int light, int overlay, Function<class_4588, class_4588> recolor)
    {
        /* Ensure correct blending state for translucent layers */
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        /* Ensure block atlas is active */
        RenderSystem.setShaderTexture(0, class_1723.field_21668);
        /* Structure foliage always Fancy — never sync down to Fast for this pass. */
        try
        {
            class_4696.method_23682(true);
        }
        catch (Throwable ignored)
        {}

        RenderInfo info = this.calculateRenderInfo(context, false);

        for (BlockEntry entry : this.biomeTintedBlocks)
        {
            boolean shadersEnabledTint;
            class_1921 layer;
            float globalAlpha;
            class_4588 vc;

            if (!this.isBiomeTinted(entry.state))
            {
                continue;
            }

            stack.method_22903();
            stack.method_46416(entry.pos.method_10263() - info.pivotX, entry.pos.method_10264() - info.pivotY, entry.pos.method_10260() - info.pivotZ);

            /* Leaves: Fancy cutout_mipped. Under Iris live entity buffers they look Fast with the
             * opacity patch — skip here (except shadow); deferred post-composite draws Fancy. */
            if (entry.state.method_26204() instanceof class_2397)
            {
                boolean irisLive = BBSRendering.isIrisShadersEnabled()
                    && BBSRendering.isRenderingWorld()
                    && !context.isShadowPass
                    && !BBSRendering.isIrisShadowPass();

                if (irisLive)
                {
                    stack.method_22909();
                    continue;
                }

                this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);
                stack.method_22909();
                continue;
            }

            shadersEnabledTint = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            layer = this.resolveStructureBlockLayer(entry.state, shadersEnabledTint);

            /* If there is global opacity (<1), force translucent layer so alpha */
            /* applies to materials originally cutout/cull and they don't "disappear". */
            globalAlpha = this.form.getFormOpacity();

            if (globalAlpha < 0.999F)
            {
                layer = shadersEnabledTint ? class_4722.method_24076() : class_1921.method_23583();
            }

            vc = consumers.getBuffer(layer);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            class_310.method_1551().method_1541().method_3355(entry.state, entry.pos, info.view, stack, vc, true, this.blockRandom);
            stack.method_22909();
        }

        /* Restore state */
        RenderSystem.disableBlend();
        /* Reset global color state (Sodium/Iris) to avoid UI tinting */
        RecolorVertexConsumer.newColor = null;
    }

    private Color resolveStructureBlendColor()
    {
        Color storedFormColor = this.form.getFormColor();
        Color rawFormColor = storedFormColor.copyWithBlendIntensity();
        Color tint = Color.white();

        if (FormColorBlend.shouldBakeFormColor(storedFormColor))
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
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.method_23760().method_23762());
        Function<class_4588, class_4588> recolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            class_4587 overlayStack = new class_4587();
            class_4597.class_4598 consumers = class_310.method_1551().method_22940().method_23000();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            try
            {
                class_4696.method_23682(true);
            }
            catch (Throwable ignored)
            {}

            /* Vanilla post-composite defaults to depthMask(false) for BE tint overlays;
             * leaves need depth write so overlapping Fancy faces sort correctly. */
            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, class_1723.field_21668);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            try
            {
                this.renderStructureLeavesOnly(context, overlayStack, consumers, recolor);
                consumers.method_22994(class_1921.method_23579());
            }
            catch (Throwable ignored)
            {
                try
                {
                    consumers.method_22993();
                }
                catch (Throwable ignoredDraw)
                {}
            }
        });
    }

    /** Draws only leaf blocks as Fancy cutout (no-shader / non-Iris live path). */
    private void renderStructureLeavesOnly(FormRenderingContext context, class_4587 stack, class_4597 consumers, Function<class_4588, class_4588> recolor)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);

        try
        {
            class_4696.method_23682(true);
        }
        catch (Throwable ignored)
        {}

        RenderSystem.setShaderTexture(0, class_1723.field_21668);

        for (BlockEntry entry : this.biomeTintedBlocks)
        {
            if (!(entry.state.method_26204() instanceof class_2397))
            {
                continue;
            }

            stack.method_22903();
            stack.method_46416(entry.pos.method_10263() - info.pivotX, entry.pos.method_10264() - info.pivotY, entry.pos.method_10260() - info.pivotZ);
            this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);
            stack.method_22909();
        }
    }

    /**
     * Bake blend / paint / grade into a ColorModulator tint for chests, beds, etc.
     * Under Iris the main-pass tint is ignored; {@link #submitDeferredStructureBlockEntityTint}
     * redraws BEs after composite where ColorModulator works again.
     */
    private Color resolveStructureBlockEntityColor()
    {
        Color tint = FormColorBlend.resolveBlockEntityTint(this.form.getFormColor(), this.form.paintSettings.get(), this.form.paintColor.get());

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
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.method_23760().method_23762());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            class_4587 overlayStack = new class_4587();
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            try
            {
                this.renderBlockEntitiesOnly(context, overlayStack, consumers, class_765.field_32767, overlay, true);
            }
            catch (Throwable ignored)
            {}
        });
    }

    private Function<class_4588, class_4588> getMainConsumer(Color color, Color resolvedPaint)
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
                Math.max(1, this.boundsMax.method_10263() - this.boundsMin.method_10263() + 1),
                Math.max(1, this.boundsMax.method_10264() - this.boundsMin.method_10264() + 1),
                Math.max(1, this.boundsMax.method_10260() - this.boundsMin.method_10260() + 1)
            );

            return;
        }

        dest.set(
            Math.max(1, this.size.method_10263()),
            Math.max(1, this.size.method_10264()),
            Math.max(1, this.size.method_10260())
        );
    }

    private void resolveStructureMaskHalf(EffectTransform transform, Vector3f dest)
    {
        Vector3f size = new Vector3f();

        this.resolveStructureMaskSize(size);
        EffectTransformMath.resolveStructureMaskHalfExtents(transform, dest, size.x, size.y, size.z);
    }

    private void prepareVaoColorTintForMainPass(class_4587 stack, Color formColor, boolean active)
    {
        if (!active || formColor == null)
        {
            return;
        }

        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();
        Vector3f colorMaskHalf = new Vector3f();

        this.resolveStructureMaskHalf(formColor.transform, colorMaskHalf);
        ModelVAORenderer.setColorEffectTransform(formRootInverse, formColor.transform, colorMaskHalf);
        ModelVAORenderer.setFormColorTint(formColor.r, formColor.g, formColor.b, formColor.a);
    }

    /**
     * Upload Color Grade into model.fsh so the baked VAO is graded without remeshing every block.
     */
    private void prepareVaoColorGradeForMainPass(Color storedFormColor)
    {
        if (storedFormColor == null || !storedFormColor.hasColorAdjustments())
        {
            return;
        }

        Vector3f size = new Vector3f();

        this.resolveStructureMaskSize(size);
        ModelVAORenderer.setFormColorGrade(
            storedFormColor.brightness,
            storedFormColor.contrast,
            storedFormColor.hue,
            storedFormColor.saturation
        );
        ModelVAORenderer.setGradeEffectTransformsForStructure(storedFormColor, size.x, size.y, size.z);
    }

    private void clearVaoColorTint()
    {
        ModelVAORenderer.clearColorEffectTransform();
        ModelVAORenderer.clearFormColorTint();
        ModelVAORenderer.clearFormColorGrade();
        ModelVAORenderer.clearGradeEffectTransforms();
    }

    private void renderStructureGlowOverlay(FormRenderingContext context, class_4587 stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean optimize, boolean useEntityLayers)
    {
        this.renderStructureGlowOverlayPass(context, stack, glowSettings, legacyGlow, glowIntensity, alpha, overlay, optimize, useEntityLayers);
    }

    private void renderStructureGlowOverlayPass(FormRenderingContext context, class_4587 stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean optimize, boolean useEntityLayers)
    {
        int layers = FormColorBlend.resolveGlowOverlayLayers(glowIntensity);

        if (optimize)
        {
            this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, () ->
            {
                CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

                this.renderStructureCulledWorld(context, stack, consumers, class_765.field_32767, overlay, useEntityLayers, null, true, true);
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

                this.renderStructureCulledWorld(context, stack, consumers, class_765.field_32767, overlay, useEntityLayers, null, true, false);
            });
        }
    }

    private void renderStructureLayerGlowOverlay(FormRenderingContext context, class_4587 stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean useEntityLayers, StructurePaintLayer layer, int layers)
    {
        this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            if (layer == StructurePaintLayer.BIOME)
            {
                this.renderBiomeTintedBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
            else if (layer == StructurePaintLayer.ANIMATED)
            {
                this.renderAnimatedBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
            else
            {
                this.renderTranslucentBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
        });
    }

    private void runStructureBlocksGlowOverlay(GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, int layers, Runnable draw)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        Color glowColor = FormColorBlend.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorBlend.resolveGlowOverlayShaderScale(glowIntensity);

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

    private void renderStructurePaintOverlay(FormRenderingContext context, class_4587 stack, Color resolvedPaint, float alpha, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        this.renderStructurePaintOverlayPass(context, stack, paintOverlay, overlay, optimize, useEntityLayers, transform, glowSettings, legacyGlow, glowIntensity, alpha);
    }

    private void submitDeferredStructurePaintOverlay(FormRenderingContext context, Color resolvedPaint, float alpha, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.method_23760().method_23762());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            this.renderStructurePaintOverlayPass(context, overlayStack, paintOverlay, overlay, optimize, useEntityLayers, transform, glowSettings, legacyGlow, glowIntensity, alpha);
        });
    }

    private void renderStructurePaintOverlayPass(FormRenderingContext context, class_4587 stack, Color paintOverlay, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        int light = class_765.field_32767;

        if (optimize)
        {
            IModelVAO vao = this.getStructureVao();
            boolean hasChunked = this.hasChunkedStructureVao();

            if (hasChunked)
            {
                FormRenderingContext paintContext = new FormRenderingContext()
                    .set(context.type, context.entity, stack, light, overlay, context.transition);

                this.renderStructureChunkedVaoPaintOverlay(paintContext, Color.white(), paintOverlay, light, overlay, transform);
            }
            else if (vao != null)
            {
                /* White tint — paint strength must not depend on Blend Color (same as Block/Item). */
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

    private void renderStructureVaoPaintOverlay(IModelVAO vao, class_4587 stack, Color tint, Color paintOverlay, int light, int overlay, EffectTransform transform)
    {
        class_757 gameRenderer = class_310.method_1551().field_1773;
        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();
        Vector3f paintMaskHalf = new Vector3f();

        /* Structures: UI scale 1 covers full AABB for box / circle / triangle. */
        this.resolveStructureMaskHalf(transform, paintMaskHalf);

        gameRenderer.method_22974().method_3316();
        gameRenderer.method_22975().method_23209();

        try
        {
            this.clearVaoColorTint();
            ModelVAORenderer.beginPaintOverlayPass(false);
            /* Stronger bias than the default paint pass — large coplanar structure faces z-fight otherwise. */
            GL11.glPolygonOffset(-1F, -2F);
            ModelVAORenderer.setPaint(paintOverlay.r, paintOverlay.g, paintOverlay.b, paintOverlay.a);
            ModelVAORenderer.setPaintEffectTransform(formRootInverse, transform, paintMaskHalf, true);
            RenderSystem.setShader(BBSShaders::getModel);
            RenderSystem.setShaderTexture(0, class_1723.field_21668);
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
            gameRenderer.method_22974().method_3315();
            gameRenderer.method_22975().method_23213();
        }
    }

    private void renderStructureChunkedVaoPaintOverlay(FormRenderingContext context, Color tint, Color paintOverlay, int light, int overlay, EffectTransform transform)
    {
        class_757 gameRenderer = class_310.method_1551().field_1773;
        Matrix4f formRootInverse = new Matrix4f(context.stack.method_23760().method_23761()).invert();
        Vector3f paintMaskHalf = new Vector3f();

        this.resolveStructureMaskHalf(transform, paintMaskHalf);

        gameRenderer.method_22974().method_3316();
        gameRenderer.method_22975().method_23209();

        try
        {
            this.clearVaoColorTint();
            ModelVAORenderer.beginPaintOverlayPass(false);
            GL11.glPolygonOffset(-1F, -2F);
            ModelVAORenderer.setPaint(paintOverlay.r, paintOverlay.g, paintOverlay.b, paintOverlay.a);
            ModelVAORenderer.setPaintEffectTransform(formRootInverse, transform, paintMaskHalf, true);
            RenderSystem.setShader(BBSShaders::getModel);
            RenderSystem.setShaderTexture(0, class_1723.field_21668);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(false);
            this.drawOpaqueStructureVaos(BBSShaders.getModel(), context, tint, light, overlay);

            IModelVAO specials = this.getStructureSpecialsVao();

            if (specials != null)
            {
                this.drawBakedSpecialsVao(BBSShaders.getModel(), context, specials, tint, light, overlay);
            }
        }
        finally
        {
            RenderSystem.depthMask(true);
            ModelVAORenderer.clearPaintEffectTransform();
            ModelVAORenderer.endPaintOverlayPass();
            this.clearVaoPaint();
            gameRenderer.method_22974().method_3315();
            gameRenderer.method_22975().method_23213();
        }
    }

    private void renderStructureLayerPaintOverlay(FormRenderingContext context, class_4587 stack, Color paintOverlay, int overlay, boolean useEntityLayers, StructurePaintLayer layer, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        this.runStructureBlocksPaintOverlay(paintOverlay, stack, transform, glowSettings, legacyGlow, glowIntensity, alpha, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            if (layer == StructurePaintLayer.BIOME)
            {
                this.renderBiomeTintedBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
            else if (layer == StructurePaintLayer.ANIMATED)
            {
                this.renderAnimatedBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
            else
            {
                this.renderTranslucentBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
        });
    }

    private void renderStructureCulledBlocksPaintOverlay(FormRenderingContext context, class_4587 stack, Color paintOverlay, int overlay, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        this.runStructureBlocksPaintOverlay(paintOverlay, stack, transform, glowSettings, legacyGlow, glowIntensity, alpha, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            this.renderStructureCulledWorld(context, stack, consumers, class_765.field_32767, overlay, useEntityLayers, null, true, false);
        });
    }

    private void runStructureBlocksPaintOverlay(Color paintOverlay, class_4587 stack, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, Runnable draw)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();
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

    private void renderStructureColorTintOverlay(FormRenderingContext context, class_4587 stack, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers, boolean includeVao)
    {
        this.renderStructureColorTintOverlayPass(context, stack, formColor, alpha, overlay, optimize, useEntityLayers, includeVao);
    }

    private void submitDeferredStructureColorTintOverlay(FormRenderingContext context, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.method_23760().method_23762());
        Color formColorSnapshot = formColor.copy();

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            this.renderStructureColorTintOverlayPass(context, overlayStack, formColorSnapshot, alpha, overlay, optimize, useEntityLayers, false);
        });
    }

    private void submitDeferredStructureVaoColorTintOverlay(FormRenderingContext context, Color formColor, float alpha, int overlay)
    {
        if (!this.hasChunkedStructureVao() && this.getStructureVao() == null)
        {
            return;
        }

        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.method_23760().method_23762());
        Color formColorSnapshot = formColor.copy();
        Color tint = Color.white();
        IEntity entitySnapshot = context.entity;
        FormRenderType typeSnapshot = context.type;
        float transitionSnapshot = context.transition;
        int overlaySnapshot = overlay;

        tint.a = alpha;

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            FormRenderingContext overlayContext = new FormRenderingContext()
                .set(typeSnapshot, entitySnapshot, overlayStack, class_765.field_32767, overlaySnapshot, transitionSnapshot);

            this.renderStructureChunkedVaoColorTintOverlay(overlayContext, tint, formColorSnapshot, class_765.field_32767, overlaySnapshot);
        });
    }

    /**
     * Iris post-composite Color Grade: regrade pack-lit VAO pixels (same as ModelFormRenderer).
     * Uses chunked opaque VAOs for large structures — never remesh blocks.
     */
    private void submitDeferredStructureColorGradeOverlay(FormRenderingContext context, Color gradeSource, float alpha, int overlay)
    {
        if (gradeSource == null || !gradeSource.hasColorAdjustments())
        {
            return;
        }

        if (!this.hasChunkedStructureVao() && this.getStructureVao() == null)
        {
            return;
        }

        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.method_23760().method_23762());
        Color gradeSnapshot = gradeSource.copy();
        float alphaSnapshot = alpha;
        int overlaySnapshot = overlay;
        Matrix4f formRootInverse = new Matrix4f(positionMatrix).invert();
        Vector3f colorMaskHalf = new Vector3f();
        Vector3f structureSize = new Vector3f();
        IEntity entitySnapshot = context.entity;
        FormRenderType typeSnapshot = context.type;
        float transitionSnapshot = context.transition;
        IModelVAO specialsSnapshot = this.getStructureSpecialsVao();

        this.resolveStructureMaskHalf(gradeSnapshot.transform, colorMaskHalf);
        this.resolveStructureMaskSize(structureSize);

        ModelVAORenderer.submitColorGradeOverlay(() ->
        {
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            FormRenderingContext overlayContext = new FormRenderingContext()
                .set(typeSnapshot, entitySnapshot, overlayStack, class_765.field_32767, overlaySnapshot, transitionSnapshot);
            Color tint = Color.white();

            tint.a = alphaSnapshot;

            try
            {
                ModelVAORenderer.setColorEffectTransform(formRootInverse, gradeSnapshot.transform, colorMaskHalf);
                ModelVAORenderer.setFormColorGrade(gradeSnapshot.brightness, gradeSnapshot.contrast, gradeSnapshot.hue, gradeSnapshot.saturation);
                ModelVAORenderer.setGradeEffectTransformsForStructure(gradeSnapshot, structureSize.x, structureSize.y, structureSize.z);
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
                RenderSystem.setShader(BBSShaders::getModel);
                RenderSystem.setShaderTexture(0, class_1723.field_21668);
                this.drawOpaqueStructureVaos(BBSShaders.getModel(), overlayContext, tint, class_765.field_32767, overlaySnapshot);

                if (specialsSnapshot != null)
                {
                    this.drawBakedSpecialsVao(BBSShaders.getModel(), overlayContext, specialsSnapshot, tint, class_765.field_32767, overlaySnapshot);
                }
            }
            finally
            {
                ModelVAORenderer.clearColorEffectTransform();
                ModelVAORenderer.clearFormColorGrade();
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
            }
        });
    }

    private void renderStructureColorTintOverlayPass(FormRenderingContext context, class_4587 stack, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers, boolean includeVao)
    {
        Color tintUniform = this.resolveStructureColorTintUniform(formColor);
        IModelVAO vao = this.getStructureVao();
        boolean hasChunked = this.hasChunkedStructureVao();

        /* Prefer VAO redraw (single or chunked). Remeshing every block for Color Grade destroyed FPS. */
        if (optimize && (vao != null || hasChunked))
        {
            Color overlayTint = Color.white();

            overlayTint.a = alpha;

            if (hasChunked)
            {
                FormRenderingContext overlayContext = new FormRenderingContext()
                    .set(context.type, context.entity, stack, class_765.field_32767, overlay, context.transition);

                this.renderStructureChunkedVaoColorTintOverlay(overlayContext, overlayTint, tintUniform != null ? tintUniform : formColor, class_765.field_32767, overlay);
            }
            else
            {
                this.renderStructureVaoColorTintOverlay(vao, stack, overlayTint, tintUniform != null ? tintUniform : formColor, class_765.field_32767, overlay);
            }

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

            return;
        }

        this.renderStructureCulledBlocksColorTintOverlay(context, stack, tintUniform, overlay, useEntityLayers);
    }

    /**
     * When Color Grade is active, FormColorTint must stay blend-only; grade is uploaded
     * separately so hue/sat regrade lit pixels (same as Block).
     */
    private Color resolveStructureColorTintUniform(Color formColor)
    {
        Color stored = this.form.getFormColor();

        if (stored != null && stored.hasColorAdjustments())
        {
            Color tint = stored.copyWithBlendIntensityOnly();

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

    private void renderStructureVaoColorTintOverlay(IModelVAO vao, class_4587 stack, Color tint, Color formColor, int light, int overlay)
    {
        class_757 gameRenderer = class_310.method_1551().field_1773;
        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();
        Vector3f colorMaskHalf = new Vector3f();

        this.resolveStructureMaskHalf(formColor.transform, colorMaskHalf);

        gameRenderer.method_22974().method_3316();
        gameRenderer.method_22975().method_23209();

        try
        {
            ModelVAORenderer.beginColorTintOverlayPass();
            /* Extra units bias for large flat structure faces (default pass is enough for models). */
            GL11.glPolygonOffset(mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);
            ModelVAORenderer.setColorEffectTransform(formRootInverse, formColor.transform, colorMaskHalf);
            ModelVAORenderer.setFormColorTint(formColor.r, formColor.g, formColor.b, formColor.a);

            Color gradeSource = this.form.getFormColor();

            if (gradeSource != null && gradeSource.hasColorAdjustments())
            {
                Vector3f structureSize = new Vector3f();

                this.resolveStructureMaskSize(structureSize);
                ModelVAORenderer.setFormColorGrade(gradeSource.brightness, gradeSource.contrast, gradeSource.hue, gradeSource.saturation);
                ModelVAORenderer.setGradeEffectTransformsForStructure(gradeSource, structureSize.x, structureSize.y, structureSize.z);
            }

            RenderSystem.setShader(BBSShaders::getModel);
            RenderSystem.setShaderTexture(0, class_1723.field_21668);
            ModelVAORenderer.render(BBSShaders.getModel(), vao, stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
        }
        finally
        {
            ModelVAORenderer.clearColorEffectTransform();
            ModelVAORenderer.clearFormColorTint();
            ModelVAORenderer.clearFormColorGrade();
            ModelVAORenderer.endColorTintOverlayPass();
            gameRenderer.method_22974().method_3315();
            gameRenderer.method_22975().method_23213();
        }
    }

    /**
     * Color-tint / grade overlay for large chunked structure VAOs (Iris post-composite).
     */
    private void renderStructureChunkedVaoColorTintOverlay(FormRenderingContext context, Color tint, Color formColor, int light, int overlay)
    {
        class_757 gameRenderer = class_310.method_1551().field_1773;
        Matrix4f formRootInverse = new Matrix4f(context.stack.method_23760().method_23761()).invert();
        Vector3f colorMaskHalf = new Vector3f();

        this.resolveStructureMaskHalf(formColor.transform, colorMaskHalf);

        gameRenderer.method_22974().method_3316();
        gameRenderer.method_22975().method_23209();

        try
        {
            ModelVAORenderer.beginColorTintOverlayPass();
            GL11.glPolygonOffset(mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);
            ModelVAORenderer.setColorEffectTransform(formRootInverse, formColor.transform, colorMaskHalf);
            ModelVAORenderer.setFormColorTint(formColor.r, formColor.g, formColor.b, formColor.a);

            Color gradeSource = this.form.getFormColor();

            if (gradeSource != null && gradeSource.hasColorAdjustments())
            {
                Vector3f structureSize = new Vector3f();

                this.resolveStructureMaskSize(structureSize);
                ModelVAORenderer.setFormColorGrade(gradeSource.brightness, gradeSource.contrast, gradeSource.hue, gradeSource.saturation);
                ModelVAORenderer.setGradeEffectTransformsForStructure(gradeSource, structureSize.x, structureSize.y, structureSize.z);
            }

            RenderSystem.setShader(BBSShaders::getModel);
            RenderSystem.setShaderTexture(0, class_1723.field_21668);
            this.drawOpaqueStructureVaos(BBSShaders.getModel(), context, tint, light, overlay);

            IModelVAO specials = this.getStructureSpecialsVao();

            if (specials != null)
            {
                this.drawBakedSpecialsVao(BBSShaders.getModel(), context, specials, tint, light, overlay);
            }
        }
        finally
        {
            ModelVAORenderer.clearColorEffectTransform();
            ModelVAORenderer.clearFormColorTint();
            ModelVAORenderer.clearFormColorGrade();
            ModelVAORenderer.endColorTintOverlayPass();
            gameRenderer.method_22974().method_3315();
            gameRenderer.method_22975().method_23213();
        }
    }

    private void renderStructureLayerColorTintOverlay(FormRenderingContext context, class_4587 stack, Color formColor, int overlay, boolean useEntityLayers, StructurePaintLayer layer)
    {
        this.runStructureBlocksColorTintOverlay(formColor, stack, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            if (layer == StructurePaintLayer.BIOME)
            {
                this.renderBiomeTintedBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
            else if (layer == StructurePaintLayer.ANIMATED)
            {
                this.renderAnimatedBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
            else
            {
                this.renderTranslucentBlocksVanilla(context, stack, consumers, class_765.field_32767, overlay, null);
            }
        });
    }

    private void renderStructureCulledBlocksColorTintOverlay(FormRenderingContext context, class_4587 stack, Color formColor, int overlay, boolean useEntityLayers)
    {
        this.runStructureBlocksColorTintOverlay(formColor, stack, () ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            this.renderStructureCulledWorld(context, stack, consumers, class_765.field_32767, overlay, useEntityLayers, null, true, false);
        });
    }

    private void runStructureBlocksColorTintOverlay(Color formColor, class_4587 stack, Runnable draw)
    {
        this.runStructureBlocksColorTintOverlay(formColor, stack, this.form.getFormColor(), draw);
    }

    private void runStructureBlocksColorTintOverlay(Color formColor, class_4587 stack, Color gradeSource, Runnable draw)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        CustomVertexConsumerProvider.clearRunnables();

        Vector3f structureSize = new Vector3f();

        this.resolveStructureMaskSize(structureSize);
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configureColorTintOverlayRenderStateStructure(formRootInverse, formColor.transform, true, formColor, gradeSource, structureSize.x, structureSize.y, structureSize.z));

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        /* Same bias as paint / FlatColorTint — large coplanar structure faces z-fight Color Grade without it. */
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);

        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());

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
            RenderSystem.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    /** Glass/ice/stained glass — translucent layer, excluding animated fluids already drawn separately. */
    private boolean isTranslucentBlock(class_2680 state)
    {
        class_1921 layer;

        if (state == null || this.isAnimatedTexture(state))
        {
            return false;
        }

        layer = class_4696.method_23679(state);

        return layer == class_1921.method_23583()
            || layer == class_1921.method_29380()
            || layer == class_1921.method_29997();
    }

    /** Determines if the block requires texture animation (portal/water/lava). */
    private boolean isAnimatedTexture(class_2680 state)
    {
        class_3610 fs;

        if (state == null)
        {
            return false;
        }

        /* Nether Portal */
        if (state.method_27852(class_2246.field_10316))
        {
            return true;
        }

        /* Fire */
        if (state.method_27852(class_2246.field_10036) || state.method_27852(class_2246.field_22089))
        {
            return true;
        }

        /* Fluids: water and lava (including flowing variants). */
        fs = state.method_26227();

        if (fs != null)
        {
            if (fs.method_15772() == class_3612.field_15910 || fs.method_15772() == class_3612.field_15909 ||
                fs.method_15772() == class_3612.field_15908 || fs.method_15772() == class_3612.field_15907)
            {
                return true;
            }
        }

        return false;
    }

    /** Heuristic: determines if the block uses biome tint (foliage/grass/vine/lily pad). */
    private boolean isBiomeTinted(class_2680 state)
    {
        class_2248 b;

        if (state == null)
        {
            return false;
        }

        b = state.method_26204();

        return (b instanceof class_2397)
            || (b instanceof class_2372)
            || (b instanceof class_2541)
            || (b instanceof class_2553)
            || (b instanceof class_2457)
            || (b instanceof class_2513)
            || (b instanceof class_2195)
            || state.method_27852(class_2246.field_10112)
            || state.method_27852(class_2246.field_10424)
            || state.method_27852(class_2246.field_10479)
            || state.method_27852(class_2246.field_10214)
            || state.method_27852(class_2246.field_10313);
    }

    /**
     * Renders only Block Entities (chests, beds, signs, skulls, etc.) over the structure already drawn via VAO.
     * Reuses the same centering/parity and world anchor calculation as the culled render.
     */
    private void renderBlockEntitiesOnly(FormRenderingContext context, class_4587 stack, class_4597 consumers, int light, int overlay)
    {
        this.renderBlockEntitiesOnly(context, stack, consumers, light, overlay, true);
    }

    /**
     * @param applyBlendTint when false, keep the caller's vertex substitute (paint / color-tint overlays).
     */
    private void renderBlockEntitiesOnly(FormRenderingContext context, class_4587 stack, class_4597 consumers, int light, int overlay, boolean applyBlendTint)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);
        class_824 beDispatcher = class_310.method_1551().method_31975();

        for (BlockEntry entry : this.blockEntitiesList)
        {
            stack.method_22903();
            stack.method_46416(entry.pos.method_10263() - info.pivotX, entry.pos.method_10264() - info.pivotY, entry.pos.method_10260() - info.pivotZ);

            class_2586 be = this.getOrCreateBlockEntity(entry);

            if (be != null)
            {
                class_827<?> renderer;
                int skyLight;
                int blockLight;
                int beLight;

                if (class_310.method_1551().field_1687 != null)
                {
                    be.method_31662(class_310.method_1551().field_1687);
                }

                renderer = beDispatcher.method_3550(be);

                skyLight = info.view.method_8314(class_1944.field_9284, entry.pos);
                blockLight = info.view.method_8314(class_1944.field_9282, entry.pos);
                /* LightmapTextureManager.pack expects block light first then sky light. */
                beLight = class_765.method_23687(blockLight, skyLight);

                if (renderer != null)
                {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    class_827 raw = (class_827) renderer;
                    CustomVertexConsumerProvider beProvider;

                    /* Apply tint outside Iris gbuffer. Iris ignores ColorModulator and
                     * setShaderColor during the entity pass also breaks pack shading. */
                    beProvider = FormUtilsClient.getProvider();

                    Color beTint = null;
                    boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

                    if (applyBlendTint)
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

                        raw.method_3569(be, 0F, stack, beProvider, beLight, overlay);
                    }
                    finally
                    {
                        if (beTint != null)
                        {
                            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                        }

                        beProvider.draw();

                        if (applyBlendTint)
                        {
                            beProvider.setSubstitute(null);
                            CustomVertexConsumerProvider.clearRunnables();
                        }
                    }
                }
            }

            stack.method_22909();
        }
    }

    private class_2586 getOrCreateBlockEntity(BlockEntry entry)
    {
        class_2586 cached = this.blockEntityCache.get(entry);

        if (cached != null)
        {
            return cached;
        }

        class_2248 block = entry.state.method_26204();

        if (!(block instanceof class_2343 provider))
        {
            return null;
        }

        class_2586 be = provider.method_10123(entry.pos, entry.state);

        if (be != null && entry.nbt != null && class_310.method_1551().field_1687 != null)
        {
            be.method_11014(entry.nbt, class_310.method_1551().field_1687.method_30349());
        }

        if (be != null)
        {
            this.blockEntityCache.put(entry, be);
        }

        return be;
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
            this.clearLocalStructureData();
            this.vaoDirty = true;
            this.vaoPickingDirty = true;
            this.clearCachedVao();
            this.lastFile = null;
            this.lastStructureDataRevision = -1;

            return;
        }

        if (file.equals(this.lastFile)
            && this.lastStructureDataRevision == STRUCTURE_DATA_REVISION
            && !this.blocks.isEmpty())
        {
            return;
        }

        this.clearCachedVao();
        this.lastFile = file;
        this.lastStructureDataRevision = STRUCTURE_DATA_REVISION;
        this.vaoDirty = true;
        this.vaoPickingDirty = true;
        this.blockEntityCache.clear();
        this.frameRenderInfoValid = false;

        ParsedStructure cached = PARSED_CACHE.get(file);

        if (cached != null)
        {
            this.applyParsedStructure(cached);

            return;
        }

        this.clearLocalStructureData();

        File nbtFile = BBSMod.getProvider().getFile(Link.create(file));

        /* Try reading as external file if exists; otherwise use internal assets InputStream. */
        if (nbtFile != null && nbtFile.exists())
        {
            try
            {
                class_2487 root = class_2507.method_30613(nbtFile.toPath(), class_2505.method_53898());

                this.parseStructure(root);
                this.cacheCurrentParsedStructure(file);

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
                class_2487 root = class_2507.method_10629(is, class_2505.method_53898());

                this.parseStructure(root);
                this.cacheCurrentParsedStructure(file);
            }
            catch (IOException e)
            {}
        }
        catch (Exception e)
        {}
    }

    private void clearLocalStructureData()
    {
        this.blocks.clear();
        this.animatedBlocks.clear();
        this.biomeTintedBlocks.clear();
        this.translucentBlocks.clear();
        this.blockEntitiesList.clear();
        this.blockEntityCache.clear();
        this.size = class_2338.field_10980;
        this.boundsMin = null;
        this.boundsMax = null;
        this.hasTranslucentLayer = false;
        this.hasCutoutLayer = false;
        this.hasAnimatedLayer = false;
        this.hasBiomeTintedLayer = false;
        this.hasLeavesLayer = false;
        this.hasBlockEntityLayer = false;
        this.entriesCache = null;
        this.cachedView = null;
        this.frameRenderInfoValid = false;
    }

    private void applyParsedStructure(ParsedStructure parsed)
    {
        this.blocks.clear();
        this.blocks.addAll(parsed.blocks);
        this.animatedBlocks.clear();
        this.animatedBlocks.addAll(parsed.animatedBlocks);
        this.biomeTintedBlocks.clear();
        this.biomeTintedBlocks.addAll(parsed.biomeTintedBlocks);
        this.translucentBlocks.clear();
        this.translucentBlocks.addAll(parsed.translucentBlocks);
        this.blockEntitiesList.clear();
        this.blockEntitiesList.addAll(parsed.blockEntitiesList);
        this.size = parsed.size;
        this.boundsMin = parsed.boundsMin;
        this.boundsMax = parsed.boundsMax;
        this.hasTranslucentLayer = parsed.hasTranslucentLayer;
        this.hasCutoutLayer = parsed.hasCutoutLayer;
        this.hasAnimatedLayer = parsed.hasAnimatedLayer;
        this.hasBiomeTintedLayer = parsed.hasBiomeTintedLayer;
        this.hasLeavesLayer = parsed.hasLeavesLayer;
        this.hasBlockEntityLayer = parsed.hasBlockEntityLayer;
        this.entriesCache = null;
        this.cachedView = null;
        this.blockEntityCache.clear();
        this.frameRenderInfoValid = false;
    }

    private void cacheCurrentParsedStructure(String file)
    {
        if (file == null || this.blocks.isEmpty())
        {
            return;
        }

        PARSED_CACHE.put(file, new ParsedStructure(
            this.blocks,
            this.animatedBlocks,
            this.biomeTintedBlocks,
            this.translucentBlocks,
            this.blockEntitiesList,
            this.size,
            this.boundsMin,
            this.boundsMax,
            this.hasTranslucentLayer,
            this.hasCutoutLayer,
            this.hasAnimatedLayer,
            this.hasBiomeTintedLayer,
            this.hasLeavesLayer,
            this.hasBlockEntityLayer
        ));
    }

    private void buildStructureVAO()
    {
        /* Capture geometry in a VAO using vanilla pipeline but substituting the consumer. */
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        boolean large = this.isLargeStructure();
        boolean chunked = large;
        StructureVAOCollector collector = chunked ? null : new StructureVAOCollector();
        LightmapStructureVAOCollector lightWrapper = chunked ? null : new LightmapStructureVAOCollector(collector);
        CaptureChunkBus chunkBus = chunked ? new CaptureChunkBus() : null;
        class_4587 captureStack = new class_4587();
        FormRenderingContext captureContext;
        boolean useEntityLayers = false; /* capture with block layers */

        if (collector != null)
        {
            collector.setComputeTangents(false);
        }

        if (chunkBus != null)
        {
            this.captureChunkBus = chunkBus;
            provider.setSubstitute(vc -> this.captureChunkBus.consumer());
        }
        else
        {
            provider.setSubstitute(vc -> lightWrapper);
        }

        captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, class_765.field_32769, class_4608.field_21444, 0F);

        try
        {
            this.syncFancyGraphicsFromOptions();
        }
        catch (Throwable ignored)
        {}

        /* Avoid rendering BlockEntities during capture to avoid mixing atlases. */
        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = false; /* skip animated / glass; bake biome when large. */
        this.capturingBakeBiome = large;

        try
        {
            Function<class_4588, class_4588> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());

            this.renderStructureCulledWorld(captureContext, captureStack, provider, class_765.field_32769, class_4608.field_21444, useEntityLayers, captureRecolor, false, false);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
            this.capturingBakeBiome = false;
            this.captureChunkBus = null;
        }

        provider.draw();
        provider.setSubstitute(null);

        String cacheKey = this.getVaoCacheKey();

        if (cacheKey != null)
        {
            VaoHolder holder = VAO_CACHE.computeIfAbsent(cacheKey, k -> new VaoHolder());

            StructureFormRenderer.deleteVao(holder.vao);
            StructureFormRenderer.deleteVao(holder.specials);

            if (holder.chunks != null)
            {
                for (VaoChunk chunk : holder.chunks)
                {
                    StructureFormRenderer.deleteVao(chunk.vao);
                }

                holder.chunks.clear();
            }

            if (chunkBus != null)
            {
                float[] pivot = this.getStructurePivot();

                holder.chunks = chunkBus.buildChunks(pivot[0], pivot[1], pivot[2]);
                holder.vao = null;
            }
            else
            {
                holder.chunks = null;
                holder.vao = new LightmapModelVAO(collector.toData(), lightWrapper.getLightmapData());
            }

            this.lastVaoCacheKey = cacheKey;

            if (large && this.hasTranslucentLayer)
            {
                this.buildSpecialsVAO(holder);
            }
            else
            {
                holder.specials = null;
            }
        }

        this.vaoDirty = false;
    }

    /**
     * Bake translucent glass once so large structures do not remesh it every frame.
     * Biome-tinted blocks bake into the opaque/chunked VAO with per-vertex colors.
     */
    private void buildSpecialsVAO(VaoHolder holder)
    {
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();
        LightmapStructureVAOCollector lightWrapper = new LightmapStructureVAOCollector(collector);
        class_4587 captureStack = new class_4587();
        FormRenderingContext captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, class_765.field_32769, class_4608.field_21444, 0F);

        collector.setComputeTangents(false);
        provider.setSubstitute(vc -> lightWrapper);

        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = true;
        this.capturingSpecialsOnly = true;

        try
        {
            Function<class_4588, class_4588> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());

            this.renderStructureCulledWorld(captureContext, captureStack, provider, class_765.field_32769, class_4608.field_21444, false, captureRecolor, true, false);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
            this.capturingSpecialsOnly = false;
        }

        provider.draw();
        provider.setSubstitute(null);

        ModelVAOData data = collector.toData();

        if (data.vertices().length < 9)
        {
            holder.specials = null;

            return;
        }

        StructureFormRenderer.deleteVao(holder.specials);
        holder.specials = new LightmapModelVAO(data, lightWrapper.getLightmapData());
    }

    /**
     * Builds a picking VAO that includes animated and biome tinted blocks,
     * so selection silhouette covers the whole structure.
     */
    private void buildStructureVAOPicking()
    {
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();
        class_4587 captureStack = new class_4587();
        FormRenderingContext captureContext;
        boolean useEntityLayers = false;
        ModelVAOData data;

        provider.setSubstitute(vc -> collector);

        captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, class_765.field_32769, class_4608.field_21444, 0F);

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
            Function<class_4588, class_4588> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());

            this.renderStructureCulledWorld(captureContext, captureStack, provider, class_765.field_32769, class_4608.field_21444, useEntityLayers, captureRecolor, false, false);
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
                this.renderBlockEntitiesOnly(captureContext, captureStack, provider, class_765.field_32769, class_4608.field_21444, false);
            }
            catch (Throwable ignored)
            {}

            this.appendBlockEntityPickCubes(collector, captureContext);
        }

        provider.draw();
        provider.setSubstitute(null);

        data = collector.toData();

        String cacheKey = this.getVaoCacheKey();

        if (cacheKey != null)
        {
            VaoHolder holder = VAO_CACHE.computeIfAbsent(cacheKey, k -> new VaoHolder());

            if (holder.picking instanceof ModelVAO)
            {
                ((ModelVAO) holder.picking).delete();
            }

            holder.picking = new ModelVAO(data);
            this.lastVaoCacheKey = cacheKey;
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
            float x0 = entry.pos.method_10263() - info.pivotX;
            float y0 = entry.pos.method_10264() - info.pivotY;
            float z0 = entry.pos.method_10260() - info.pivotZ;

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
        collector.method_22912(x0, y0, z0).method_1336(255, 255, 255, 255).method_22913(0F, 0F).method_60796(0, 0).method_22921(0, 0).method_22914(nx, ny, nz);
        collector.method_22912(x1, y1, z1).method_1336(255, 255, 255, 255).method_22913(1F, 0F).method_60796(0, 0).method_22921(0, 0).method_22914(nx, ny, nz);
        collector.method_22912(x2, y2, z2).method_1336(255, 255, 255, 255).method_22913(1F, 1F).method_60796(0, 0).method_22921(0, 0).method_22914(nx, ny, nz);
        collector.method_22912(x3, y3, z3).method_1336(255, 255, 255, 255).method_22913(0F, 1F).method_60796(0, 0).method_22921(0, 0).method_22914(nx, ny, nz);
    }

    private IModelVAO getStructureVao()
    {
        String key = this.getVaoCacheKey();

        if (key == null)
        {
            return null;
        }

        VaoHolder holder = VAO_CACHE.get(key);

        return holder != null ? holder.vao : null;
    }

    private IModelVAO getStructureSpecialsVao()
    {
        String key = this.getVaoCacheKey();

        if (key == null)
        {
            return null;
        }

        VaoHolder holder = VAO_CACHE.get(key);

        return holder != null ? holder.specials : null;
    }

    private boolean hasChunkedStructureVao()
    {
        String key = this.getVaoCacheKey();

        if (key == null)
        {
            return false;
        }

        VaoHolder holder = VAO_CACHE.get(key);

        return holder != null && holder.chunks != null && !holder.chunks.isEmpty();
    }

    private void drawOpaqueStructureVaos(class_5944 shader, FormRenderingContext context, Color tint, int light, int overlay)
    {
        String key = this.getVaoCacheKey();
        VaoHolder holder = key == null ? null : VAO_CACHE.get(key);

        if (holder == null)
        {
            return;
        }

        if (holder.chunks != null && !holder.chunks.isEmpty())
        {
            for (VaoChunk chunk : holder.chunks)
            {
                if (context != null && !context.ui && !this.isLocalChunkVisible(context, chunk))
                {
                    continue;
                }

                ModelVAORenderer.render(shader, chunk.vao, context.stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
            }

            return;
        }

        if (holder.vao != null)
        {
            ModelVAORenderer.render(shader, holder.vao, context.stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
        }
    }

    private void drawBakedSpecialsVao(class_5944 shader, FormRenderingContext context, IModelVAO specialsVao, Color tint, int light, int overlay)
    {
        if (specialsVao == null)
        {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        try
        {
            ModelVAORenderer.render(shader, specialsVao, context.stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
        }
        finally
        {
            RenderSystem.depthMask(savedDepthMask);
        }
    }

    private boolean isLocalChunkVisible(FormRenderingContext context, VaoChunk chunk)
    {
        if (context == null || context.stack == null)
        {
            return true;
        }

        double maxRange = this.getPlayerRenderDistanceBlocks() + CHUNK_SIZE * 2D;
        double maxRangeSq = maxRange * maxRange;

        /* Same matrix path as ModelVAORenderer: ModelView * stack → eye space. Cull by
         * distance to the camera (origin in eye space), not a wrong world AABB. */
        Matrix4f mat = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(context.stack.method_23760().method_23761());
        float[] xs = {chunk.minX, chunk.maxX};
        float[] ys = {chunk.minY, chunk.maxY};
        float[] zs = {chunk.minZ, chunk.maxZ};
        double eyeMinX = Double.POSITIVE_INFINITY;
        double eyeMinY = Double.POSITIVE_INFINITY;
        double eyeMinZ = Double.POSITIVE_INFINITY;
        double eyeMaxX = Double.NEGATIVE_INFINITY;
        double eyeMaxY = Double.NEGATIVE_INFINITY;
        double eyeMaxZ = Double.NEGATIVE_INFINITY;
        Vector4f corner = new Vector4f();

        for (float x : xs)
        {
            for (float y : ys)
            {
                for (float z : zs)
                {
                    corner.set(x, y, z, 1F).mul(mat);
                    eyeMinX = Math.min(eyeMinX, corner.x);
                    eyeMinY = Math.min(eyeMinY, corner.y);
                    eyeMinZ = Math.min(eyeMinZ, corner.z);
                    eyeMaxX = Math.max(eyeMaxX, corner.x);
                    eyeMaxY = Math.max(eyeMaxY, corner.y);
                    eyeMaxZ = Math.max(eyeMaxZ, corner.z);
                }
            }
        }

        double nx = mchorse.bbs_mod.utils.MathUtils.clamp(0D, eyeMinX, eyeMaxX);
        double ny = mchorse.bbs_mod.utils.MathUtils.clamp(0D, eyeMinY, eyeMaxY);
        double nz = mchorse.bbs_mod.utils.MathUtils.clamp(0D, eyeMinZ, eyeMaxZ);

        return nx * nx + ny * ny + nz * nz <= maxRangeSq;
    }

    /**
     * Blocks covered by the client's chunk view distance (same idea as vanilla world load).
     */
    private double getPlayerRenderDistanceBlocks()
    {
        class_310 mc = class_310.method_1551();
        int chunks = 12;

        try
        {
            if (mc != null && mc.field_1690 != null)
            {
                chunks = Math.max(2, mc.field_1690.method_42503().method_41753());
            }
        }
        catch (Throwable ignored)
        {}

        return chunks * 16D;
    }

    private IModelVAO getStructureVaoPicking()
    {
        String key = this.getVaoCacheKey();

        if (key == null)
        {
            return null;
        }

        VaoHolder holder = VAO_CACHE.get(key);

        return holder != null ? holder.picking : null;
    }

    private String getVaoCacheKey()
    {
        if (this.lastFile == null)
        {
            return null;
        }

        StructureLightSettings sl = this.form.structureLight.getRuntimeValue();
        boolean emit = sl != null ? sl.enabled : this.form.emitLight.get();
        int intensity = sl != null ? sl.intensity : this.form.lightIntensity.get();
        String biome = this.form.biomeId.get();

        if (biome == null)
        {
            biome = "";
        }

        return this.lastFile + "|" + (emit ? 1 : 0) + "|" + intensity + "|b" + biome + "|r" + LIGHTING_REVISION;
    }

    private void clearCachedVao()
    {
        String key = this.lastVaoCacheKey != null ? this.lastVaoCacheKey : this.getVaoCacheKey();

        if (key == null)
        {
            return;
        }

        VaoHolder holder = VAO_CACHE.remove(key);

        StructureFormRenderer.deleteHolderVaos(holder);
        this.lastVaoCacheKey = null;
    }

    /**
     * Routes structure bake vertices into 16³ chunk collectors for frustum culling.
     */
    private static final class CaptureChunkBus
    {
        private final Map<Long, StructureVAOCollector> collectors = new LinkedHashMap<>();
        private final Map<Long, LightmapStructureVAOCollector> lights = new LinkedHashMap<>();
        private long currentKey = Long.MIN_VALUE;
        private LightmapStructureVAOCollector current;

        void setBlock(class_2338 pos)
        {
            long key = class_2338.method_10064(
                Math.floorDiv(pos.method_10263(), CHUNK_SIZE),
                Math.floorDiv(pos.method_10264(), CHUNK_SIZE),
                Math.floorDiv(pos.method_10260(), CHUNK_SIZE)
            );

            if (key == this.currentKey && this.current != null)
            {
                return;
            }

            this.currentKey = key;
            this.current = this.lights.computeIfAbsent(key, k ->
            {
                StructureVAOCollector collector = new StructureVAOCollector();

                collector.setComputeTangents(false);
                this.collectors.put(k, collector);

                return new LightmapStructureVAOCollector(collector);
            });
        }

        class_4588 consumer()
        {
            if (this.current == null)
            {
                this.setBlock(class_2338.field_10980);
            }

            return this.current;
        }

        List<VaoChunk> buildChunks(float pivotX, float pivotY, float pivotZ)
        {
            List<VaoChunk> chunks = new ArrayList<>();

            for (Map.Entry<Long, StructureVAOCollector> entry : this.collectors.entrySet())
            {
                long key = entry.getKey();
                StructureVAOCollector collector = entry.getValue();
                LightmapStructureVAOCollector light = this.lights.get(key);
                ModelVAOData data = collector.toData();

                if (data.vertices().length < 9)
                {
                    continue;
                }

                int cx = class_2338.method_10061(key);
                int cy = class_2338.method_10071(key);
                int cz = class_2338.method_10083(key);
                float minX = cx * CHUNK_SIZE - pivotX;
                float minY = cy * CHUNK_SIZE - pivotY;
                float minZ = cz * CHUNK_SIZE - pivotZ;
                float maxX = minX + CHUNK_SIZE;
                float maxY = minY + CHUNK_SIZE;
                float maxZ = minZ + CHUNK_SIZE;
                IModelVAO vao = new LightmapModelVAO(data, light.getLightmapData());

                chunks.add(new VaoChunk(vao, minX, minY, minZ, maxX, maxY, maxZ));
            }

            return chunks;
        }
    }

    private static class LightmapStructureVAOCollector implements class_4588
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
        public class_4588 method_22912(float x, float y, float z)
        {
            this.delegate.method_22912(x, y, z);
            return this;
        }

        @Override
        public class_4588 method_1336(int red, int green, int blue, int alpha)
        {
            this.delegate.method_1336(red, green, blue, alpha);
            return this;
        }

        @Override
        public class_4588 method_22913(float u, float v)
        {
            this.delegate.method_22913(u, v);
            return this;
        }

        @Override
        public class_4588 method_60796(int u, int v)
        {
            this.delegate.method_60796(u, v);
            return this;
        }

        @Override
        public class_4588 method_22921(int u, int v)
        {
            this.quadLights[this.quadIndex] = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
            this.delegate.method_22921(u, v);
            return this;
        }

        @Override
        public class_4588 method_22914(float x, float y, float z)
        {
            this.delegate.method_22914(x, y, z);

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

    private void parseStructure(class_2487 root)
    {
        /* Size */
        if (root.method_10573("size", class_2520.field_33261))
        {
            int[] sz = root.method_10561("size");

            if (sz.length >= 3)
            {
                this.size = new class_2338(sz[0], sz[1], sz[2]);
            }
        }

        /* Palette -> state list */
        List<class_2680> paletteStates = new ArrayList<>();

        if (root.method_10573("palette", class_2520.field_33259))
        {
            class_2499 palette = root.method_10554("palette", class_2520.field_33260);

            for (int i = 0; i < palette.size(); i++)
            {
                class_2487 entry = palette.method_10602(i);
                class_2680 state = this.readBlockState(entry);

                paletteStates.add(state);
            }
        }

        /* Blocks */
        if (root.method_10573("blocks", class_2520.field_33259))
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            class_2499 list = root.method_10554("blocks", class_2520.field_33260);

            this.syncFancyGraphicsFromOptions();

            for (int i = 0; i < list.size(); i++)
            {
                class_2487 be = list.method_10602(i);
                class_2338 pos = this.readBlockPos(be.method_10554("pos", class_2520.field_33253));
                int stateIndex = be.method_10550("state");

                if (stateIndex >= 0 && stateIndex < paletteStates.size())
                {
                    class_2680 state = paletteStates.get(stateIndex);

                    if (state == null || state.method_26215())
                    {
                        continue;
                    }

                    class_2487 nbt = be.method_10573("nbt", class_2520.field_33260) ? be.method_10562("nbt") : null;
                    BlockEntry blockEntry = new BlockEntry(state, pos, nbt);

                    this.blocks.add(blockEntry);

                    class_1921 baseLayer = class_4696.method_23679(state);

                    if (baseLayer == class_1921.method_23581() || baseLayer == class_1921.method_23579())
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

                    if (state.method_26204() instanceof class_2397)
                    {
                        this.hasLeavesLayer = true;
                    }

                    if (this.isTranslucentBlock(state))
                    {
                        this.translucentBlocks.add(blockEntry);
                        this.hasTranslucentLayer = true;
                    }

                    if (state.method_26204() instanceof class_2343)
                    {
                        this.blockEntitiesList.add(blockEntry);
                        this.hasBlockEntityLayer = true;
                    }

                    /* Update bounds */
                    if (pos.method_10263() < minX) minX = pos.method_10263();
                    if (pos.method_10264() < minY) minY = pos.method_10264();
                    if (pos.method_10260() < minZ) minZ = pos.method_10260();
                    if (pos.method_10263() > maxX) maxX = pos.method_10263();
                    if (pos.method_10264() > maxY) maxY = pos.method_10264();
                    if (pos.method_10260() > maxZ) maxZ = pos.method_10260();
                }
            }

            if (!this.blocks.isEmpty())
            {
                this.boundsMin = new class_2338(minX, minY, minZ);
                this.boundsMax = new class_2338(maxX, maxY, maxZ);
            }
        }
    }

    private class_2338 readBlockPos(class_2499 list)
    {
        int x;
        int y;
        int z;

        if (list == null || list.size() < 3)
        {
            return class_2338.field_10980;
        }

        x = list.method_10600(0);
        y = list.method_10600(1);
        z = list.method_10600(2);

        return new class_2338(x, y, z);
    }

    private class_2680 readBlockState(class_2487 entry)
    {
        String name = entry.method_10558("Name");
        class_2248 block;
        class_2680 state;

        try
        {
            class_2960 id = class_2960.method_60654(name);

            block = class_7923.field_41175.method_10223(id);

            if (block == null)
            {
                block = class_2246.field_10124;
            }
        }
        catch (Exception e)
        {
            block = class_2246.field_10124;
        }

        if ("minecraft:jigsaw".equals(name) || block == class_2246.field_16540)
        {
            return class_2246.field_10124.method_9564();
        }

        state = block.method_9564();

        if (entry.method_10573("Properties", class_2520.field_33260))
        {
            class_2487 props = entry.method_10562("Properties");

            for (String key : props.method_10541())
            {
                String value = props.method_10558(key);
                class_2769<?> property = block.method_9595().method_11663(key);

                if (property != null)
                {
                    Optional<?> parsed = property.method_11900(value);

                    if (parsed.isPresent())
                    {
                        try
                        {
                            @SuppressWarnings({"rawtypes", "unchecked"})
                            class_2769 raw = property;
                            @SuppressWarnings("unchecked")
                            Comparable c = (Comparable) parsed.get();

                            state = state.method_11657(raw, c);
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
        final class_2680 state;
        final class_2338 pos;
        final class_2487 nbt;

        BlockEntry(class_2680 state, class_2338 pos, class_2487 nbt)
        {
            this.state = state;
            this.pos = pos;
            this.nbt = nbt;
        }
    }

    private static class TransformingVertexConsumer implements class_4588
    {
        private final class_4588 parent;
        private final Matrix4f positionMatrix;
        private final Matrix3f normalMatrix;
        private final class_2338 offset;
        private final boolean injectOverlay;

        public TransformingVertexConsumer(class_4588 parent, class_4587.class_4665 entry, class_2338 offset, boolean injectOverlay)
        {
            this.parent = parent;
            this.positionMatrix = new Matrix4f(entry.method_23761());
            this.normalMatrix = new Matrix3f(entry.method_23762());
            this.offset = offset;
            this.injectOverlay = injectOverlay;
        }

        @Override
        public class_4588 method_22912(float x, float y, float z)
        {
            float nx = x - this.offset.method_10263();
            float ny = y - this.offset.method_10264();
            float nz = z - this.offset.method_10260();

            float tx = this.positionMatrix.m00() * nx + this.positionMatrix.m10() * ny + this.positionMatrix.m20() * nz + this.positionMatrix.m30();
            float ty = this.positionMatrix.m01() * nx + this.positionMatrix.m11() * ny + this.positionMatrix.m21() * nz + this.positionMatrix.m31();
            float tz = this.positionMatrix.m02() * nx + this.positionMatrix.m12() * ny + this.positionMatrix.m22() * nz + this.positionMatrix.m32();

            this.parent.method_22912(tx, ty, tz);
            return this;
        }

        @Override
        public class_4588 method_1336(int red, int green, int blue, int alpha)
        {
            this.parent.method_1336(red, green, blue, alpha);
            return this;
        }

        @Override
        public class_4588 method_22913(float u, float v)
        {
            this.parent.method_22913(u, v);
            return this;
        }

        @Override
        public class_4588 method_60796(int u, int v)
        {
            this.parent.method_60796(u, v);
            return this;
        }

        @Override
        public class_4588 method_22921(int u, int v)
        {
            if (this.injectOverlay)
            {
                this.parent.method_60796(0, 10);
            }
            this.parent.method_22921(u, v);
            return this;
        }

        @Override
        public class_4588 method_22914(float x, float y, float z)
        {
            float tx = this.normalMatrix.m00() * x + this.normalMatrix.m10() * y + this.normalMatrix.m20() * z;
            float ty = this.normalMatrix.m01() * x + this.normalMatrix.m11() * y + this.normalMatrix.m21() * z;
            float tz = this.normalMatrix.m02() * x + this.normalMatrix.m12() * y + this.normalMatrix.m22() * z;

            this.parent.method_22914(tx, ty, tz);
            return this;
        }
    }
}
