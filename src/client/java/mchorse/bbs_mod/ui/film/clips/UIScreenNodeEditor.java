package mchorse.bbs_mod.ui.film.clips;

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
import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.toolbar.ToolbarItem;
import mchorse.bbs_mod.ui.forms.editors.panels.shape.UIShapeNodeEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;

/**
 * Node editor for ScreenNodeGraph. Extends UIShapeNodeEditor to reuse all shared
 * rendering and interaction logic, overriding only what is specific to screen nodes.
 */
public class UIScreenNodeEditor extends UIShapeNodeEditor
{
    protected static final int HEADER_SCREEN_OUTPUT = Colors.A100 | 0xFF4400;
    protected static final int HEADER_WAVE          = Colors.A100 | 0x2277EE;
    protected static final int HEADER_BLEND         = Colors.A100 | 0xAA44CC;
    protected static final int HEADER_GRADE         = Colors.A100 | 0xFF9922;
    protected static final int HEADER_GLITCH        = Colors.A100 | 0xFF1166;
    protected static final int HEADER_EFFECT        = Colors.A100 | 0x226644;

    @Override
    protected String getNodeTitle(ShapeNode node)
    {
        if (node instanceof ScreenOutputNode)   return UIKeys.SCREEN_NODE_SCREEN_OUTPUT.get();
        if (node instanceof ScreenUVNode)       return UIKeys.SCREEN_NODE_SCREEN_UV.get();
        if (node instanceof ColorGradeEffectNode) return UIKeys.SCREEN_NODE_COLOR_GRADE.get();
        if (node instanceof LayerNode)          return UIKeys.SCREEN_NODE_LAYER.get();
        if (node instanceof DistortionEffectNode) return UIKeys.SCREEN_NODE_DISTORTION.get();
        if (node instanceof VignetteEffectNode) return UIKeys.SCREEN_NODE_VIGNETTE.get();
        if (node instanceof GrainEffectNode)    return UIKeys.SCREEN_NODE_GRAIN.get();
        if (node instanceof LetterboxEffectNode) return UIKeys.SCREEN_NODE_LETTERBOX.get();
        if (node instanceof OverlayEffectNode)  return UIKeys.SCREEN_NODE_OVERLAY.get();
        if (node instanceof SineWaveNode)     return UIKeys.SCREEN_NODE_SINE_WAVE.get();
        if (node instanceof SquareWaveNode)   return UIKeys.SCREEN_NODE_SQUARE_WAVE.get();
        if (node instanceof ScreenBlendNode)  return UIKeys.SCREEN_NODE_SCREEN_BLEND.get();
        if (node instanceof OverlayBlendNode) return UIKeys.SCREEN_NODE_OVERLAY_BLEND.get();
        if (node instanceof GammaCorrectionNode)  return UIKeys.SCREEN_NODE_GAMMA_CORRECTION.get();
        if (node instanceof HueSaturationNode)    return UIKeys.SCREEN_NODE_HUE_SATURATION.get();
        if (node instanceof BrightnessContrastNode) return UIKeys.SCREEN_NODE_BRIGHTNESS_CONTRAST.get();
        if (node instanceof LevelsNode)       return UIKeys.SCREEN_NODE_LEVELS.get();
        if (node instanceof GlitchNode)       return UIKeys.SCREEN_NODE_GLITCH.get();
        if (node instanceof PosterizeNode)
        {
            return ((PosterizeNode) node).mode == 1 ? UIKeys.SCREEN_NODE_POSTERIZE_COLOR.get() : UIKeys.SCREEN_NODE_POSTERIZE.get();
        }

        return super.getNodeTitle(node);
    }

    @Override
    protected int getNodeHeaderColor(ShapeNode node)
    {
        if (node instanceof ScreenOutputNode)    return HEADER_SCREEN_OUTPUT;
        if (node instanceof ScreenUVNode)        return HEADER_INPUT;
        if (node instanceof VignetteEffectNode || node instanceof GrainEffectNode
            || node instanceof LetterboxEffectNode || node instanceof OverlayEffectNode
            || node instanceof DistortionEffectNode || node instanceof ColorGradeEffectNode) return HEADER_EFFECT;
        if (node instanceof LayerNode) return HEADER_DEFAULT;
        if (node instanceof SineWaveNode || node instanceof SquareWaveNode) return HEADER_WAVE;
        if (node instanceof ScreenBlendNode || node instanceof OverlayBlendNode) return HEADER_BLEND;
        if (node instanceof GammaCorrectionNode || node instanceof HueSaturationNode
            || node instanceof BrightnessContrastNode || node instanceof LevelsNode) return HEADER_GRADE;
        if (node instanceof GlitchNode)    return HEADER_GLITCH;
        if (node instanceof PosterizeNode) return HEADER_MATH;

        return super.getNodeHeaderColor(node);
    }

