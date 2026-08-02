package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoController;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MatrixStackUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CML-native gizmo core.
 *
 * Owns the addon-facing handle registry, the deferred (Iris) render queue, and the
 * translate/scale/rotate/combined/trackball handle table used by every 3D viewport
 * in the editor (Model, Form, Film replays, Animation state). Individual viewports
 * feed their picking/hover/drag lifecycle through {@link GizmoController}
 * instead of talking to this class directly, but this class remains a singleton so
 * addons registered through {@link #register(int, IGizmoHandler)} keep working unchanged.
 */
public class Gizmo
{
    private static final class DeferredGizmo
    {
        private final Matrix4f matrix;
        private final Matrix4f projection;
        private final boolean stencil;
        private final StencilMap stencilMap;

        private DeferredGizmo(Matrix4f matrix, boolean stencil, StencilMap stencilMap)
        {
            this.matrix = matrix;
            this.projection = new Matrix4f();
            this.stencil = stencil;
            this.stencilMap = stencilMap;
        }
    }

    public interface IGizmoHandler
    {
        public void start(Gizmo gizmo, int index, int mouseX, int mouseY, UIPropTransform transform);
    }

    /* Solo-mode handle ids (translate/scale/rotate), unchanged since the original
     * implementation. Any precompiled addon referencing these constants keeps working. */
    public final static int STENCIL_X = 1;
    public final static int STENCIL_Y = 2;
    public final static int STENCIL_Z = 3;
    public final static int STENCIL_XZ = 4;
    public final static int STENCIL_XY = 5;
    public final static int STENCIL_ZY = 6;
    public final static int STENCIL_FREE = 7;

    /* Combined-mode-only handle ids: scale and rotate need distinct ids from the
     * move bars/planes above so translate + scale + rotate can be picked simultaneously. */
    public final static int STENCIL_SCALE_X = 8;
    public final static int STENCIL_SCALE_Y = 9;
    public final static int STENCIL_SCALE_Z = 10;
    public final static int STENCIL_ROTATE_X = 11;
    public final static int STENCIL_ROTATE_Y = 12;
    public final static int STENCIL_ROTATE_Z = 13;

    /* Free-rotate trackball hit sphere, available in solo Rotate and Combined mode. */
    public final static int STENCIL_TRACKBALL = 14;

    /* Reserved for a future screen-space translate handle. Not drawn or picked yet. */
    public final static int STENCIL_SCREEN = 15;

    /* Camera-facing view/arcball ring, drawn in Combined mode. */
    public final static int STENCIL_VIEW = 16;

    public final static int STENCIL_HANDLE_MAX = STENCIL_VIEW;

    private static final float[] COLOR_ACTIVE = { 1.00F, 0.80F, 0.25F };
    private static final float[] COLOR_X_IDLE = { 0.80F, 0.28F, 0.28F };
    private static final float[] COLOR_X_HOVER = { 1.00F, 0.35F, 0.35F };
    private static final float[] COLOR_Y_IDLE = { 0.30F, 0.75F, 0.35F };
    private static final float[] COLOR_Y_HOVER = { 0.40F, 1.00F, 0.45F };
    private static final float[] COLOR_Z_IDLE = { 0.28F, 0.50F, 0.95F };
    private static final float[] COLOR_Z_HOVER = { 0.35F, 0.62F, 1.00F };
    /* Plane handle colors are a vivid blend of their two constituent axis colors
     * (X = red, Y = green, Z = blue), so e.g. the XY plane reads as a saturated
     * yellow (red + green) rather than an arbitrary unrelated hue. */
    private static final float[] COLOR_XZ_IDLE = { 0.85F, 0.25F, 0.85F };
    private static final float[] COLOR_XZ_HOVER = { 1.00F, 0.35F, 1.00F };
    private static final float[] COLOR_XY_IDLE = { 0.85F, 0.80F, 0.20F };
    private static final float[] COLOR_XY_HOVER = { 1.00F, 0.95F, 0.25F };
    private static final float[] COLOR_ZY_IDLE = { 0.20F, 0.75F, 0.80F };
    private static final float[] COLOR_ZY_HOVER = { 0.30F, 0.90F, 0.95F };
    private static final float[] COLOR_FREE_IDLE = { 1.00F, 1.00F, 1.00F };
    private static final float[] COLOR_VIEW_IDLE = { 0.80F, 0.80F, 0.80F };
    private static final float[] COLOR_VIEW_HOVER = { 1.00F, 1.00F, 1.00F };

    private static final float PLANE_ALPHA_IDLE = 0.25F;
    private static final float PLANE_ALPHA_HOVER = 0.45F;
    private static final float PLANE_ALPHA_ACTIVE = 0.85F;

    /* Relative sizing so the rotate rings visibly enclose the move arrows and scale cubes
     * (largest to smallest: rotate rings > scale cubes > move arrows), matching a Blender-style
     * combined gizmo cage rather than three same-size handle sets stacked on top of each other. */
    private static final float COMBINED_MOVE_SCALE = 0.85F;
    private static final float COMBINED_ROTATE_SCALE = 1.6F;
    private static final float COMBINED_SCALE_HANDLE_SCALE = 1.0F;

    public final static Gizmo INSTANCE = new Gizmo();

    private Mode mode = Mode.TRANSLATE;
    /* BBSSettings.gizmoDefaultMode can't be read at class-init time (settings aren't loaded
     * from disk yet), so the configured starting mode is applied lazily the first time the
     * gizmo actually renders instead. */
    private boolean appliedDefaultMode = false;

    private int index = -1;
    private int hoveredIndex = -1;

    public final Matrix4f lastGizmoMatrix = new Matrix4f();
    public boolean hasGizmoMatrix;

    private UIPropTransform currentTransform;
    private Map<Integer, IGizmoHandler> handlers = new HashMap<>();
    private final List<DeferredGizmo> deferredGizmos = new ArrayList<>();

    private float lastSx = 1F;
    private float lastSy = 1F;
    private float lastSz = 1F;

    /** Orbit-camera zoom multiplier from {@link UIModelRenderer}; 1 outside model viewports. */
    private float viewportZoomScale = 1F;

    /* Direction from the gizmo origin toward the camera in gizmo-local space, refreshed
     * every frame by computeScale(). Used to billboard the view/arcball ring. */
    private final Vector3f lastCamDir = new Vector3f(0F, 1F, 0F);

    /* Visual state of the in-progress rotation sweep: which ring is being rotated, at what
     * angle (in the ring's arc3D u-parameterization) the drag started, and how far it has
     * swept since. Purely cosmetic; fed by UIPropTransform's ring drag. */
    private boolean arcActive;
    /** True when the active sweep belongs to the camera-facing view ring instead of an
     *  axis ring; its angles are measured in the billboarded ring's own space. */
    private boolean arcView;
    private Axis arcAxis = Axis.Y;
    private float arcStartU;
    private float arcLastU;
    private float arcSweep;
    /** When true the sweep fan is drawn in the gizmo orientation captured at drag start
     *  instead of the live (rotating) matrix, so local-mode drags keep a fixed origin. */
    private boolean arcFrozenOrientation;
    private final Matrix4f arcFrozenMatrix = new Matrix4f();
    private boolean arcFrozenViewRing;
    private final Vector3f arcFrozenCamDir = new Vector3f(0F, 1F, 0F);

    /* Yellow progress segment drawn while a translate/scale handle is being dragged. */
    private boolean dragProgressActive;
    private final Vector3f dragProgressStart = new Vector3f();
    private final Vector3f dragProgressEnd = new Vector3f();

    private Gizmo()
    {}

    public void register(int index, IGizmoHandler handler)
    {
        this.handlers.put(index, handler);
    }

    public static boolean isHandleIndex(int index)
    {
        return index >= STENCIL_X && index <= STENCIL_HANDLE_MAX;
    }

    public boolean isDragging()
    {
        return this.index != -1;
    }

    public int getActiveHandle()
    {
        return this.index;
    }

    public Mode getMode()
    {
        return this.mode;
    }

    /** The trackball sphere shows in the dedicated rotate mode and in combined. */
    public boolean hasSphere()
    {
        return this.mode == Mode.ROTATE || this.mode == Mode.COMBINED;
    }

    public boolean isTrackballPickable()
    {
        if (!BBSSettings.gizmos.get() || !BBSSettings.gizmoTrackball.get())
        {
            return false;
        }

        return this.hasSphere();
    }

    public boolean hitsTrackball(Matrix4f gizmoMatrix, Vector3d rayOrigin, Vector3f rayDirection)
    {
        if (!this.isTrackballPickable())
        {
            return false;
        }

        Matrix4f inv = new Matrix4f(gizmoMatrix).invert();
        Vector4f localOrigin = new Vector4f((float) rayOrigin.x, (float) rayOrigin.y, (float) rayOrigin.z, 1F);
        inv.transform(localOrigin);
        Vector4f localDir = new Vector4f(rayDirection.x, rayDirection.y, rayDirection.z, 0F);
        inv.transform(localDir);

        float a = localDir.x * localDir.x + localDir.y * localDir.y + localDir.z * localDir.z;
        float b = 2F * (localOrigin.x * localDir.x + localOrigin.y * localDir.y + localOrigin.z * localDir.z);
        float c = localOrigin.x * localOrigin.x + localOrigin.y * localOrigin.y + localOrigin.z * localOrigin.z - 1F;
        float disc = b * b - 4F * a * c;

        if (disc < 0F)
        {
            return false;
        }

        float t = (-b - (float) Math.sqrt(disc)) / (2F * a);

        return t > 0F;
    }

    public void setViewportZoomScale(float viewportZoomScale)
    {
        this.viewportZoomScale = viewportZoomScale;
    }

    public boolean setMode(Mode mode)
    {
        if (!BBSSettings.gizmos.get())
        {
            return false;
        }

        boolean same = this.mode == mode;

        this.mode = mode;

        return !same;
    }

    /** Continuously-updated (not just while dragging) handle hover state, fed by
     *  {@link GizmoController} every frame so the render
     *  pass can brighten a handle the mouse is over before the user commits to a drag. */
    public int getHoveredIndex()
    {
        return this.hoveredIndex;
    }

    public void setHoveredIndex(int hoveredIndex)
    {
        this.hoveredIndex = hoveredIndex;
    }

    public boolean start(int index, int mouseX, int mouseY, UIPropTransform transform)
    {
        if (!BBSSettings.gizmos.get())
        {
            return false;
        }

        if (this.handlers.containsKey(index))
        {
            this.handlers.get(index).start(this, index, mouseX, mouseY, transform);
            this.index = index;

            return true;
        }

        if (!isHandleIndex(index))
        {
            return false;
        }

        this.index = index;
        this.currentTransform = transform;

        if (transform != null)
        {
            if (this.mode == Mode.COMBINED)
            {
                this.startCombined(index, transform);
            }
            else
            {
                this.startSolo(index, transform);
            }
        }

        return true;
    }

    private void startSolo(int index, UIPropTransform transform)
    {
        int mode = this.mode.ordinal();

        if (index == STENCIL_X) transform.enableMode(mode, Axis.X);
        else if (index == STENCIL_Y) transform.enableMode(mode, Axis.Y);
        else if (index == STENCIL_Z) transform.enableMode(mode, Axis.Z);
        else if (index == STENCIL_XY) transform.enablePlaneMode(mode, Axis.X, Axis.Y);
        else if (index == STENCIL_XZ) transform.enablePlaneMode(mode, Axis.X, Axis.Z);
        else if (index == STENCIL_ZY) transform.enablePlaneMode(mode, Axis.Z, Axis.Y);
        else if (index == STENCIL_TRACKBALL) transform.enableTrackballRotate(Mode.ROTATE.ordinal());
        else if (index == STENCIL_FREE)
        {
            if (this.mode == Mode.TRANSLATE) transform.enableFreeTranslation(mode);
            else if (this.mode == Mode.ROTATE) transform.enableFreeRotation(mode, Axis.X);
            else if (this.mode == Mode.SCALE) transform.enableUniformScale(mode);
        }
    }

    /** Same handle dispatch as {@link #startSolo(int, UIPropTransform)}, but routed through
     *  the *KeepGizmoMode variants so grabbing e.g. a scale cube while in Combined mode
     *  doesn't flip the whole gizmo back to solo Scale mode. */
    private void startCombined(int index, UIPropTransform transform)
    {
        int move = Mode.TRANSLATE.ordinal();
        int scale = Mode.SCALE.ordinal();
        int rotate = Mode.ROTATE.ordinal();

        if (index == STENCIL_X) transform.enableModeKeepGizmoMode(move, Axis.X);
        else if (index == STENCIL_Y) transform.enableModeKeepGizmoMode(move, Axis.Y);
        else if (index == STENCIL_Z) transform.enableModeKeepGizmoMode(move, Axis.Z);
        else if (index == STENCIL_XY) transform.enablePlaneModeKeepGizmoMode(move, Axis.X, Axis.Y);
        else if (index == STENCIL_XZ) transform.enablePlaneModeKeepGizmoMode(move, Axis.X, Axis.Z);
        else if (index == STENCIL_ZY) transform.enablePlaneModeKeepGizmoMode(move, Axis.Z, Axis.Y);
        else if (index == STENCIL_FREE) transform.enableFreeTranslation(move);
        else if (index == STENCIL_SCALE_X) transform.enableModeKeepGizmoMode(scale, Axis.X);
        else if (index == STENCIL_SCALE_Y) transform.enableModeKeepGizmoMode(scale, Axis.Y);
        else if (index == STENCIL_SCALE_Z) transform.enableModeKeepGizmoMode(scale, Axis.Z);
        else if (index == STENCIL_ROTATE_X) transform.enableModeKeepGizmoMode(rotate, Axis.X);
        else if (index == STENCIL_ROTATE_Y) transform.enableModeKeepGizmoMode(rotate, Axis.Y);
        else if (index == STENCIL_ROTATE_Z) transform.enableModeKeepGizmoMode(rotate, Axis.Z);
        else if (index == STENCIL_TRACKBALL) transform.enableTrackballRotate(rotate);
        else if (index == STENCIL_VIEW) transform.enableViewRotate(rotate);
    }

    public void stop()
    {
        this.index = -1;
        this.arcActive = false;
        this.clearDragProgress();

        if (this.currentTransform != null)
        {
            this.currentTransform.acceptChanges();
        }

        this.currentTransform = null;
    }

    public void setDragProgress(Vector3f start, Vector3f end)
    {
        this.dragProgressStart.set(start);
        this.dragProgressEnd.set(end);
        this.dragProgressActive = true;
    }

    public void clearDragProgress()
    {
        this.dragProgressActive = false;
    }

    /** Camera-facing flip sign (+1/-1) applied to handles on the given axis, so drag logic
     *  can be radial ("away from center") instead of tied to the positive axis direction. */
    public float getFlipSign(Axis axis)
    {
        if (axis == Axis.X) return this.lastSx;
        if (axis == Axis.Y) return this.lastSy;

        return this.lastSz;
    }

    /* ---- rotation sweep arc (visual only) ---- */

    public void startRotationArc(Vector3f local)
    {
        if (this.index == STENCIL_VIEW)
        {
            this.arcView = true;
            this.arcStartU = this.viewRingAngle(local);
            this.arcLastU = this.arcStartU;
            this.arcSweep = 0F;
            this.arcActive = true;

            return;
        }

        Axis axis = this.ringAxisFromIndex();

        if (axis == null)
        {
            this.arcActive = false;

            return;
        }

        this.arcView = false;
        this.arcAxis = axis;
        this.arcStartU = this.ringAngle(axis, local);
        this.arcLastU = this.arcStartU;
        this.arcSweep = 0F;
        this.arcActive = true;
    }

    /** Re-baselines the per-frame arc delta after a cursor teleport (window edge wrap,
     * mouse leaving the window, etc.) without clearing the accumulated {@link #arcSweep}. */
    public void reanchorRotationArc(Vector3f local)
    {
        if (!this.arcActive)
        {
            this.startRotationArc(local);

            return;
        }

        this.arcLastU = this.arcView ? this.viewRingAngle(local) : this.ringAngle(this.arcAxis, local);
    }

    public void updateRotationArc(Vector3f local)
    {
        if (!this.arcActive)
        {
            return;
        }

        float u = this.arcView ? this.viewRingAngle(local) : this.ringAngle(this.arcAxis, local);
        float delta = u - this.arcLastU;

        while (delta > 180F) delta -= 360F;
        while (delta < -180F) delta += 360F;

        this.arcSweep += delta;
        this.arcLastU = u;
    }

    /** Accumulates sweep from the rotation delta already applied to the value (used in
     *  local-mode drags where the gizmo matrix rotates and geometric arc tracking would
     *  drift or run too fast). */
    public void addRotationSweep(float deltaDegrees)
    {
        if (!this.arcActive)
        {
            return;
        }

        this.arcSweep += deltaDegrees;
    }

    /** Pins the sweep fan to the gizmo matrix at drag start so only its length grows. */
    public void setRotationArcFrozen(Matrix4f matrix, boolean viewRing)
    {
        this.arcFrozenOrientation = true;
        this.arcFrozenMatrix.set(matrix);
        this.arcFrozenViewRing = viewRing;

        if (viewRing)
        {
            this.arcFrozenCamDir.set(this.lastCamDir);
        }
    }

    public void clearRotationArc()
    {
        this.arcActive = false;
        this.arcView = false;
        this.arcSweep = 0F;
        this.arcFrozenOrientation = false;
        this.arcFrozenViewRing = false;
    }

    public boolean hasRotationArc()
    {
        return this.arcActive;
    }

    public float getRotationSweep()
    {
        return this.arcSweep;
    }

    private Axis ringAxisFromIndex()
    {
        if (this.index == STENCIL_X || this.index == STENCIL_ROTATE_X) return Axis.X;
        if (this.index == STENCIL_Y || this.index == STENCIL_ROTATE_Y) return Axis.Y;
        if (this.index == STENCIL_Z || this.index == STENCIL_ROTATE_Z) return Axis.Z;

        return null;
    }

    /** Angle (degrees) of a gizmo-local point in the given ring's {@link Draw#arc3D}
     *  u-parameterization, so the sweep fan lines up with the grab point exactly. */
    private float ringAngle(Axis axis, Vector3f local)
    {
        double u;

        if (axis == Axis.X) u = Math.atan2(local.z, local.y);
        else if (axis == Axis.Y) u = Math.atan2(local.z, local.x);
        else u = Math.atan2(-local.y, local.x);

        return (float) Math.toDegrees(u);
    }

    /** Same as {@link #ringAngle(Axis, Vector3f)} but for the camera-facing view ring: the
     *  gizmo-local point is rotated back into the billboarded ring's space (where the ring is
     *  a plain Y ring) before measuring the angle. */
    private float viewRingAngle(Vector3f local)
    {
        Quaternionf toRing = new Quaternionf().rotationTo(0F, 1F, 0F, this.lastCamDir.x, this.lastCamDir.y, this.lastCamDir.z).conjugate();
        Vector3f ringLocal = new Vector3f(local);

        toRing.transform(ringLocal);

        return this.ringAngle(Axis.Y, ringLocal);
    }

    /** During a drag only the grabbed handle stays visible in the colored pass; everything
     *  else is hidden until release so the user can see the model unobstructed. */
    private boolean showHandle(int handleId)
    {
        return this.index == -1 || this.index == handleId;
    }

    public void deferRender(Matrix4f matrix, boolean stencil, StencilMap stencilMap)
    {
        this.deferredGizmos.add(new DeferredGizmo(new Matrix4f(matrix), stencil, stencilMap));
    }

    /**
     * Record the gizmo's model-view during the world pass without drawing. The
     * colored visual is composited later in {@link #renderInterface} so Iris
     * shader packs cannot displace it from the on-ground hitbox.
     */
    public void captureVisual(MatrixStack stack)
    {
        if (BBSRendering.isIrisShadowPass())
        {
            return;
        }

        this.lastGizmoMatrix.set(stack.peek().getPositionMatrix());
        this.hasGizmoMatrix = true;
    }

    public void clearVisual()
    {
        this.hasGizmoMatrix = false;
    }

    /**
     * Draw the captured gizmo visual in the UI pass with the same projection and
     * viewport rect as the film/model preview that rendered the world this frame.
     */
    public void renderInterface(UIContext context, Matrix4f projection, Area area)
    {
        if (BBSRendering.isIrisShadowPass() || !this.hasGizmoMatrix
            || context == null || projection == null || area == null)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        context.batcher.flush();

        MatrixStackUtils.cacheMatrices();
        RenderSystem.setProjectionMatrix(new RawProjectionMatrix("gizmo_interface").set(projection), ProjectionType.ORTHOGRAPHIC);

        float rx = (float) Math.round(mc.getWindow().getWidth() / (double) context.menu.width);
        float ry = (float) Math.round(mc.getWindow().getHeight() / (double) context.menu.height);
        float size = BBSModClient.getOriginalFramebufferScale();
        int vx = (int) (area.x * rx);
        int vy = (int) (mc.getWindow().getHeight() - (area.y + area.h) * ry);
        int vw = (int) (area.w * rx);
        int vh = (int) (area.h * ry);

        GlStateManager._viewport((int) (vx * size), (int) (vy * size), (int) (vw * size), (int) (vh * size));

        MatrixStack stack = new MatrixStack();

        MatrixStackUtils.multiply(stack, this.lastGizmoMatrix);

        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        this.render(stack);
        GlStateManager._depthMask(true);
        GlStateManager._enableDepthTest();

        GlStateManager._viewport(0, 0, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        MatrixStackUtils.restoreMatrices();
    }

    /**
     * Stencil pick pass counterpart to {@link #renderInterface}: identical viewport,
     * projection and captured matrix so handle ids line up with the drawn visual.
     */
    public void renderStencilInterface(UIContext context, Matrix4f projection, Area area, StencilMap map)
    {
        if (BBSRendering.isIrisShadowPass() || !this.hasGizmoMatrix
            || context == null || projection == null || area == null || map == null)
        {
            return;
        }

        if (!BBSSettings.gizmos.get())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        MatrixStackUtils.cacheMatrices();
        RenderSystem.setProjectionMatrix(new RawProjectionMatrix("gizmo_stencil").set(projection), ProjectionType.ORTHOGRAPHIC);

        float rx = (float) Math.round(mc.getWindow().getWidth() / (double) context.menu.width);
        float ry = (float) Math.round(mc.getWindow().getHeight() / (double) context.menu.height);
        float size = BBSModClient.getOriginalFramebufferScale();
        int vx = (int) (area.x * rx);
        int vy = (int) (mc.getWindow().getHeight() - (area.y + area.h) * ry);
        int vw = (int) (area.w * rx);
        int vh = (int) (area.h * ry);

        GlStateManager._viewport((int) (vx * size), (int) (vy * size), (int) (vw * size), (int) (vh * size));

        MatrixStack stack = new MatrixStack();

        MatrixStackUtils.multiply(stack, this.lastGizmoMatrix);
        this.renderStencil(stack, map);

        GlStateManager._viewport(0, 0, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        MatrixStackUtils.restoreMatrices();
    }

    public boolean hasDeferred()
    {
        return !this.deferredGizmos.isEmpty();
    }

    public void clearDeferred()
    {
        this.deferredGizmos.clear();
    }

    public void renderDeferred(MatrixStack stack)
    {
        if (this.deferredGizmos.isEmpty())
        {
            return;
        }

        for (DeferredGizmo deferred : this.deferredGizmos)
        {
            stack.push();

            stack.peek().getPositionMatrix().set(deferred.matrix);
            stack.peek().getNormalMatrix().identity();

            if (deferred.stencil && deferred.stencilMap != null)
            {
                this.renderStencil(stack, deferred.stencilMap);
            }
            else
            {
                this.render(stack);
            }

            stack.pop();
        }

        this.deferredGizmos.clear();
    }

    /* ---- shared per-frame scale/orientation bookkeeping ---- */

    private float computeScale(MatrixStack stack)
    {
        if (!this.appliedDefaultMode)
        {
            this.appliedDefaultMode = true;

            if (BBSSettings.gizmoDefaultMode != null)
            {
                Mode[] modes = Mode.values();
                int def = BBSSettings.gizmoDefaultMode.get();

                if (def >= 0 && def < modes.length)
                {
                    this.mode = modes[def];
                }
            }
        }

        Matrix4f inv = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        Vector4f camPos = new Vector4f(0, 0, 0, 1).mul(inv);
        double dist = Math.sqrt(camPos.x * camPos.x + camPos.y * camPos.y + camPos.z * camPos.z);
        float axesScale = BBSSettings.axesScale == null ? 1F : BBSSettings.axesScale.get();

        this.updateFlipSigns(camPos.x, camPos.y, camPos.z);

        if (dist > 1.0E-6D)
        {
            this.lastCamDir.set((float) (camPos.x / dist), (float) (camPos.y / dist), (float) (camPos.z / dist));
        }

        boolean constantSize = BBSSettings.gizmoConstantSize == null || BBSSettings.gizmoConstantSize.get();

        if (!constantSize)
        {
            /* Fixed world size: appears smaller on screen when the camera moves away. */
            return 1.4F * 0.5F * axesScale;
        }

        float minFloor = BBSSettings.gizmoConstantSizeMin == null ? 0.5F : BBSSettings.gizmoConstantSizeMin.get();
        double distanceFactor = dist * 0.12D;

        if (minFloor <= 0F)
        {
            return (float) (1.4F * distanceFactor * axesScale);
        }

        return (float) (1.4F * Math.max(minFloor, distanceFactor) * axesScale);
    }

    /**
     * Extra line/hitbox fattening that grows smoothly with camera distance so thin rings
     * stay easy to see and click when zoomed out (visual and stencil use the same boost).
     */
    private float distanceThicknessBoost(float dist)
    {
        float t = MathUtils.clamp((dist - 3F) / 28F, 0F, 1F);

        /* Close: 1× — far: up to ~2.4× thickness on both preview and hitbox. */
        return 1F + t * 1.4F;
    }

    private float resolveThickness(boolean stencil)
    {
        float thickness = BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get();
        boolean constantSize = BBSSettings.gizmoConstantSize == null || BBSSettings.gizmoConstantSize.get();

        if (!constantSize)
        {
            thickness *= this.distanceThicknessBoost(this.lastScaleDist);
        }

        if (stencil)
        {
            thickness *= BBSSettings.gizmoHitbox == null ? 1F : BBSSettings.gizmoHitbox.get();
        }

        return thickness;
    }

    private void updateFlipSigns(float camX, float camY, float camZ)
    {
        if (this.index == -1)
        {
            this.lastSx = camX >= 0 ? 1F : -1F;
            this.lastSy = camY >= 0 ? 1F : -1F;
            this.lastSz = camZ >= 0 ? 1F : -1F;
        }
    }

    /* ---- visual (colored) render pass ---- */

    public void render(MatrixStack stack)
    {
        this.lastGizmoMatrix.set(stack.peek().getPositionMatrix());
        this.hasGizmoMatrix = true;

        float thickness = BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get();

        if (!BBSSettings.gizmos.get())
        {
            Draw.coolerAxes(stack, 0.25F, 0.015F * thickness, 0.26F, 0.025F * thickness);
            return;
        }

        float scale = this.computeScale(stack);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        if (this.mode == Mode.ROTATE) this.drawRotate(builder, stack, scale, thickness, false, null);
        else if (this.mode == Mode.SCALE) this.drawScale(builder, stack, scale, thickness, false, null);
        else if (this.mode == Mode.COMBINED) this.drawCombined(builder, stack, scale, thickness, false, null);
        else this.drawTranslate(builder, stack, scale, thickness, false, null);

        this.drawActiveGuide(builder, stack, scale, thickness);
        this.drawDragProgress(builder, stack, scale, thickness);

        // RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        /* shader binding handled by RenderLayer in 1.21.11 */
        GlStateManager._depthFunc(GL11.GL_ALWAYS);

        RenderLayers.debugFilledBox().draw(builder.end());


        GlStateManager._depthFunc(GL11.GL_LEQUAL);
    }

    /* ---- stencil (id-encoded) render pass ---- */

    public void renderStencil(MatrixStack stack, StencilMap map)
    {
        this.lastGizmoMatrix.set(stack.peek().getPositionMatrix());
        this.hasGizmoMatrix = true;

        if (!BBSSettings.gizmos.get())
        {
            return;
        }

        float scale = this.computeScale(stack);
        float thickness = BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get();

        /* The pick pass gets its own thickness multiplier so the clickable area can be made
         * fatter (or thinner) than the visible handles. */
        thickness *= BBSSettings.gizmoHitbox == null ? 1F : BBSSettings.gizmoHitbox.get();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        if (this.mode == Mode.ROTATE) this.drawRotate(builder, stack, scale, thickness, true, map);
        else if (this.mode == Mode.SCALE) this.drawScale(builder, stack, scale, thickness, true, map);
        else if (this.mode == Mode.COMBINED) this.drawCombined(builder, stack, scale, thickness, true, map);
        else this.drawTranslate(builder, stack, scale, thickness, true, map);

        GlStateManager._depthFunc(GL11.GL_ALWAYS);
        GlStateManager._depthMask(false);

        if (BBSRendering.isIrisShadersEnabled())
        {
            MatrixStackUtils.pushIdentityModelView();
        }

        RenderLayers.debugFilledBox().draw(builder.end());

        if (BBSRendering.isIrisShadersEnabled())
        {
            MatrixStackUtils.popModelView();
        }

        GlStateManager._depthMask(true);
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
    }

    /* ---- color helpers ---- */

    private float[] pickColor(int handleId, float[] idle, float[] hover)
    {
        if (this.index == handleId) return COLOR_ACTIVE;
        if (this.index == -1 && this.hoveredIndex == handleId) return hover;

        return idle;
    }

    private float pickPlaneAlpha(int handleId)
    {
        if (this.index == handleId) return PLANE_ALPHA_ACTIVE;
        if (this.index == -1 && this.hoveredIndex == handleId) return PLANE_ALPHA_HOVER;

        return PLANE_ALPHA_IDLE;
    }

    private float[] stencilColor(int handleId)
    {
        return new float[] { handleId / 255F, 0F, 0F };
    }

    private void box(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float[] color, float a)
    {
        Draw.fillBox(builder, stack, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), color[0], color[1], color[2], a);
    }

    /* ---- translate handles (bars with cone tips + move planes + screen-move cube) ---- */

    private void drawTranslate(BufferBuilder builder, MatrixStack stack, float scale, float thickness, boolean stencil, StencilMap map)
    {
        float axisSize = 0.30F * scale;
        float axisOffset = 0.012F * scale * thickness;
        float planeInner = 0.08F * scale;
        float planeOuter = 0.20F * scale;
        float offset = 0.001F * scale;

        this.drawMoveBars(builder, stack, axisSize, axisOffset, stencil, map);
        this.drawMovePlanes(builder, stack, planeInner, planeOuter, offset, stencil, map);
        this.drawScreenCube(builder, stack, axisOffset, stencil, map);
    }

    private void drawMoveBars(BufferBuilder builder, MatrixStack stack, float axisSize, float axisOffset, boolean stencil, StencilMap map)
    {
        float[] xCol = stencil ? this.stencilColor(STENCIL_X) : this.pickColor(STENCIL_X, COLOR_X_IDLE, COLOR_X_HOVER);
        float[] yCol = stencil ? this.stencilColor(STENCIL_Y) : this.pickColor(STENCIL_Y, COLOR_Y_IDLE, COLOR_Y_HOVER);
        float[] zCol = stencil ? this.stencilColor(STENCIL_Z) : this.pickColor(STENCIL_Z, COLOR_Z_IDLE, COLOR_Z_HOVER);
        float tipRadius = axisOffset * 2.4F;
        float tipLength = axisOffset * 6F;

        if (this.showHandle(STENCIL_X))
        {
            this.box(builder, stack, 0, -axisOffset, -axisOffset, axisSize * this.lastSx, axisOffset, axisOffset, xCol, 1F);
            /* Apex is the far/outward point of the arrow, base is the wide disc that meets the bar. */
            Draw.cone(builder, stack, (axisSize + tipLength) * this.lastSx, 0, 0, axisSize * this.lastSx, 0, 0, tipRadius, 10, xCol[0], xCol[1], xCol[2], 1F);
        }

        if (this.showHandle(STENCIL_Y))
        {
            this.box(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize * this.lastSy, axisOffset, yCol, 1F);
            Draw.cone(builder, stack, 0, (axisSize + tipLength) * this.lastSy, 0, 0, axisSize * this.lastSy, 0, tipRadius, 10, yCol[0], yCol[1], yCol[2], 1F);
        }

        if (this.showHandle(STENCIL_Z))
        {
            this.box(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize * this.lastSz, zCol, 1F);
            Draw.cone(builder, stack, 0, 0, (axisSize + tipLength) * this.lastSz, 0, 0, axisSize * this.lastSz, tipRadius, 10, zCol[0], zCol[1], zCol[2], 1F);
        }
    }

    private void drawMovePlanes(BufferBuilder builder, MatrixStack stack, float planeInner, float planeOuter, float offset, boolean stencil, StencilMap map)
    {
        float xzAlpha = stencil ? 1F : this.pickPlaneAlpha(STENCIL_XZ);
        float xyAlpha = stencil ? 1F : this.pickPlaneAlpha(STENCIL_XY);
        float zyAlpha = stencil ? 1F : this.pickPlaneAlpha(STENCIL_ZY);

        float[] xzCol = stencil ? this.stencilColor(STENCIL_XZ) : COLOR_XZ_IDLE;
        float[] xyCol = stencil ? this.stencilColor(STENCIL_XY) : COLOR_XY_IDLE;
        float[] zyCol = stencil ? this.stencilColor(STENCIL_ZY) : COLOR_ZY_IDLE;

        if (!stencil && this.index == -1 && this.hoveredIndex == STENCIL_XZ) xzCol = COLOR_XZ_HOVER;
        if (!stencil && this.index == -1 && this.hoveredIndex == STENCIL_XY) xyCol = COLOR_XY_HOVER;
        if (!stencil && this.index == -1 && this.hoveredIndex == STENCIL_ZY) zyCol = COLOR_ZY_HOVER;
        if (!stencil && this.index == STENCIL_XZ) xzCol = COLOR_ACTIVE;
        if (!stencil && this.index == STENCIL_XY) xyCol = COLOR_ACTIVE;
        if (!stencil && this.index == STENCIL_ZY) zyCol = COLOR_ACTIVE;

        if (this.showHandle(STENCIL_XZ))
        {
            this.box(builder, stack, planeInner * this.lastSx, -offset, planeInner * this.lastSz, planeOuter * this.lastSx, offset, planeOuter * this.lastSz, xzCol, xzAlpha);
        }

        if (this.showHandle(STENCIL_XY))
        {
            this.box(builder, stack, planeInner * this.lastSx, planeInner * this.lastSy, -offset, planeOuter * this.lastSx, planeOuter * this.lastSy, offset, xyCol, xyAlpha);
        }

        if (this.showHandle(STENCIL_ZY))
        {
            this.box(builder, stack, -offset, planeInner * this.lastSy, planeInner * this.lastSz, offset, planeOuter * this.lastSy, planeOuter * this.lastSz, zyCol, zyAlpha);
        }
    }

    private void drawScreenCube(BufferBuilder builder, MatrixStack stack, float axisOffset, boolean stencil, StencilMap map)
    {
        if (!this.showHandle(STENCIL_FREE))
        {
            return;
        }

        float[] color = stencil ? this.stencilColor(STENCIL_FREE) : this.pickColor(STENCIL_FREE, COLOR_FREE_IDLE, COLOR_FREE_IDLE);

        this.box(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, color, 1F);
    }

    /* ---- scale handles (bars with cube tips, unchanged shape from the original design) ---- */

    private void drawScale(BufferBuilder builder, MatrixStack stack, float scale, float thickness, boolean stencil, StencilMap map)
    {
        float axisOffset = 0.012F * scale * thickness;
        float axisSize = 0.30F * scale;

        float[] xCol = stencil ? this.stencilColor(STENCIL_X) : this.pickColor(STENCIL_X, COLOR_X_IDLE, COLOR_X_HOVER);
        float[] yCol = stencil ? this.stencilColor(STENCIL_Y) : this.pickColor(STENCIL_Y, COLOR_Y_IDLE, COLOR_Y_HOVER);
        float[] zCol = stencil ? this.stencilColor(STENCIL_Z) : this.pickColor(STENCIL_Z, COLOR_Z_IDLE, COLOR_Z_HOVER);
        float[] freeCol = stencil ? this.stencilColor(STENCIL_FREE) : this.pickColor(STENCIL_FREE, COLOR_FREE_IDLE, COLOR_FREE_IDLE);

        float half = axisOffset * 2F;

        if (this.showHandle(STENCIL_X))
        {
            this.box(builder, stack, 0, -axisOffset, -axisOffset, axisSize * this.lastSx, axisOffset, axisOffset, xCol, 1F);
            this.box(builder, stack, axisSize * this.lastSx - half, -half, -half, axisSize * this.lastSx + half, half, half, xCol, 1F);
        }

        if (this.showHandle(STENCIL_Y))
        {
            this.box(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize * this.lastSy, axisOffset, yCol, 1F);
            this.box(builder, stack, -half, axisSize * this.lastSy - half, -half, half, axisSize * this.lastSy + half, half, yCol, 1F);
        }

        if (this.showHandle(STENCIL_Z))
        {
            this.box(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize * this.lastSz, zCol, 1F);
            this.box(builder, stack, -half, -half, axisSize * this.lastSz - half, half, half, axisSize * this.lastSz + half, zCol, 1F);
        }

        if (this.showHandle(STENCIL_FREE))
        {
            this.box(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, freeCol, 1F);
        }
    }

    /* ---- rotate handles (rings + trackball) ---- */

    private void drawRotate(BufferBuilder builder, MatrixStack stack, float scale, float thickness, boolean stencil, StencilMap map)
    {
        float radius = 0.22F * scale;
        float ringThickness = 0.020F * scale * thickness;

        /* Trackball goes first in the stencil pass so the rings (drawn later, with depth
         * testing off) win the pick wherever they overlap the sphere. */
        this.drawTrackball(builder, stack, this.trackballRadius(radius, 0.62F), stencil, map);
        this.drawRings(builder, stack, radius, ringThickness, STENCIL_X, STENCIL_Y, STENCIL_Z, stencil);
    }

    /** Shared ring drawing for solo Rotate mode and Combined mode (they only differ in the
     *  stencil ids their rings pick as). Also draws the sweep fan on the active ring. */
    private void drawRings(BufferBuilder builder, MatrixStack stack, float radius, float ringThickness, int idX, int idY, int idZ, boolean stencil)
    {
        float[] xCol = stencil ? this.stencilColor(idX) : this.pickColor(idX, COLOR_X_IDLE, COLOR_X_HOVER);
        float[] yCol = stencil ? this.stencilColor(idY) : this.pickColor(idY, COLOR_Y_IDLE, COLOR_Y_HOVER);
        float[] zCol = stencil ? this.stencilColor(idZ) : this.pickColor(idZ, COLOR_Z_IDLE, COLOR_Z_HOVER);

        if (this.showHandle(idZ)) Draw.arc3D(builder, stack, Axis.Z, radius, ringThickness, zCol[0], zCol[1], zCol[2]);
        if (this.showHandle(idX)) Draw.arc3D(builder, stack, Axis.X, radius, ringThickness, xCol[0], xCol[1], xCol[2]);
        if (this.showHandle(idY)) Draw.arc3D(builder, stack, Axis.Y, radius, ringThickness, yCol[0], yCol[1], yCol[2]);

        /* Swept-angle fan on the ring being dragged, drawn slightly smaller so it reads as a
         * pie inside the ring rather than covering it. */
        if (!stencil && this.arcActive && !this.arcView && (this.index == idX || this.index == idY || this.index == idZ))
        {
            this.drawRotationSweepArc(builder, stack, this.arcAxis, radius * 0.85F, ringThickness * 1.5F, false);
        }
    }

    /** Free-rotate hit sphere. Idle it's invisible in the colored pass (the stencil-driven
     *  hover overlay already communicates hover); while actively dragged it's drawn as a
     *  translucent highlight sphere so the user sees what they grabbed. */
    private float trackballRadius(float gizmoRadius, float modeFactor)
    {
        int setting = BBSSettings.gizmoTrackballScale == null ? 1 : BBSSettings.gizmoTrackballScale.get();

        /* Setting 1 = half the legacy trackball size; each step scales linearly up to 5× that. */
        return gizmoRadius * modeFactor * 0.5F * setting;
    }

    private void drawTrackball(BufferBuilder builder, MatrixStack stack, float radius, boolean stencil, StencilMap map)
    {
        if (!BBSSettings.gizmoTrackball.get())
        {
            return;
        }

        if (!this.showHandle(STENCIL_TRACKBALL))
        {
            return;
        }

        if (stencil)
        {
            float[] color = this.stencilColor(STENCIL_TRACKBALL);

            Draw.sphere(builder, stack, radius, 10, 16, color[0], color[1], color[2], 1F);
        }
        else if (this.index == STENCIL_TRACKBALL)
        {
            Draw.sphere(builder, stack, radius, 10, 16, COLOR_ACTIVE[0], COLOR_ACTIVE[1], COLOR_ACTIVE[2], 0.4F);
        }
    }

    /** Camera-facing arcball ring, slightly larger than the axis rings. Dragging it rotates
     *  the object around the view axis, following the mouse around the ring. */
    private void drawViewRing(BufferBuilder builder, MatrixStack stack, float radius, float ringThickness, boolean stencil, StencilMap map)
    {
        if (!this.showHandle(STENCIL_VIEW))
        {
            return;
        }

        float[] color = stencil ? this.stencilColor(STENCIL_VIEW) : this.pickColor(STENCIL_VIEW, COLOR_VIEW_IDLE, COLOR_VIEW_HOVER);

        stack.push();
        stack.multiply(new Quaternionf().rotationTo(0F, 1F, 0F, this.lastCamDir.x, this.lastCamDir.y, this.lastCamDir.z));
        Draw.arc3D(builder, stack, Axis.Y, radius, ringThickness, color[0], color[1], color[2]);

        /* Same swept-angle fan the axis rings get, drawn slightly inside the view ring. */
        if (!stencil && this.arcActive && this.arcView && this.index == STENCIL_VIEW)
        {
            this.drawRotationSweepArc(builder, stack, Axis.Y, radius * 0.9F, ringThickness * 1.5F, true);
        }

        stack.pop();
    }

    private void drawRotationSweepArc(BufferBuilder builder, MatrixStack stack, Axis axis, float radius, float thickness, boolean viewRing)
    {
        if (Math.abs(this.arcSweep) <= 0.01F)
        {
            return;
        }

        if (this.arcFrozenOrientation)
        {
            MatrixStack arcStack = new MatrixStack();

            MatrixStackUtils.multiply(arcStack, this.arcFrozenMatrix);

            if (viewRing && this.arcFrozenViewRing)
            {
                arcStack.multiply(new Quaternionf().rotationTo(0F, 1F, 0F, this.arcFrozenCamDir.x, this.arcFrozenCamDir.y, this.arcFrozenCamDir.z));
            }

            Draw.arc3D(builder, arcStack, axis, radius, thickness, COLOR_ACTIVE[0], COLOR_ACTIVE[1], COLOR_ACTIVE[2], this.arcStartU, this.arcSweep);
        }
        else
        {
            Draw.arc3D(builder, stack, axis, radius, thickness, COLOR_ACTIVE[0], COLOR_ACTIVE[1], COLOR_ACTIVE[2], this.arcStartU, this.arcSweep);
        }
    }

    /** Faint "infinite" line through the gizmo origin along the axis being dragged: the
     *  movement direction for translate/scale, or the rotation axis for the rings. */
    private void drawActiveGuide(BufferBuilder builder, MatrixStack stack, float scale, float thickness)
    {
        if (this.index == STENCIL_XY)
        {
            this.drawGuideLine(builder, stack, scale, thickness, Axis.X, COLOR_X_HOVER);
            this.drawGuideLine(builder, stack, scale, thickness, Axis.Y, COLOR_Y_HOVER);

            return;
        }
        else if (this.index == STENCIL_XZ)
        {
            this.drawGuideLine(builder, stack, scale, thickness, Axis.X, COLOR_X_HOVER);
            this.drawGuideLine(builder, stack, scale, thickness, Axis.Z, COLOR_Z_HOVER);

            return;
        }
        else if (this.index == STENCIL_ZY)
        {
            this.drawGuideLine(builder, stack, scale, thickness, Axis.Z, COLOR_Z_HOVER);
            this.drawGuideLine(builder, stack, scale, thickness, Axis.Y, COLOR_Y_HOVER);

            return;
        }
    }

    private void drawCombined(BufferBuilder builder, MatrixStack stack, float scale, float thickness, boolean stencil, StencilMap map)
    {
        float moveScale = scale * COMBINED_MOVE_SCALE;
        float scaScale = scale * COMBINED_SCALE_HANDLE_SCALE;
        float rotScale = scale * COMBINED_ROTATE_SCALE;

        float axisSize = 0.30F * moveScale;
        float axisOffset = 0.012F * moveScale * thickness;
        float planeInner = 0.08F * moveScale;
        float planeOuter = 0.20F * moveScale;
        float offset = 0.001F * moveScale;

        this.drawMoveBars(builder, stack, axisSize, axisOffset, stencil, map);
        this.drawMovePlanes(builder, stack, planeInner, planeOuter, offset, stencil, map);
        this.drawScreenCube(builder, stack, axisOffset, stencil, map);

        float scaOffset = 0.012F * scaScale * thickness;
        float scaSize = 0.30F * scaScale;
        float half = scaOffset * 2F;

        float[] xCol = stencil ? this.stencilColor(STENCIL_SCALE_X) : this.pickColor(STENCIL_SCALE_X, COLOR_X_IDLE, COLOR_X_HOVER);
        float[] yCol = stencil ? this.stencilColor(STENCIL_SCALE_Y) : this.pickColor(STENCIL_SCALE_Y, COLOR_Y_IDLE, COLOR_Y_HOVER);
        float[] zCol = stencil ? this.stencilColor(STENCIL_SCALE_Z) : this.pickColor(STENCIL_SCALE_Z, COLOR_Z_IDLE, COLOR_Z_HOVER);

        if (this.showHandle(STENCIL_SCALE_X))
        {
            this.box(builder, stack, 0, -scaOffset, -scaOffset, scaSize * this.lastSx, scaOffset, scaOffset, xCol, 1F);
            this.box(builder, stack, scaSize * this.lastSx - half, -half, -half, scaSize * this.lastSx + half, half, half, xCol, 1F);
        }

        if (this.showHandle(STENCIL_SCALE_Y))
        {
            this.box(builder, stack, -scaOffset, 0, -scaOffset, scaOffset, scaSize * this.lastSy, scaOffset, yCol, 1F);
            this.box(builder, stack, -half, scaSize * this.lastSy - half, -half, half, scaSize * this.lastSy + half, half, yCol, 1F);
        }

        if (this.showHandle(STENCIL_SCALE_Z))
        {
            this.box(builder, stack, -scaOffset, -scaOffset, 0, scaOffset, scaOffset, scaSize * this.lastSz, zCol, 1F);
            this.box(builder, stack, -half, -half, scaSize * this.lastSz - half, half, half, scaSize * this.lastSz + half, zCol, 1F);
        }

        float radius = 0.22F * rotScale;
        float ringThickness = 0.020F * rotScale * thickness;

        this.drawTrackball(builder, stack, this.trackballRadius(radius, 0.62F), stencil, map);
        this.drawRings(builder, stack, radius, ringThickness, STENCIL_ROTATE_X, STENCIL_ROTATE_Y, STENCIL_ROTATE_Z, stencil);
        this.drawViewRing(builder, stack, radius * 1.1F, ringThickness, stencil, map);
    }

    private void drawDragProgress(BufferBuilder builder, MatrixStack stack, float scale, float thickness)
    {
        if (!this.dragProgressActive)
        {
            return;
        }

        float r = 0.010F * scale * thickness;

        Draw.fillBox(builder, stack,
            Math.min(this.dragProgressStart.x, this.dragProgressEnd.x) - r,
            Math.min(this.dragProgressStart.y, this.dragProgressEnd.y) - r,
            Math.min(this.dragProgressStart.z, this.dragProgressEnd.z) - r,
            Math.max(this.dragProgressStart.x, this.dragProgressEnd.x) + r,
            Math.max(this.dragProgressStart.y, this.dragProgressEnd.y) + r,
            Math.max(this.dragProgressStart.z, this.dragProgressEnd.z) + r,
            1F, 0.85F, 0.15F, 1F);
    }

    private void drawGuideLine(BufferBuilder builder, MatrixStack stack, float scale, float thickness, Axis axis, float[] color)
    {
        float len = 3F * scale;
        float r = 0.004F * scale * thickness;

        float sx = 0F, sy = 0F, sz = 0F;
        float ex = 0F, ey = 0F, ez = 0F;

        if (axis == Axis.X) { sx = -len * this.lastSx; ex = len * this.lastSx; }
        else if (axis == Axis.Y) { sy = -len * this.lastSy; ey = len * this.lastSy; }
        else if (axis == Axis.Z) { sz = -len * this.lastSz; ez = len * this.lastSz; }

        this.box(builder, stack,
            Math.min(sx, ex) - r, Math.min(sy, ey) - r, Math.min(sz, ez) - r,
            Math.max(sx, ex) + r, Math.max(sy, ey) + r, Math.max(sz, ez) + r,
            color, 0.3F);
    }

    public static enum Mode
    {
        TRANSLATE, SCALE, ROTATE, COMBINED;
    }
}
