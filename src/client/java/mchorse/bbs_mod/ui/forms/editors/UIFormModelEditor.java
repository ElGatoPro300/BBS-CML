package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.BBSClient;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.cubic.model.ModelRepository;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.forms.editors.panels.UIGeneralFormPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.model.IUIModelPanelHost;
import mchorse.bbs_mod.ui.model.UIModelArmorSection;
import mchorse.bbs_mod.ui.model.UIModelConstraintsPanel;
import mchorse.bbs_mod.ui.model.UIModelEditorRenderer;
import mchorse.bbs_mod.ui.model.UIModelFirstPersonTransformEditor;
import mchorse.bbs_mod.ui.model.UIModelGeneralSection;
import mchorse.bbs_mod.ui.model.UIModelGeometryPanel;
import mchorse.bbs_mod.ui.model.UIModelHandsSection;
import mchorse.bbs_mod.ui.model.UIModelIKPanel;
import mchorse.bbs_mod.ui.model.UIModelItemsSection;
import mchorse.bbs_mod.ui.model.UIModelLookAtSection;
import mchorse.bbs_mod.ui.model.UIModelPanel;
import mchorse.bbs_mod.ui.model.UIModelPartsSection;
import mchorse.bbs_mod.ui.model.UIModelPhysBonePanel;
import mchorse.bbs_mod.ui.model.UIModelSection;
import mchorse.bbs_mod.ui.model.UIModelSneakingSection;
import mchorse.bbs_mod.ui.model.UIModelUIStyles;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.Direction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class UIFormModelEditor extends UIElement implements IUIModelPanelHost
{
    private final UIFormEditor parent;

    public UIModelEditorRenderer renderer;
    public UIElement mainView;
    public UIElement modelSettingsPanel;
    public UIScrollView sectionsView;
    public UIScrollView rightView;
    public UIElement iconBar;
    public UIIcon saveIcon;
    public UIIcon reloadIcon;

    public UIModelIKPanel ikPanel;
    public UIModelPhysBonePanel physBonesPanel;
    public UIModelConstraintsPanel constraintsPanel;
    public UIModelGeometryPanel geometryPanel;

    private final List<UIModelSection> sections = new ArrayList<>();
    private final List<UIElement> panels = new ArrayList<>();
    private final List<UIIcon> panelButtons = new ArrayList<>();

    private ModelConfig config;
    private ModelForm form;
    private String selectedBone;
    private boolean dirty;
    private boolean inTransformEditor;
    private UIDashboardPanel embeddedPanel;
    private UIDashboardPanel dashboardPanelBeforeTransform;
    private boolean formTransformGizmoMode;
    private boolean pickingBone;

    public UIFormModelEditor(UIFormEditor parent)
    {
        this.parent = parent;

        this.renderer = new UIModelEditorRenderer();
        this.renderer.relative(this).x(UIModelUIStyles.STRIP_WIDTH + 200).w(1F, -(UIModelUIStyles.STRIP_WIDTH + 400)).h(1F);
        this.renderer.setCallback(this::pickBone);

        this.mainView = new UIElement();
        this.mainView.relative(this).x(UIModelUIStyles.STRIP_WIDTH).w(1F, -UIModelUIStyles.STRIP_WIDTH).h(1F);

        this.modelSettingsPanel = new UIElement();
        this.modelSettingsPanel.relative(this.mainView).w(1F).h(1F);

        this.sectionsView = UI.scrollView(20, 10);
        this.sectionsView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.sectionsView.relative(this.modelSettingsPanel).y(0).w(200).h(1F);

        this.rightView = UI.scrollView(20, 10);
        this.rightView.scroll.cancelScrolling().scrollSpeed *= 3;
        this.rightView.relative(this.modelSettingsPanel).x(1F, -200).y(0).w(200).h(1F);

        this.modelSettingsPanel.add(this.sectionsView, this.rightView);

        this.iconBar = new UIElement();
        this.iconBar.relative(this).y(0).w(UIModelUIStyles.STRIP_WIDTH).h(1F).column(0).vertical().stretch();

        this.ikPanel = new UIModelIKPanel(this);
        this.physBonesPanel = new UIModelPhysBonePanel(this);
        this.constraintsPanel = new UIModelConstraintsPanel(this);
        this.geometryPanel = new UIModelGeometryPanel(this);

        this.addSection(new UIModelGeneralSection(this));

        UIModelPartsSection parts = new UIModelPartsSection(this);
        this.addSection(parts);

        this.addSection(new UIModelArmorSection(this));
        this.addSection(new UIModelItemsSection(this));
        this.addSection(new UIModelHandsSection(this));
        this.addSection(new UIModelSneakingSection(this));
        this.addSection(new UIModelLookAtSection(this));

        this.registerWorkspacePanel(this.modelSettingsPanel, UIKeys.MODELS_SETTINGS, Icons.MODELS_SETTINGS);
        this.registerWorkspacePanel(this.ikPanel, UIKeys.MODELS_IK_EDITOR, Icons.IK);
        this.registerWorkspacePanel(this.physBonesPanel, UIKeys.MODELS_PHYS_BONES_EDITOR, Icons.DYNAMIC_BONES);
        this.registerWorkspacePanel(this.constraintsPanel, UIKeys.MODELS_CONSTRAINTS_EDITOR, Icons.LOCKED);
        this.registerWorkspacePanel(this.geometryPanel, UIKeys.MODELS_GEOMETRY_EDITOR, Icons.GEOMETRY_EDITOR);

        UIElement iconSpacer = new UIElement();
        iconSpacer.relative(this.iconBar).w(1F).h(10);

        this.reloadIcon = new UIIcon(Icons.REFRESH, (b) -> this.resetToDefaults());
        this.reloadIcon.tooltip(UIKeys.MODELS_RELOAD, Direction.RIGHT);

        this.saveIcon = new UIIcon(Icons.SAVED, (b) -> this.forceSave());
        this.saveIcon.tooltip(UIKeys.GENERAL_SAVE, Direction.RIGHT);

        this.iconBar.add(iconSpacer, this.reloadIcon, this.saveIcon);

        UIRenderable backgroundStrip = new UIRenderable((context) ->
        {
            context.batcher.box(this.area.x, this.area.y, this.area.x + UIModelUIStyles.STRIP_WIDTH, this.area.ey(), UIModelUIStyles.FORM_STRIP_BACKGROUND);
        });

        UIRenderable panelBackgrounds = new UIRenderable((context) ->
        {
            if (this.inTransformEditor || this.modelSettingsPanel.getParent() != this.mainView)
            {
                return;
            }

            context.batcher.box(this.sectionsView.area.x, this.sectionsView.area.y, this.sectionsView.area.ex(), this.sectionsView.area.ey(), 0xFF111115);
            context.batcher.outline(this.sectionsView.area.x - 1, this.sectionsView.area.y - 1, this.sectionsView.area.ex() + 1, this.sectionsView.area.ey() + 1, 0xFF5A5A5A);
            context.batcher.box(this.rightView.area.x, this.rightView.area.y, this.rightView.area.ex(), this.rightView.area.ey(), 0xFF111115);
            context.batcher.outline(this.rightView.area.x - 1, this.rightView.area.y - 1, this.rightView.area.ex() + 1, this.rightView.area.ey() + 1, 0xFF5A5A5A);
        });

        UIRenderable viewportBackground = new UIRenderable((context) ->
        {
            if (this.inTransformEditor)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), UIModelUIStyles.FORM_VIEWPORT_BACKGROUND);

                return;
            }

            if (this.renderer.area.w > 0 && this.renderer.area.h > 0)
            {
                context.batcher.box(this.renderer.area.x, this.renderer.area.y, this.renderer.area.ex(), this.renderer.area.ey(), UIModelUIStyles.FORM_VIEWPORT_BACKGROUND);
            }
        });

        UIRenderable iconHighlight = new UIRenderable(this::renderPanelIcons);

        this.add(backgroundStrip, viewportBackground, panelBackgrounds, iconHighlight, this.mainView, this.iconBar);
        /* Center viewport only — side panels stay clickable; last child = first for picks (see UIModelPanel). */
        this.add(this.renderer);
    }

    private void renderPanelIcons(UIContext context)
    {
        for (int i = 0, c = this.panels.size(); i < c; i++)
        {
            if (this.mainView.getChildren().contains(this.panels.get(i)))
            {
                if (i >= 0 && i < this.panelButtons.size())
                {
                    UIIcon button = this.panelButtons.get(i);

                    UIDashboardPanels.renderHighlight(context.batcher, button.area);
                }
            }
        }
    }

    private void addSection(UIModelSection section)
    {
        this.sections.add(section);
        this.sectionsView.add(section);
    }

    private UIIcon registerWorkspacePanel(UIElement panel, IKey tooltip, Icon icon)
    {
        UIIcon button = new UIIcon(icon, (b) -> this.setWorkspacePanel(panel));

        if (tooltip != null)
        {
            button.tooltip(tooltip, Direction.RIGHT);
        }

        if (this.panelButtons.isEmpty())
        {
            button.relative(this.iconBar).y(0);
        }
        else
        {
            button.relative(this.panelButtons.get(this.panelButtons.size() - 1)).y(1F);
        }

        this.panels.add(panel);
        this.panelButtons.add(button);
        this.iconBar.add(button);

        return button;
    }

    public void open(ModelForm form)
    {
        this.form = form;
        this.loadConfig(form.model.get());
        this.setWorkspacePanel(this.modelSettingsPanel);
        this.resize();
    }

    @Override
    public void resize()
    {
        super.resize();
        this.renderer.resize();
    }

    public void close()
    {
        this.exitFormTransformGizmoMode();
        this.returnFromSubEditor();

        if (this.dirty && this.config != null)
        {
            ModelRepository repository = (ModelRepository) ContentType.MODELS.getRepository();

            repository.save(this.config.getId(), this.config.toData().asMap());
            this.dirty = false;
        }

        this.form = null;
        this.config = null;
    }

    public ModelForm getForm()
    {
        return this.form;
    }

    private void loadConfig(String modelId)
    {
        this.dirty = false;
        this.selectedBone = null;

        if (modelId == null || modelId.isEmpty())
        {
            this.config = null;
            this.applyConfig(null);

            return;
        }

        ModelRepository repository = (ModelRepository) ContentType.MODELS.getRepository();

        repository.load(modelId, (loaded) ->
        {
            this.config = loaded;
            this.renderer.setModel(loaded.getId());
            this.renderer.setConfig(loaded);
            this.applyConfig(loaded);
        });
    }

    private void applyConfig(ModelConfig loaded)
    {
        for (UIModelSection section : this.sections)
        {
            section.setConfig(loaded);
        }

        if (this.ikPanel != null)
        {
            this.ikPanel.setConfig(loaded);
        }

        if (this.physBonesPanel != null)
        {
            this.physBonesPanel.setConfig(loaded);
        }

        if (this.constraintsPanel != null)
        {
            this.constraintsPanel.setConfig(loaded);
        }

        if (this.geometryPanel != null)
        {
            this.geometryPanel.setConfig(loaded);
        }

        UIPoseEditor poseEditor = this.getPoseEditor();

        if (poseEditor != null)
        {
            if (this.formTransformGizmoMode)
            {
                this.syncFormTransformGizmo();
            }
            else
            {
                this.setRight(poseEditor);
                this.assignGizmoTransform(poseEditor.transform, false);
            }
        }

        this.sectionsView.resize();
        this.rightView.resize();
        this.resize();

        for (UIIcon button : this.panelButtons)
        {
            button.setEnabled(loaded != null);
        }

        if (this.reloadIcon != null)
        {
            this.reloadIcon.setEnabled(loaded != null);
        }

        if (this.saveIcon != null)
        {
            this.saveIcon.setEnabled(loaded != null);
            this.saveIcon.both(Icons.SAVED);
        }
    }

    private void resetToDefaults()
    {
        if (this.config == null)
        {
            return;
        }

        String modelId = this.config.getId();

        this.returnFromSubEditor();

        ModelRepository repository = (ModelRepository) ContentType.MODELS.getRepository();
        File modelFolder = new File(repository.getFolder(), modelId);
        File configFile = new File(modelFolder, ModelManager.CONFIG_FILE);
        File dynamicConfigFile = new File(modelFolder, ModelManager.DYNAMIC_CONFIG_FILE);

        if (configFile.exists())
        {
            configFile.delete();
        }

        if (dynamicConfigFile.exists())
        {
            dynamicConfigFile.delete();
        }

        BBSClient.getModels().loadModel(modelId);
        this.renderer.invalidatePreviewModel();
        this.loadConfig(modelId);
    }

    public void addRightElement(UIElement element)
    {
        if (element.getParent() != null && element.getParent() != this.rightView)
        {
            element.removeFromParent();
        }

        this.rightView.removeAll();
        this.rightView.add(element);
        this.rightView.scroll.setScroll(0);
        this.rightView.resize();
    }

    private void resetEditorScrolls()
    {
        this.sectionsView.scroll.dragging = false;
        this.rightView.scroll.dragging = false;
        this.sectionsView.scroll.setScroll(0);
        this.rightView.scroll.setScroll(0);
    }

    private void pickBone(String bone)
    {
        /* UIModelPartsSection.selectBone() -> UIPoseEditor.selectBone() -> pickCallback ->
           setSelectedBone() would otherwise re-enter this path with the same bone. */
        if (this.pickingBone)
        {
            return;
        }

        this.pickingBone = true;

        try
        {
            if (this.formTransformGizmoMode)
            {
                this.exitFormTransformGizmoMode();

                if (this.parent != null)
                {
                    this.parent.disableFormTransformGizmo();
                }
            }

            this.setSelectedBone(bone);
        }
        finally
        {
            this.pickingBone = false;
        }
    }

    private void saveIfDirty()
    {
        if (!this.dirty || this.config == null)
        {
            return;
        }

        this.forceSave();
    }

    @Override
    public UIElement getMainView()
    {
        return this.mainView;
    }

    @Override
    public UIModelEditorRenderer getModelRenderer()
    {
        return this.renderer;
    }

    @Override
    public void setWorkspacePanel(UIElement panel)
    {
        this.returnFromSubEditor();
        this.mainView.removeAll();
        this.mainView.add(panel);
        this.resetEditorScrolls();
        this.rightView.removeAll();

        if (panel == this.modelSettingsPanel)
        {
            if (this.formTransformGizmoMode)
            {
                this.syncFormTransformGizmo();
            }
            else
            {
                UIPoseEditor poseEditor = this.getPoseEditor();

                if (poseEditor != null)
                {
                    this.setRight(poseEditor);
                    this.assignGizmoTransform(poseEditor.transform, false);
                }
            }
        }
        else if (panel == this.geometryPanel)
        {
            this.assignGizmoTransform(this.geometryPanel.getGizmoTransformEditor(), false);
        }

        for (int i = 0; i < this.panelButtons.size(); i++)
        {
            this.panelButtons.get(i).setEnabled(this.config != null);
        }

        this.mainView.resize();
    }

    @Override
    public ModelConfig getModelConfig()
    {
        return this.config;
    }

    @Override
    public void openTransformEditor(UIDashboardPanel panel)
    {
        this.returnFromSubEditor();

        if (panel instanceof UIModelFirstPersonTransformEditor)
        {
            UIDashboard dashboard = this.getDashboard();

            this.dashboardPanelBeforeTransform = dashboard.getPanels().panel;
            this.embeddedPanel = panel;
            this.inTransformEditor = true;
            dashboard.getPanels().setPanel(panel);

            return;
        }

        this.embeddedPanel = panel;
        this.inTransformEditor = true;
        this.mainView.setVisible(false);
        this.iconBar.setVisible(false);
        this.renderer.setVisible(true);
        this.renderer.setPickingEnabled(false);
        panel.full(this);
        this.add(panel);
        panel.appear();
        panel.resize();
    }

    @Override
    public void returnFromSubEditor()
    {
        if (this.embeddedPanel instanceof UIModelFirstPersonTransformEditor)
        {
            UIDashboard dashboard = this.getDashboard();

            if (this.dashboardPanelBeforeTransform != null)
            {
                dashboard.getPanels().setPanel(this.dashboardPanelBeforeTransform);
                this.dashboardPanelBeforeTransform = null;
            }
            else
            {
                this.embeddedPanel.disappear();
                this.embeddedPanel.removeFromParent();
            }

            this.embeddedPanel = null;
            this.inTransformEditor = false;

            return;
        }

        if (this.embeddedPanel != null)
        {
            this.embeddedPanel.disappear();
            this.embeddedPanel.removeFromParent();
            this.embeddedPanel = null;
        }

        this.inTransformEditor = false;
        this.mainView.setVisible(true);
        this.iconBar.setVisible(true);
        this.renderer.setVisible(true);
        this.renderer.setPickingEnabled(true);
        this.resize();
    }

    @Override
    public void dirty()
    {
        this.dirty = true;
        this.renderer.dirty();

        if (this.saveIcon != null)
        {
            this.saveIcon.both(Icons.SAVE);
        }
    }

    @Override
    public void forceSave()
    {
        if (this.config == null)
        {
            return;
        }

        ModelRepository repository = (ModelRepository) ContentType.MODELS.getRepository();

        repository.saveConfigOnly(this.config.getId(), this.config.toData().asMap());
        this.renderer.invalidatePreviewModel();
        this.dirty = false;

        if (this.saveIcon != null)
        {
            this.saveIcon.both(Icons.SAVED);
        }
    }

    @Override
    public UIPoseEditor getPoseEditor()
    {
        for (UIModelSection section : this.sections)
        {
            if (section instanceof UIModelPartsSection parts)
            {
                return parts.poseEditor;
            }
        }

        return null;
    }

    @Override
    public String getSelectedBone()
    {
        return this.selectedBone;
    }

    @Override
    public void setSelectedBone(String bone)
    {
        if (Objects.equals(this.selectedBone, bone))
        {
            return;
        }

        this.selectedBone = bone;
        this.renderer.setSelectedBone(bone);

        if (this.formTransformGizmoMode)
        {
            return;
        }

        for (UIModelSection section : this.sections)
        {
            section.deselect();
            section.onBoneSelected(bone);

            if (section instanceof UIModelPartsSection parts)
            {
                Consumer<String> callback = parts.poseEditor.pickCallback;

                parts.poseEditor.pickCallback = null;

                try
                {
                    parts.selectBone(bone);
                }
                finally
                {
                    parts.poseEditor.pickCallback = callback;
                }

                this.setRight(parts.poseEditor);
            }
        }

        if (this.ikPanel != null && this.ikPanel.hasParent())
        {
            this.ikPanel.onBoneSelected(bone);
        }

        if (this.physBonesPanel != null && this.physBonesPanel.hasParent())
        {
            this.physBonesPanel.onBoneSelected(bone);
        }

        if (this.constraintsPanel != null && this.constraintsPanel.hasParent())
        {
            this.constraintsPanel.onBoneSelected(bone);
        }
    }

    @Override
    public List<UIModelSection> getSections()
    {
        return this.sections;
    }

    @Override
    public void setRight(UIElement element)
    {
        if (element != null)
        {
            this.addRightElement(element);
        }
    }

    @Override
    public UIDashboard getDashboard()
    {
        return BBSModClient.getDashboard();
    }

    @Override
    public UIModelPanel getModelPanel()
    {
        return null;
    }

    public boolean isFormTransformGizmoMode()
    {
        return this.formTransformGizmoMode;
    }

    public void enterFormTransformGizmoMode()
    {
        if (this.parent == null || this.parent.editor == null || this.parent.editor.generalPanel == null)
        {
            return;
        }

        this.formTransformGizmoMode = true;
        this.setWorkspacePanel(this.modelSettingsPanel);

        UIGeneralFormPanel general = this.parent.editor.generalPanel;

        general.transform.setTransform(this.parent.editor.form.transform.get());
        general.transform.removeFromParent();
        this.setRight(general.transform);

        for (UIModelSection section : this.sections)
        {
            if (section instanceof UIModelGeneralSection)
            {
                section.fields.setVisible(true);
            }
        }

        this.sectionsView.scroll.setScroll(0);
        this.sectionsView.resize();
        this.syncFormTransformGizmo();
    }

    public void onGeneralSectionOpened()
    {
        if (this.parent != null)
        {
            this.parent.enableFormTransformGizmo();
        }
    }

    public void onPoseSectionOpened()
    {
        if (this.parent != null)
        {
            this.parent.disableFormTransformGizmo();
        }
    }

    public void exitFormTransformGizmoMode()
    {
        if (!this.formTransformGizmoMode)
        {
            return;
        }

        this.formTransformGizmoMode = false;
        this.renderer.setFormTransformGizmoOrigin(null);
        this.restoreFormTransformWidget();

        UIPoseEditor poseEditor = this.getPoseEditor();

        if (poseEditor != null && this.modelSettingsPanel.getParent() == this.mainView)
        {
            this.setRight(poseEditor);
            this.assignGizmoTransform(poseEditor.transform, false);
        }
    }

    private void assignGizmoTransform(UIPropTransform transform, boolean formTransform)
    {
        this.renderer.setFormTransformGizmoDrag(formTransform);
        this.renderer.transform = transform;
    }

    private void syncFormTransformGizmo()
    {
        if (!this.formTransformGizmoMode || this.parent == null || this.parent.editor == null || this.parent.editor.generalPanel == null)
        {
            return;
        }

        UIPropTransform formTransform = this.parent.editor.generalPanel.transform;

        formTransform.setTransform(this.parent.editor.form.transform.get());
        this.assignGizmoTransform(formTransform, true);
        this.renderer.setFormTransformGizmoOrigin(this.parent::getOrigin);
    }

    private void restoreFormTransformWidget()
    {
        if (this.parent == null || this.parent.editor == null || this.parent.editor.generalPanel == null)
        {
            return;
        }

        UIGeneralFormPanel general = this.parent.editor.generalPanel;

        if (general.transform.getParent() == general.options)
        {
            return;
        }

        general.transform.removeFromParent();
        general.options.add(general.transform.marginTop(8));
        general.options.resize();
    }
}
