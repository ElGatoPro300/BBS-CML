package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.film.FormRenderDepth;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_7833;
import org.joml.Matrix4f;

public class FormRenderingContext
{
    public FormRenderType type;
    public IEntity entity;
    public class_4587 stack;
    public class_4587 world;
    public int light;
    public int overlay;
    public float transition;
    public final Camera camera = new Camera();
    public StencilMap stencilMap;
    public boolean ui;
    public int color;
    public boolean modelRenderer;
    public boolean relative;
    public boolean isShadowPass;
    public Matrix4f viewMatrix;
    public boolean renderEquipment;

    public FormRenderDepth.Frame renderDepthFrame;

    /** Overrides the form texture for this render pass only (e.g. illusion copies). */
    public Link textureOverride;

    /** Overrides texture crossfade for this render pass only. */
    public TextureBlend textureBlendOverride;

    public FormRenderingContext()
    {}

    public FormRenderingContext set(FormRenderType type, IEntity entity, class_4587 stack, int light, int overlay, float transition)
    {
        this.type = type == null ? FormRenderType.ENTITY : type;
        this.entity = entity;
        this.stack = stack;
        this.world = new class_4587();
        this.light = light;
        this.overlay = overlay;
        this.transition = transition;
        this.stencilMap = null;
        this.ui = false;
        this.color = 0xffffffff;
        this.relative = false;
        this.isShadowPass = false;
        this.viewMatrix = null;
        this.renderEquipment = true;
        this.renderDepthFrame = null;
        this.textureOverride = null;
        this.textureBlendOverride = null;

        if (entity != null && (this.type == FormRenderType.ENTITY || this.type == FormRenderType.MODEL_BLOCK))
        {
            double x = Lerps.lerp(entity.getPrevX(), entity.getX(), transition);
            double y = Lerps.lerp(entity.getPrevY(), entity.getY(), transition);
            double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition);

            float bodyYaw = Lerps.lerp(entity.getPrevBodyYaw(), entity.getBodyYaw(), transition);

            this.world.method_22904(x, y, z);
            this.world.method_22907(class_7833.field_40716.rotationDegrees(-bodyYaw));
        }

        return this;
    }

    public FormRenderingContext camera(Camera camera)
    {
        this.camera.copy(camera);
        this.camera.updateView();

        return this;
    }

    public FormRenderingContext camera(net.minecraft.class_4184 camera)
    {
        this.camera.position.set(camera.method_19326().field_1352, camera.method_19326().field_1351, camera.method_19326().field_1350);
        this.camera.rotation.set(MathUtils.toRad(-camera.method_19329()), MathUtils.toRad(camera.method_19330()), 0F);
        this.camera.fov = MathUtils.toRad(class_310.method_1551().field_1690.method_41808().method_41753());
        this.camera.view.identity().rotate(camera.method_23767());

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

    public FormRenderingContext renderDepthFrame(FormRenderDepth.Frame frame)
    {
        this.renderDepthFrame = frame;

        return this;
    }

    public FormRenderingContext equipment(boolean renderEquipment)
    {
        this.renderEquipment = renderEquipment;

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
