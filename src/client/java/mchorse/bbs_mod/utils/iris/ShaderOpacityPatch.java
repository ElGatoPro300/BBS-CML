package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.mixin.client.iris.IrisRenderingPipelineAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

import net.irisshaders.iris.gl.texture.DepthCopyStrategy;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import net.irisshaders.iris.targets.RenderTargets;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Runtime soft-opacity queue. Soft forms draw after translucent terrain with depth writes
 * (fluids stay, limbs do not X-ray). Pack GLSL / shaders.properties are left vanilla —
 * Complementary light shafts sample the same shadow map those patches used to rewrite.
 */
public class ShaderOpacityPatch
{
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
     * Settings toggle for the Iris opacity-fix path. Pack GLSL is no longer rewritten
     * ({@link #shouldApplyPackGlslPatches}); soft forms use the post-deferred queue either way.
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
     * Pack GLSL / shaders.properties rewrites. Always off: Complementary 5.8 light shafts
     * share shadowtex with lighting, and the old wrap / alpha-test / separateEntityDraws
     * patches leaked god rays through solid terrain.
     */
    public static boolean shouldApplyPackGlslPatches()
    {
        return false;
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
     * translucent terrain; vanilla also waits until after vanilla clouds via LAST) with depth
     * write so limbs do not X-ray and fluids stay intact.
     * Fully solid keeps the live path.
     */
    public static final float LIVE_DEPTH_WRITE_ALPHA = 0.999F;

    /**
     * Queue soft-opacity forms until after translucent terrain.
     * Works with or without Iris and with or without the Complementary/BSL opacity patch —
     * patched packs get the best lighting; unpatched / no-shader still get correct depth
     * occlusion and no self X-ray. Never delay the shadow pass.
     */
    public static boolean shouldDelayUntilPostDeferred(float alpha)
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
        return alpha < LIVE_DEPTH_WRITE_ALPHA;
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
     * Soft opacity waits until after water/lava/portals.
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
         * Paint/blend/grade overlays stay queued until onWorldRenderEnd — Iris composites after
         * translucent terrain would overwrite an early color-tint multiply. */
        postDeferredPhase = true;
    }

    /**
     * After translucent terrain (water/lava/portals).
     * <p>
     * Iris: flush soft forms here (pack clouds are already composited on that path).
     * Vanilla: do <em>not</em> flush yet — Fabric draws vanilla clouds after this event;
     * flushing with depth write here hides clouds behind soft actors. Hold until
     * {@link #onAfterVanillaClouds()} ({@code WorldRenderEvents.LAST}).
     */
    public static void onAfterTranslucentTerrain()
    {
        if (BBSRendering.isIrisShadersEnabled())
        {
            flushPostDeferredForms(null);

            return;
        }

        postDeferredPhase = true;
    }

    /**
     * After vanilla clouds / weather ({@code WorldRenderEvents.LAST}). Soft forms kept from
     * {@link #onAfterTranslucentTerrain()} draw here so depth writes no longer erase clouds.
     * Iris already flushed earlier — this is a no-op safety net when the queue is empty.
     */
    public static void onAfterVanillaClouds()
    {
        if (BBSRendering.isIrisShadersEnabled())
        {
            return;
        }

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
            /* Soft-opacity flushes can leave depthMask dirty for later world draws. */
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
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

            if (!bindIrisDefault)
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
        MatrixStack modelViewStack = RenderSystem.getModelViewStack();
        Matrix4f savedModelView = new Matrix4f(modelViewStack.peek().getPositionMatrix());
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
                modelViewStack.peek().getPositionMatrix().set(entry.modelView);
                RenderSystem.applyModelViewMatrix();
            }
            else
            {
                modelViewStack.loadIdentity();
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
            modelViewStack.peek().getPositionMatrix().set(savedModelView);
            RenderSystem.applyModelViewMatrix();
        }
    }

    public static String patchPropertiesContents(String contents)
    {
        /* Pack shaders.properties stay vanilla. Forcing separateEntityDraws and rewriting
         * GLSL/alpha tests is what the opacity-fix toggle used to do, and it leaks
         * Complementary light shafts through solid terrain. Soft forms already use the
         * post-deferred queue without mutating the pack. */
        return contents;
    }

    public static void applyAlphaTestOverrides(ShaderProperties properties)
    {
        /* No-op: hardware alphaTest GREATER 0.0001 on gbuffers was part of the VL leak. */
    }

    public static void applySeparateEntityDraws(Consumer<OptionalBoolean> setter)
    {
        /* No-op: Complementary does not set separateEntityDraws. */
    }

