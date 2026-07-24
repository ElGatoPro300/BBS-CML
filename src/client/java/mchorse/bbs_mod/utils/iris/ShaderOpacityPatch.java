package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.mixin.client.iris.IrisRenderingPipelineAccessor;

import net.minecraft.client.MinecraftClient;

import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTestFunction;
import net.irisshaders.iris.gl.texture.DepthCopyStrategy;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import net.irisshaders.iris.targets.RenderTargets;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

/**
 * Runtime opacity / film render-depth queue (Complementary / BSL patch optional).
 * Soft opacity and film {@code renderDepth} draw after translucent terrain with depth
 * writes — fluids stay, limbs do not X-ray — whether or not the pack patch is active.
 * Near-opaque without render depth stays on the live path for pack lighting.
 */
public class ShaderOpacityPatch
{
    public static final float LOW_ALPHA_TEST_REF = 0.0001F;

    private static final Pattern ALPHA_TEST_REF_COMPARE = Pattern.compile(
        "\\b([A-Za-z_][\\w.]*)\\.a\\s*<\\s*alphaTestRef\\b"
    );
    private static final Pattern LITERAL_POINT_ONE_COMPARE = Pattern.compile(
        "\\b([A-Za-z_][\\w.]*)\\.a\\s*<\\s*0\\.1\\b"
    );

    private static final String[] ALPHA_TEST_PASSES = {
        "gbuffers_entities",
        "gbuffers_entities_translucent",
        "gbuffers_block",
        "gbuffers_block_translucent",
        /* Billboard/shape deferred draws use position_tex_color → Iris basic/textured. */
        "gbuffers_basic",
        "gbuffers_textured",
        "gbuffers_textured_lit"
    };

    private static final List<PostDeferredEntry> postDeferredForms = new ArrayList<>();
    private static boolean postDeferredPhase;
    private static boolean flushingPostDeferred;
    private static boolean flushingDepthWrite = true;
    private static boolean forceLiveDepthWrite;
    private static boolean suppressLiveDepthWrite;

    private static String loadingPackName = "";

    private static final class PostDeferredEntry
    {
        private final double renderDepth;
        private final double distanceSq;
        private final boolean depthWrite;
        private final boolean afterFluids;
        private final boolean irisCamera;
        private final Matrix4f projection;
        private final Matrix4f modelView;
        private final Runnable draw;

        private PostDeferredEntry(double renderDepth, double distanceSq, boolean depthWrite, boolean afterFluids, boolean irisCamera, Matrix4f projection, Matrix4f modelView, Runnable draw)
        {
            this.renderDepth = renderDepth;
            this.distanceSq = distanceSq;
            this.depthWrite = depthWrite;
            this.afterFluids = afterFluids;
            this.irisCamera = irisCamera;
            this.projection = projection;
            this.modelView = modelView;
            this.draw = draw;
        }
    }

    public static void setLoadingPackName(String name)
    {
        loadingPackName = name == null ? "" : name;
    }

    public static String getLoadingPackName()
    {
        return loadingPackName == null ? "" : loadingPackName;
    }

    public static void clearLoadingPackName()
    {
        loadingPackName = "";
    }

    public static boolean isComplementaryPack(String name)
    {
        return name != null && name.toLowerCase(Locale.ROOT).contains("complementary");
    }

    public static boolean isBslPack(String name)
    {
        return name != null && name.toLowerCase(Locale.ROOT).contains("bsl");
    }

    /**
     * Global Iris translucency pipeline (post-deferred queue + generic Iris property patches).
     */
    public static boolean isActive()
    {
        if (BBSSettings.irisOpacityFix != null && BBSSettings.irisOpacityFix.get())
        {
            return true;
        }

        /* Legacy: old Complementary/BSL toggles before migration. */
        if (BBSSettings.complementaryOpacityFix != null && BBSSettings.complementaryOpacityFix.get()
            && isComplementaryPack(resolvePackName()))
        {
            return true;
        }

        return BBSSettings.bslOpacityFix != null && BBSSettings.bslOpacityFix.get()
            && isBslPack(resolvePackName());
    }

