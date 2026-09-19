package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.screen.nodes.BrightnessContrastNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ColorGradeEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.DistortionEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GammaCorrectionNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GlitchNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.GrainEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.HueSaturationNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.LayerNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.LetterboxEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.LevelsNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.OverlayBlendNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.OverlayEffectNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.PosterizeNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ScreenBlendNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ScreenOutputNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.ScreenUVNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.SineWaveNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.SquareWaveNode;
import mchorse.bbs_mod.camera.clips.screen.nodes.VignetteEffectNode;
import mchorse.bbs_mod.forms.forms.shape.ShapeFormGraph;
import mchorse.bbs_mod.forms.forms.shape.ShapeGraphEvaluator;
import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Evaluator for ScreenNodeGraph. Extends ShapeGraphEvaluator to handle all
 * screen-specific nodes in addition to the shared shape math / color / noise nodes.
 */
public class ScreenNodeEvaluator extends ShapeGraphEvaluator
{
    public ScreenNodeEvaluator(ShapeFormGraph graph)
    {
        super(graph);
    }

    /**
     * Evaluates the ScreenOutputNode and all standalone effect nodes, writing
     * results into the provided effect struct scaled by the envelope factor.
     */
    public void computeEffect(double time, double factor, ScreenNodeEffect effect)
    {
        /* Standalone effect nodes — detected by type, no connections required */
        for (ShapeNode node : this.nodes.values())
        {
            if (node instanceof ColorGradeEffectNode)
            {
                effect.brightness = (float) (this.getInput(node.id, 0, 0, 0, 0, time) * factor);
                effect.contrast   = (float) (this.getInput(node.id, 1, 0, 0, 0, time) * factor);
                effect.saturation = (float) (this.getInput(node.id, 2, 0, 0, 0, time) * factor);
            }
            else if (node instanceof VignetteEffectNode)
            {
                effect.vignetteStrength   = (float) (this.getInput(node.id, 0, 0, 0, 0, time) * factor);
                effect.vignetteSmoothness = (float) this.getInput(node.id, 1, 0, 0, 0, time);
                effect.vignetteColor      = this.hasInput(node.id, 2)
                    ? (int) this.getInput(node.id, 2, 0, 0, 0, time) : Colors.A100;
            }
            else if (node instanceof GrainEffectNode)
            {
                effect.grainStrength = (float) (this.getInput(node.id, 0, 0, 0, 0, time) * factor);
                effect.grainSize     = this.hasInput(node.id, 1)
                    ? Math.max(0.25F, (float) this.getInput(node.id, 1, 0, 0, 0, time)) : 1F;
            }
            else if (node instanceof LetterboxEffectNode)
            {
                effect.letterboxSize  = (float) (this.getInput(node.id, 0, 0, 0, 0, time) * factor);
                effect.letterboxColor = this.hasInput(node.id, 1)
                    ? (int) this.getInput(node.id, 1, 0, 0, 0, time) : Colors.A100;
            }
            else if (node instanceof OverlayEffectNode)
            {
                effect.overlayColor = this.hasInput(node.id, 0)
                    ? (int) this.getInput(node.id, 0, 0, 0, 0, time) : Colors.A100;
                effect.overlayAlpha = (float) (this.getInput(node.id, 1, 0, 0, 0, time) * factor);
            }
            else if (node instanceof DistortionEffectNode)
            {
                effect.distortX = (float) (this.getInput(node.id, 0, 0, 0, 0, time) * factor);
                effect.distortY = (float) (this.getInput(node.id, 1, 0, 0, 0, time) * factor);
            }
        }
    }

