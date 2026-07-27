package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.Factor;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_8251;
import org.joml.Intersectiond;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

/**
 * Model renderer GUI element
 *
 * This base class can be used for full screen model viewer.
 */
public abstract class UIModelRenderer extends UIElement
{
    private static Vector3d vec = new Vector3d();
    private static Matrix3d mat = new Matrix3d();

    protected IEntity entity = new StubEntity();

    protected int timer;
    protected int dragging;

    public Camera camera = new Camera();

    public Vector3f pos = new Vector3f();
    public Factor distance = new Factor(0, 0, 100, (x) -> Math.pow(x, 2) / 100D);
    public boolean grid = true;

    private Vector3d cachedPlaneIntersection = new Vector3d();
    private Vector3f cachedPos = new Vector3f();
    private Camera cachedCamera = new Camera();
    private Vector3d plane = new Vector3d();
    private float lastX;
    private float lastY;

    private long tick;
    private Matrix4f transform = new Matrix4f();

    private boolean stencilViewport;
    private int stencilViewportW;
    private int stencilViewportH;

    public UIModelRenderer()
    {
        super();

        this.reset();
    }

    /**
     * When rendering the stencil pick pass into an FBO, the GL viewport must be {@code 0,0,fboW,fboH}
     * instead of window-relative coordinates so pick pixels align with the on-screen gizmo.
     */
    protected void beginStencilViewport(int fboW, int fboH)
    {
        this.stencilViewport = true;
        this.stencilViewportW = fboW;
        this.stencilViewportH = fboH;
    }

    protected void endStencilViewport()
    {
        this.stencilViewport = false;
    }

    public void setTransform(Matrix4f transform)
    {
        this.transform = transform;
    }

    public void setRotation(float yaw, float pitch)
    {
        this.camera.rotation.y = MathUtils.toRad(yaw);
        this.camera.rotation.x = MathUtils.toRad(pitch);
    }

    public void setPosition(float x, float y, float z)
    {
        this.pos.set(x, y, z);
    }

    public void setDistance(int distanceX)
    {
        this.distance.setX(distanceX);
    }

    public void setEntity(IEntity entity)
    {
        this.entity = entity;
    }

    public IEntity getEntity()
    {
        return this.entity;
    }

    public void reset()
    {
        this.setDistance(15);
        this.setPosition(0, 1, 0);
        this.setRotation(0, 0);
    }

    public boolean isDragging()
    {
        return this.dragging != 0;
    }

    public boolean isDraggingPosition()
    {
        return this.dragging == 2;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.isDragging() && this.area.isInside(context) && (context.mouseButton == 0 || context.mouseButton == 2))
        {
            this.dragging = Window.isShiftPressed() || context.mouseButton == 2 ? 2 : 1;
            this.lastX = context.mouseX;
            this.lastY = context.mouseY;

            this.cachedPos.set(this.pos);
            this.cachedCamera.copy(this.camera);
            this.plane.set(0, 0, 1);
            this.rotateVector(this.plane);

            this.cachedPlaneIntersection = this.calculateOnPlane(context);
        }

