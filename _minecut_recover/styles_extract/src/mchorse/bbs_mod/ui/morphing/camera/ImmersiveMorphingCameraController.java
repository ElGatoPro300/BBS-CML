package mchorse.bbs_mod.ui.morphing.camera;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Matrices;
import net.minecraft.class_310;
import net.minecraft.class_746;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class ImmersiveMorphingCameraController implements ICameraController
{
    private Supplier<UIModelRenderer> modelRenderer;

    public ImmersiveMorphingCameraController(Supplier<UIModelRenderer> modelRenderer)
    {
        this.modelRenderer = modelRenderer;
    }

    @Override
    public void setup(Camera camera, float transition)
    {
        UIModelRenderer renderer = this.modelRenderer.get();
        class_746 player = class_310.method_1551().field_1724;

        float bodyYaw = MathUtils.toRad(Lerps.lerp(player.field_6220, player.field_6283, transition));

        camera.position.set(player.field_6014, player.field_6036, player.field_5969);
        camera.position.lerp(new Vector3d(player.method_19538().field_1352, player.method_19538().field_1351, player.method_19538().field_1350), transition);
        camera.rotation.set(0, bodyYaw, 0);

        if (renderer == null)
        {
            Vector3f rotation = Matrices.rotation(0F, -bodyYaw);

            rotation.mul(2F);
            camera.position.add(rotation.x, rotation.y + 1F, rotation.z);
            camera.setFov(class_310.method_1551().field_1690.method_41808().method_41753());
        }
        else
        {
            renderer.setupPosition();

            Camera rendererCamera = renderer.camera;

            Vector3f rotate = Matrices.rotate(new Vector3f().set(rendererCamera.position), 0F, -bodyYaw);

            camera.position.add(rotate);
            camera.rotation.add(rendererCamera.rotation);
            camera.fov = rendererCamera.fov;
        }
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