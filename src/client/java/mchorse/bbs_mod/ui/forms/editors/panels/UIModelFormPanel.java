package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormColorAdjustments;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectTransformCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.shapes.UIShapeKeys;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.Set;

public class UIModelFormPanel extends UIFormPanel<ModelForm>
{
    public UIColor color;
    public UIFormColorAdjustments colorAdjustments;
    public UIEffectTransformCollapse colorTransform;
    public UIColor paintColor;
    public UITrackpad paintIntensity;
    public UIEffectTransformCollapse paintTransform;
    public UIColor glowingColor;
    public UITrackpad glowIntensity;

    public UIPoseSectionCollapse colorSection;
    public UIPoseSectionCollapse glowSection;

    public UIModelPoseEditor poseEditor;
    public UIShapeKeys shapeKeys;
    public UITrackpad pbrNormalIntensity;
    public UITrackpad pbrSpecularIntensity;

    public UIButton pickModel;
    public UIButton pick;

    public UIModelFormPanel(UIForm editor)
    {
        super(editor);

        this.pickModel = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_MODEL, (b) ->
        {
            UIListOverlayPanel list = new UIListOverlayPanel(UIKeys.FORMS_EDITOR_MODEL_MODELS, (l) ->
            {
                this.form.model.set(l);

                if (Window.isCtrlPressed())
                {
                    ModelInstance model = ModelFormRenderer.getModel(this.form);

                    if (model != null)
                    {
                        this.form.texture.set(model.texture);
                    }
                }

                this.editor.startEdit(this.form);
            });

            list.addValues(BBSModClient.getModels().getAvailableKeys());
            list.list.list.sort();
            list.setValue(this.form.model.get());

            UIOverlay.addOverlay(this.getContext(), list);
        });
        this.color = new UIColor((c) ->
        {
            Color color = this.form.color.get().copy();
            Color value = new Color().set(c);

            color.set(value.r, value.g, value.b, value.a);
            this.form.color.set(color);
        }).withAlpha();
        this.color.direction(Direction.LEFT);
        this.color.context((menu) -> menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, this::resetMainColor));
        this.colorAdjustments = new UIFormColorAdjustments(() -> this.form.color.get(), (color) ->
        {
            this.form.color.setRuntimeValue(null);
            this.form.color.set(color);
        });
        this.colorTransform = new UIEffectTransformCollapse((apply) ->
        {
            Color copy = this.form.color.get().copy();

            if (copy.transform == null)
            {
                copy.transform = new EffectTransform();
            }

            apply.accept(copy.transform);
            this.form.color.setRuntimeValue(null);
            this.form.color.set(copy);
        });
        this.paintColor = new UIColor((c) ->
        {
            Color color = new Color().set(c);
            PaintSettings settings = this.form.paintSettings.get().copy();

            color.a = settings.intensity;
            this.form.paintColor.set(color);

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            settings.applyAutoShaderShadow();
            this.form.paintSettings.set(settings);
        });
        this.paintColor.direction(Direction.LEFT);
        this.paintColor.tooltip(UIKeys.FORMS_EDITORS_PAINT_COLOR);
        this.paintColor.context((menu) -> menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, this::resetPaintColor));
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
        this.paintTransform = new UIEffectTransformCollapse((apply) ->
        {
            PaintSettings settings = this.form.paintSettings.get().copy();

            if (settings.transform == null)
            {
                settings.transform = new EffectTransform();
            }

            apply.accept(settings.transform);
            this.form.paintSettings.set(settings);
        });
        this.glowingColor = new UIColor((c) ->
        {
            Color color = new Color().set(c);

            color.a = 1F;
            this.form.glowingColor.set(color);

            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            this.form.glowSettings.set(settings);
        });
        this.glowingColor.direction(Direction.LEFT);
        this.glowingColor.tooltip(UIKeys.FORMS_EDITORS_GLOW);
        this.glowingColor.context((menu) -> menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, this::resetGlowColor));
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
                this.colorTransform,
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
        this.poseEditor = new UIModelPoseEditor();
        this.poseEditor.setDefaultTextureSupplier(() ->
        {
            Link base = this.form.texture.get();
            if (base != null)
            {
                return base;
            }

            ModelInstance model = ModelFormRenderer.getModel(this.form);
            return model != null ? model.texture : null;
        });
        this.poseEditor.setTexturePreviewFormSupplier(() -> this.form);
        this.shapeKeys = new UIShapeKeys();
        this.pick = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, (b) ->
        {
            Link link = this.form.texture.get();
            ModelInstance model = ModelFormRenderer.getModel(this.form);

            if (model != null && link == null)
            {
                link = model.texture;
            }

            UITexturePicker picker = UITexturePicker.open(this.getContext(), link, (l) -> this.form.texture.set(l));

            if (picker != null)
            {
                picker.withFormPreview(() -> this.form);
            }
        });
        this.pbrNormalIntensity = new UITrackpad((value) -> this.form.pbrNormalIntensity.set(value.floatValue()));
        this.pbrNormalIntensity.tooltip(UIKeys.FORMS_EDITOR_MODEL_PBR_NORMAL_INTENSITY);
        this.pbrSpecularIntensity = new UITrackpad((value) -> this.form.pbrSpecularIntensity.set(value.floatValue()));
        this.pbrSpecularIntensity.tooltip(UIKeys.FORMS_EDITOR_MODEL_PBR_SPECULAR_INTENSITY);

        this.options.add(this.pickModel);
        if (BBSSettings.pickLimbTexture.get())
        {
            this.options.add(this.pick);
        }
        if (BBSSettings.modelPbrPanelControls != null && BBSSettings.modelPbrPanelControls.get())
        {
            this.options.add(this.pbrNormalIntensity, this.pbrSpecularIntensity);
        }

        this.options.add(this.colorSection, this.glowSection, this.poseEditor);
    }

    private void resetMainColor()
    {
        if (this.form == null)
        {
            return;
        }

        Color white = Color.white();

        this.form.color.set(white.copy());
        this.color.setColor(white.getARGBColor());
        this.colorTransform.setEffectTransform(new EffectTransform());
        this.colorAdjustments.syncFromForm();
        this.editor.startEdit(this.form);
    }

    private void resetPaintColor()
    {
        if (this.form == null)
        {
            return;
        }

        Color legacy = new Color().set(1F, 1F, 1F, 0F);
        PaintSettings settings = new PaintSettings();

        this.form.paintColor.set(legacy.copy());
        this.form.paintSettings.set(settings);
        this.paintColor.setColor(legacy.getRGBColor());
        this.paintIntensity.setValue(settings.intensity);
        this.paintTransform.setEffectTransform(new EffectTransform());
        this.editor.startEdit(this.form);
    }

    private void resetGlowColor()
    {
        if (this.form == null)
        {
            return;
        }

        Color legacy = new Color().set(1F, 1F, 1F, 1F);
        GlowSettings settings = new GlowSettings();

        this.form.glowingColor.set(legacy.copy());
        this.form.glowSettings.set(settings);
        this.glowingColor.setColor(legacy.getRGBColor());
        this.glowIntensity.setValue(settings.intensity);
        this.editor.startEdit(this.form);
    }

    private void pickGroup(String group)
    {
        this.poseEditor.selectBone(group);
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);

        ModelInstance model = ModelFormRenderer.getModel(this.form);
        String poseGroup = model == null ? this.form.model.get() : model.poseGroup;
        if (poseGroup == null || poseGroup.isEmpty())
        {
            poseGroup = model == null ? this.form.model.get() : model.id;
        }

        this.poseEditor.setValuePose(form.pose);
        this.poseEditor.setPose(form.pose.get(), poseGroup);
        this.poseEditor.fillGroups(model == null ? null : model.model, model == null ? null : model.flippedParts, true);
        this.pbrNormalIntensity.setValue(form.pbrNormalIntensity.get());
        this.pbrSpecularIntensity.setValue(form.pbrSpecularIntensity.get());
        this.color.setColor(form.color.get().getARGBColor());
        this.colorAdjustments.syncFromForm();
        Color formColor = form.color.get();

        this.colorTransform.setEffectTransform(formColor.transform == null ? new EffectTransform() : formColor.transform);
        PaintSettings paint = form.paintSettings.get();
        Color paintDisplay = new Color();

        paint.resolveColor(form.paintColor.get(), paintDisplay);
        this.paintColor.setColor(paintDisplay.getRGBColor());
        this.paintIntensity.setValue(paint.intensity);
        this.paintTransform.setEffectTransform(paint.transform == null ? new EffectTransform() : paint.transform);
        GlowSettings glow = form.glowSettings.get();
        Color glowDisplay = new Color();

        glow.resolveColor(form.glowingColor.get(), glowDisplay);
        this.glowingColor.setColor(glowDisplay.getRGBColor());

        this.glowIntensity.setValue(glow.intensity);

        this.shapeKeys.removeFromParent();

        if (model != null)
        {
            Set<String> modelShapeKeys = model.model.getShapeKeys();

            if (!modelShapeKeys.isEmpty())
            {
                this.options.add(this.shapeKeys);
                this.shapeKeys.setShapeKeys(poseGroup, modelShapeKeys, this.form.shapeKeys.get());
            }
        }

        this.options.resize();
    }

    @Override
    public void pickBone(String bone)
    {
        super.pickBone(bone);

        this.pickGroup(bone);
    }
}
