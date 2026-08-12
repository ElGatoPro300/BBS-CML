package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.FlatGlowOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.Camera;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrailFormRenderer extends FormRenderer<TrailForm> implements ITickable 
{
    private final Map<Integer, Map<FormRenderType, ArrayDeque<Trail>>> recordsByInstance = new HashMap<>();
    /* Vanilla has no plain, depth-tested, translucent POSITION_TEXTURE pipeline anymore (see
     * .port_1.21.11_notes.md #5); the closest built-ins (GUI_TEXTURED, POSITION_TEX_COLOR_CELESTIAL, GLINT,
     * RENDERTYPE_WORLD_BORDER) either disable depth testing or bake a different blend mode. This wraps the
     * real vanilla "core/position_tex_color" shader (already shipped, used by GUI_TEXTURED) in our own
     * pipeline/RenderLayer with world-appropriate translucent + depth-tested state, mirroring how
     * BBSShaders builds its own pipelines. */
    private static RenderPipeline trailPipeline;
    private static RenderType trailLayer;

    /* Axes gizmo: opaque POSITION_COLOR triangles drawn without depth testing so the gizmo stays visible
     * on top while previewing in the model editor (old code bracketed the draw with
     * GlStateManager._disableDepthTest()/enableDepthTest(), which no longer exists as a mutable global toggle). */
    private static RenderPipeline axesPipeline;
    private static RenderType axesLayer;

    private final Map<FormRenderType, ArrayDeque<Trail>> record = new HashMap<>();
    private final Matrix4f formRootInverse = new Matrix4f();
    private final Vector3f maskLocal = new Vector3f();
    private int tick;

    public TrailFormRenderer(TrailForm form) 
    {
        super(form);
    }

    private ArrayDeque<Trail> getTrails(FormRenderType type, int trailInstance)
    {
        Map<FormRenderType, ArrayDeque<Trail>> byType = this.recordsByInstance.computeIfAbsent(trailInstance, (k) -> new HashMap<>());

        return byType.computeIfAbsent(type, (k) -> new ArrayDeque<>());
    }

    private static RenderType getTrailLayer()
    {
        if (trailPipeline == null)
        {
            trailPipeline = RenderPipelines.register(RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/trail"))
                .withVertexShader("core/position_tex_color")
                .withFragmentShader("core/position_tex_color")
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler0")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.DrawMode.QUADS)
                .build());
        }

        if (trailLayer == null)
        {
            RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(trailPipeline)
                .bufferSize(RenderType.BIG_BUFFER_SIZE)
                .sortOnUpload();

            trailLayer = RenderType.create(BBSMod.MOD_ID + "_trail", setup.createRenderSetup());
        }

        return trailLayer;
    }

    private static RenderType getAxesLayer()
    {
        if (axesPipeline == null)
        {
            axesPipeline = RenderPipelines.register(RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/trail_axes"))
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
                .build());
        }

        if (axesLayer == null)
        {
            RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(axesPipeline)
                .bufferSize(RenderType.BIG_BUFFER_SIZE);

            axesLayer = RenderType.create(BBSMod.MOD_ID + "_trail_axes", setup.createRenderSetup());
        }

        return axesLayer;
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2) 
    {
        Texture texture = context.render.getTextures().getTexture(this.form.texture.get());
        float min = Math.min(texture.width, texture.height);
        int ow = (x2 - x1) - 4;
        int oh = (y2 - y1) - 4;
        int w = (int) ((texture.width / min) * ow);
        int h = (int) ((texture.height / min) * ow);
        int x = x1 + (ow - w) / 2 + 2;
        int y = y1 + (oh - h) / 2 + 2;


        context.batcher.fullTexturedBox(texture, x, y, w, h);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        super.render3D(context);

        if (BBSRendering.isIrisShadowPass() || context.type == FormRenderType.ITEM_INVENTORY)
        {
            return;
        }

        if (context.modelRenderer || context.ui)
        {
            PoseStack stack = context.stack;
            float scale = BBSSettings.axesScale.get();
            float axisOffset = 0.01F * scale;
            float outlineSize = 1.01F;
            float outlineOffset = 0.02F * scale;

            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

            Draw.fillBox(builder, stack, -outlineOffset, -outlineSize, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
            Draw.fillBox(builder, stack, -axisOffset, -1F, -axisOffset, axisOffset, 1F, axisOffset, 0, 1, 0);
            GlStateManager._disableDepthTest();
            builder.buildOrThrow().close();
            GlStateManager._enableDepthTest();

            return;
        }

        if (!BBSRendering.isRenderingWorld())
        {
            return;
        }

        PoseStack stack = context.stack;
        Camera camera = context.camera;
        double baseX = camera.position.x;
        double baseY = camera.position.y;
        double baseZ = camera.position.z;
        float current = (float) this.tick + context.transition;
        ArrayDeque<Trail> trails = this.getTrails(context.type, context.trailInstance);

        if (!this.form.paused.get())
        {
            Matrix4f modelPosMatrix = new Matrix4f(stack.last().pose());
            Vector4f topVec = new Vector4f(0F, 1F, 0F, 1F);
            Vector4f bottomVec = new Vector4f(0F, -1F, 0F, 1F);

            modelPosMatrix.transform(topVec);
            modelPosMatrix.transform(bottomVec);

            Trail record = new Trail();
            record.tick = current;
            record.top = new Vector3d(topVec.x + baseX, topVec.y + baseY, topVec.z + baseZ);
            record.bottom = new Vector3d(bottomVec.x + baseX, bottomVec.y + baseY, bottomVec.z + baseZ);
            record.stop = new Vector3f((float) (topVec.x - bottomVec.x), (float) (topVec.y - bottomVec.y), (float) (topVec.z - bottomVec.z)).lengthSquared() < 1.0E-4D;

            /* Same frame may re-render (illusion streaks); keep one sample per tick. */
            Trail last = trails.peekLast();

            if (last != null && Math.abs(last.tick - current) < 0.0001F)
            {
                trails.removeLast();
            }

            trails.addLast(record);
        }

        boolean loop = this.form.loop.get();
        float length = this.form.length.get();
        float end = current - length;
        Iterator<Trail> it = trails.iterator();
        boolean hasSomethingToRender = false;
        boolean lastStop = true;

        while (it.hasNext())
        {
            Trail trail = it.next();

            if (trail.tick < end)
            {
                it.remove();
            }
            else
            {
                hasSomethingToRender |= !trail.stop && !lastStop;
                lastStop = trail.stop;
            }
        }

        if (!hasSomethingToRender || trails.size() <= 1 || !(length > 0.001D))
        {
            return;
        }

        Link defaultTexture = this.form.texture.get();
        Color storedFormColor = this.form.color.get();
        Color blendedTint = new Color().set(context.color, true);
        Color unblendedTint = new Color().set(context.color, true);

        /* When color Transform is active, mask tint in form-local space per vertex. */
        blendedTint.mul(storedFormColor.copyBakingColorGrade());
        FormColorEffects.applyShadowPassColorFix(blendedTint, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), context.isShadowPass || BBSRendering.isIrisShadowPass());
        FormColorEffects.applyShadowPassColorFix(unblendedTint, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), context.isShadowPass || BBSRendering.isIrisShadowPass());

        if (blendedTint.a <= 0.001F && !context.isShadowPass && !BBSRendering.isIrisShadowPass())
        {
            return;
        }

        this.formRootInverse.set(stack.last().pose()).invert();

        FormTextureBlendRenderer.draw(this.form.textureBlend, defaultTexture, (link, alphaFactor) ->
        {
            this.renderTrailPass(stack, trails, loop, length, current, baseX, baseY, baseZ, link, unblendedTint, blendedTint, alphaFactor);
        });
    }

    private void renderTrailPass(PoseStack stack, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Link textureLink, Color unblendedTint, Color blendedTint, float alphaFactor)
    {
        if (textureLink == null)
        {
            return;
        }

        BBSModClient.getTextures().bindTexture(textureLink);
        stack.pushPose();

        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        float paintStrength = paintSettings.resolveIntensity(legacyPaint);
        boolean positivePaint = FormColorEffects.hasPositivePaint(paintSettings, legacyPaint);
        Color resolvedPaint = positivePaint ? FormColorEffects.resolvePaintColor(paintSettings, legacyPaint) : null;
        EffectTransform colorTransform = this.form.color.get().transform;
        EffectTransform paintTransform = paintSettings.transform;

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        Color unblended = unblendedTint.copy();
        Color blended = blendedTint.copy();

        unblended.a *= alphaFactor;
        blended.a *= alphaFactor;

        if (paintStrength < 0F)
        {
            FormColorEffects.applyPaintBlend(unblended, paintSettings, legacyPaint);
            FormColorEffects.applyPaintBlend(blended, paintSettings, legacyPaint);
        }

        if (glowIntensity < 0F)
        {
            FormColorEffects.blendFormGlowBrighten(unblended, glowSettings, legacyGlow);
            FormColorEffects.blendFormGlowBrighten(blended, glowSettings, legacyGlow);
        }

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f identityMatrix = new Matrix4f();

        this.buildTrailQuads(builder, identityMatrix, trails, loop, length, current, baseX, baseY, baseZ, unblended, blended, colorTransform);

        // RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        builder.buildOrThrow().close();

        if (positivePaint)
        {
            this.submitDeferredTrailPaintOverlay(stack, trails, loop, length, current, baseX, baseY, baseZ, textureLink, resolvedPaint, blended.a, paintTransform);
        }

        if (glowIntensity > 0F)
        {
            this.renderGlowOverlay(tessellator, identityMatrix, trails, loop, length, current, baseX, baseY, baseZ, glowSettings, legacyGlow, blended.a, glowIntensity, this.resolveGlowEffectTransform(glowSettings, legacyGlow));
        }

        GlStateManager._enableDepthTest();
        stack.popPose();
    }

    private void submitDeferredTrailPaintOverlay(PoseStack stack, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Link textureLink, Color resolvedPaint, float alpha, EffectTransform paintTransform)
    {
        ArrayDeque<Trail> trailSnapshot = this.copyTrails(trails);
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);
        Matrix4f paintMatrix = new Matrix4f(stack.last().pose());
        EffectTransform paintTransformSnapshot = paintTransform == null ? null : paintTransform.copy();
        Matrix4f formRootInverseSnapshot = new Matrix4f(this.formRootInverse);

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(
            RenderSystem.getProjectionMatrixBuffer(),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
            false,
            () ->
            {
                this.formRootInverse.set(formRootInverseSnapshot);
                BBSModClient.getTextures().bindTexture(textureLink);
                this.renderPaintOverlayPass(trailSnapshot, loop, length, current, baseX, baseY, baseZ, paintOverlay, paintMatrix, paintTransformSnapshot);
            }
        );
    }

    private ArrayDeque<Trail> copyTrails(ArrayDeque<Trail> trails)
    {
        ArrayDeque<Trail> copy = new ArrayDeque<>();

        for (Trail trail : trails)
        {
            Trail snapshot = new Trail();

            snapshot.tick = trail.tick;
            snapshot.stop = trail.stop;
            snapshot.top = new Vector3d(trail.top);
            snapshot.bottom = new Vector3d(trail.bottom);
            copy.addLast(snapshot);
        }

        return copy;
    }

    private void renderPaintOverlayPass(ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Color paintOverlay, Matrix4f vertexMatrix, EffectTransform paintTransform)
    {
        Tesselator tessellator = Tesselator.getInstance();

        FlatPaintOverlayPass.render(() ->
        {
            BufferBuilder paintBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            int paintLight = LightTexture.FULL_BRIGHT;
            int overlay = OverlayTexture.NO_OVERLAY;

            this.buildTrailPaintQuads(paintBuilder, vertexMatrix, trails, loop, length, current, baseX, baseY, baseZ, paintOverlay, overlay, paintLight, paintTransform);
            paintBuilder.buildOrThrow().close();
        });
    }

    private void renderGlowOverlay(Tesselator tessellator, Matrix4f matrix, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, EffectTransform glowTransform)
    {
        FlatGlowOverlayPass.render(glowSettings, legacyGlow, alpha, glowIntensity, (glowColor) ->
        {
            /* Outside the mask: fully transparent; inside: full glow. Same soft volume as Color/Paint. */
            Color glowOutside = glowColor.copy();

            glowOutside.a = 0F;

            BufferBuilder glowBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            // RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            this.buildTrailQuads(glowBuilder, matrix, trails, loop, length, current, baseX, baseY, baseZ, glowOutside, glowColor, glowTransform);
            glowBuilder.end().close();
        });
    }

    /**
     * Prefer {@link GlowSettings#transform}; fall back to legacy {@code glowingColor.transform}.
     */
    private EffectTransform resolveGlowEffectTransform(GlowSettings glow, Color legacyGlow)
    {
        if (glow != null && glow.transform != null && glow.transform.isActive())
        {
            return glow.transform;
        }

        if (legacyGlow != null && legacyGlow.hasActiveTransform())
        {
            return legacyGlow.transform;
        }

        if (glow != null && glow.transform != null)
        {
            return glow.transform;
        }

        if (legacyGlow != null && legacyGlow.transform != null)
        {
            return legacyGlow.transform;
        }

        return new EffectTransform();
    }

    private void buildTrailQuads(BufferBuilder builder, Matrix4f matrix, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Color unblended, Color blended, EffectTransform colorTransform)
    {
        Trail lastTrail = null;

        for (Iterator<Trail> trailIt = trails.iterator(); trailIt.hasNext(); )
        {
            Trail trail = trailIt.next();

            if (lastTrail != null && !lastTrail.stop && !trail.stop)
            {
                float x1 = (float) (trail.top.x - baseX);
                float x2 = (float) (trail.bottom.x - baseX);
                float x3 = (float) (lastTrail.bottom.x - baseX);
                float x4 = (float) (lastTrail.top.x - baseX);

                float y1 = (float) (trail.top.y - baseY);
                float y2 = (float) (trail.bottom.y - baseY);
                float y3 = (float) (lastTrail.bottom.y - baseY);
                float y4 = (float) (lastTrail.top.y - baseY);

                float z1 = (float) (trail.top.z - baseZ);
                float z2 = (float) (trail.bottom.z - baseZ);
                float z3 = (float) (lastTrail.bottom.z - baseZ);
                float z4 = (float) (lastTrail.top.z - baseZ);

                float u1 = loop ? trail.tick / length : (current - trail.tick) / length;
                float u2 = loop ? lastTrail.tick / length : (current - lastTrail.tick) / length;

                /* Front face */
                builder.addVertex(matrix, x1, y1, z1).setUv(u1, 0F).setColor(1F, 1F, 1F, 1F);
                builder.addVertex(matrix, x2, y2, z2).setUv(u1, 1F).setColor(1F, 1F, 1F, 1F);
                builder.addVertex(matrix, x3, y3, z3).setUv(u2, 1F).setColor(1F, 1F, 1F, 1F);
                builder.addVertex(matrix, x4, y4, z4).setUv(u2, 0F).setColor(1F, 1F, 1F, 1F);

                /* Back face */
                builder.addVertex(matrix, x4, y4, z4).setUv(u2, 0F).setColor(1F, 1F, 1F, 1F);
                builder.addVertex(matrix, x3, y3, z3).setUv(u2, 1F).setColor(1F, 1F, 1F, 1F);
                builder.addVertex(matrix, x2, y2, z2).setUv(u1, 1F).setColor(1F, 1F, 1F, 1F);
                builder.addVertex(matrix, x1, y1, z1).setUv(u1, 0F).setColor(1F, 1F, 1F, 1F);
            }

            lastTrail = trail;
        }
    }

    private void buildTrailPaintQuads(BufferBuilder builder, Matrix4f matrix, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Color color, int overlay, int light, EffectTransform paintTransform)
    {
        Trail lastTrail = null;

        for (Iterator<Trail> trailIt = trails.iterator(); trailIt.hasNext(); )
        {
            Trail trail = trailIt.next();

            if (lastTrail != null && !lastTrail.stop && !trail.stop)
            {
                float u1 = loop ? trail.tick / length : (current - trail.tick) / length;
                float u2 = loop ? lastTrail.tick / length : (current - lastTrail.tick) / length;

                this.addTrailPaintSegment(builder, matrix, trail, lastTrail, baseX, baseY, baseZ, u1, u2, color, overlay, light, paintTransform);
            }

            lastTrail = trail;
        }
    }

    private void addTrailSegment(BufferBuilder builder, Matrix4f matrix, Trail trail, Trail lastTrail, double baseX, double baseY, double baseZ, float u1, float u2, Color unblended, Color blended, EffectTransform colorTransform)
    {
        float x1 = (float) (trail.top.x - baseX);
        float x2 = (float) (trail.bottom.x - baseX);
        float x3 = (float) (lastTrail.bottom.x - baseX);
        float x4 = (float) (lastTrail.top.x - baseX);

        float y1 = (float) (trail.top.y - baseY);
        float y2 = (float) (trail.bottom.y - baseY);
        float y3 = (float) (lastTrail.bottom.y - baseY);
        float y4 = (float) (lastTrail.top.y - baseY);

        float z1 = (float) (trail.top.z - baseZ);
        float z2 = (float) (trail.bottom.z - baseZ);
        float z3 = (float) (lastTrail.bottom.z - baseZ);
        float z4 = (float) (lastTrail.top.z - baseZ);

        this.fillTrailVertex(builder, matrix, x1, y1, z1, u1, 0F, unblended, blended, colorTransform);
        this.fillTrailVertex(builder, matrix, x2, y2, z2, u1, 1F, unblended, blended, colorTransform);
        this.fillTrailVertex(builder, matrix, x3, y3, z3, u2, 1F, unblended, blended, colorTransform);
        this.fillTrailVertex(builder, matrix, x4, y4, z4, u2, 0F, unblended, blended, colorTransform);

        this.fillTrailVertex(builder, matrix, x4, y4, z4, u2, 0F, unblended, blended, colorTransform);
        this.fillTrailVertex(builder, matrix, x3, y3, z3, u2, 1F, unblended, blended, colorTransform);
        this.fillTrailVertex(builder, matrix, x2, y2, z2, u1, 1F, unblended, blended, colorTransform);
        this.fillTrailVertex(builder, matrix, x1, y1, z1, u1, 0F, unblended, blended, colorTransform);
    }

    private void addTrailPaintSegment(BufferBuilder builder, Matrix4f matrix, Trail trail, Trail lastTrail, double baseX, double baseY, double baseZ, float u1, float u2, Color color, int overlay, int light, EffectTransform paintTransform)
    {
        float x1 = (float) (trail.top.x - baseX);
        float x2 = (float) (trail.bottom.x - baseX);
        float x3 = (float) (lastTrail.bottom.x - baseX);
        float x4 = (float) (lastTrail.top.x - baseX);

        float y1 = (float) (trail.top.y - baseY);
        float y2 = (float) (trail.bottom.y - baseY);
        float y3 = (float) (lastTrail.bottom.y - baseY);
        float y4 = (float) (lastTrail.top.y - baseY);

        float z1 = (float) (trail.top.z - baseZ);
        float z2 = (float) (trail.bottom.z - baseZ);
        float z3 = (float) (lastTrail.bottom.z - baseZ);
        float z4 = (float) (lastTrail.top.z - baseZ);

        this.fillPaintVertex(builder, matrix, x1, y1, z1, u1, 0F, color, overlay, light, paintTransform);
        this.fillPaintVertex(builder, matrix, x2, y2, z2, u1, 1F, color, overlay, light, paintTransform);
        this.fillPaintVertex(builder, matrix, x3, y3, z3, u2, 1F, color, overlay, light, paintTransform);
        this.fillPaintVertex(builder, matrix, x4, y4, z4, u2, 0F, color, overlay, light, paintTransform);

        this.fillPaintVertex(builder, matrix, x4, y4, z4, u2, 0F, color, overlay, light, paintTransform);
        this.fillPaintVertex(builder, matrix, x3, y3, z3, u2, 1F, color, overlay, light, paintTransform);
        this.fillPaintVertex(builder, matrix, x2, y2, z2, u1, 1F, color, overlay, light, paintTransform);
        this.fillPaintVertex(builder, matrix, x1, y1, z1, u1, 0F, color, overlay, light, paintTransform);
    }

    private void fillTrailVertex(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, float u, float v, Color unblended, Color blended, EffectTransform colorTransform)
    {
        float mask = this.sampleMask(x, y, z, colorTransform);
        float r = unblended.r + (blended.r - unblended.r) * mask;
        float g = unblended.g + (blended.g - unblended.g) * mask;
        float b = unblended.b + (blended.b - unblended.b) * mask;
        float a = unblended.a + (blended.a - unblended.a) * mask;

        builder.addVertex(matrix, x, y, z).setUv(u, v).setColor(r, g, b, a);
    }

    private void fillPaintVertex(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, float u, float v, Color color, int overlay, int light, EffectTransform paintTransform)
    {
        float mask = this.sampleMask(x, y, z, paintTransform);

        builder.addVertex(matrix, x, y, z).setColor(color.r, color.g, color.b, color.a * mask).texture(u, v).overlay(overlay).light(light).normal(0F, 0F, 1F);
    }

    /**
     * Soft EffectTransform mask in current form-local space (emitter root).
     */
    private float sampleMask(float x, float y, float z, EffectTransform transform)
    {
        if (!EffectTransformMath.isTransformActive(transform))
        {
            return 1F;
        }

        this.maskLocal.set(x, y, z);
        this.formRootInverse.transformPosition(this.maskLocal);

        return EffectTransformMath.maskBillboard(this.maskLocal.x, this.maskLocal.y, this.maskLocal.z, transform);
    }

    @Override
    public void tick(IEntity entity)
    {
        this.tick += 1;

        float end = this.tick - Math.max(this.form.length.get(), 0F) - 1F;
        Iterator<Map.Entry<Integer, Map<FormRenderType, ArrayDeque<Trail>>>> instances = this.recordsByInstance.entrySet().iterator();

        while (instances.hasNext())
        {
            Map.Entry<Integer, Map<FormRenderType, ArrayDeque<Trail>>> instanceEntry = instances.next();
            Map<FormRenderType, ArrayDeque<Trail>> byType = instanceEntry.getValue();
            Iterator<Map.Entry<FormRenderType, ArrayDeque<Trail>>> types = byType.entrySet().iterator();

            while (types.hasNext())
            {
                Map.Entry<FormRenderType, ArrayDeque<Trail>> typeEntry = types.next();
                ArrayDeque<Trail> trails = typeEntry.getValue();

                while (!trails.isEmpty() && trails.peekFirst().tick < end)
                {
                    trails.removeFirst();
                }

                if (trails.isEmpty() && instanceEntry.getKey() != 0)
                {
                    types.remove();
                }
            }

            if (instanceEntry.getKey() != 0 && byType.isEmpty())
            {
                instances.remove();
            }
        }
    }

    public static class Trail
    {
        public Vector3d top;
        public Vector3d bottom;
        public float tick;
        public boolean stop;
    }
}
