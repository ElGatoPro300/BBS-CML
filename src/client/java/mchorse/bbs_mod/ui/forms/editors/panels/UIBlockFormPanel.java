package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIBlockStateEditor;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormColorAdjustments;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormPaintTransform;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.block.BlockState;

public class UIBlockFormPanel extends UIFormPanel<BlockForm>
{
    public UIColor color;
    public UIFormColorAdjustments colorAdjustments;
    public UIColor paintColor;
    public UITrackpad paintIntensity;
    public UIFormPaintTransform paintTransform;
    public UIColor glowingColor;
    public UITrackpad glowIntensity;
    public UIPoseSectionCollapse colorSection;
    public UIPoseSectionCollapse glowSection;
    public UIBlockStateEditor stateEditor;
    public UITrackpad breaking;
    public UITrackpad repeatX;
    public UITrackpad repeatY;
    public UITrackpad repeatZ;
    public UIToggle repeatCenterX;
    public UIToggle repeatCenterY;
    public UIToggle repeatCenterZ;

    public UIBlockFormPanel(UIForm editor)
    {
        super(editor);

        this.color = new UIColor((c) ->
        {
            Color color = this.form.color.get().copy();
            Color value = Color.rgba(c);

            color.set(value.r, value.g, value.b, value.a);
            this.form.color.set(color);
        }).withAlpha();
        this.colorAdjustments = new UIFormColorAdjustments(() -> this.form.color.get(), (color) ->
        {
            this.form.color.setRuntimeValue(null);
            this.form.color.set(color);
        });
        this.paintColor = new UIColor((c) ->
        {
            Color color = Color.rgba(c);
            PaintSettings settings = this.form.paintSettings.get().copy();

            color.a = settings.intensity;
            this.form.paintColor.set(color);

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            settings.applyAutoShaderShadow();
            this.form.paintSettings.set(settings);
        });
        this.paintColor.tooltip(UIKeys.FORMS_EDITORS_PAINT_COLOR);
        this.paintIntensity = new UITrackpad((value) ->
        {
            PaintSettings settings = this.form.paintSettings.get().copy();
            float intensity = PaintSettings.clampIntensity(value.floatValue());

            settings.intensity = intensity;
            settings.applyAutoShaderShadow();
            this.form.paintSettings.set(settings);

            Color legacy = this.form.paintColor.get().copy();

            legacy.a = intensity;
            this.form.paintColor.set(legacy);
        });
        this.paintIntensity.increment(0.05D).values(0.1D, 0.05D, 0.2D).limit(PaintSettings.MIN_INTENSITY, PaintSettings.MAX_INTENSITY);
        this.paintIntensity.tooltip(UIKeys.FORMS_EDITORS_PAINT_INTENSITY);
        this.paintTransform = new UIFormPaintTransform(() -> this.form.paintSettings.get(), (settings) -> this.form.paintSettings.set(settings));
        this.glowingColor = new UIColor((c) ->
        {
            Color color = Color.rgba(c);

            color.a = 1F;
            this.form.glowingColor.set(color);

            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            this.form.glowSettings.set(settings);
        });
        this.glowingColor.tooltip(UIKeys.FORMS_EDITORS_GLOW);
        this.glowIntensity = new UITrackpad((value) ->
        {
            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.intensity = value.floatValue();
            this.form.glowSettings.set(settings);
        });
        this.glowIntensity.increment(0.05D).values(0.1D, 0.05D, 0.2D);
        this.glowIntensity.tooltip(UIKeys.FORMS_EDITORS_GLOW_INTENSITY);
        this.colorSection = new UIPoseSectionCollapse(
            UIKeys.FILM_REPLAY_TRACK_COLOR,
            UIReplaysEditor.getColor("color"),
            UI.column(
                UI.label(UIKeys.FORMS_EDITORS_BLEND_COLOR).marginTop(4),
                this.color,
                UI.label(UIKeys.FORMS_EDITORS_PAINT_COLOR).marginTop(4),
                this.paintColor,
                UI.label(UIKeys.FORMS_EDITORS_PAINT_INTENSITY),
                this.paintIntensity,
                this.paintTransform,
                this.colorAdjustments.marginTop(4)
            )
        );
        this.glowSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_GLOW,
            Colors.ORANGE,
            UI.column(
                UI.label(UIKeys.FORMS_EDITORS_GLOWING_COLOR).marginTop(4),
                this.glowingColor,
                UI.label(UIKeys.FORMS_EDITORS_GLOW_INTENSITY),
                this.glowIntensity
            )
        );
        this.stateEditor = new UIBlockStateEditor((blockState) -> this.form.blockState.set(blockState));
        this.breaking = new UITrackpad((v) -> this.form.breaking.set(v.intValue())).integer().limit(0, 10);
        this.breaking.tooltip(UIKeys.FORMS_EDITORS_BLOCK_BREAKING);
        this.repeatX = new UITrackpad((v) -> this.form.repeatX.set(v.intValue())).integer().limit(1, 64);
        this.repeatX.tooltip(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_X);
        this.repeatY = new UITrackpad((v) -> this.form.repeatY.set(v.intValue())).integer().limit(1, 64);
        this.repeatY.tooltip(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_Y);
        this.repeatZ = new UITrackpad((v) -> this.form.repeatZ.set(v.intValue())).integer().limit(1, 64);
        this.repeatZ.tooltip(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_Z);
        this.repeatCenterX = new UIToggle(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_CENTER_X, (b) -> this.form.repeatCenterX.set(b.getValue()));
        this.repeatCenterX.tooltip(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_CENTER_X_TOOLTIP);
        this.repeatCenterY = new UIToggle(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_CENTER_Y, (b) -> this.form.repeatCenterY.set(b.getValue()));
        this.repeatCenterY.tooltip(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_CENTER_Y_TOOLTIP);
        this.repeatCenterZ = new UIToggle(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_CENTER_Z, (b) -> this.form.repeatCenterZ.set(b.getValue()));
        this.repeatCenterZ.tooltip(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_CENTER_Z_TOOLTIP);