        return false;
    }

    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        if (this.area.isInside(context) && !this.isDragging())
        {
            int x = Integer.compare(-(int) context.mouseWheel, 0);

            if (Window.isCtrlPressed())
            {
                x *= 8;
            }

            this.distance.setX(this.distance.getX() + x);
        }

        return super.subMouseScrolled(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        this.dragging = 0;

        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        this.updateLogic(context);

        context.batcher.clip(this.area, context);
        this.renderModel(context);
        context.batcher.unclip(context);

        super.render(context);
    }

    private void updateLogic(UIContext context)
    {
        long tick = context.getTick();
        long i = tick - this.tick;

        if (i > 10)
        {
            i = 10;
        }

        while (i > 0)
        {
            this.update();
            i --;
        }

        this.tick = tick;
    }

    /**
     * Update logic
     */
    protected void update()
    {
        this.timer += 1;
        this.entity.setAge(this.timer);
    }

    /**
     * Draw currently edited model
     */
    private void renderModel(UIContext context)
    {
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);

        this.setupPosition();
        this.setupViewport(context);

        class_4587 stack = context.render.batcher.getContext().method_51448();

        /* Cache the global stuff */
        MatrixStackUtils.cacheMatrices();

        RenderSystem.setProjectionMatrix(this.camera.projection, class_8251.field_43361);

        /* Rendering begins... */
        stack.method_22903();
        MatrixStackUtils.multiply(stack, this.camera.view);
        stack.method_22904(-this.camera.position.x, -this.camera.position.y, -this.camera.position.z);
        MatrixStackUtils.multiply(stack, this.transform);

        /* Keep diffuse normals in model/block space. Baking the orbit camera into NormalMat
         * made face shading follow the view angle; the world / F7 pass keeps lighting tied to
         * how the model sits in the world instead. */
        Matrix3f lightingNormals = new Matrix3f();

        this.transform.normal(lightingNormals);
        stack.method_23760().method_23762().set(lightingNormals);

        /* Vanilla level diffuse lights (same basis DiffuseLighting uses for the world pass).
         * MorphRenderer-style (±0.85, 0.85, ∓1) over-lit X-aligned faces in the editor preview
         * compared to model-block / F7 world shading. */
        Vector3f light0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
        Vector3f light1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();

        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        if (this.grid)
        {
            this.renderGrid(context);
        }

        this.renderUserModel(context);

        class_308.method_24210();

        stack.method_22909();

        /* Return back to orthographic projection */
        class_310 mc = class_310.method_1551();

        RenderSystem.viewport(0, 0, mc.method_22683().method_4489(), mc.method_22683().method_4506());
        MatrixStackUtils.restoreMatrices();
        context.resetMatrix();

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        mchorse.bbs_mod.client.BBSRendering.restoreGuiRenderState();

        this.processInputs(context);
    }

    protected void processInputs(UIContext context)
    {
        int mouseX = context.mouseX;
        int mouseY = context.mouseY;

        if (this.isDragging())
        {
            if (this.isDraggingPosition())
            {
                if (this.lastX != context.mouseX || this.lastY != context.mouseY)
                {
                    Vector3d newPoint = this.calculateOnPlane(context);

                    this.pos.set(this.cachedPos);
                    this.pos.sub((float) newPoint.x, (float) newPoint.y, (float) newPoint.z);
                    this.pos.add((float) this.cachedPlaneIntersection.x, (float) this.cachedPlaneIntersection.y, (float) this.cachedPlaneIntersection.z);

                    this.lastX = mouseX;
                    this.lastY = mouseY;
                }
            }
            else
            {
                this.camera.rotation.y -= MathUtils.toRad(this.lastX - mouseX);
                this.camera.rotation.x -= MathUtils.toRad(this.lastY - mouseY);

                this.lastX = mouseX;
                this.lastY = mouseY;
            }
        }
    }

    public void setupPosition()
    {
        this.camera.position.set(this.pos);

        vec.set(0, 0, -this.distance.getValue());
        this.rotateVector(vec);

        this.camera.position.x += vec.x;
        this.camera.position.y += vec.y;
        this.camera.position.z += vec.z;
    }

    private Vector3d calculateOnPlane(UIContext context)
    {
        Vector3d vector = new Vector3d();
        Vector3d origin = new Vector3d(this.cachedCamera.position).sub(this.cachedPos);
        Vector3d destination = new Vector3d(this.cachedCamera.getMouseDirection(context.mouseX, context.mouseY, context.globalX(this.area.x), context.globalY(this.area.y), this.area.w, this.area.h)).mul(this.distance.getValue() * 2).add(origin);
        Intersectiond.intersectLineSegmentPlane(origin.x, origin.y, origin.z, destination.x, destination.y, destination.z, this.plane.x, this.plane.y, this.plane.z, 0, vector);

        return vector;
    }

    private void rotateVector(Vector3d vec)
    {
        mat.identity().rotateX(this.camera.rotation.x);
        mat.transform(vec);
        mat.identity().rotateY(MathUtils.PI - this.camera.rotation.y);
        mat.transform(vec);
    }

    protected void setupViewport(UIContext context)
    {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        class_310 mc = class_310.method_1551();

        if (this.stencilViewport)
        {
            RenderSystem.viewport(0, 0, this.stencilViewportW, this.stencilViewportH);
            this.camera.updatePerspectiveProjection(this.stencilViewportW, this.stencilViewportH);
            this.camera.updateView();

            return;
        }

        /* Exact physical-to-logical ratio (the UI scale factor). Rounding this snapped fractional scales
           like 1.5 up to 2, which offset the viewport and misaligned model/morph previews. */
        float rx = (float) (mc.method_22683().method_4480() / (double) context.menu.width);
        float ry = (float) (mc.method_22683().method_4507() / (double) context.menu.height);
        float size = BBSModClient.getOriginalFramebufferScale();

        int vx = (int) (context.globalX(this.area.x) * rx);
        int vy = (int) (mc.method_22683().method_4507() - (context.globalY(this.area.y) + this.area.h) * ry);
        int vw = (int) (this.area.w * rx);
        int vh = (int) (this.area.h * ry);

        RenderSystem.viewport((int) (vx * size), (int) (vy * size), (int) (vw * size), (int) (vh * size));
        this.camera.updatePerspectiveProjection(vw, vh);
        this.camera.updateView();
    }

    /**
     * Draw your model here
     */
    protected abstract void renderUserModel(UIContext context);

    /**
     * Render block of grass under the model (which signify where
     * located the ground below the model)
     */
    protected void renderGrid(UIContext context)
    {
        Matrix4f matrix4f = context.batcher.getContext().method_51448().method_23760().method_23761();
        class_287 builder = class_289.method_1348().method_60827(class_293.class_5596.field_29344, class_290.field_1576);

        RenderSystem.setShader(class_757::method_34540);

        for (int x = 0; x <= 10; x ++)
        {
            if (x == 0)
            {
                builder.method_22918(matrix4f, x - 5, 0, -5).method_22915(0F, 0F, 1F, 1F);
                builder.method_22918(matrix4f, x - 5, 0, 5).method_22915(0F, 0F, 1F, 1F);
            }
            else
            {
                builder.method_22918(matrix4f, x - 5, 0, -5).method_22915(0.25F, 0.25F, 0.25F, 1F);
                builder.method_22918(matrix4f, x - 5, 0, 5).method_22915(0.25F, 0.25F, 0.25F, 1F);
            }
        }

        for (int x = 0; x <= 10; x ++)
        {
            if (x == 0)
            {
                builder.method_22918(matrix4f, -5, 0, x - 5).method_22915(1F, 0F, 0F, 1F);
                builder.method_22918(matrix4f, 5, 0, x - 5).method_22915(1F, 0F, 0F, 1F);
            }
            else
            {
                builder.method_22918(matrix4f, -5, 0, x - 5).method_22915(0.25F, 0.25F, 0.25F, 1F);
                builder.method_22918(matrix4f, 5, 0, x - 5).method_22915(0.25F, 0.25F, 0.25F, 1F);
            }
        }

        class_286.method_43433(builder.method_60800());
    }
}
