package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;

public class FormColorEffects
{
    public static final float EMISSION_STRENGTH = 1F;
    public static final float OVERLAY_GLOW_BOOST = EMISSION_STRENGTH;
    /**
     * Soft ceiling on albedo multiply. High enough that Intensity ~50 still climbs
     * visibly (was 2.25 — flatlined by Intensity ~5).
     */
    public static final float MAX_ALBEDO_GLOW_SCALE = 12F;
    /** Cap additive overlay passes per bundle — intensity itself stays unbounded. */
    public static final int MAX_GLOW_OVERLAY_LAYERS = 8;
    /** Extra additive bundles for high intensity (FPS ceiling ≈ layers × bundles). */
    public static final int MAX_GLOW_OVERLAY_BUNDLES = 8;
    public static final int MAX_GLOW_SIZE_SHELLS = 10;

    /**
     * Shadow-pass alpha follows form opacity (0 = no ground shadow). Kept for call-site
     * compatibility; no longer boosts zero opacity to a faint silhouette.
     */
    public static void finishShadowOpacity(Color color, boolean shadowPass)
    {
        /* no-op: traditional color.a / applyFormOpacity already own caster alpha */
    }

    /**
     * Shadow-map alpha scale for Paint / form color tint / Color Grade.
     * <p>
     * Previously returned {@code 0.001} whenever any Color-track intensity was non-zero so
     * Complementary would drop a cursor-side fringe. Iris shadow programs still alpha-test
     * around ~0.1, so that crush also deleted the actor's ground shadow. Casting now stays
     * at full shadow-pass alpha; opacity 0 still skips via callers / {@code applyFormOpacity}.
     */
    public static float resolveEffectShaderShadow(Color storedFormColor, PaintSettings paintSettings, Color legacyPaint)
    {
        return resolveEffectShaderShadow(storedFormColor, paintSettings, legacyPaint, false);
    }

    /**
     * @param anyPaintActive true when form-level or per-bone paint is on (Model forms).
     */
    public static float resolveEffectShaderShadow(Color storedFormColor, PaintSettings paintSettings, Color legacyPaint, boolean anyPaintActive)
    {
        return PaintSettings.SHADER_SHADOW_DEFAULT;
    }

    /**
     * Kept for call-site compatibility. No longer multiplies shadow-pass alpha for Color-track
     * effects (see {@link #resolveEffectShaderShadow}).
     */
    public static void applyShadowPassColorFix(Color color, Color storedFormColor, PaintSettings paintSettings, Color legacyPaint, boolean shadowPass)
    {
        applyShadowPassColorFix(color, storedFormColor, paintSettings, legacyPaint, shadowPass, false);
    }

    public static void applyShadowPassColorFix(Color color, Color storedFormColor, PaintSettings paintSettings, Color legacyPaint, boolean shadowPass, boolean anyPaintActive)
    {
        /* no-op: crushing alpha for the Complementary fringe removed ground shadows under Iris */
    }

    public enum BlendMode
    {
        MULTIPLY,
        BRIGHTEN
    }

    public static void blend(Color base, Color overlay, boolean additive)
    {
        blend(base, overlay, additive ? BlendMode.BRIGHTEN : BlendMode.MULTIPLY);
    }

    public static void blendFormGlowBrighten(Color base, GlowSettings glow, Color fallback)
    {
        blendFormGlowBrighten(base, glow, fallback, null, null, null);
    }

    public static void blendFormGlowBrighten(Color base, GlowSettings glow, Color fallback, PaintSettings paint, Color legacyPaint, Color formColor)
    {
        if (base == null || glow == null)
        {
            return;
        }

        float intensity = glow.resolveIntensity(fallback);

        if (intensity == 0F)
        {
            return;
        }

        mchorse.bbs_mod.utils.iris.FormGlowBloomPatch.setFromGlow(glow, fallback);

        if (intensity > 0F)
        {
            /* Complementary/BSL: FormGlowBloomPatch drives pack emission — keep form Color intact. */
            if (mchorse.bbs_mod.utils.iris.FormGlowBloomPatch.shouldSkipAlbedoBrighten())
            {
                return;
            }

            /* Soft scale only. Huge intensity * 8 washed Color to white under HDR bloom. */
            float scale = resolveAlbedoGlowScale(intensity);

            base.r *= scale;
            base.g *= scale;
            base.b *= scale;
        }
        else
        {
            float factor = Math.max(0F, 1F + intensity);

            base.r *= factor;
            base.g *= factor;
            base.b *= factor;
        }
    }