        this.options.add(this.colorSection, this.glowSection, this.stateEditor);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_BLOCK_REPEAT).marginTop(6), UI.row(this.repeatX, this.repeatY, this.repeatZ));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_BLOCK_REPEAT_CENTER).marginTop(6), UI.row(this.repeatCenterX, this.repeatCenterY, this.repeatCenterZ));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_BLOCK_BREAKING).marginTop(6), this.breaking);
    }

    @Override
    public void startEdit(BlockForm form)
    {
        super.startEdit(form);

        BlockState blockState = this.form.blockState.get();

        this.color.setColor(form.color.get().getARGBColor());
        this.colorAdjustments.syncFromForm();
        PaintSettings paint = form.paintSettings.get();
        Color paintDisplay = new Color();

        paint.resolveColor(form.paintColor.get(), paintDisplay);
        this.paintColor.setColor(paintDisplay.getRGBColor());
        this.paintIntensity.setValue(paint.intensity);
        this.paintTransform.syncFromForm();
        GlowSettings glow = form.glowSettings.get();
        Color glowDisplay = new Color();

        glow.resolveColor(form.glowingColor.get(), glowDisplay);
        this.glowingColor.setColor(glowDisplay.getRGBColor());

        this.glowIntensity.setValue(glow.intensity);
        this.stateEditor.setBlockState(blockState);
        this.breaking.setValue(form.breaking.get());
        this.repeatX.setValue(form.repeatX.get());
        this.repeatY.setValue(form.repeatY.get());
        this.repeatZ.setValue(form.repeatZ.get());
        this.repeatCenterX.setValue(form.repeatCenterX.get());
        this.repeatCenterY.setValue(form.repeatCenterY.get());
        this.repeatCenterZ.setValue(form.repeatCenterZ.get());
    }
}