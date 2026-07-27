package mchorse.bbs_mod.ui.model_blocks.camera;

import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.class_2338;

public class ImmersiveModelBlockCameraController implements ICameraController
{
    private final UIFormEditor formEditor;
    private final ModelBlockEntity modelBlock;

    public ImmersiveModelBlockCameraController(UIFormEditor formEditor, ModelBlockEntity modelBlock)
    {
        this.formEditor = formEditor;
        this.modelBlock = modelBlock;
    }

    private UIModelRenderer getActiveRenderer()
    {
        if (this.formEditor.modelSettingsEditor != null && this.formEditor.modelSettingsEditor.isVisible())
        {
            return this.formEditor.modelSettingsEditor.renderer;
        }

        return this.formEditor.renderer;
    }

    @Override
    public void setup(Camera camera, float transition)
    {
        UIModelRenderer renderer = this.getActiveRenderer();
        Transform transform = this.modelBlock.getProperties().getTransform();

        renderer.setupPosition();

        class_2338 pos = this.modelBlock.method_11016();

        camera.position.set(pos.method_10263() + transform.translate.x + 0.5D, pos.method_10264() + transform.translate.y, pos.method_10260() + transform.translate.z + 0.5D);
        camera.rotation.set(0, 0, 0);

        Camera rendererCamera = renderer.camera;

        camera.position.add(rendererCamera.position);
        camera.rotation.add(rendererCamera.rotation);
        camera.fov = rendererCamera.fov;
    }

    @Override
    public int getPriority()
    {
        return 100500;
    }

    @Override
    public void update()
    {}
}