    /**
     * Soft albedo boost that keeps climbing through the UI range.
     * Intensity 1 ≈ 2.3×, Intensity 10 ≈ 6.5×, Intensity 50 ≈ 10× (toward {@link #MAX_ALBEDO_GLOW_SCALE}).
     */
    public static float resolveAlbedoGlowScale(float intensity)
    {
        if (intensity <= 0F)
        {
            return 1F;
        }

        /* Slower asymptote than intensity/(1+0.55*i) which flatlined near Intensity 5. */
        float soft = intensity / (1F + intensity * 0.12F);

        return 1F + soft * (MAX_ALBEDO_GLOW_SCALE - 1F);
    }

    /**
     * Strength for BBS {@code GlowingColor} / model.fsh.
     * Mild intensities stay linear; very high values keep climbing without early soft-cap.
     */
    public static float resolveShaderGlowStrength(float intensity)
    {
        if (intensity <= 0F)
        {
            return 0F;
        }

        /* Keep UI Intensity meaningful past ~5 (old albedo path stopped changing look). */
        return intensity;
    }

    /**
     * When Glow Size is 0, high Intensity still needs a bloom radius for Iris packs /
     * outer shells — otherwise Intensity 50 looks the same as Intensity 2 on LDR.
     */
    public static float resolveBloomSizeFromIntensity(float intensity, float size)
    {
        if (Math.abs(size) > 0.001F)
        {
            return size;
        }

        if (intensity <= 0.5F)
        {
            return 0F;
        }

        /* Intensity 10 → ~3.3, Intensity 50 → ~16 (clamped) — Size 0 must still bloom. */
        return Math.min(16F, (intensity - 0.5F) * 0.35F);
    }

    /**
     * Glow RGB when the glow swatch is still default white: prefer active Paint Color, then
     * form Color, then legacy {@code glowing_color}. Explicit non-white glow RGB always wins
     * (timeline / editor can still lock a custom emission hue).
     */
    public static void resolveGlowTint(GlowSettings glow, Color legacyGlow, PaintSettings paint, Color legacyPaint, Color formColor, Color out)
    {
        if (out == null)
        {
            return;
        }

        if (glow == null)
        {
            out.set(1F, 1F, 1F, 1F);

            return;
        }

        if (glow.r < 0.999F || glow.g < 0.999F || glow.b < 0.999F)
        {
            out.set(glow.r, glow.g, glow.b, 1F);

            return;
        }

        if (paint != null && paint.resolveIntensity(legacyPaint) != 0F)
        {
            Color paintRgb = new Color();

            paint.resolveColor(legacyPaint, paintRgb);
            out.set(paintRgb.r, paintRgb.g, paintRgb.b, 1F);

            return;
        }

        if (formColor != null)
        {
            Color baked = formColor.copyBakingColorGrade();
            float lum = baked.r * 0.2126F + baked.g * 0.7152F + baked.b * 0.0722F;

            /* Near-black form Color must not zero emission (common when Color track is unset / #000). */
            if (lum > 0.04F && (baked.r < 0.999F || baked.g < 0.999F || baked.b < 0.999F))
            {
                out.set(baked.r, baked.g, baked.b, 1F);

                return;
            }
        }

        glow.resolveColor(legacyGlow, out);
    }

    public static void blendBrighten(Color base, Color glowColor, float intensity)
    {
        blendEmission(base, glowColor, intensity);
    }

    public static void blendEmission(Color base, Color glowColor, float intensity)
    {
        if (base == null || glowColor == null || intensity == 0F)
        {
            return;
        }

        float r = MathUtils.clamp(glowColor.r, 0F, 1F);
        float g = MathUtils.clamp(glowColor.g, 0F, 1F);
        float b = MathUtils.clamp(glowColor.b, 0F, 1F);

        if (intensity > 0F)
        {
            float t = Math.min(intensity, 1F);
            base.r = base.r + (r - base.r) * t + r * intensity * 7F;
            base.g = base.g + (g - base.g) * t + g * intensity * 7F;
            base.b = base.b + (b - base.b) * t + b * intensity * 7F;
        }
        else
        {
            /* Linear darken: 0 = unchanged, -1 = fully black (smooth for keyframe animation). */
            float factor = Math.max(0F, 1F + intensity);

            base.r *= factor;
            base.g *= factor;
            base.b *= factor;
        }
    }

    public static boolean hasPositiveGlow(GlowSettings glow, Color legacyGlow)
    {
        return glow.resolveIntensity(legacyGlow) > 0F;
    }

