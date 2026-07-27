package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.UIFormModelEditor;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.colors.Colors;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

public class UIModelPartsSection extends UIModelSection
{
    public UIButton texture;
    public UIButton openModel;
    public UIColor color;
    public UIPoseEditor poseEditor;

    public UIModelPartsSection(IUIModelPanelHost editor)
    {
        super(editor);
        
        this.texture = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, (b) ->
        {
            if (this.config != null)
            {
                Link current = this.config.texture.get();

                if (current == null)
                {
                    current = BBSSettings.textureDefaultPath.get();
                }

                UITexturePicker.open(b.getContext(), current, (l) ->
                {
                    this.config.texture.set(l);
                    this.editor.dirty();
                });
            }
        });

        this.openModel = new UIButton(UIKeys.FORMS_EDITOR_MODEL_OPEN_IN, this::openModel);

        this.color = new UIColor((c) ->
        {
            if (this.config != null)
            {
                this.config.color.set(Colors.A100 | c);
                this.editor.dirty();
            }
        });
        
        /* Drag signs come from UIModelEditorRenderer.prepareGizmoDrag. Do not enable the legacy
         * setModel() path — it permanently forces X/Z ring invert and fights that prepare. */
        this.poseEditor = new UIPoseEditor()
        {
            @Override
            protected boolean useModelGizmoDrag()
            {
                return false;
            }

            @Override
            protected float getGizmoTranslationScale()
            {
                ModelConfig cfg = UIModelPartsSection.this.config;

                if (cfg != null)
                {
                    ModelInstance instance = BBSModClient.getModels().getModel(cfg.getId());

                    if (instance != null && ModelFormRenderer.isBobjModel(instance.model))
                    {
                        return 1F;
                    }
                }

                return 16F;
            }
        };
        this.poseEditor.onChange = this.editor::dirty;
        this.poseEditor.pickCallback = (bone) ->
        {
            this.editor.setSelectedBone(bone);

            for (UIModelSection section : this.editor.getSections())
            {
                if (section != this)
                {
                    section.deselect();
                }

                section.onBoneSelected(bone);
            }
        };
        this.poseEditor.prepend(this.color);
        if (BBSSettings.pickLimbTexture.get())
        {
            this.poseEditor.prepend(this.texture);
        }
        this.poseEditor.prepend(this.openModel);

        this.section.onToggle(() ->
        {
            this.editor.setRight(this.poseEditor);

            if (this.editor instanceof UIFormModelEditor formModelEditor)
            {
                formModelEditor.onPoseSectionOpened();
            }
        });
    }

    public void selectBone(String bone)
    {
        this.poseEditor.selectBone(bone);
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.MODELS_PARTS;
    }

    @Override
    public void setConfig(ModelConfig config)
    {
        String oldId = this.config == null ? null : this.config.getId();

        super.setConfig(config);
        
        if (config != null)
        {
            this.color.setColor(config.color.get());

            String group = config.poseGroup.get();

            this.poseEditor.setPose(config.parts.get(), group.isEmpty() ? config.getId() : group);
            
            ModelInstance model = BBSModClient.getModels().getModel(config.getId());
            
            if (model != null)
            {
                this.poseEditor.fillGroups(model.getModel().getAllGroupKeys(), oldId == null || !oldId.equals(config.getId()));
                this.poseEditor.setDefaultTextureSupplier(() -> model.texture);
            }
        }
    }

    private void openModel(UIButton b)
    {
        if (this.config == null)
        {
            return;
        }

        String modelId = this.config.getId();
        File folder = BBSMod.getProvider().getFile(Link.assets("models/" + modelId));

        if ((folder == null || !folder.exists()) && BBSMod.class.getClassLoader() != null)
        {
            URL url = BBSMod.class.getClassLoader().getResource("assets/bbs/assets/models/" + modelId);

            if (url != null)
            {
                try
                {
                    folder = Paths.get(url.toURI()).toFile();
                }
                catch (Exception e)
                {}
            }
        }

        if (folder != null && folder.isDirectory())
        {
            File target = this.findModelFile(folder);

            if (target != null)
            {
                try
                {
                    Desktop.getDesktop().open(target);
                }
                catch (Throwable e)
                {
                    UIUtils.openFolder(target);
                }
            }
        }
    }

    private File findModelFile(File folder)
    {
        File[] files = folder.listFiles();

        if (files == null)
        {
            return null;
        }

        for (File f : files)
        {
            if (f.getName().endsWith(".bbmodel"))
            {
                return f;
            }
        }

        for (File f : files)
        {
            if (f.getName().endsWith(".geo.json"))
            {
                return f;
            }
        }

        for (File f : files)
        {
            if (f.getName().equals("model.json"))
            {
                return f;
            }
        }

        for (File f : files)
        {
            if (f.getName().endsWith(".json") && !f.getName().equals("config.json"))
            {
                return f;
            }
        }

        return null;
    }
}