    public static String processSource(String source)
    {
        if (source == null || source.isEmpty())
        {
            return source;
        }

        if (BBSSettings.shaderShadowDither != null && BBSSettings.shaderShadowDither.get())
        {
            return processShadowCasterAlpha(source);
        }

        return source;
    }

    public static void beginShadowForm()
    {
        uploadShadowFormUniform(1F);
    }

    public static void endShadowForm()
    {
        uploadShadowFormUniform(0F);
    }

    public static void uploadShadowFormUniform()
    {
        if (BBSRendering.isIrisShadowPass())
        {
            uploadShadowFormUniform(1F);
        }
    }

    public static void uploadShadowFormUniform(float value)
    {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        if (program > 0)
        {
            int location = GL20.glGetUniformLocation(program, "bbs_is_shadow_form");

            if (location >= 0)
            {
                GL20.glUniform1f(location, value);
            }
        }
    }

    private static String insertShadowUniform(String source)
    {
        if (source.contains("bbs_is_shadow_form"))
        {
            return source;
        }

        int version = source.indexOf("#version");

        if (version < 0)
        {
            return "uniform float bbs_is_shadow_form;\n" + source;
        }

        int nextNewLine = source.indexOf('\n', version);

        if (nextNewLine < 0)
        {
            return source + "\nuniform float bbs_is_shadow_form;\n";
        }

        return source.substring(0, nextNewLine + 1) + "uniform float bbs_is_shadow_form;\n" + source.substring(nextNewLine + 1);
    }

    /**
     * Experimental: apply ordered Bayer 4x4 dither discard exclusively on entity fragments
     * (bbs_is_shadow_form > 0.5) when shader_shadow_dither setting is enabled by the user.
     */
    public static String processShadowCasterAlpha(String source)
    {
        if (source == null || source.isEmpty() || source.contains("BBS_SHADOW_CASTER_DITHER"))
        {
            return source;
        }

        /* Complementary shadow.glsl: only inject when bbs_is_shadow_form > 0.5 */
        if (source.contains("DoNaturalShadowCalculation"))
        {
            String dither =
                "/* BBS_SHADOW_CASTER_DITHER */\n"
                    + "    if (bbs_is_shadow_form > 0.5 && glColor.a < 0.999) {\n"
                    + "        const float bbsBayer4x4[16] = float[16](\n"
                    + "            0.0625, 0.5625, 0.1875, 0.6875,\n"
                    + "            0.8125, 0.3125, 0.9375, 0.4375,\n"
                    + "            0.2500, 0.7500, 0.1250, 0.6250,\n"
                    + "            1.0000, 0.5000, 0.8750, 0.3750\n"
                    + "        );\n"
                    + "        ivec2 bbsCoord = ivec2(mod(gl_FragCoord.xy, 4.0));\n"
                    + "        if (glColor.a < bbsBayer4x4[bbsCoord.y * 4 + bbsCoord.x]) discard;\n"
                    + "    }\n";

            String patched = insertShadowUniform(source);

            if (patched.contains("gl_FragData[0] = color1;"))
            {
                return patched.replace(
                    "gl_FragData[0] = color1;",
                    dither + "    gl_FragData[0] = color1;"
                );
            }

            if (patched.contains("shadowColor = color1;"))
            {
                return patched.replace(
                    "shadowColor = color1;",
                    dither + "    shadowColor = color1;"
                );
            }
        }

        /* BSL shadow.glsl: only inject when bbs_is_shadow_form > 0.5 */
        if (source.contains("float premult = float(mat > 0.98") && source.contains("gl_FragData[0] = albedo;"))
        {
            String dither =
                "\t/* BBS_SHADOW_CASTER_DITHER */\n"
                    + "\tif (bbs_is_shadow_form > 0.5 && color.a < 0.999) {\n"
                    + "\t\tconst float bbsBayer4x4[16] = float[16](\n"
                    + "\t\t\t0.0625, 0.5625, 0.1875, 0.6875,\n"
                    + "\t\t\t0.8125, 0.3125, 0.9375, 0.4375,\n"
                    + "\t\t\t0.2500, 0.7500, 0.1250, 0.6250,\n"
                    + "\t\t\t1.0000, 0.5000, 0.8750, 0.3750\n"
                    + "\t\t);\n"
                    + "\t\tivec2 bbsCoord = ivec2(mod(gl_FragCoord.xy, 4.0));\n"
                    + "\t\tif (color.a < bbsBayer4x4[bbsCoord.y * 4 + bbsCoord.x]) discard;\n"
                    + "\t}\n";

            String patched = insertShadowUniform(source);

            return patched.replace(
                "\tgl_FragData[0] = albedo;",
                dither + "\tgl_FragData[0] = albedo;"
            );
        }

        return source;
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
}