    public static boolean hasPositivePaint(PaintSettings paintSettings, Color legacyPaint)
    {
        return paintSettings.resolveIntensity(legacyPaint) > 0F;
    }

    /**
     * Paint overlay (spatial mask) for positive tint, or negative darken when a paint transform
     * is active — same contract as ModelForm {@code PaintColor.a < 0} + {@code PaintEffect} mask.
     * Uniform negative paint without a transform still bakes into the main pass.
     */
    public static boolean wantsPaintOverlay(PaintSettings paintSettings, Color legacyPaint)
    {
        if (paintSettings == null)
        {
            return false;
        }

        float intensity = paintSettings.resolveIntensity(legacyPaint);

        if (intensity > 0F)
        {
            return true;
        }

        return intensity < 0F && paintSettings.transform != null && paintSettings.transform.isActive();
    }

    /**
     * True when negative paint must stay off the main/BER bake because the masked overlay owns it.
     */
    public static boolean defersNegativePaintToOverlay(PaintSettings paintSettings, Color legacyPaint)
    {
        if (paintSettings == null)
        {
            return false;
        }

        return paintSettings.resolveIntensity(legacyPaint) < 0F
            && paintSettings.transform != null
            && paintSettings.transform.isActive();
    }

    /**
     * Negative glow with an active spatial transform uses a multiply darken overlay
     * (ModelForm GlowEffect mask). Uniform negative glow stays on the main pass.
     */
    public static boolean wantsNegativeGlowOverlay(GlowSettings glow, Color legacyGlow)
    {
        if (glow == null)
        {
            return false;
        }

        float intensity = glow.resolveIntensity(legacyGlow);

        if (intensity >= 0F || glow.resolvePaintOnly())
        {
            return false;
        }

        EffectTransform transform = resolveGlowEffectTransform(glow, legacyGlow);

        return transform != null && transform.isActive();
    }

    /**
     * Positive paint: RGB + strength×formAlpha in {@code a}.
     * Negative paint: RGB = darken factor, {@code a} = form opacity (multiply-mask coverage).
     */
    public static Color resolvePaintOverlayDrawColor(Color resolvedPaint, float formAlpha)
    {
        if (resolvedPaint == null)
        {
            return new Color(1F, 1F, 1F, formAlpha);
        }

        if (resolvedPaint.a < 0F)
        {
            float factor = Math.max(0F, 1F + resolvedPaint.a);

            return new Color(factor, factor, factor, formAlpha);
        }

        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= formAlpha;

        return paintOverlay;
    }

    /**
     * Multiply-darken overlay color for masked negative glow (RGB = factor, a = form opacity).
     */
    public static Color resolveNegativeGlowDarkenOverlayColor(float glowIntensity, float formAlpha)
    {
        float factor = Math.max(0F, 1F + glowIntensity);

        return new Color(factor, factor, factor, formAlpha);
    }

    /**
     * True when Color should use a spatial mask / FormColorTint overlay instead of baking
     * into vertex color — same rules as ModelFormRenderer.canApplyColorTransformMask, without
     * requiring a ModelInstance.
     * <p>
     * Only an active color transform forces this path. Plain RGB tint bakes into the form
     * geometry (legacy behavior). Color Grade alone does not force this; see
     * {@link #wantsColorTintForAdjustments} / {@link #wantsColorTintOverlay}.
     * Alpha is traditional opacity and is ignored here.
     */
    public static boolean wantsColorTransformMask(Color color)
    {
        return color != null && (color.hasActiveTransform() || color.hasActiveGradeTransform());
    }

    /**
     * Whether Iris / BBS FormColorTint overlay should run for brightness-only grading when the
     * live Iris pass cannot run FormColorGrade (no BBS model.fsh).
     */
    public static boolean wantsColorTintForAdjustments(Color color, boolean shaderGradeActive)
    {
        return color != null && color.hasColorAdjustments() && !shaderGradeActive;
    }

    /**
     * Block / item / structure / billboard / shape: use FormColorTint overlay for color
     * spatial masks <b>or</b> Color Grade. Plain RGB without a transform bakes into vertices
     * ({@link #shouldBakeFormColor}).
     */
    public static boolean wantsColorTintOverlay(Color color)
    {
        return wantsColorTransformMask(color) || wantsColorTintForAdjustments(color, false);
    }

    /**
     * Bake form color into vertex tint when no FormColorTint overlay will run
     * (no active color transform and no Color Grade overlay).
     */
    public static boolean shouldBakeFormColor(Color color)
    {
        return !wantsColorTintOverlay(color);
    }

