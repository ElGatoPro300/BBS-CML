package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.shapes.UIShapeKeys;

import java.util.Set;

public class UIModelFormPanel extends UIFormPanel<ModelForm>
{
    public UIModelPoseEditor poseEditor;
    public UIShapeKeys shapeKeys;
    public UITrackpad pbrNormalIntensity;
    public UITrackpad pbrSpecularIntensity;
    public UIToggle toggleSolidHitbox;

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
        this.toggleSolidHitbox = new UIToggle(UIKeys.FORMS_EDITORS_MODEL_HITBOX, false, (t) -> this.form.solidHitbox.set(t.getValue()));
        this.toggleSolidHitbox.tooltip(UIKeys.FORMS_EDITORS_MODEL_HITBOX_TOOLTIP);

        this.options.add(this.pickModel);
        if (BBSSettings.pickLimbTexture.get())
        {
            this.options.add(this.pick);
        }
        if (BBSSettings.modelPbrPanelControls != null && BBSSettings.modelPbrPanelControls.get())
        {
            this.options.add(this.pbrNormalIntensity, this.pbrSpecularIntensity);
        }

        this.options.add(this.toggleSolidHitbox);
        this.options.add(this.poseEditor);
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
        this.toggleSolidHitbox.setValue(form.solidHitbox.get());

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