    @Override
    protected int getNodeWidth(ShapeNode node)
    {
        if (node instanceof ScreenOutputNode) return 160;

        return super.getNodeWidth(node);
    }

    @Override
    protected void appendNodeContextMenu(UIContext context, ShapeNode node, ContextMenuManager menu)
    {
        if (node instanceof PosterizeNode)
        {
            PosterizeNode p = (PosterizeNode) node;

            menu.action(Icons.GEAR, UIKeys.RAW_MODE_SCALAR, p.mode == 0 ? Colors.ACTIVE : 0,
                () -> p.mode = 0);
            menu.action(Icons.GEAR, UIKeys.RAW_MODE_COLOR,  p.mode == 1 ? Colors.ACTIVE : 0,
                () -> p.mode = 1);
        }
    }

    @Override
    protected void populateAddMenu(UIContext context, ContextMenuManager menu)
    {
        /* Input — green */
        ContextMenuManager inputSub = new ContextMenuManager();
        inputSub.action(Icons.MAXIMIZE,       UIKeys.RAW_VALUE,      Colors.POSITIVE, () -> this.addNode("value"));
        inputSub.action(Icons.MATERIAL,       UIKeys.RAW_COLOR,      Colors.POSITIVE, () -> this.addNode("color"));
        inputSub.action(Icons.TIME,           UIKeys.RAW_TIME,       Colors.POSITIVE, () -> this.addNode("time"));
        inputSub.action(Icons.ALL_DIRECTIONS, UIKeys.RAW_COORDINATE, Colors.POSITIVE, () -> this.addNode("coordinate"));
        inputSub.action(Icons.IMAGE,          UIKeys.RAW_TEXTURE,    Colors.POSITIVE, () -> this.addNode("texture"));
        menu.action(Icons.DOWNLOAD, UIKeys.RAW_INPUT, Colors.POSITIVE, () -> context.replaceContextMenu(inputSub.create()));

        /* Math — blue */
        ContextMenuManager mathSub = new ContextMenuManager();
        mathSub.action(Icons.GEAR,            UIKeys.RAW_MATH,       Colors.ACTIVE, () -> this.addNode("math"));
        mathSub.action(Icons.ALL_DIRECTIONS,  UIKeys.RAW_VECTOR_MATH,Colors.ACTIVE, () -> this.addNode("vector_math"));
        mathSub.action(Icons.GEAR,            UIKeys.RAW_REMAP,      Colors.ACTIVE, () -> this.addNode("remap"));
        mathSub.action(Icons.GEAR,            UIKeys.RAW_CLAMP,      Colors.ACTIVE, () -> this.addNode("clamp"));
        mathSub.action(Icons.GEAR,            UIKeys.RAW_SMOOTHSTEP, Colors.ACTIVE, () -> this.addNode("smoothstep"));
        mathSub.action(Icons.REFRESH,         UIKeys.RAW_INVERT,     Colors.ACTIVE, () -> this.addNode("invert"));
        mathSub.action(Icons.GEAR,            UIKeys.RAW_POSTERIZE,  Colors.ACTIVE, () -> this.addNode("posterize"));
        menu.action(Icons.GEAR, UIKeys.RAW_MATH, Colors.ACTIVE, () -> context.replaceContextMenu(mathSub.create()));

        /* Color — orange */
        ContextMenuManager colorSub = new ContextMenuManager();
        colorSub.action(Icons.REFRESH, UIKeys.RAW_MIX_COLOR,      Colors.ORANGE, () -> this.addNode("mix_color"));
        colorSub.action(Icons.FILTER,  UIKeys.RAW_SPLIT_COLOR,    Colors.ORANGE, () -> this.addNode("split_color"));
        colorSub.action(Icons.FILTER,  UIKeys.RAW_COMBINE_COLOR,  Colors.ORANGE, () -> this.addNode("combine_color"));
        colorSub.action(Icons.FILTER,  UIKeys.RAW_SCREEN_BLEND,   Colors.ORANGE, () -> this.addNode("screen_blend"));
        colorSub.action(Icons.FILTER,  UIKeys.RAW_OVERLAY_BLEND,  Colors.ORANGE, () -> this.addNode("overlay_blend"));
        menu.action(Icons.MATERIAL, UIKeys.RAW_COLOR, Colors.ORANGE, () -> context.replaceContextMenu(colorSub.create()));

        /* Noise — yellow */
        ContextMenuManager noiseSub = new ContextMenuManager();
        noiseSub.action(Icons.SOUND, UIKeys.RAW_PERLIN_NOISE, Colors.INACTIVE, () -> this.addNode("noise"));
        noiseSub.action(Icons.SOUND, UIKeys.RAW_VORONOI,      Colors.INACTIVE, () -> this.addNode("voronoi"));
        noiseSub.action(Icons.SOUND, UIKeys.RAW_FLOW_NOISE,   Colors.INACTIVE, () -> this.addNode("flow_noise"));
        menu.action(Icons.SOUND, UIKeys.RAW_NOISE, Colors.INACTIVE, () -> context.replaceContextMenu(noiseSub.create()));

        /* Wave */
        ContextMenuManager waveSub = new ContextMenuManager();
        waveSub.action(Icons.ARC,  UIKeys.RAW_SINE_WAVE,   Colors.BLUE, () -> this.addNode("sine_wave"));
        waveSub.action(Icons.ARC,  UIKeys.RAW_SQUARE_WAVE, Colors.BLUE, () -> this.addNode("square_wave"));
        menu.action(Icons.ARC, UIKeys.RAW_WAVE, Colors.BLUE, () -> context.replaceContextMenu(waveSub.create()));

        /* Adjust / color grade */
        ContextMenuManager adjustSub = new ContextMenuManager();
        adjustSub.action(Icons.FILTER, UIKeys.RAW_GAMMA_CORRECTION,    Colors.YELLOW, () -> this.addNode("gamma_correction"));
        adjustSub.action(Icons.FILTER, UIKeys.RAW_HUE_SATURATION,    Colors.YELLOW, () -> this.addNode("hue_saturation"));
        adjustSub.action(Icons.FILTER, UIKeys.RAW_BRIGHTNESS_CONTRAST, Colors.YELLOW, () -> this.addNode("brightness_contrast"));
        adjustSub.action(Icons.FILTER, UIKeys.RAW_LEVELS,              Colors.YELLOW, () -> this.addNode("levels"));
        adjustSub.action(Icons.EXCHANGE, UIKeys.RAW_GLITCH,            Colors.MAGENTA, () -> this.addNode("glitch"));
        menu.action(Icons.FILTER, UIKeys.RAW_ADJUST, Colors.YELLOW, () -> context.replaceContextMenu(adjustSub.create()));

        /* Utility — no color */
        ContextMenuManager utilitySub = new ContextMenuManager();
        utilitySub.action(Icons.GEAR, UIKeys.RAW_TRIGGER, () -> this.addNode("trigger"));
        utilitySub.action(Icons.EDIT, UIKeys.RAW_COMMENT, () -> this.addNode("comment"));
        menu.action(Icons.MORE, UIKeys.RAW_UTILITY, () -> context.replaceContextMenu(utilitySub.create()));

        /* Effects — standalone effect nodes */
        ContextMenuManager effectSub = new ContextMenuManager();
        effectSub.action(Icons.FILM,     UIKeys.RAW_VIGNETTE,   0xFF226644, () -> this.addNode("screen_vignette"));
        effectSub.action(Icons.FILM,     UIKeys.RAW_GRAIN,      0xFF226644, () -> this.addNode("screen_grain"));
        effectSub.action(Icons.FILM,     UIKeys.RAW_LETTERBOX,  0xFF226644, () -> this.addNode("screen_letterbox"));
        effectSub.action(Icons.FILM,     UIKeys.RAW_OVERLAY,    0xFF226644, () -> this.addNode("screen_overlay"));
        effectSub.action(Icons.EXCHANGE, UIKeys.RAW_DISTORTION,   0xFF226644, () -> this.addNode("screen_distortion"));
        effectSub.action(Icons.FILTER,   UIKeys.RAW_COLOR_GRADE,  0xFF226644, () -> this.addNode("screen_color_grade"));
        menu.action(Icons.FILM, UIKeys.RAW_EFFECTS, 0xFF226644, () -> context.replaceContextMenu(effectSub.create()));

        /* Output — screen_output + screen_uv */
        ContextMenuManager outputSub = new ContextMenuManager();
        outputSub.action(Icons.DOWNLOAD, UIKeys.RAW_SCREEN_OUTPUT, Colors.NEGATIVE, () -> this.addNode("screen_output"));
        outputSub.action(Icons.GLOBE,    UIKeys.RAW_SCREEN_UV,     Colors.NEGATIVE, () -> this.addNode("screen_uv"));
        outputSub.action(Icons.COPY,     UIKeys.RAW_LAYER,         Colors.INACTIVE, () -> this.addNode("screen_layer"));
        menu.action(Icons.UPLOAD, UIKeys.RAW_OUTPUT, Colors.NEGATIVE, () -> context.replaceContextMenu(outputSub.create()));
    }