    /**
     * Block-entity renderers (beds, chests, …) are incompatible with
     * {@code block_color_tint_overlay} / {@code block_paint_overlay}: those force the block
     * atlas and (for Color Grade) regrade framebuffer pixels that often sample the UI.
     * Bake form color, Color Grade, and uniform paint into the tint instead (Iris reapplies
     * this tint after composite via a deferred BE redraw).
     */
    public static Color resolveBlockEntityTint(Color storedFormColor, PaintSettings paintSettings, Color legacyPaint)
    {
        Color tint = storedFormColor == null ? Color.white() : storedFormColor.copyBakingColorGrade();

        if (paintSettings != null && paintSettings.resolveIntensity(legacyPaint) != 0F)
        {
            applyPaintBlend(tint, paintSettings, legacyPaint);
        }

        return tint;
    }

    public static Color resolvePaintColor(PaintSettings paintSettings, Color legacyPaint)
    {
        Color resolvedPaint = new Color();

        paintSettings.resolveColor(legacyPaint, resolvedPaint);
        resolvedPaint.a = paintSettings.resolveIntensity(legacyPaint);

        return resolvedPaint;
    }

    public static void applyPaintBlend(Color base, Color paintRgb, float paintStrength)
    {
        if (base == null || paintRgb == null || paintStrength == 0F)
        {
            return;
        }

        if (paintStrength >= 1F)
        {
            base.r = paintRgb.r;
            base.g = paintRgb.g;
            base.b = paintRgb.b;
        }
        else if (paintStrength > 0F)
        {
            base.r = base.r + (paintRgb.r - base.r) * paintStrength;
            base.g = base.g + (paintRgb.g - base.g) * paintStrength;
            base.b = base.b + (paintRgb.b - base.b) * paintStrength;
        }
        else
        {
            float factor = Math.max(0F, 1F + paintStrength);

            base.r *= factor;
            base.g *= factor;
            base.b *= factor;
        }
    }

    public static void applyPaintBlend(Color base, PaintSettings paintSettings, Color legacyPaint)
    {
        Color paint = new Color();

        paintSettings.resolveColor(legacyPaint, paint);
        applyPaintBlend(base, paint, paintSettings.resolveIntensity(legacyPaint));
    }

    public static void applyPaintBlendToBytes(int[] rgb, Color paintColor)
    {
        if (paintColor == null || rgb == null || rgb.length < 3 || Math.abs(paintColor.a) == 0F)
        {
            return;
        }

        Color vertex = new Color(rgb[0] / 255F, rgb[1] / 255F, rgb[2] / 255F, 1F);

        applyPaintBlend(vertex, paintColor, paintColor.a);
        rgb[0] = MathUtils.clamp((int) (vertex.r * 255F), 0, 255);
        rgb[1] = MathUtils.clamp((int) (vertex.g * 255F), 0, 255);
        rgb[2] = MathUtils.clamp((int) (vertex.b * 255F), 0, 255);
    }

    public static int resolveGlowOverlayLayers(float intensity)
    {
        if (intensity <= 0F)
        {
            return 0;
        }

        int desired = Math.max(1, (int) Math.ceil(intensity * OVERLAY_GLOW_BOOST));

        return Math.min(desired, MAX_GLOW_OVERLAY_LAYERS);
    }

    /**
     * Extra additive bundles so unbounded intensity keeps getting brighter without
     * drawing hundreds of layers (FPS ceiling ≈ layers × bundles).
     */
    public static int resolveGlowOverlayBundles(float intensity)
    {
        if (intensity <= 0F)
        {
            return 0;
        }

        int desired = Math.max(1, (int) Math.ceil(intensity * OVERLAY_GLOW_BOOST));

        if (desired <= MAX_GLOW_OVERLAY_LAYERS)
        {
            return 1;
        }

        int bundles = (desired + MAX_GLOW_OVERLAY_LAYERS - 1) / MAX_GLOW_OVERLAY_LAYERS;

        return Math.min(bundles, MAX_GLOW_OVERLAY_BUNDLES);
    }

    /**
     * Soft outer-glow shells from Size. Spread densifies the core.
     */
    public static int resolveGlowSizeShells(float size, float spread)
    {
        float absSize = Math.abs(size);

        if (absSize <= 0.001F)
        {
            return 0;
        }

        float spread01 = MathUtils.clamp(spread, 0F, 1F);
        /* More shells = smoother Size ramp (Spread densifies toward the core). */
        int shells = Math.max(3, (int) Math.ceil(3F + absSize * (2.5F + (1F - spread01) * 2F)));

        return Math.min(shells, MAX_GLOW_SIZE_SHELLS);
    }