    @Override
    protected double evaluate(ShapeNode node, int outputIndex, double x, double y, double z, double time)
    {
        if (node instanceof ScreenUVNode)
        {
            /* Screen centre UV in effect-evaluation context */
            return outputIndex == 0 ? 0.5 : outputIndex == 1 ? 0.5 : 0;
        }

        if (node instanceof SineWaveNode)
        {
            double freq  = this.hasInput(node.id, 0) ? this.getInput(node.id, 0, x, y, z, time) : 1;
            double amp   = this.hasInput(node.id, 1) ? this.getInput(node.id, 1, x, y, z, time) : 1;
            double phase = this.hasInput(node.id, 2) ? this.getInput(node.id, 2, x, y, z, time) : 0;

            return amp * Math.sin(2 * Math.PI * freq * time + phase);
        }

        if (node instanceof SquareWaveNode)
        {
            double freq = this.hasInput(node.id, 0) ? this.getInput(node.id, 0, x, y, z, time) : 1;
            double duty = this.hasInput(node.id, 1) ? this.getInput(node.id, 1, x, y, z, time) : 0.5;

            if (freq <= 0) return 0;

            return (time * freq) % 1.0 < duty ? 1 : 0;
        }

        if (node instanceof ScreenBlendNode)
        {
            int base  = (int) this.getInput(node.id, 0, x, y, z, time);
            int blend = (int) this.getInput(node.id, 1, x, y, z, time);

            return blendScreen(base, blend);
        }

        if (node instanceof OverlayBlendNode)
        {
            int base  = (int) this.getInput(node.id, 0, x, y, z, time);
            int blend = (int) this.getInput(node.id, 1, x, y, z, time);

            return blendOverlay(base, blend);
        }

        if (node instanceof GammaCorrectionNode)
        {
            int color    = (int) this.getInput(node.id, 0, x, y, z, time);
            double gamma = this.hasInput(node.id, 1) ? this.getInput(node.id, 1, x, y, z, time) : 1;

            return applyGamma(color, gamma);
        }

        if (node instanceof HueSaturationNode)
        {
            int color      = (int) this.getInput(node.id, 0, x, y, z, time);
            double hueShift  = this.getInput(node.id, 1, x, y, z, time);
            double saturation = this.getInput(node.id, 2, x, y, z, time);

            return applyHueSaturation(color, hueShift, saturation);
        }

        if (node instanceof BrightnessContrastNode)
        {
            int color      = (int) this.getInput(node.id, 0, x, y, z, time);
            double brightness = this.getInput(node.id, 1, x, y, z, time);
            double contrast   = this.getInput(node.id, 2, x, y, z, time);

            return applyBrightnessContrast(color, brightness, contrast);
        }

        if (node instanceof LevelsNode)
        {
            int color     = (int) this.getInput(node.id, 0, x, y, z, time);
            double black    = this.hasInput(node.id, 1) ? this.getInput(node.id, 1, x, y, z, time) : 0;
            double midGamma = this.hasInput(node.id, 2) ? this.getInput(node.id, 2, x, y, z, time) : 1;
            double white    = this.hasInput(node.id, 3) ? this.getInput(node.id, 3, x, y, z, time) : 1;

            return applyLevels(color, black, midGamma, white);
        }

        if (node instanceof GlitchNode)
        {
            double strength = this.getInput(node.id, 0, x, y, z, time);
            double freq     = this.hasInput(node.id, 1) ? this.getInput(node.id, 1, x, y, z, time) : 1;

            if (strength <= 0 || freq <= 0) return 0;

            double slot    = Math.floor(time * freq);
            double active  = fract(Math.sin(slot * 127.1 + 311.7) * 43758.5) < strength ? 1 : 0;

            if (outputIndex == 0) return active;
            if (active == 0) return 0;
            if (outputIndex == 1) return fract(Math.sin(slot * 269.5) * 43758.5) * 2 - 1;
            if (outputIndex == 2) return fract(Math.sin(slot * 183.3) * 43758.5) * 2 - 1;

            return 0;
        }

        if (node instanceof PosterizeNode)
        {
            PosterizeNode p = (PosterizeNode) node;
            double value = this.getInput(node.id, 0, x, y, z, time);
            double steps = this.getInput(node.id, 1, x, y, z, time);

            if (steps <= 0) return value;

            if (p.mode == 1) /* Color */
            {
                int argb = (int) value;
                int a    = (argb >> 24) & 0xFF;
                int r    = clamp255((int) (Math.floor(((argb >> 16) & 0xFF) / 255.0 * steps) * 255.0 / steps));
                int g    = clamp255((int) (Math.floor(((argb >> 8) & 0xFF) / 255.0 * steps) * 255.0 / steps));
                int b    = clamp255((int) (Math.floor((argb & 0xFF) / 255.0 * steps) * 255.0 / steps));

                return (a << 24) | (r << 16) | (g << 8) | b;
            }

            return Math.floor(value * steps) / steps;
        }

        if (node instanceof ScreenOutputNode)
        {
            return this.getInput(node.id, outputIndex, x, y, z, time);
        }

        if (node instanceof VignetteEffectNode || node instanceof GrainEffectNode
            || node instanceof LetterboxEffectNode || node instanceof OverlayEffectNode
            || node instanceof DistortionEffectNode || node instanceof ColorGradeEffectNode
            || node instanceof LayerNode)
        {
            return 1;
        }

        return super.evaluate(node, outputIndex, x, y, z, time);
    }

    /* ------------------------------------------------------------------
     * Static color helpers
     * ------------------------------------------------------------------ */

