package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.ProceduralDefaults;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.UIFormModelEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;

import org.joml.Vector3f;

public class UIModelGeneralSection extends UIModelSection
{
    public UIToggle procedural;
    public UIToggle culling;
    public UITextbox poseGroup;
    public UITextbox anchorGroup;
    public UITrackpad uiScale;
    public UITrackpad scaleX;
    public UITrackpad scaleY;
    public UITrackpad scaleZ;

    public UIModelGeneralSection(IUIModelPanelHost editor)
    {
        super(editor);

        this.title.label = UIKeys.MODELS_GENERAL;

        this.procedural = new UIToggle(UIKeys.MODELS_PROCEDURAL, (b) ->
        {
            if (this.config != null)
            {
                this.config.procedural.set(b.getValue());

                if (b.getValue())
                {
                    ModelInstance preview = this.editor.getModelRenderer().getPreviewModelInstance();

                    if (preview != null && preview.model != null)
                    {
                        ProceduralDefaults.ensureForConfig(this.config, preview.model);
                    }
                }

                this.editor.dirty();
            }
        });
        this.procedural.tooltip(UIKeys.MODELS_PROCEDURAL_TOOLTIP);
        
        this.culling = new UIToggle(UIKeys.MODELS_CULLING, (b) ->
        {
            if (this.config != null)
            {
                this.config.culling.set(b.getValue());
                this.editor.dirty();
            }
        });
        this.culling.tooltip(UIKeys.MODELS_CULLING_TOOLTIP);
        
        this.poseGroup = new UITextbox(1000, (str) ->
        {
            if (this.config != null)
            {
                this.config.poseGroup.set(str);
                this.editor.dirty();

                UIPoseEditor poseEditor = this.editor.getPoseEditor();

                if (poseEditor != null)
                {
                    poseEditor.setPose(this.config.parts.get(), str.isEmpty() ? this.config.getId() : str);
                }
            }
        });
        this.poseGroup.tooltip(UIKeys.MODELS_POSE_GROUP_TOOLTIP);
        
        this.anchorGroup = new UITextbox(1000, (str) ->
        {
            if (this.config != null) this.config.anchorGroup.set(str);
        });
        this.anchorGroup.tooltip(UIKeys.MODELS_ANCHOR_GROUP_TOOLTIP);

        this.uiScale = new UITrackpad((v) ->
        {
            if (this.config != null) this.config.uiScale.set(v.floatValue());
        });
        this.uiScale.tooltip(UIKeys.MODELS_UI_SCALE);
        
        this.scaleX = new UITrackpad((v) -> this.updateScale(0, v.floatValue()));
        this.scaleY = new UITrackpad((v) -> this.updateScale(1, v.floatValue()));
        this.scaleZ = new UITrackpad((v) -> this.updateScale(2, v.floatValue()));
        
        this.scaleX.tooltip(IKey.constant("X"));
        this.scaleY.tooltip(IKey.constant("Y"));
        this.scaleZ.tooltip(IKey.constant("Z"));

        UIElement poseGroupRow = UI.row(this.poseGroup);
        UIElement scaleRow = UI.row(this.scaleX, this.scaleY, this.scaleZ);
        
        this.fields.add(this.procedural, this.culling);
        this.fields.add(UI.label(UIKeys.MODELS_POSE_GROUP), poseGroupRow);
        this.fields.add(UI.label(UIKeys.MODELS_ANCHOR_GROUP), this.anchorGroup);
        this.fields.add(UI.label(UIKeys.MODELS_UI_SCALE), this.uiScale);
        this.fields.add(UI.label(UIKeys.TRANSFORMS_SCALE), scaleRow);
    }
    
    @Override
    public boolean subMouseClicked(UIContext context)
    {
        boolean wasVisible = this.fields.isVisible();
        boolean handled = super.subMouseClicked(context);

        if (handled && !wasVisible && this.fields.isVisible() && this.editor instanceof UIFormModelEditor formModelEditor)
        {
            formModelEditor.onGeneralSectionOpened();
        }

        return handled;
    }

    private void updateScale(int axis, float value)
    {
        if (this.config == null)
        {
            return;
        }

        Vector3f vec = this.config.scale.get();
        
        if (axis == 0) vec.x = value;
        else if (axis == 1) vec.y = value;
        else if (axis == 2) vec.z = value;
        
        this.editor.dirty();
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.MODELS_GENERAL;
    }

    @Override
    public void setConfig(ModelConfig config)
    {
        super.setConfig(config);

        if (config != null)
        {
            this.procedural.setValue(config.procedural.get());
            this.culling.setValue(config.culling.get());
            this.poseGroup.setText(config.poseGroup.get());
            this.anchorGroup.setText(config.anchorGroup.get());
            this.uiScale.setValue(config.uiScale.get());
            this.scaleX.setValue(config.scale.get().x);
            this.scaleY.setValue(config.scale.get().y);
            this.scaleZ.setValue(config.scale.get().z);
        }
    }
}
