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
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
        // context.batcher.getContext().draw();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        PoseStack matrices = new PoseStack();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.pushPose();
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
        // RenderSystem.setupLevelDiffuseLighting(light0, light1);

        consumers.setSubstitute(this.getBlockMainConsumer(set, resolvedPaint));
        consumers.setUI(true);
        this.renderRepeatedBlocks(null, matrices, consumers, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, false, true, false, false);

        consumers.draw();

        if (positivePaint && !blockEntityVisual)
        {
            this.submitDeferredBlockPaintOverlay(null, matrices, resolvedPaint, set.a, OverlayTexture.NO_OVERLAY, this.form.paintSettings.get().transform, glowSettings, legacyGlow, glowIntensity, true);
        }

        if (colorTransformWanted && !blockEntityVisual)
        {
            Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

            this.form.applyFormOpacity(overlayTint);
            this.renderBlockColorTintOverlay(null, matrices, overlayTint, set.a, OverlayTexture.NO_OVERLAY, true, storedFormColor);
        }

        if (glowIntensity > 0F && !glowSettings.resolvePaintOnly() && !blockEntityVisual)
        {
            this.renderGlowOverlay(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, OverlayTexture.NO_OVERLAY, true);
        }

        consumers.setUI(false);
        consumers.setSubstitute(null);

        // DiffuseLighting.disableGuiDepthLighting();

        matrices.popPose();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        context.stack.pushPose();

        try
        {
            if (context.isPicking())
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    /* Unit pick cubes need both faces; culling clipped the volume to a flat slab. */
                    GlStateManager._disableCull();
                });

                light = 0;
                /* Form opacity / blend intensity must not discard pick pixels (picker_models a < 0.1). */
                consumers.setSubstitute(BBSRendering.getColorConsumer(new Color(1F, 1F, 1F, 1F)));
            }
            else
            {
                CustomVertexConsumerProvider.hijackVertexFormat((l) ->
                {
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(770, 771, 1, 0);
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

            if (positiveGlow && !glowSettings.resolvePaintOnly() && !blockEntityVisual)
            {
                this.renderGlowOverlay(context, context.stack, consumers, glowSettings, legacyGlow, glowIntensity, color.a, context.overlay, false);
            }
            else
            {
                CustomVertexConsumerProvider.clearRunnables();
            }

            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        }
        finally
        {
            if (context.isPicking())
            {
                GlStateManager._enableCull();
                CustomVertexConsumerProvider.clearRunnables();
            }

            context.stack.popPose();
        }

        GlStateManager._enableDepthTest();
    }

    private Function<VertexConsumer, VertexConsumer> getBlockMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void renderRepeatedBlocks(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay)
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
                    stack.pushPose();
                    stack.translate(startX + x, startY + y, startZ + z);

                    int blockLight = light;

                    if (!glowOverlay && context != null)
                    {
                        blockLight = this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);
                    }

                    this.renderSingleBlock(stack, consumers, blockLight, overlay, picking, ui, glowOverlay, paintOverlay);
                    stack.popPose();
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

        Level world = null;

        if (context.entity != null)
        {
            world = context.entity.getWorld();
        }

        if (world == null)
        {
            world = Minecraft.getInstance().level;
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

        int sampled = LevelRenderer.getLightColor(world, blockPos);
        float lf = 1F - MathUtils.clamp(this.form.lighting.get(), 0F, 1F);
        int u = sampled & '\uffff';
        int v = sampled >> 16 & '\uffff';

        u = (int) Lerps.lerp(u, LightTexture.FULL_BLOCK, lf);

        return u | v << 16;
    }

    private BlockPos getRepeatBlockWorldPos(FormRenderingContext context, int localX, int localY, int localZ)
    {
        if (context.world != null)
        {
            PoseStack probe = new PoseStack();

            probe.last().pose().set(context.world.last().pose());
            probe.translate(localX, localY, localZ);

            Vector3f translation = probe.last().pose().getTranslation(new Vector3f());

            return BlockPos.containing(translation.x, translation.y + 0.5D, translation.z);
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

        return BlockPos.containing(x, y, z);
    }

    private void renderSingleBlock(PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay)
    {
        stack.pushPose();
        stack.translate(-0.5F, 0F, -0.5F);

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
            savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GlStateManager._depthMask(false);
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
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockState, stack, consumers, light, overlay);

                /* Skip BE on paint / color-tint / glow overlay redraw — those shaders expect block atlas. */
                if (!picking && !glowOverlay && !paintOverlay)
                {
                    this.renderBlockEntity(stack, consumers, light, overlay, false);
                }

                int breakingLevel = this.form.breaking.get();

                if (!picking && !glowOverlay && !paintOverlay && breakingLevel > 0 && breakingLevel <= 10)
                {
                    RenderType crackingLayer = ModelBakery.DESTROY_TYPES.get(breakingLevel - 1);
                    VertexConsumer delegateConsumer = consumers.getBuffer(crackingLayer);
                    VertexConsumer crackingConsumer = new SheetedDecalTextureGenerator(delegateConsumer, stack.last(), 1.0F);
                    Function<VertexConsumer, VertexConsumer> previousSubstitute = consumers.getSubstitute();

                    consumers.setSubstitute((vertexConsumer) -> crackingConsumer);

                    try
                    {
                        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(this.form.blockState.get(), stack, consumers, light, overlay);
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
                GlStateManager._depthMask(savedDepthMask);
            }
        }

        stack.popPose();
    }

    private boolean isTranslucentBlockState(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        return !state.isSolidRender();
    }

    private boolean needsPickVolume(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        /* Signs / hanging signs / chests / beds / … — animated or invisible mesh, or any BE. */
        if (state.getRenderShape() == RenderShape.INVISIBLE
            || state.getBlock() instanceof EntityBlock)
        {
            return true;
        }

        try
        {
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());

            if (shape.isEmpty())
            {
                return true;
            }

            AABB box = shape.bounds();

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
    private void renderPickVolume(PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        GlStateManager._disableCull();

        VertexConsumer buffer = consumers.getBuffer(RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose entry = stack.last();
        Matrix4f matrix = entry.pose();
        float[] uv = this.getOpaquePickUv();

        this.emitPickCube(buffer, entry, matrix, 0F, 0F, 0F, 1F, 1F, 1F, uv[0], uv[1], light, overlay);
    }

    private float[] getOpaquePickUv()
    {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
            .getAtlasOrThrow(AtlasIds.BLOCKS)
            .getSprite(Identifier.fromNamespaceAndPath("minecraft", "block/white_concrete"));
        float u = (sprite.getU0() + sprite.getU1()) * 0.5F;
        float v = (sprite.getV0() + sprite.getV1()) * 0.5F;

        return new float[] {u, v};
    }

    private void emitPickCube(VertexConsumer buffer, PoseStack.Pose entry, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, int light, int overlay)
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

    private void emitPickQuad(VertexConsumer buffer, PoseStack.Pose entry, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz, float u, float v, int light, int overlay)
    {
        buffer.addVertex(matrix, x0, y0, z0).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz);
        buffer.addVertex(matrix, x1, y1, z1).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz);
        buffer.addVertex(matrix, x2, y2, z2).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz);
        buffer.addVertex(matrix, x3, y3, z3).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz);
    }

    private boolean isBlockEntityVisual()
    {
        BlockState state = this.form.blockState.get();

        if (state == null)
        {
            return false;
        }

        return state.getBlock() instanceof EntityBlock
            || state.getRenderShape() == RenderShape.INVISIBLE;
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
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.last().pose()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.last().normal());

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
                // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            }
        });
    }

    private void renderRepeatedBlockEntitiesTinted(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
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
                    stack.pushPose();
                    stack.translate(startX + x, startY + y, startZ + z);
                    stack.translate(-0.5F, 0F, -0.5F);

                    int blockLight = light;

                    if (context != null)
                    {
                        blockLight = this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);
                    }

                    this.renderBlockEntity(stack, consumers, blockLight, overlay, true);
                    stack.popPose();
                }
            }
        }
    }

    private void renderBlockEntity(PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean forceTint)
    {
        if (!(this.form.blockState.get().getBlock() instanceof EntityBlock provider))
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        BlockEntity blockEntity = provider.newBlockEntity(BlockPos.ZERO, this.form.blockState.get());

        if (blockEntity == null)
        {
            return;
        }

        if (client.level != null)
        {
            blockEntity.setLevel(client.level);
        }

        BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();
        @SuppressWarnings({"rawtypes", "unchecked"})
        BlockEntityRenderer renderer = dispatcher.getRenderer(blockEntity);

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
            @SuppressWarnings({"rawtypes", "unchecked"})
            var renderState = renderer.createRenderState();
            if (renderState != null)
            {
                renderer.extractRenderState(blockEntity, renderState, 0F, Vec3.ZERO, null);
                renderer.submit(renderState, stack, null, null);
            }
        }
        catch (Exception e)
        {
        }
    }

    private void submitDeferredBlockColorTintOverlay(FormRenderingContext context, PoseStack stack, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.last().pose()));
        Matrix3f normalMatrix = new Matrix3f(stack.last().normal());
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

    private void renderBlockColorTintOverlay(FormRenderingContext context, PoseStack stack, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

        this.renderColorTintOverlayPass(context, stack, consumers, formColor, alpha, overlay, ui, gradeSource);
    }

    private void renderColorTintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.last().pose()).invert();
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configureColorTintOverlayRenderState(formRootInverse, formColor.transform, true, formColor, 0.5F, gradeSource));

        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(false);
        /* Pull tint overlay toward camera so it does not z-fight the main block pass. */
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -2F);

        /* Neutral vertices — lighting lives in the scene copy when grading. */
        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, false, ui, false, true);
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
            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void submitDeferredBlockPaintOverlay(FormRenderingContext context, PoseStack stack, Color resolvedPaint, float alpha, int overlay, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean ui)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.last().pose()));
        Matrix3f normalMatrix = new Matrix3f(stack.last().normal());
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

    private void renderPaintOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform)
    {
        this.renderPaintOverlay(context, stack, consumers, resolvedPaint, alpha, overlay, ui, transform, null, null, 0F);
    }

    private void renderPaintOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, glowSettings, legacyGlow, glowIntensity, alpha);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, null, null, 0F, 1F);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.last().pose()).invert();

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configurePaintOverlayRenderState(formRootInverse, transform, true, glowSettings, legacyGlow, glowIntensity, alpha));

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager._depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, false, ui, false, true);
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            GlStateManager._depthMask(true);
            // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void renderGlowOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui)
    {
        Color glowColor = FormColorEffects.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0);
        GlStateManager._depthMask(false);
        // RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

        consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, false, ui, true, false);
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            GlStateManager._depthMask(true);
            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        }
    }
}
