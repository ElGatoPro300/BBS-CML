package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

public interface IUIModelPanelHost extends IUIModelSectionEditor
{
    UIElement getMainView();

    UIModelEditorRenderer getModelRenderer();

    void setWorkspacePanel(UIElement panel);

    ModelConfig getModelConfig();

    void openTransformEditor(UIDashboardPanel panel);

    void returnFromSubEditor();

    /**
     * Resolves the model instance used while editing — prefers the live preview,
     * then forces a load + preview invalidate (same pattern as Geometry panel).
     */
    default ModelInstance resolveEditingModel(ModelConfig config)
    {
        ModelInstance instance = this.getModelRenderer().getPreviewModelInstance();

        if (instance == null && config != null)
        {
            BBSModClient.getModels().loadModel(config.getId());
            this.getModelRenderer().invalidatePreviewModel();
            instance = this.getModelRenderer().getPreviewModelInstance();

            if (instance == null)
            {
                instance = BBSModClient.getModels().getModel(config.getId());
            }
        }

        return instance;
    }
}
