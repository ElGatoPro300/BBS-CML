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
import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

/**
 * Node graph for screen post-process effects. Extends ShapeFormGraph to reuse
 * all shared math/color/noise nodes; adds screen-specific node types and
 * replaces the shape OutputNode with ScreenOutputNode.
 */
public class ScreenNodeGraph extends ShapeFormGraph
{
    @Override
    public ShapeNode createNode(String type)
    {
        /* Screen-specific nodes */
        if ("screen_output".equals(type))       return new ScreenOutputNode();
        if ("screen_uv".equals(type))           return new ScreenUVNode();
        if ("screen_vignette".equals(type))     return new VignetteEffectNode();
        if ("screen_grain".equals(type))        return new GrainEffectNode();
        if ("screen_letterbox".equals(type))    return new LetterboxEffectNode();
        if ("screen_overlay".equals(type))      return new OverlayEffectNode();
        if ("screen_distortion".equals(type))   return new DistortionEffectNode();
        if ("screen_color_grade".equals(type))  return new ColorGradeEffectNode();
        if ("screen_layer".equals(type))        return new LayerNode();
        if ("sine_wave".equals(type))           return new SineWaveNode();
        if ("square_wave".equals(type))         return new SquareWaveNode();
        if ("screen_blend".equals(type))        return new ScreenBlendNode();
        if ("overlay_blend".equals(type))       return new OverlayBlendNode();
        if ("gamma_correction".equals(type))    return new GammaCorrectionNode();
        if ("hue_saturation".equals(type))      return new HueSaturationNode();
        if ("brightness_contrast".equals(type)) return new BrightnessContrastNode();
        if ("levels".equals(type))              return new LevelsNode();
        if ("glitch".equals(type))              return new GlitchNode();
        if ("posterize".equals(type))           return new PosterizeNode();

        /* Disable shape-only nodes that have no meaning in screen context */
        if ("output".equals(type))        return null;
        if ("bump".equals(type))          return null;
        if ("iris_shader".equals(type))   return null;
        if ("iris_attribute".equals(type)) return null;

        /* Delegate shared nodes (math, color, noise, value, etc.) */
        return super.createNode(type);
    }
}
