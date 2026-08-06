package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrailFormRenderer extends FormRenderer<TrailForm> implements ITickable 
{
    private final Map<FormRenderType, ArrayDeque<Trail>> record = new HashMap<>();
    private int tick;

    public TrailFormRenderer(TrailForm form) 
    {
        super(form);
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2) 
    {
        Texture texture = context.render.getTextures().getTexture(this.form.texture.get());
        float min = Math.min(texture.width, texture.height);
        int ow = (x2 - x1) - 4;
        int oh = (y2 - y1) - 4;
        int w = (int) ((texture.width / min) * ow);
        int h = (int) ((texture.height / min) * ow);
        int x = x1 + (ow - w) / 2 + 2;
        int y = y1 + (oh - h) / 2 + 2;


        context.batcher.fullTexturedBox(texture, x, y, w, h);
    }

    @Override
    protected void render3D(FormRenderingContext context) 
    {
        super.render3D(context);


        if (BBSRendering.isIrisShadowPass() || context.type == FormRenderType.ITEM_INVENTORY) 
        {
            return;
        }


        if (context.modelRenderer || context.ui) 
        {
            MatrixStack stack = context.stack;
            float scale = BBSSettings.axesScale.get();
            float axisOffset = 0.01F * scale;
            float outlineSize = 1.01F;
            float outlineOffset = 0.02F * scale;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);


            Draw.fillBox(builder, stack, -outlineOffset, -outlineSize, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
            Draw.fillBox(builder, stack, -axisOffset, -1F, -axisOffset, axisOffset, 1F, axisOffset, 0, 1, 0);


            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableDepthTest();
            BufferRenderer.drawWithGlobalProgram(builder.end());
            RenderSystem.enableDepthTest();

            return;
        }


        if (!BBSRendering.isRenderingWorld()) 
        {
            return;
        }


        MatrixStack stack = context.stack;
        Camera camera = context.camera;
        double baseX = camera.position.x;
        double baseY = camera.position.y;
        double baseZ = camera.position.z;
        float current = (float) this.tick + context.transition;
        ArrayDeque<Trail> trails = this.record.computeIfAbsent(context.type, (k) -> new ArrayDeque<>());


        if (!this.form.paused.get()) 
        {
            Matrix4f modelPosMatrix = new Matrix4f(stack.peek().getPositionMatrix());
            Vector4f topVec = new Vector4f(0F, 1F, 0F, 1F);
            Vector4f bottomVec = new Vector4f(0F, -1F, 0F, 1F);

            modelPosMatrix.transform(topVec);
            modelPosMatrix.transform(bottomVec);

            Trail record = new Trail();
            record.tick = current;
            record.top = new Vector3d(topVec.x + baseX, topVec.y + baseY, topVec.z + baseZ);
            record.bottom = new Vector3d(bottomVec.x + baseX, bottomVec.y + baseY, bottomVec.z + baseZ);
            record.stop = new Vector3f((float) (topVec.x - bottomVec.x), (float) (topVec.y - bottomVec.y), (float) (topVec.z - bottomVec.z)).lengthSquared() < 1.0E-4D;

            trails.addLast(record);
        }


        boolean loop = this.form.loop.get();
        float length = this.form.length.get();
        float end = current - length;
        Iterator<Trail> it = trails.iterator();
        boolean hasSomethingToRender = false;
        boolean lastStop = true;


        while (it.hasNext()) 
        {
            Trail trail = it.next();

            if (trail.tick < end) 
            {
                it.remove();
            }
            else 
            {
                hasSomethingToRender |= !trail.stop && !lastStop;
                lastStop = trail.stop;
            }
        }


        if (!hasSomethingToRender || trails.size() <= 1 || !(length > 0.001D)) 
        {
            return;
        }


        BBSModClient.getTextures().bindTexture(this.form.texture.get());
        stack.push();


        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        Matrix4f identityMatrix = new Matrix4f();
        Trail lastTrail = null;


        for (it = trails.iterator(); it.hasNext(); ) 
        {
            Trail trail = it.next();

            if (lastTrail != null && !lastTrail.stop && !trail.stop) 
            {
                float x1 = (float) (trail.top.x - baseX);
                float x2 = (float) (trail.bottom.x - baseX);
                float x3 = (float) (lastTrail.bottom.x - baseX);
                float x4 = (float) (lastTrail.top.x - baseX);

                float y1 = (float) (trail.top.y - baseY);
                float y2 = (float) (trail.bottom.y - baseY);
                float y3 = (float) (lastTrail.bottom.y - baseY);
                float y4 = (float) (lastTrail.top.y - baseY);

                float z1 = (float) (trail.top.z - baseZ);
                float z2 = (float) (trail.bottom.z - baseZ);
                float z3 = (float) (lastTrail.bottom.z - baseZ);
                float z4 = (float) (lastTrail.top.z - baseZ);

                float u1 = loop ? trail.tick / length : (current - trail.tick) / length;
                float u2 = loop ? lastTrail.tick / length : (current - lastTrail.tick) / length;

                /* Front face */
                builder.vertex(identityMatrix, x1, y1, z1).texture(u1, 0F);
                builder.vertex(identityMatrix, x2, y2, z2).texture(u1, 1F);
                builder.vertex(identityMatrix, x3, y3, z3).texture(u2, 1F);
                builder.vertex(identityMatrix, x4, y4, z4).texture(u2, 0F);

                /* Back face */
                builder.vertex(identityMatrix, x4, y4, z4).texture(u2, 0F);
                builder.vertex(identityMatrix, x3, y3, z3).texture(u2, 1F);
                builder.vertex(identityMatrix, x2, y2, z2).texture(u1, 1F);
                builder.vertex(identityMatrix, x1, y1, z1).texture(u1, 0F);
            }

            lastTrail = trail;
        }


        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableDepthTest();


        stack.pop();
    }

    @Override
    public void tick(IEntity entity) 
    {
        this.tick += 1;
    }

    public static class Trail 
    {
        public Vector3d top;
        public Vector3d bottom;
        public float tick;
        public boolean stop;
    }
}