    private static int blendScreen(int base, int blend)
    {
        int a  = (base >> 24) & 0xFF;
        int r  = 255 - (255 - ((base >> 16) & 0xFF)) * (255 - ((blend >> 16) & 0xFF)) / 255;
        int g  = 255 - (255 - ((base >> 8) & 0xFF))  * (255 - ((blend >> 8) & 0xFF))  / 255;
        int b  = 255 - (255 - (base & 0xFF))          * (255 - (blend & 0xFF))          / 255;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendOverlay(int base, int blend)
    {
        int a    = (base >> 24) & 0xFF;
        float br = ((base  >> 16) & 0xFF) / 255.0F;
        float bg = ((base  >> 8)  & 0xFF) / 255.0F;
        float bb = (base  & 0xFF)          / 255.0F;
        float lr = ((blend >> 16) & 0xFF) / 255.0F;
        float lg = ((blend >> 8)  & 0xFF) / 255.0F;
        float lb = (blend & 0xFF)          / 255.0F;

        float r = br < 0.5F ? 2 * br * lr : 1 - 2 * (1 - br) * (1 - lr);
        float g = bg < 0.5F ? 2 * bg * lg : 1 - 2 * (1 - bg) * (1 - lg);
        float b = bb < 0.5F ? 2 * bb * lb : 1 - 2 * (1 - bb) * (1 - lb);

        return (a << 24) | (clamp255((int) (r * 255)) << 16)
            | (clamp255((int) (g * 255)) << 8) | clamp255((int) (b * 255));
    }

    private static int applyGamma(int color, double gamma)
    {
        int a     = (color >> 24) & 0xFF;
        double inv = 1.0 / Math.max(1e-4, gamma);
        int r     = clamp255((int) (Math.pow(((color >> 16) & 0xFF) / 255.0, inv) * 255));
        int g     = clamp255((int) (Math.pow(((color >> 8)  & 0xFF) / 255.0, inv) * 255));
        int b     = clamp255((int) (Math.pow((color & 0xFF)          / 255.0, inv) * 255));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int applyHueSaturation(int color, double hueShift, double saturation)
    {
        int a    = (color >> 24) & 0xFF;
        float r  = ((color >> 16) & 0xFF) / 255.0F;
        float g  = ((color >> 8)  & 0xFF) / 255.0F;
        float b  = (color & 0xFF)          / 255.0F;

        float maxC  = Math.max(r, Math.max(g, b));
        float minC  = Math.min(r, Math.min(g, b));
        float delta = maxC - minC;
        float l     = (maxC + minC) * 0.5F;
        float s     = delta < 1e-5F ? 0F : delta / (1F - Math.abs(2 * l - 1));
        float h     = 0F;

        if (delta > 1e-5F)
        {
            if      (maxC == r) h = (float) ((((g - b) / delta % 6) + 6) % 6) / 6.0F;
            else if (maxC == g) h = ((b - r) / delta + 2) / 6.0F;
            else                h = ((r - g) / delta + 4) / 6.0F;
        }

        h = (float) ((h + hueShift / 360.0) % 1.0);
        if (h < 0) h += 1;
        s = (float) Math.max(0, Math.min(1, s * (1 + saturation)));

        float C   = (1F - Math.abs(2 * l - 1)) * s;
        float X   = C * (1F - Math.abs(h * 6 % 2 - 1));
        float m   = l - C * 0.5F;
        float nr, ng, nb;

        int sector = (int) (h * 6) % 6;

        if (sector == 0)      { nr = C; ng = X; nb = 0; }
        else if (sector == 1) { nr = X; ng = C; nb = 0; }
        else if (sector == 2) { nr = 0; ng = C; nb = X; }
        else if (sector == 3) { nr = 0; ng = X; nb = C; }
        else if (sector == 4) { nr = X; ng = 0; nb = C; }
        else                  { nr = C; ng = 0; nb = X; }

        return (a << 24)
            | (clamp255((int) ((nr + m) * 255)) << 16)
            | (clamp255((int) ((ng + m) * 255)) << 8)
            | clamp255((int) ((nb + m) * 255));
    }

    private static int applyBrightnessContrast(int color, double brightness, double contrast)
    {
        int a     = (color >> 24) & 0xFF;
        double r  = ((color >> 16) & 0xFF) / 255.0;
        double g  = ((color >> 8)  & 0xFF) / 255.0;
        double b  = (color & 0xFF)          / 255.0;

        r = 0.5 + (1 + contrast) * (r + brightness - 0.5);
        g = 0.5 + (1 + contrast) * (g + brightness - 0.5);
        b = 0.5 + (1 + contrast) * (b + brightness - 0.5);

        return (a << 24)
            | (clamp255((int) (r * 255)) << 16)
            | (clamp255((int) (g * 255)) << 8)
            | clamp255((int) (b * 255));
    }

    private static int applyLevels(int color, double black, double midGamma, double white)
    {
        int a      = (color >> 24) & 0xFF;
        double range = white - black;

        if (range == 0) range = 1;

        double inv = 1.0 / Math.max(1e-4, midGamma);
        int r      = clamp255((int) (Math.pow(Math.max(0, (((color >> 16) & 0xFF) / 255.0 - black) / range), inv) * 255));
        int g      = clamp255((int) (Math.pow(Math.max(0, (((color >> 8) & 0xFF) / 255.0 - black) / range), inv) * 255));
        int b      = clamp255((int) (Math.pow(Math.max(0, ((color & 0xFF) / 255.0 - black) / range), inv) * 255));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static double fract(double x)
    {
        return x - Math.floor(x);
    }

    private static int clamp255(int v)
    {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
