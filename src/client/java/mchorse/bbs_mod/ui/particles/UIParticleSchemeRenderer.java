package mchorse.bbs_mod.ui.particles;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.particles.components.expiration.ParticleComponentKillPlane;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.renderer.texture.OverlayTexture;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

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

        // MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();

        PoseStack stack = this.createCameraStack();
        Matrix4f modelMatrix = new Matrix4f(stack.last().pose());

        this.emitter.lastGlobal.set(new Vector3d(modelMatrix.getTranslation(Vectors.TEMP_3F)));
        this.emitter.rotation.set(modelMatrix);
        this.emitter.modelRenderer = true;

        stack.pushPose();
        stack.setIdentity();

        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        this.emitter.render(DefaultVertexFormat.PARTICLE, BBSRendering::getParticleProgram, stack, OverlayTexture.NO_OVERLAY, context.getTransition());
        GlStateManager._disableDepthTest();
        GlStateManager._disableBlend();

        stack.popPose();

        ParticleComponentKillPlane plane = this.emitter.scheme.get(ParticleComponentKillPlane.class);

        if (plane.a != 0 || plane.b != 0 || plane.c != 0)
        {
            this.renderPlane(context, plane.a, plane.b, plane.c, plane.d);
        }
    }

    private void renderPlane(UIContext context, float a, float b, float c, float d)
    {
        Matrix4f matrix = this.createCameraStack().last().pose();

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        final float alpha = 0.5F;

        this.calculate(0, 0, a, b, c, d);
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 1, 0, alpha);
        this.calculate(0, 1, a, b, c, d);
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 1, 0, alpha);
        this.calculate(1, 0, a, b, c, d);
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 1, 0, alpha);

        this.calculate(1, 0, a, b, c, d);
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 1, 0, alpha);
        this.calculate(0, 1, a, b, c, d);
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 1, 0, alpha);
        this.calculate(1, 1, a, b, c, d);
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 1, 0, alpha);

        GlStateManager._disableCull();
        builder.buildOrThrow().close();
        GlStateManager._enableCull();
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
            Draw.coolerAxes(this.createCameraStack(), 1F, 0.01F, 1.01F, 0.02F);
        }
    }


}