    public static float resolveGlowShellExpand(float size, float spread, int shellIndex, int shellCount)
    {
        if (shellCount <= 0 || Math.abs(size) <= 0.001F)
        {
            return 0F;
        }

        float t = (shellIndex + 1F) / shellCount;
        float spread01 = MathUtils.clamp(spread, 0F, 1F);
        float falloff = (float) Math.pow(t, 1F + (1F - spread01) * 1.75F);
        /* Size is UI units → quad pad for Outer Glow (Size 5 ≈ +100% pad on outer shell). */
        float scaled = size * 0.2F * falloff;

        if (scaled > 2F)
        {
            return 2F;
        }

        if (scaled < -0.9F)
        {
            return -0.9F;
        }

        return scaled;
    }

    /**
     * Soft radial falloff — outer rings fade out so Size does not look like a hard silhouette.
     */
    public static float resolveGlowShellFade(float spread, int shellIndex, int shellCount)
    {
        if (shellCount <= 0)
        {
            return 1F;
        }

        float t = (shellIndex + 1F) / (float) shellCount;
        float spread01 = MathUtils.clamp(spread, 0F, 1F);
        float sigma = 1.15F + spread01 * 0.85F;
        float gaussian = (float) Math.exp(-(t * t) * (3.2F / sigma));
        float tip = 1F - t;

        return MathUtils.clamp(gaussian * tip * tip * (0.55F + spread01 * 0.45F), 0.02F, 1F);
    }

    public static Color resolveGlowOverlayColor(GlowSettings glow, Color legacyGlow, float alpha, float intensity, int layers)
    {
        return resolveGlowOverlayColor(glow, legacyGlow, null, null, null, alpha, intensity, layers);
    }

    public static Color resolveGlowOverlayColor(GlowSettings glow, Color legacyGlow, PaintSettings paint, Color legacyPaint, Color formColor, float alpha, float intensity, int layers)
    {
        Color resolved = new Color();
        Color color = new Color();
        int safeLayers = Math.max(1, layers);
        int bundles = Math.max(1, resolveGlowOverlayBundles(intensity));
        float total = intensity * OVERLAY_GLOW_BOOST;
        /* Allow HDR-style overbright per layer so Intensity > ~1 keeps getting brighter. */
        float layerStrength = Math.max(0F, total / (safeLayers * (float) bundles));

        resolveGlowTint(glow, legacyGlow, paint, legacyPaint, formColor, resolved);
        color.r = Math.max(0F, resolved.r * layerStrength);
        color.g = Math.max(0F, resolved.g * layerStrength);
        color.b = Math.max(0F, resolved.b * layerStrength);
        color.a = alpha;

        return color;
    }

    /**
     * Full-strength glow tint for a single additive overlay pass. Pair with
     * {@link #resolveGlowOverlayShaderScale(float)} so total emission matches the legacy multi-layer path.
     */
    public static Color resolveGlowOverlayEmissionColor(GlowSettings glow, Color legacyGlow, float alpha, float intensity)
    {
        return resolveGlowOverlayEmissionColor(glow, legacyGlow, null, null, null, alpha, intensity);
    }

    public static Color resolveGlowOverlayEmissionColor(GlowSettings glow, Color legacyGlow, PaintSettings paint, Color legacyPaint, Color formColor, float alpha, float intensity)
    {
        Color resolved = new Color();
        Color color = new Color();

        resolveGlowTint(glow, legacyGlow, paint, legacyPaint, formColor, resolved);
        color.set(resolved.r, resolved.g, resolved.b, alpha);

        return color;
    }

    public static float resolveGlowOverlayShaderScale(float intensity)
    {
        if (intensity <= 0F)
        {
            return 0F;
        }

        return resolveShaderGlowStrength(intensity) * 2F;
    }

    public static EffectTransform resolveGlowEffectTransform(GlowSettings glow, Color legacyGlow)
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

    public static void blend(Color base, Color overlay, BlendMode mode)
    {
        if (base == null || overlay == null)
        {
            return;
        }

        float a = MathUtils.clamp(overlay.a, 0F, 1F);
        float r = MathUtils.clamp(overlay.r, 0F, 1F);
        float g = MathUtils.clamp(overlay.g, 0F, 1F);
        float b = MathUtils.clamp(overlay.b, 0F, 1F);

        if (mode == BlendMode.BRIGHTEN)
        {
            blendBrighten(base, overlay, a);
        }
        else
        {
            base.r *= r;
            base.g *= g;
            base.b *= b;
            base.a *= a;
        }
    }
}
