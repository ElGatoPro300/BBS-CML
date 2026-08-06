package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

public class FormRenderingContext
{
    public FormRenderType type;
    public IEntity entity;
    public MatrixStack stack;
    public int light;
    public int overlay;
    public float transition;
    public final Camera camera = new Camera();
    public StencilMap stencilMap;
    public boolean ui;
    public int color;
    public boolean modelRenderer;

    public FormRenderingContext()
    {}

    public FormRenderingContext set(FormRenderType type, IEntity entity, MatrixStack stack, int light, int overlay, float transition)
    {
        this.type = type == null ? FormRenderType.ENTITY : type;
        this.entity = entity;
        this.stack = stack;
        this.light = light;
        this.overlay = overlay;
        this.transition = transition;
        this.stencilMap = null;
        this.ui = false;
        this.color = 0xffffffff;

        return this;
    }

    public FormRenderingContext camera(Camera camera)
    {
        this.camera.copy(camera);
        this.camera.updateView();

        return this;
    }

    public FormRenderingContext camera(net.minecraft.client.render.Camera camera)
    {
        this.camera.position.set(camera.getPos().x, camera.getPos().y, camera.getPos().z);
        this.camera.rotation.set(MathUtils.toRad(-camera.getPitch()), MathUtils.toRad(camera.getYaw()), 0F);
        this.camera.fov = MathUtils.toRad(MinecraftClient.getInstance().options.getFov().getValue());
        this.camera.view.identity().rotate(camera.getRotation());

        return this;
    }

    public FormRenderingContext stencilMap(StencilMap stencilMap)
    {
        this.stencilMap = stencilMap;

        return this;
    }

    public FormRenderingContext inUI()
    {
        this.ui = true;

        return this;
    }

    public FormRenderingContext color(int color)
    {
        this.color = color;

        return this;
    }

    public FormRenderingContext modelRenderer()
    {
        this.modelRenderer = true;

        return this;
    }

    public float getTransition()
    {
        return this.transition;
    }

    public boolean isPicking()
    {
        return this.stencilMap != null;
    }

    public int getPickingIndex()
    {
        return this.stencilMap == null ? -1 : this.stencilMap.objectIndex;
    }
}