    /**
     * Pack-specific GLSL string rewrites (shadow caster dither, shadow opacity scaling).
     * Only Complementary / BSL source layouts are known; other packs skip these.
     */
    public static boolean shouldApplyPackGlslPatches()
    {
        if (!isActive())
        {
            return false;
        }

        String pack = resolvePackName();

        return isComplementaryPack(pack) || isBslPack(pack);
    }

    private static String resolvePackName()
    {
        if (loadingPackName != null && !loadingPackName.isEmpty())
        {
            return loadingPackName;
        }

        try
        {
            String current = net.irisshaders.iris.Iris.getCurrentPackName();

            return current == null ? "" : current;
        }
        catch (Throwable t)
        {
            return "";
        }
    }

    public static boolean isFlushingPostDeferred()
    {
        return flushingPostDeferred;
    }

    public static void setForceLiveDepthWrite(boolean force)
    {
        forceLiveDepthWrite = force;
    }

    public static void setSuppressLiveDepthWrite(boolean suppress)
    {
        suppressLiveDepthWrite = suppress;
    }

    public static void reassertPostDeferredDepthState()
    {
        if (flushingPostDeferred)
        {
            reassertPostDeferredDepthState(flushingDepthWrite);

            return;
        }

        if (forceLiveDepthWrite)
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
        }
        else if (suppressLiveDepthWrite)
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
        }
    }

    public static void reassertPostDeferredDepthState(boolean depthWrite)
    {
        if (!flushingPostDeferred)
        {
            return;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(depthWrite);
    }

    /**
     * True while Iris is in the post-deferred translucent phase (after clouds are composited).
     */
    public static boolean isPostDeferredPhase()
    {
        return postDeferredPhase;
    }

    /**
     * Fully opaque floor. Softer alpha joins the post-deferred queue (after VL clouds /
     * translucent terrain) with depth write so limbs do not X-ray and fluids stay intact.
     * Fully solid keeps the live path unless film {@code renderDepth} needs the sorted queue.
     */
    public static final float LIVE_DEPTH_WRITE_ALPHA = 0.999F;

    /**
     * Queue soft-opacity and film {@code renderDepth} forms until after translucent terrain.
     * Works with or without Iris and with or without the Complementary/BSL opacity patch —
     * patched packs get the best lighting; unpatched / no-shader still get correct depth
     * occlusion and no self X-ray. Never delay the shadow pass.
     */
    public static boolean shouldDelayUntilPostDeferred(float alpha)
    {
        return shouldDelayUntilPostDeferred(alpha, false);
    }

    public static boolean shouldDelayUntilPostDeferred(float alpha, boolean filmRenderDepth)
    {
        if (postDeferredPhase || flushingPostDeferred || alpha <= 0.001F)
        {
            return false;
        }

        try
        {
            /* Casters must hit the shadow map live — post-deferred never writes shadows. */
            if (BBSRendering.isIrisShadowPass())
            {
                return false;
            }
        }
        catch (Throwable t)
        {
            return false;
        }

        /* Soft opacity: after fluids + depth write (water stays, no self X-ray). */
        if (alpha < LIVE_DEPTH_WRITE_ALPHA)
        {
            return true;
        }

        /* Opaque film actors share the sorted post-deferred depth queue (shaders or not). */
        return filmRenderDepth;
    }

    public static boolean shouldJoinPostDeferredQueue(float alpha)
    {
        return shouldDelayUntilPostDeferred(alpha, true);
    }

    /**
     * Live-path fallback only. Soft opacity should already be post-deferred; if a draw still
     * lands live, keep depth writes so the mesh does not X-ray itself (screenshot at 254).
     */
    public static boolean shouldSuppressDepthWrite(float alpha)
    {
        return false;
    }

    /**
     * Soft opacity waits until after water/lava/portals. Near-opaque film depth stays early
     * (beginTranslucents) so it can occlude with depth before translucent terrain.
     */
    public static boolean shouldFlushAfterFluids(float alpha)
    {
        return alpha > 0.001F && alpha < LIVE_DEPTH_WRITE_ALPHA;
    }

    /**
     * Post-deferred meshes always write depth when visible so limbs do not X-ray themselves.
     * Soft forms still keep fluids: they flush {@link #shouldFlushAfterFluids after fluids},
     * so depth stamps cannot erase water/lava/portals already in the color buffer.
     */
    public static boolean shouldWriteDepthForOpacity(float alpha)
    {
        return alpha > 0.001F;
    }

    public static boolean shouldForceLiveDepthWrite(float alpha)
    {
        /* Near-opaque live path — force depth even if a pack left depthMask false. */
        return alpha >= LIVE_DEPTH_WRITE_ALPHA;
    }

    /**
     * Iris-lit mesh: restore camera ModelView; pass entity-local stack matrices in {@code draw}.
     */
    public static void submitPostDeferredForm(double renderDepth, boolean depthWrite, boolean afterFluids, Runnable draw)
    {
        submit(renderDepth, 0D, depthWrite, afterFluids, true, draw);
    }

    public static void submitPostDeferredForm(double renderDepth, double distanceSq, boolean depthWrite, boolean afterFluids, Runnable draw)
    {
        submit(renderDepth, distanceSq, depthWrite, afterFluids, true, draw);
    }

    /**
     * BBS model-shader flat: identity ModelView; pass camera-baked stack matrices in {@code draw}.
     */
    public static void submitPostDeferredBbsForm(double renderDepth, boolean depthWrite, boolean afterFluids, Runnable draw)
    {
        submit(renderDepth, 0D, depthWrite, afterFluids, false, draw);
    }

    public static void submitPostDeferredBbsForm(double renderDepth, double distanceSq, boolean depthWrite, boolean afterFluids, Runnable draw)
    {
        submit(renderDepth, distanceSq, depthWrite, afterFluids, false, draw);
    }

    public static void submitPostDeferredForm(Runnable draw)
    {
        submitPostDeferredForm(0D, 0D, true, false, draw);
    }

    private static void submit(double renderDepth, double distanceSq, boolean depthWrite, boolean afterFluids, boolean irisCamera, Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        postDeferredForms.add(new PostDeferredEntry(
            renderDepth,
            distanceSq,
            depthWrite,
            afterFluids,
            irisCamera,
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
            draw
        ));
    }

    public static void onBeginTranslucents()
    {
        /* Soft-opacity (and other after-fluids) forms must not flush here: Iris beginTranslucents
         * can run mid-frame while WorldRenderer still has an unbalanced pose stack; flushing
         * then throws IllegalStateException on pop(). Only mark the phase — actual soft-opacity
         * flush is WorldRenderEvents.AFTER_TRANSLUCENT / onAfterTranslucentTerrain().
         * Paint/blend/grade overlays stay queued until onWorldRenderEnd — the BBS model shader
         * only composites correctly on the final framebuffer after Iris finishes. */
        postDeferredPhase = true;
    }

    /**
     * After translucent terrain (water/lava/portals). Default soft forms (Opacity
     * "No shading" off) flush here with depth so pack body shadows stay; end-of-frame
     * paint stays clipped behind them. Noshading soft forms skip this queue and redraw
     * after paint in {@link ModelVAORenderer}'s deferred queue.
     */
    public static void onAfterTranslucentTerrain()
    {
        flushPostDeferredForms(null);
    }

    public static void onWorldRenderBegin()
    {
        postDeferredForms.clear();
        postDeferredPhase = false;
        flushingPostDeferred = false;
    }

    public static void onWorldRenderEnd()
    {
        flushPostDeferredForms(null);
        postDeferredPhase = false;
    }

    public static void flushPostDeferredForms()
    {
        flushPostDeferredForms(null);
    }

    /**
     * @param afterFluidsOnly {@code true} = soft opacity (after water/lava/portals);
     *                        {@code false} = early batch (beginTranslucents);
     *                        {@code null} = everything remaining (frame-end safety net).
     */
    private static void flushPostDeferredForms(Boolean afterFluidsOnly)
    {
        if (postDeferredForms.isEmpty())
        {
            return;
        }

        List<PostDeferredEntry> batch = new ArrayList<>();

        for (PostDeferredEntry entry : postDeferredForms)
        {
            if (afterFluidsOnly == null || entry.afterFluids == afterFluidsOnly)
            {
                batch.add(entry);
            }
        }

        if (batch.isEmpty())
        {
            return;
        }

        postDeferredForms.removeAll(batch);
        flushingPostDeferred = true;

        try
        {
            /* Same order as film entities: lower render depth first; within a depth, farther
             * first so closer forms depth-test against what is already in the buffer. */
            batch.sort(Comparator
                .comparingDouble((PostDeferredEntry entry) -> entry.renderDepth)
                .thenComparing((PostDeferredEntry a, PostDeferredEntry b) -> Double.compare(b.distanceSq, a.distanceSq))
            );

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc != null && mc.gameRenderer != null)
            {
                mc.gameRenderer.getLightmapTextureManager().enable();
                mc.gameRenderer.getOverlayTexture().setupOverlayColor();
            }

            for (PostDeferredEntry entry : batch)
            {
                runEntry(entry);
            }
        }
        finally
        {
            flushingPostDeferred = false;
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    /**
     * Complementary/BSL deferred can leave the live depth buffer unusable for occlusion. Iris
     * snapshots opaque depth into {@code depthtex1} at {@code beginTranslucents}; copy it back
     * so translucent BBS forms depth-test against models/terrain in front (render depth).
     *
     * @param bindIrisDefault when true, draw into Iris' translucent target (mid-pipeline only).
     *                        At world-render end keep Minecraft/film FB so draws stay visible.
     */
    private static void preparePostDeferredFramebufferAndDepth(boolean bindIrisDefault)
    {
        try
        {
            BBSRendering.ensurePaintOverlayTargetFramebuffer();

            WorldRenderingPipeline pipeline =
                net.irisshaders.iris.Iris.getPipelineManager().getPipelineNullable();

            if (!(pipeline instanceof IrisRenderingPipeline irisPipeline))
            {
                return;
            }

            IrisRenderingPipelineAccessor access = (IrisRenderingPipelineAccessor) irisPipeline;
            RenderTargets targets = access.bbs$renderTargets();

            if (targets == null)
            {
                return;
            }

            int width = targets.getCurrentWidth();
            int height = targets.getCurrentHeight();
            int opaqueDepth = targets.getDepthTextureNoTranslucents().getTextureId();
            int liveDepth = targets.getDepthTexture();

            if (width > 0 && height > 0 && opaqueDepth > 0 && liveDepth > 0)
            {
                DepthCopyStrategy.fastest(false)
                    .copy(null, opaqueDepth, null, liveDepth, width, height);
            }

            if (bindIrisDefault)
            {
                access.bbs$bindDefault();
            }
            else
            {
                /* Depth copy may have switched FBOs — return to the visible target. */
                BBSRendering.ensurePaintOverlayTargetFramebuffer();
            }
        }
        catch (Throwable ignored)
        {
            /* Iris API drift — still attempt draws with whatever depth is bound. */
        }
    }

    private static void runEntry(PostDeferredEntry entry)
    {
        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        Matrix4f savedModelView = new Matrix4f(modelViewStack);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean beganDeferredPass = false;

        try
        {
            RenderSystem.setProjectionMatrix(entry.projection, VertexSorter.BY_Z);
            flushingDepthWrite = entry.depthWrite;
            RenderSystem.depthMask(entry.depthWrite);

            /* Never push/pop ModelView during world render — unbalanced depth trips
             * WorldRenderer's "Pose stack not empty" check with Iris/Sodium. */
            if (entry.irisCamera)
            {
                modelViewStack.set(entry.modelView);
                RenderSystem.applyModelViewMatrix();
            }
            else
            {
                modelViewStack.identity();
                RenderSystem.applyModelViewMatrix();
                ModelVAORenderer.beginDeferredTranslucentModelPass(entry.depthWrite, true);
                beganDeferredPass = true;
            }

            reassertPostDeferredDepthState(entry.depthWrite);
            entry.draw.run();
        }
        finally
        {
            if (beganDeferredPass)
            {
                ModelVAORenderer.endDeferredTranslucentModelPass();
            }

            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.setProjectionMatrix(savedProjection, VertexSorter.BY_Z);
            modelViewStack.set(savedModelView);
            RenderSystem.applyModelViewMatrix();
        }
    }

    public static String patchPropertiesContents(String contents)
    {
        if (!isActive() || contents == null)
        {
            return contents;
        }

        if (contents.contains("separateEntityDraws"))
        {
            return contents.replaceAll("(?m)^\\s*separateEntityDraws\\s*=.*$", "separateEntityDraws=true");
        }

        return "separateEntityDraws=true\n" + contents;
    }

    public static void applyAlphaTestOverrides(ShaderProperties properties)
    {
        if (!isActive() || properties == null)
        {
            return;
        }

        Object2ObjectMap<String, AlphaTest> map = properties.getAlphaTestOverrides();
        AlphaTest low = new AlphaTest(AlphaTestFunction.GREATER, LOW_ALPHA_TEST_REF);

        for (String pass : ALPHA_TEST_PASSES)
        {
            map.put(pass, low);
        }
    }

    public static void applySeparateEntityDraws(Consumer<OptionalBoolean> setter)
    {
        if (!isActive() || setter == null)
        {
            return;
        }

        setter.accept(OptionalBoolean.TRUE);
    }

    public static String processSource(String source)
    {
        if (!isActive() || source == null || source.isEmpty())
        {
            return source;
        }

        String patched = source;

        /* Shadow casters: skip alpha-test rewrites (those hole foliage/terrain shadows), but
         * keep vertex-alpha dither so per-actor Opacity / shadow_opacity can fade ground
         * shadows on otherwise binary Iris shadow maps. */
        if (isShadowCasterSource(source))
        {
            return processShadowOpacity(processShadowCasterAlpha(patched));
        }

        /* Only relax alpha discards on gbuffer/entity paths. Do not rewrite translucentMult. */
        patched = ALPHA_TEST_REF_COMPARE.matcher(patched).replaceAll("$1.a < " + LOW_ALPHA_TEST_REF);
        patched = LITERAL_POINT_ONE_COMPARE.matcher(patched).replaceAll("$1.a < " + LOW_ALPHA_TEST_REF);

        return processShadowOpacity(patched);
    }

    public static boolean isShadowCasterSourcePublic(String source)
    {
        return isShadowCasterSource(source);
    }

    private static boolean isShadowCasterSource(String source)
    {
        return source.contains("DoNaturalShadowCalculation")
            || source.contains("float premult = float(mat > 0.98")
            || source.contains("BBS_SHADOW_CASTER_DITHER");
    }

    /**
     * Complementary/BSL shadow map programs: dither-discard by <b>vertex color alpha only</b>
     * so form Opacity and replay shadow_opacity fade per-actor ground shadows linearly
     * (coverage ≈ alpha). Does not multiply texture alpha (that made leaves/grass holey).
     */
    public static String processShadowCasterAlpha(String source)
    {
        if (!isActive() || source == null || source.isEmpty())
        {
            return source;
        }

        if (source.contains("BBS_SHADOW_CASTER_DITHER"))
        {
            return source;
        }

        /* Complementary shadow.glsl */
        if (source.contains("DoNaturalShadowCalculation") && source.contains("gl_FragData[0] = color1;"))
        {
            String dither =
                "/* BBS_SHADOW_CASTER_DITHER */\n"
                    + "    {\n"
                    + "        float bbsCasterAlpha = glColor.a;\n"
                    + "        if (bbsCasterAlpha < 0.999){\n"
                    + "            float bbsShadowDither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);\n"
                    + "            if (bbsShadowDither > bbsCasterAlpha) discard;\n"
                    + "        }\n"
                    + "    }\n";

            return source.replace(
                "    /* DRAWBUFFERS:0 */\n    gl_FragData[0] = color1; // Shadow Color",
                dither + "    /* DRAWBUFFERS:0 */\n    gl_FragData[0] = color1; // Shadow Color"
            );
        }

        /* BSL shadow.glsl */
        if (source.contains("float premult = float(mat > 0.98") && source.contains("gl_FragData[0] = albedo;"))
        {
            String dither =
                "\t/* BBS_SHADOW_CASTER_DITHER */\n"
                    + "\t{\n"
                    + "\t\tfloat bbsCasterAlpha = color.a;\n"
                    + "\t\tif (bbsCasterAlpha < 0.999){\n"
                    + "\t\t\tfloat bbsShadowDither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);\n"
                    + "\t\t\tif (bbsShadowDither > bbsCasterAlpha) discard;\n"
                    + "\t\t}\n"
                    + "\t}\n";

            if (source.contains("\tgl_FragData[0] = albedo;"))
            {
                return source.replace("\tgl_FragData[0] = albedo;", dither + "\tgl_FragData[0] = albedo;");
            }
        }

        return source;
    }

    /**
     * Injects {@code bbs_shader_shadow_opacity} into Complementary/BSL shaders that sample
     * shadow maps and scales sampled shadow visibility: 1 = full shadows, 0 = no shadows.
     */
    public static String processShadowOpacity(String source)
    {
        if (!shouldApplyPackGlslPatches() || source == null || source.isEmpty())
        {
            return source;
        }

        if (!containsShadowSampler(source))
        {
            return source;
        }

        ensureShadowOpacityVariable();

        String patched = insertShadowOpacityHelpers(source);

        patched = wrapShadowTextureCalls(patched, "texture");
        patched = wrapShadowTextureCalls(patched, "texture2D");
        patched = wrapShadowTextureCalls(patched, "textureLod");
        patched = wrapShadowTextureCalls(patched, "textureGrad");
        patched = wrapShadowTextureCalls(patched, "shadow2D");
        patched = wrapShadowTextureCalls(patched, "shadow2DLod");

        return patched;
    }

    public static void ensureShadowOpacityVariable()
    {
        if (!shouldApplyPackGlslPatches())
        {
            return;
        }

        ShaderCurves.ShaderVariable variable = ShaderCurves.variableMap.get(ShaderCurves.SHADER_SHADOW_OPACITY);

        if (variable == null)
        {
            variable = new ShaderCurves.ShaderVariable(ShaderCurves.SHADER_SHADOW_OPACITY, "1.0", false);
            ShaderCurves.variableMap.put(ShaderCurves.SHADER_SHADOW_OPACITY, variable);
        }

        syncShadowOpacityDefault(variable);
    }

    public static void syncShadowOpacityDefault()
    {
        ShaderCurves.ShaderVariable variable = ShaderCurves.variableMap.get(ShaderCurves.SHADER_SHADOW_OPACITY);

        if (variable != null)
        {
            syncShadowOpacityDefault(variable);
        }
    }

    private static void syncShadowOpacityDefault(ShaderCurves.ShaderVariable variable)
    {
        float value = 1F;

        if (BBSSettings.shaderShadowOpacity != null)
        {
            value = BBSSettings.shaderShadowOpacity.get();
        }

        variable.defaultValue = Math.max(0F, Math.min(1F, value));
    }

    private static boolean containsShadowSampler(String source)
    {
        return source.contains("shadowtex0")
            || source.contains("shadowtex1")
            || source.contains("shadowtex0HW")
            || source.contains("shadowtex1HW")
            || source.contains("waterShadow");
    }

    private static String insertShadowOpacityHelpers(String source)
    {
        String uniform = "bbs_" + ShaderCurves.SHADER_SHADOW_OPACITY;

        if (source.contains(uniform))
        {
            return source;
        }

        int version = source.indexOf("#version");

        if (version < 0)
        {
            return source;
        }

        int nextNewLine = source.indexOf('\n', version);

        if (nextNewLine < 0)
        {
            return source;
        }

        String helpers =
            "uniform float " + uniform + ";\n"
                + "#ifndef BBS_SHADOW_OPACITY_HELPERS\n"
                + "#define BBS_SHADOW_OPACITY_HELPERS\n"
                + "float bbsApplyShadowOpacity(float s){return mix(1.0,s," + uniform + ");}\n"
                + "vec2 bbsApplyShadowOpacity(vec2 s){return mix(vec2(1.0),s," + uniform + ");}\n"
                + "vec3 bbsApplyShadowOpacity(vec3 s){return mix(vec3(1.0),s," + uniform + ");}\n"
                + "vec4 bbsApplyShadowOpacity(vec4 s){return mix(vec4(1.0),s," + uniform + ");}\n"
                + "#endif\n";

        return source.substring(0, nextNewLine + 1) + helpers + source.substring(nextNewLine + 1);
    }

    /**
     * Wraps {@code func(shadowtexN...)} calls with {@code bbsApplyShadowOpacity(...)} so pack
     * lighting still runs, but shadow darkness scales with the BBS uniform / curve.
     */
    private static String wrapShadowTextureCalls(String source, String functionName)
    {
        String marker = "bbsApplyShadowOpacity(";
        StringBuilder out = new StringBuilder(source.length() + 64);
        int i = 0;

        while (i < source.length())
        {
            int found = indexOfIdentifierCall(source, functionName, i);

            if (found < 0)
            {
                out.append(source, i, source.length());
                break;
            }

            out.append(source, i, found);

            int open = found + functionName.length();

            while (open < source.length() && Character.isWhitespace(source.charAt(open)))
            {
                open++;
            }

            if (open >= source.length() || source.charAt(open) != '(')
            {
                out.append(source, found, found + functionName.length());
                i = found + functionName.length();
                continue;
            }

            int close = findMatchingParen(source, open);

            if (close < 0)
            {
                out.append(source, found, source.length());
                break;
            }

            String call = source.substring(found, close + 1);
            String args = source.substring(open + 1, close).trim();

            if (isShadowSamplerArg(args) && !isAlreadyWrapped(source, found, marker))
            {
                out.append(marker).append(call).append(')');
            }
            else
            {
                out.append(call);
            }

            i = close + 1;
        }

        return out.toString();
    }

    private static boolean isAlreadyWrapped(String source, int callStart, String marker)
    {
        int lookBehind = Math.max(0, callStart - marker.length() - 8);
        String before = source.substring(lookBehind, callStart);

        return before.contains(marker);
    }

    private static boolean isShadowSamplerArg(String args)
    {
        if (args.isEmpty())
        {
            return false;
        }

        int comma = findTopLevelComma(args);
        String sampler = (comma < 0 ? args : args.substring(0, comma)).trim();

        return sampler.equals("shadowtex0")
            || sampler.equals("shadowtex1")
            || sampler.equals("shadowtex0HW")
            || sampler.equals("shadowtex1HW")
            || sampler.equals("waterShadow");
    }

    private static int findTopLevelComma(String args)
    {
        int depth = 0;

        for (int i = 0; i < args.length(); i++)
        {
            char c = args.charAt(i);

            if (c == '(')
            {
                depth++;
            }
            else if (c == ')')
            {
                depth--;
            }
            else if (c == ',' && depth == 0)
            {
                return i;
            }
        }

        return -1;
    }

    private static int indexOfIdentifierCall(String source, String name, int from)
    {
        int index = from;

        while (index < source.length())
        {
            int found = source.indexOf(name, index);

            if (found < 0)
            {
                return -1;
            }

            boolean startOk = found == 0 || !isIdentChar(source.charAt(found - 1));
            int after = found + name.length();
            boolean endOk = after >= source.length() || !isIdentChar(source.charAt(after));

            if (startOk && endOk)
            {
                int probe = after;

                while (probe < source.length() && Character.isWhitespace(source.charAt(probe)))
                {
                    probe++;
                }

                if (probe < source.length() && source.charAt(probe) == '(')
                {
                    return found;
                }
            }

            index = found + 1;
        }

        return -1;
    }

    private static boolean isIdentChar(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static int findMatchingParen(String source, int openIndex)
    {
        int depth = 0;

        for (int i = openIndex; i < source.length(); i++)
        {
            char c = source.charAt(i);

            if (c == '(')
            {
                depth++;
            }
            else if (c == ')')
            {
                depth--;

                if (depth == 0)
                {
                    return i;
                }
            }
        }

        return -1;
    }
}
