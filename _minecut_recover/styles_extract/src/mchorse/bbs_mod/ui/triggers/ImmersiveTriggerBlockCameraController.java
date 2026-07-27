package mchorse.bbs_mod.ui.triggers;

import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import net.minecraft.class_2338;

public class ImmersiveTriggerBlockCameraController implements ICameraController
{
    private UIModelRenderer renderer;
    private TriggerBlockEntity entity;

    public ImmersiveTriggerBlockCameraController(UIModelRenderer renderer, TriggerBlockEntity entity)
    {
        this.renderer = renderer;
        this.entity = entity;
    }

    @Override
    public void setup(Camera camera, float transition)
    {
        if (this.entity == null)
        {
            return;
        }

        this.renderer.setupPosition();

        class_2338 pos = this.entity.method_11016();
        Camera rendererCamera = this.renderer.camera;

        camera.position.set(pos.method_10263() + 0.5D, pos.method_10264() + 0.5D, pos.method_10260() + 0.5D);
        camera.rotation.set(0, 0, 0);

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
