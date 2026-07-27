package mchorse.bbs_mod.ui.particles;

import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.particles.components.expiration.ParticleComponentKillPlane;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4608;
import net.minecraft.class_757;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

public class UIParticleSchemeRenderer extends UIModelRenderer
{
    public ParticleEmitter emitter;

    private Vector3f vector = new Vector3f(0, 0, 0);

    public UIParticleSchemeRenderer()
    {
        super();

        this.emitter = new ParticleEmitter();
    }

    public void setScheme(ParticleScheme scheme)
    {
        this.emitter = new ParticleEmitter();
        this.emitter.setScheme(scheme);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        /* Debug readout (particle count and emitter age) in the preview's bottom-right corner. */
        if (this.emitter != null && this.emitter.scheme != null)
        {
            String label = this.emitter.particles.size() + "P - " + this.emitter.age + "A";

            context.batcher.textShadow(label, this.area.ex() - 4 - context.batcher.getFont().getWidth(label), this.area.ey() - 12);
        }
    }

    @Override
    protected void update()
    {
        super.update();

        if (this.emitter != null)
        {
            this.emitter.rotation.identity();
            this.emitter.update();
        }
    }

    @Override
    protected void renderUserModel(UIContext context)
    {
        if (this.emitter == null || this.emitter.scheme == null)
        {
            return;
        }

        /* Temporarily reset camera rotation and position to 0 so CPU billboarding calculations
         * are relative to the view matrix translation on the stack */
        float originalPitch = this.camera.rotation.x;
        float originalYaw = this.camera.rotation.y;
        double originalX = this.camera.position.x;
        double originalY = this.camera.position.y;
        double originalZ = this.camera.position.z;

        this.camera.rotation.set(0F, 0F, 0F);
        this.camera.position.set(0D, 0D, 0D);

        this.emitter.setupCameraProperties(this.camera);

        this.camera.rotation.x = originalPitch;
        this.camera.rotation.y = originalYaw;
        this.camera.position.set(originalX, originalY, originalZ);

        class_310.method_1551().field_1773.method_22974().method_3316();

        class_4587 stack = context.batcher.getContext().method_51448();
        Matrix4f modelMatrix = new Matrix4f(stack.method_23760().method_23761());

        this.emitter.lastGlobal.set(new Vector3d(modelMatrix.getTranslation(Vectors.TEMP_3F)));
        this.emitter.rotation.set(modelMatrix);
        this.emitter.modelRenderer = true;

        stack.method_22903();
        stack.method_34426();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        this.emitter.render(class_290.field_1575, class_757::method_34543, stack, class_4608.field_21444, context.getTransition());
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        stack.method_22909();

        ParticleComponentKillPlane plane = this.emitter.scheme.get(ParticleComponentKillPlane.class);

        if (plane.a != 0 || plane.b != 0 || plane.c != 0)
        {
            this.renderPlane(context, plane.a, plane.b, plane.c, plane.d);
        }
    }

    private void renderPlane(UIContext context, float a, float b, float c, float d)
    {
        Matrix4f matrix = context.batcher.getContext().method_51448().method_23760().method_23761();

        class_287 builder = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1576);
        final float alpha = 0.5F;

        this.calculate(0, 0, a, b, c, d);
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_22915(0, 1, 0, alpha);
        this.calculate(0, 1, a, b, c, d);
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_22915(0, 1, 0, alpha);
        this.calculate(1, 0, a, b, c, d);
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_22915(0, 1, 0, alpha);

        this.calculate(1, 0, a, b, c, d);
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_22915(0, 1, 0, alpha);
        this.calculate(0, 1, a, b, c, d);
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_22915(0, 1, 0, alpha);
        this.calculate(1, 1, a, b, c, d);
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_22915(0, 1, 0, alpha);

        RenderSystem.setShader(class_757::method_34540);
        RenderSystem.disableCull();
        class_286.method_43433(builder.method_60800());
        RenderSystem.enableCull();
    }

    private void calculate(float i, float j, float a, float b, float c, float d)
    {
        final float radius = 5;

        if (b != 0)
        {
            this.vector.x = -radius + radius * 2 * i;
            this.vector.z = -radius + radius * 2 * j;
            this.vector.y = (a * this.vector.x + c * this.vector.z + d) / -b;
        }
        else if (a != 0)
        {
            this.vector.y = -radius + radius * 2 * i;
            this.vector.z = -radius + radius * 2 * j;
            this.vector.x = (b * this.vector.y + c * this.vector.z + d) / -a;
        }
        else if (c != 0)
        {
            this.vector.x = -radius + radius * 2 * i;
            this.vector.y = -radius + radius * 2 * j;
            this.vector.z = (b * this.vector.y + a * this.vector.x + d) / -c;
        }
    }

    @Override
    protected void renderGrid(UIContext context)
    {
        super.renderGrid(context);

        if (UIBaseMenu.renderAxes)
        {
            Draw.coolerAxes(context.batcher.getContext().method_51448(), 1F, 0.01F, 1.01F, 0.02F);
        }
    }


}