    /**
     * Fills the screen-node graph toolbar "Add" section with the same node
     * categories as the editor context menu.
     */
    public void populateToolbarAddItems(List<ToolbarItem> items)
    {
        items.add(ToolbarItem.submenu(UIKeys.RAW_INPUT,
            ToolbarItem.action(UIKeys.RAW_VALUE).icon(Icons.MAXIMIZE).accentColor(Colors.POSITIVE).run(() -> this.addNode("value")),
            ToolbarItem.action(UIKeys.RAW_COLOR).icon(Icons.MATERIAL).accentColor(Colors.POSITIVE).run(() -> this.addNode("color")),
            ToolbarItem.action(UIKeys.RAW_TIME).icon(Icons.TIME).accentColor(Colors.POSITIVE).run(() -> this.addNode("time")),
            ToolbarItem.action(UIKeys.RAW_COORDINATE).icon(Icons.ALL_DIRECTIONS).accentColor(Colors.POSITIVE).run(() -> this.addNode("coordinate")),
            ToolbarItem.action(UIKeys.RAW_TEXTURE).icon(Icons.IMAGE).accentColor(Colors.POSITIVE).run(() -> this.addNode("texture"))
        ).icon(Icons.DOWNLOAD).accentColor(Colors.POSITIVE));

        items.add(ToolbarItem.submenu(UIKeys.RAW_MATH,
            ToolbarItem.action(UIKeys.RAW_MATH).icon(Icons.GEAR).accentColor(Colors.ACTIVE).run(() -> this.addNode("math")),
            ToolbarItem.action(UIKeys.RAW_VECTOR_MATH).icon(Icons.ALL_DIRECTIONS).accentColor(Colors.ACTIVE).run(() -> this.addNode("vector_math")),
            ToolbarItem.action(UIKeys.RAW_REMAP).icon(Icons.GEAR).accentColor(Colors.ACTIVE).run(() -> this.addNode("remap")),
            ToolbarItem.action(UIKeys.RAW_CLAMP).icon(Icons.GEAR).accentColor(Colors.ACTIVE).run(() -> this.addNode("clamp")),
            ToolbarItem.action(UIKeys.RAW_SMOOTHSTEP).icon(Icons.GEAR).accentColor(Colors.ACTIVE).run(() -> this.addNode("smoothstep")),
            ToolbarItem.action(UIKeys.RAW_INVERT).icon(Icons.REFRESH).accentColor(Colors.ACTIVE).run(() -> this.addNode("invert")),
            ToolbarItem.action(UIKeys.RAW_POSTERIZE).icon(Icons.GEAR).accentColor(Colors.ACTIVE).run(() -> this.addNode("posterize"))
        ).icon(Icons.GEAR).accentColor(Colors.ACTIVE));

        items.add(ToolbarItem.submenu(UIKeys.RAW_COLOR,
            ToolbarItem.action(UIKeys.RAW_MIX_COLOR).icon(Icons.REFRESH).accentColor(Colors.ORANGE).run(() -> this.addNode("mix_color")),
            ToolbarItem.action(UIKeys.RAW_SPLIT_COLOR).icon(Icons.FILTER).accentColor(Colors.ORANGE).run(() -> this.addNode("split_color")),
            ToolbarItem.action(UIKeys.RAW_COMBINE_COLOR).icon(Icons.FILTER).accentColor(Colors.ORANGE).run(() -> this.addNode("combine_color")),
            ToolbarItem.action(UIKeys.RAW_SCREEN_BLEND).icon(Icons.FILTER).accentColor(Colors.ORANGE).run(() -> this.addNode("screen_blend")),
            ToolbarItem.action(UIKeys.RAW_OVERLAY_BLEND).icon(Icons.FILTER).accentColor(Colors.ORANGE).run(() -> this.addNode("overlay_blend"))
        ).icon(Icons.MATERIAL).accentColor(Colors.ORANGE));

        items.add(ToolbarItem.submenu(UIKeys.RAW_NOISE,
            ToolbarItem.action(UIKeys.RAW_PERLIN_NOISE).icon(Icons.SOUND).accentColor(Colors.INACTIVE).run(() -> this.addNode("noise")),
            ToolbarItem.action(UIKeys.RAW_VORONOI).icon(Icons.SOUND).accentColor(Colors.INACTIVE).run(() -> this.addNode("voronoi")),
            ToolbarItem.action(UIKeys.RAW_FLOW_NOISE).icon(Icons.SOUND).accentColor(Colors.INACTIVE).run(() -> this.addNode("flow_noise"))
        ).icon(Icons.SOUND).accentColor(Colors.INACTIVE));

        items.add(ToolbarItem.submenu(UIKeys.RAW_WAVE,
            ToolbarItem.action(UIKeys.RAW_SINE_WAVE).icon(Icons.ARC).accentColor(Colors.BLUE).run(() -> this.addNode("sine_wave")),
            ToolbarItem.action(UIKeys.RAW_SQUARE_WAVE).icon(Icons.ARC).accentColor(Colors.BLUE).run(() -> this.addNode("square_wave"))
        ).icon(Icons.ARC).accentColor(Colors.BLUE));

        items.add(ToolbarItem.submenu(UIKeys.RAW_ADJUST,
            ToolbarItem.action(UIKeys.RAW_GAMMA_CORRECTION).icon(Icons.FILTER).accentColor(Colors.YELLOW).run(() -> this.addNode("gamma_correction")),
            ToolbarItem.action(UIKeys.RAW_HUE_SATURATION).icon(Icons.FILTER).accentColor(Colors.YELLOW).run(() -> this.addNode("hue_saturation")),
            ToolbarItem.action(UIKeys.RAW_BRIGHTNESS_CONTRAST).icon(Icons.FILTER).accentColor(Colors.YELLOW).run(() -> this.addNode("brightness_contrast")),
            ToolbarItem.action(UIKeys.RAW_LEVELS).icon(Icons.FILTER).accentColor(Colors.YELLOW).run(() -> this.addNode("levels")),
            ToolbarItem.action(UIKeys.RAW_GLITCH).icon(Icons.EXCHANGE).accentColor(Colors.MAGENTA).run(() -> this.addNode("glitch"))
        ).icon(Icons.FILTER).accentColor(Colors.YELLOW));

        items.add(ToolbarItem.submenu(UIKeys.RAW_UTILITY,
            ToolbarItem.action(UIKeys.RAW_TRIGGER).icon(Icons.GEAR).run(() -> this.addNode("trigger")),
            ToolbarItem.action(UIKeys.RAW_COMMENT).icon(Icons.EDIT).run(() -> this.addNode("comment"))
        ).icon(Icons.MORE));

        items.add(ToolbarItem.submenu(UIKeys.RAW_EFFECTS,
            ToolbarItem.action(UIKeys.RAW_VIGNETTE).icon(Icons.FILM).accentColor(0xFF226644).run(() -> this.addNode("screen_vignette")),
            ToolbarItem.action(UIKeys.RAW_GRAIN).icon(Icons.FILM).accentColor(0xFF226644).run(() -> this.addNode("screen_grain")),
            ToolbarItem.action(UIKeys.RAW_LETTERBOX).icon(Icons.FILM).accentColor(0xFF226644).run(() -> this.addNode("screen_letterbox")),
            ToolbarItem.action(UIKeys.RAW_OVERLAY).icon(Icons.FILM).accentColor(0xFF226644).run(() -> this.addNode("screen_overlay")),
            ToolbarItem.action(UIKeys.RAW_DISTORTION).icon(Icons.EXCHANGE).accentColor(0xFF226644).run(() -> this.addNode("screen_distortion")),
            ToolbarItem.action(UIKeys.RAW_COLOR_GRADE).icon(Icons.FILTER).accentColor(0xFF226644).run(() -> this.addNode("screen_color_grade"))
        ).icon(Icons.FILM).accentColor(0xFF226644));

        items.add(ToolbarItem.submenu(UIKeys.RAW_OUTPUT,
            ToolbarItem.action(UIKeys.RAW_SCREEN_OUTPUT).icon(Icons.DOWNLOAD).accentColor(Colors.NEGATIVE).run(() -> this.addNode("screen_output")),
            ToolbarItem.action(UIKeys.RAW_SCREEN_UV).icon(Icons.GLOBE).accentColor(Colors.NEGATIVE).run(() -> this.addNode("screen_uv")),
            ToolbarItem.action(UIKeys.RAW_LAYER).icon(Icons.COPY).accentColor(Colors.INACTIVE).run(() -> this.addNode("screen_layer"))
        ).icon(Icons.UPLOAD).accentColor(Colors.NEGATIVE));
    }
}