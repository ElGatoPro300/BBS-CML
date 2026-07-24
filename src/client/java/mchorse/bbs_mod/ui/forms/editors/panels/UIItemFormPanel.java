package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormColorAdjustments;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormPaintTransform;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.render.model.json.ModelTransformationMode;

public class UIItemFormPanel extends UIFormPanel<ItemForm>
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
    public UIButton modelTransform;
    public UIToggle sameAnimationWhenDropped;
    public UIItemStack itemStackEditor;

    public UIItemFormPanel(UIForm editor)
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
        this.modelTransform = new UIButton(IKey.EMPTY, (b) ->
        {
            this.getContext().replaceContextMenu((menu) ->
            {
                for (ModelTransformationMode value : ModelTransformationMode.values())
                {
                    if (this.form.modelTransform.get() == value)
                    {
                        menu.action(Icons.LINE, IKey.constant(value.asString()), true, () -> {});
                    }
                    else
                    {
                        menu.action(Icons.LINE, IKey.constant(value.asString()), () -> this.setModelTransform(value));
                    }
                }
            });
        });

        this.sameAnimationWhenDropped = new UIToggle(UIKeys.FORMS_EDITORS_ITEM_SAME_ANIMATION_WHEN_DROPPED, (b) -> this.form.sameAnimationWhenDropped.set(b.getValue()));
        this.sameAnimationWhenDropped.tooltip(UIKeys.FORMS_EDITORS_ITEM_SAME_ANIMATION_WHEN_DROPPED_TOOLTIP);
        this.itemStackEditor = new UIItemStack((itemStack) -> this.form.stack.set(itemStack.copy()));

        this.options.add(this.colorSection, this.glowSection, UI.label(UIKeys.FORMS_EDITORS_ITEM_TRANSFORMS), this.modelTransform, this.sameAnimationWhenDropped, this.itemStackEditor);
    }

    private void setModelTransform(ModelTransformationMode value)
    {
        this.form.modelTransform.set(value);

        this.modelTransform.label = IKey.constant(value.asString());
    }

    @Override
    public void startEdit(ItemForm form)
    {
        super.startEdit(form);

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
        this.modelTransform.label = IKey.constant(form.modelTransform.get().asString());
        this.sameAnimationWhenDropped.setValue(form.sameAnimationWhenDropped.get());
        this.itemStackEditor.setStack(form.stack.get());
    }
}
