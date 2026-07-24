package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.IValueNotifier;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.gizmo.TransformOrientation;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.MinecraftClient;

import org.joml.Intersectiond;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class UIPropTransform extends UITransform
{
    public interface IGizmoRayProvider
    {
        public boolean getMouseRay(UIContext context, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection);
        public boolean getGizmoMatrix(Matrix4f matrix);
    }

    public static final List<BiConsumer<UIPropTransform, ContextMenuManager>> contextMenuExtensions = new ArrayList<>();

    private static final double[] CURSOR_X = new double[1];
    private static final double[] CURSOR_Y = new double[1];

    private Transform transform;
    private Runnable preCallback;
    private Runnable postCallback;

    private boolean editing;
    private int mode;
    private Axis axis = Axis.X;
    private Axis secondaryAxis;
    private int lastX;
    private int lastY;
    private Transform cache = new Transform();
    private Timer checker = new Timer(30);

    private boolean model;
    private boolean invertGizmoTranslateZ;
    private boolean invertGizmoRotationX;
    private boolean invertGizmoRotationZ;
    private boolean invertGizmoRotationY;
    private boolean invertTrackballRotationX;
    private boolean invertTrackballRotationY;
    private boolean invertTrackballRotationZ;
    private boolean invertTrackballDragY;
    private boolean invertGizmoTrackballTuning;
    private boolean invertGizmoViewRing;
    private boolean invertGizmoViewRingTuning;
    private boolean invertGizmoTrackball;
    /**
     * When true, flip the rotation process-bar sweep relative to the value delta.
     * Used by model-editor General where arc3D winds opposite every ring's plane sweep.
     */
    private boolean invertRotationArcSweep;
    /* Model-editor General: Y-ring process bar winds opposite the value delta. */
    private boolean invertRotationArcY;
    /* Film Anchor: Z-ring process bar winds opposite the value delta. */
    private boolean invertRotationArcZ;
    /* Film Transform: white view-ring process bar only (value stays mouse-correct). */
    private boolean invertRotationArcViewRing;
    private boolean filmMatchPoseTrackball;
    /* Film Anchor: keep process-bar orientation fixed even in Global (same as Local). */
    private boolean forceFrozenRotationArc;
    /* Film pose keyframe trackball: arcball on a gizmo-local unit sphere via view-space rays
     * (same space as the axis rings). Screen-delta trackball does not work there. */
    private boolean filmArcballTrackball;
    private boolean invertFilmArcballDragY;
    private boolean invertFilmPoseGizmoAxes;
    private TransformOrientation orientation = TransformOrientation.PARENT;
    private boolean freeRotation;
    private boolean freeTranslation;
    private boolean uniformScale;
    /**
     * Project translate/scale ray drags onto the gizmo axes and compensate parent scale
     * via {@code / axisWorldScale}. Used for cubic model-pixel pose (scale 16) and for
     * BOBJ block-unit pose (scale 1) so nested parent scales stay correct.
     */
    private boolean axisProjectedTranslation;
    private boolean rayDragInitialized;
    private boolean rayDragReanchor;
    /** Ignore sub-pixel / 1–2px click jitter so a pure click never writes transform values. */
    private static final int DRAG_APPLY_THRESHOLD_SQ = 9;

    /* Trackball (free-rotate sphere) drag state. Kept separate from the legacy
     * {@link #freeRotation} 2D-delta mode: the trackball accumulates rotation as a
     * quaternion so dragging never runs into gimbal lock, regardless of orientation. */
    private boolean trackball;
    private final Quaternionf trackballStart = new Quaternionf();
    private final Quaternionf trackballAccum = new Quaternionf();

    /* View (arcball) ring drag state: rotates around the camera-to-gizmo axis, reusing the
     * trackball quaternion pipeline but driven by ray/plane angle tracking so the rotation
     * follows the mouse around the ring exactly. */
    private boolean viewRing;

    private IGizmoRayProvider gizmoRayProvider;
    private final Vector3d rayOrigin = new Vector3d();
    private final Vector3f rayDirection = new Vector3f();
    private final Matrix4f rayGizmoMatrix = new Matrix4f();
    private final Vector3f rayPrimaryAxis = new Vector3f();
    private final Vector3f raySecondaryAxis = new Vector3f();
    private final Vector3f rayPlaneNormal = new Vector3f();
    private final Vector3d rayLastPoint = new Vector3d();
    private final Vector3d rayCurrentPoint = new Vector3d();
    private final Vector3d rayDragStartPoint = new Vector3d();
    private final Vector3f rayDragStartTranslate = new Vector3f();
    private final Vector3f rayDragProgressStart = new Vector3f();
    private final Vector3f rayDragProgressEnd = new Vector3f();
    private final Vector3f screenDragProgressEnd = new Vector3f();
    private double rayDragStartAxisValue;
    private float rayPrimaryAxisWorldScale = 1F;
    private float raySecondaryAxisWorldScale = 1F;
    private float rayAxisWorldScaleX = 1F;
    private float rayAxisWorldScaleY = 1F;
    private float rayAxisWorldScaleZ = 1F;
    private float scaleProgressLength;
    private int dragAnchorX;
    private int dragAnchorY;
    private int rayDragMouseX;
    private int rayDragMouseY;
    private final Vector3f rayGizmoOrigin = new Vector3f();
    private final Vector3f rayLastSpherePoint = new Vector3f();
    private boolean raySphereDragInitialized;
    private double rayLastAxisValue;
    /** Accumulated ring sweep preserved across screen-edge cursor warps (degrees). */
    private float rayDragPlaneAngleOffset;
    /** Same for the camera-facing view ring (radians). */
    private float rayDragViewRingAngleOffsetRad;
    /**
     * Continuous (unwrapped) sweep from {@link #rayDragStartPoint} so process-bar / value
     * keep growing past ±180° instead of atan2-wrapping and reversing.
     */
    private float rayPlaneSweepUnwrappedDeg;
    private float rayViewSweepUnwrappedRad;
    private boolean rayPlaneSweepUnwrappedInit;
    private boolean rayViewSweepUnwrappedInit;
    private final Vector3d planeOrigin = new Vector3d();
    private final Vector3d planeNormal = new Vector3d();
    private final Vector3d rayDirectionD = new Vector3d();

    private UITransformHandler handler;
    private float translationScale = 1F;

    public UIPropTransform()
    {
        this.handler = new UITransformHandler(this);

        this.iconT.callback = (b) -> this.toggleLocal();
        this.iconT.both(() -> this.isLocal() ? Icons.MINIMIZE : Icons.FULLSCREEN);
        this.iconT.hoverColor = Colors.LIGHTEST_GRAY;
        this.iconT.setEnabled(true);
        this.updateLocalIcon();

        this.noCulling();
    }

    @Override
    protected void addGeneralTabActions(ContextMenuManager menu, ListType transforms)
    {
        menu.action(this.isLocal() ? Icons.MINIMIZE : Icons.FULLSCREEN, this.isLocal() ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL, this::toggleLocal);

        for (BiConsumer<UIPropTransform, ContextMenuManager> consumer : contextMenuExtensions)
        {
            consumer.accept(this, menu);
        }
    }

    public UIPropTransform callbacks(Supplier<IValueNotifier> notifier)
    {
        return this.callbacks(
            () -> notifier.get().preNotify(),
            () -> notifier.get().postNotify()
        );
    }

    public UIPropTransform callbacks(Runnable pre, Runnable post)
    {
        if (pre != null)
        {
            Runnable existing = this.preCallback;
            this.preCallback = existing == null ? pre : () ->
            {
                existing.run();
                pre.run();
            };
        }

        if (post != null)
        {
            Runnable existing = this.postCallback;
            this.postCallback = existing == null ? post : () ->
            {
                existing.run();
                post.run();
            };
        }

        return this;
    }

    public void preCallback()
    {
        if (this.preCallback != null) this.preCallback.run();
    }

    public void postCallback()
    {
        if (this.postCallback != null) this.postCallback.run();
    }

    public void setModel()
    {
        this.setModel(true);
    }

    public void setModel(boolean model)
    {
        this.model = model;
    }

    /**
     * Pose model editor gizmo: invert Z translation and X/Z rotation rings without enabling
     * the full legacy {@link #model} drag path. Trackball euler X/Y/Z are sign-flipped separately.
     */
    public UIPropTransform poseModelGizmoTuning()
    {
        this.configurePoseRingTuning(false);
        this.invertModelPoseTrackballXYZ();

        return this;
    }

    /**
     * BOBJ armatures: the renderer PRE-multiplies the whole model by Ry(180°) instead of
     * post-multiplying each bone like cubic models do (see ModelInstance.captureMatrices),
     * so the displayed gizmo axes equal the actual euler rotation axes — no ring flips and
     * no translate flip. The trackball keeps only the context's vertical drag flip (the X/Z
     * euler flips compensate the cubic bone-local mirror, which BOBJ does not have).
     */
    public UIPropTransform bobjPoseGizmoTuning()
    {
        this.configurePoseRingTuning(true);
        this.invertModelPoseTrackballDragY();
        /* BOBJ pose translate is in blocks; keep axis-projected sensitivity so parent
         * bone scale still divides out correctly (same path as cubic's pixel mode). */
        this.axisProjectedTranslation = true;

        return this;
    }

    /**
     * Ring / translate sign correction for pose bone gizmos. Cubic (.bbs.json) models bake a
     * bone-local Ry(180°) into their captured matrices, flipping the displayed X/Z axes
     * relative to the euler rotation axes, so their X/Z rings and Z translate need a sign
     * flip. BOBJ pre-multiplies that Ry(180°) globally instead, so its displayed axes match
     * the euler axes exactly — no inversion at all (same as bbs-fs' sampled rotate axes).
     */
    public UIPropTransform configurePoseRingTuning(boolean bobj)
    {
        if (bobj)
        {
            this.invertGizmoTranslateZ = false;
            this.invertGizmoRotationX = false;
            this.invertGizmoRotationY = false;
            this.invertGizmoRotationZ = false;
        }
        else
        {
            this.invertGizmoTranslateZ = true;
            this.invertGizmoRotationX = true;
            this.invertGizmoRotationY = false;
            this.invertGizmoRotationZ = true;
        }

        return this;
    }

    /**
     * Pose limb overlay gizmo ring signs. The limb context flips the Y ring on top of the
     * model's own convention: cubic (bone-local Ry(180°) → X/Z flipped) ends up inverting
     * all three rings; BOBJ (no bone-local flip) keeps only the context's Y inversion.
     */
    public UIPropTransform configurePoseLimbRingTuning(boolean bobj)
    {
        if (bobj)
        {
            this.invertGizmoTranslateZ = false;
            this.invertGizmoRotationX = false;
            this.invertGizmoRotationY = true;
            this.invertGizmoRotationZ = false;
        }
        else
        {
            this.invertGizmoTranslateZ = true;
            this.invertGizmoRotationX = true;
            this.invertGizmoRotationY = true;
            this.invertGizmoRotationZ = true;
        }

        return this;
    }

    /**
     * Trackball sphere only: negate decomposed X, Y, and Z euler angles after drag so the
     * numeric fields match mouse drag direction in pose / model-editor contexts.
     */
    public UIPropTransform invertModelPoseTrackballXYZ()
    {
        this.invertTrackballRotationX = true;
        this.invertTrackballRotationY = true;
        this.invertTrackballRotationZ = true;

        return this;
    }

    /**
     * Model-editor pose trackball: flip decomposed X and Z euler only so drag matches the
     * General transform feel while keeping Y sign unchanged.
     */
    public UIPropTransform invertModelPoseTrackballXZ()
    {
        this.invertTrackballRotationX = true;
        this.invertTrackballRotationY = false;
        this.invertTrackballRotationZ = true;

        return this;
    }

    /** Model-editor pose trackball: flip vertical mouse contribution only. */
    public UIPropTransform invertModelPoseTrackballDragY()
    {
        this.invertTrackballDragY = true;

        return this;
    }

    /** Film pose arcball: flip decomposed Y euler only so vertical drag matches the viewport. */
    public UIPropTransform invertModelPoseTrackballY()
    {
        this.invertTrackballRotationY = true;

        return this;
    }

    /** Film pose arcball: flip decomposed Z euler so horizontal drag matches the viewport. */
    public UIPropTransform invertModelPoseTrackballZ()
    {
        this.invertTrackballRotationZ = true;

        return this;
    }

    /** Model-editor General transform trackball: flip drag rotation direction to match mouse. */
    public UIPropTransform invertModelEditorTrackball()
    {
        this.invertGizmoTrackball = true;

        return this;
    }

    /** Film pose arcball sphere: mirror gizmo-local Y on the unit sphere so vertical drag is inverted. */
    public UIPropTransform invertFilmArcballDragY()
    {
        this.invertFilmArcballDragY = true;

        return this;
    }

    public void setInvertFilmArcballDragY(boolean invertFilmArcballDragY)
    {
        this.invertFilmArcballDragY = invertFilmArcballDragY;
    }

    /** Film replay transform trackball / view-ring path: no pose euler sign flips. */
    public UIPropTransform clearTrackballEulerInverts()
    {
        this.invertTrackballRotationX = false;
        this.invertTrackballRotationY = false;
        this.invertTrackballRotationZ = false;

        return this;
    }

    /**
     * Film anchor gizmo: attachment-bone space does not need X/Z ring sign flips for mouse
     * sense (those flips made the rings oppose the cursor).
     */
    public UIPropTransform anchorGizmoTuning()
    {
        this.invertGizmoRotationX = false;
        this.invertGizmoRotationZ = false;

        return this;
    }

    /**
     * Film replay pose-to-limb gizmo: invert Z translation and X/Z rotation rings. Trackball
     * euler X/Z only (sphere numbers + drag) — same path as main pose keyframe (rotate, not rotate2).
     */
    public UIPropTransform poseLimbGizmoTuning()
    {
        this.configurePoseLimbRingTuning(false);
        this.invertModelPoseTrackballXZ();
        this.invertTrackballDragY = true;

        return this;
    }

    /**
     * BOBJ variant of {@link #poseLimbGizmoTuning()}: keeps the limb context's vertical drag
     * flip but drops the cubic-only X/Z euler flips (no bone-local mirror in BOBJ frames).
     */
    public UIPropTransform bobjPoseLimbGizmoTuning()
    {
        this.configurePoseLimbRingTuning(true);
        this.invertTrackballDragY = true;
        this.axisProjectedTranslation = true;

        return this;
    }

    public void setInvertGizmoTrackball(boolean invertGizmoTrackball)
    {
        this.invertGizmoTrackball = invertGizmoTrackball;
    }

    public void setInvertGizmoViewRing(boolean invertGizmoViewRing)
    {
        this.invertGizmoViewRing = invertGizmoViewRing;
    }

    public void setInvertRotationArcSweep(boolean invertRotationArcSweep)
    {
        this.invertRotationArcSweep = invertRotationArcSweep;
    }

    public void setInvertRotationArcY(boolean invertRotationArcY)
    {
        this.invertRotationArcY = invertRotationArcY;
    }

    public void setInvertRotationArcZ(boolean invertRotationArcZ)
    {
        this.invertRotationArcZ = invertRotationArcZ;
    }

    public void setInvertRotationArcViewRing(boolean invertRotationArcViewRing)
    {
        this.invertRotationArcViewRing = invertRotationArcViewRing;
    }

    public void setForceFrozenRotationArc(boolean forceFrozenRotationArc)
    {
        this.forceFrozenRotationArc = forceFrozenRotationArc;
    }

    public void setInvertGizmoRotationX(boolean invertGizmoRotationX)
    {
        this.invertGizmoRotationX = invertGizmoRotationX;
    }

    public void setInvertGizmoRotationZ(boolean invertGizmoRotationZ)
    {
        this.invertGizmoRotationZ = invertGizmoRotationZ;
    }

    public void setInvertGizmoTranslateZ(boolean invertGizmoTranslateZ)
    {
        this.invertGizmoTranslateZ = invertGizmoTranslateZ;
    }

    public void setInvertTrackballDragY(boolean invertTrackballDragY)
    {
        this.invertTrackballDragY = invertTrackballDragY;
    }

    public void setFilmMatchPoseTrackball(boolean filmMatchPoseTrackball)
    {
        this.filmMatchPoseTrackball = filmMatchPoseTrackball;
    }

    public void setFilmArcballTrackball(boolean filmArcballTrackball)
    {
        this.filmArcballTrackball = filmArcballTrackball;
    }

    /** Film replay pose gizmo: flip translate on X/Y and the Y rotation ring only (not Z / X/Z rings). */
    public void setInvertFilmPoseGizmoAxes(boolean invertFilmPoseGizmoAxes)
    {
        this.invertFilmPoseGizmoAxes = invertFilmPoseGizmoAxes;
    }

    private boolean shouldInvertTranslateZ()
    {
        return this.model || this.invertGizmoTranslateZ;
    }

    private boolean shouldInvertTranslateZ(Axis axis)
    {
        return axis == Axis.Z && this.shouldInvertTranslateZ();
    }

    private boolean shouldInvertTranslate(Axis axis)
    {
        if (this.invertFilmPoseGizmoAxes && (axis == Axis.X || axis == Axis.Y))
        {
            return true;
        }

        return this.shouldInvertTranslateZ(axis);
    }

    private boolean shouldInvertRotationRing(Axis axis)
    {
        if (this.invertFilmPoseGizmoAxes && axis == Axis.Y)
        {
            return true;
        }

        if (this.model && (axis == Axis.X || axis == Axis.Z))
        {
            return true;
        }

        if (axis == Axis.X && this.invertGizmoRotationX)
        {
            return true;
        }

        if (axis == Axis.Y && this.invertGizmoRotationY)
        {
            return true;
        }

        if (axis == Axis.Z && this.invertGizmoRotationZ)
        {
            return true;
        }

        return false;
    }

    private float applyTranslateDelta(Axis axis, float delta)
    {
        return this.shouldInvertTranslate(axis) ? -delta : delta;
    }

    private float applyScaleDelta(Axis axis, float delta)
    {
        return delta * Gizmo.INSTANCE.getFlipSign(axis);
    }

    /** Progress bar direction along the visible scale handle; Z needs an extra sign flip. */
    private float getScaleProgressVisualSign(Axis axis)
    {
        float sign = Gizmo.INSTANCE.getFlipSign(axis);

        if (axis == Axis.Z)
        {
            sign = -sign;
        }

        return sign;
    }

    public boolean isLocal()
    {
        return this.orientation.isLocal();
    }

    public TransformOrientation getOrientation()
    {
        return this.orientation;
    }

    public void setOrientation(TransformOrientation orientation)
    {
        if (orientation == null)
        {
            orientation = TransformOrientation.PARENT;
        }

        boolean wasLocal = this.isLocal();

        this.orientation = orientation;

        if (wasLocal && !this.isLocal() && this.transform != null)
        {
            this.fillT(this.transform.translate.x, this.transform.translate.y, this.transform.translate.z);
        }

        this.updateLocalIcon();
    }

    public void setLocalMode(boolean local)
    {
        this.setOrientation(local ? TransformOrientation.LOCAL : TransformOrientation.PARENT);
    }

    private void toggleLocal()
    {
        this.setLocalMode(!this.isLocal());
    }

    private void updateLocalIcon()
    {
        this.tx.forcedLabel(this.isLocal() ? UIKeys.GENERAL_X : null);
        this.ty.forcedLabel(this.isLocal() ? UIKeys.GENERAL_Y : null);
        this.tz.forcedLabel(this.isLocal() ? UIKeys.GENERAL_Z : null);
        this.tx.relative(this.isLocal());
        this.ty.relative(this.isLocal());
        this.tz.relative(this.isLocal());
        this.iconT.iconColor(Colors.WHITE);
        this.iconT.tooltip(this.isLocal() ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL);
    }

    private Vector3f calculateLocalVector(double factor, Axis axis)
    {
        Vector3f vector3f = new Vector3f(
            (float) (axis == Axis.X ? factor : 0D),
            (float) (axis == Axis.Y ? factor : 0D),
            (float) (axis == Axis.Z ? (this.shouldInvertTranslateZ() ? -factor : factor) : 0D)
        );
        /* I have no fucking idea why I have to rotate it 180 degrees by X axis... but it works! */
        Matrix3f matrix = new Matrix3f()
            .rotateX(this.model ? MathUtils.PI : 0F)
            .mul(this.transform.createRotationMatrix());

        matrix.transform(vector3f);

        return vector3f;
    }

    public UIPropTransform enableHotkeys()
    {
        IKey category = UIKeys.TRANSFORMS_KEYS_CATEGORY;
        Supplier<Boolean> active = () -> this.editing;

        this.keys().register(Keys.TRANSFORMATIONS_TRANSLATE, () -> this.enableMode(0)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_SCALE, () -> this.enableMode(1)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_ROTATE, () -> this.enableMode(2)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_COMBINED, () -> Gizmo.INSTANCE.setMode(Gizmo.Mode.COMBINED)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_X, () -> this.axis = Axis.X).active(active).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_Y, () -> this.axis = Axis.Y).active(active).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_Z, () -> this.axis = Axis.Z).active(active).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_TOGGLE_LOCAL, () ->
        {
            this.toggleLocal();
            UIUtils.playClick();
        }).category(category);

        return this;
    }

    public Transform getTransform()
    {
        return this.transform;
    }

    public void refillTransform()
    {
        this.setTransform(this.getTransform());
    }

    public void setTransform(Transform transform)
    {
        if (transform == null)
        {
            this.transform = null;

            return;
        }

        this.transform = transform;

        float minScale = Math.min(transform.scale.x, Math.min(transform.scale.y, transform.scale.z));
        float maxScale = Math.max(transform.scale.x, Math.max(transform.scale.y, transform.scale.z));

        if (BBSSettings.uniformScale.get())
        {
            if (
                (minScale == maxScale && !this.isUniformScale()) ||
                (minScale != maxScale && this.isUniformScale())
            ) {
                this.toggleUniformScale();
            }
        }

        /* Never unfocus here: keyframe/pose edits call setTransform on every
         * keystroke via sync helpers, and stealing focus made numeric fields
         * uneditable. Focused trackpads already ignore setValue(). */
        this.fillT(transform.translate.x, transform.translate.y, transform.translate.z);
        this.fillS(transform.scale.x, transform.scale.y, transform.scale.z);
        this.fillR(MathUtils.toDeg(transform.rotate.x), MathUtils.toDeg(transform.rotate.y), MathUtils.toDeg(transform.rotate.z));
        this.fillR2(MathUtils.toDeg(transform.rotate2.x), MathUtils.toDeg(transform.rotate2.y), MathUtils.toDeg(transform.rotate2.z));
        this.fillP(transform.pivot.x, transform.pivot.y, transform.pivot.z);
    }

    public void setGizmoRayProvider(IGizmoRayProvider provider)
    {
        this.gizmoRayProvider = provider;
    }

    public IGizmoRayProvider getGizmoRayProvider()
    {
        return this.gizmoRayProvider;
    }

    public UIPropTransform translationScale(float translationScale)
    {
        this.translationScale = translationScale;
        return this;
    }

    /**
     * When true, ray translate sensitivity divides by the gizmo axis world length
     * ({@code scale / axisWorldScale}). Required for BOBJ block-space translate when
     * {@link #translationScale} is 1 (otherwise only scale ≥ 15.5 enables that path).
     */
    public UIPropTransform setAxisProjectedTranslation(boolean axisProjectedTranslation)
    {
        this.axisProjectedTranslation = axisProjectedTranslation;

        return this;
    }

    /* Default {@link BBSSettings#gizmoTranslateSpeed}; at this value ray translate drag matches
     * the tuned 1:1 baseline (speed / neutral == 1). */
    private static final float GIZMO_TRANSLATE_SPEED_NEUTRAL = 5F;

    private float getBaseTranslationScale()
    {
        /* Per-context unit conversion only (pose model pixels = 16 per block, etc.). */
        return this.translationScale;
    }

    private float getGizmoTranslateSpeedMultiplier()
    {
        float speed = BBSSettings.gizmoTranslateSpeed == null ? GIZMO_TRANSLATE_SPEED_NEUTRAL : BBSSettings.gizmoTranslateSpeed.get();

        return speed / GIZMO_TRANSLATE_SPEED_NEUTRAL;
    }

    private float getEffectiveTranslationScale()
    {
        float scale = this.getBaseTranslationScale() * this.getGizmoTranslateSpeedMultiplier();

        if (Window.isAltPressed())
        {
            scale /= 5F;
        }

        return scale;
    }

    /**
     * Pose / geometry drags that project onto gizmo axes (cubic model pixels at scale 16,
     * or BOBJ block units with {@link #axisProjectedTranslation}).
     */
    private boolean usesModelPixelTranslation()
    {
        return this.axisProjectedTranslation || this.translationScale >= 15.5F;
    }

    /**
     * How much the gizmo origin moves per local translate unit along an axis. Parent scale is
     * baked into the gizmo matrix but stripped when axis directions are normalized for rays.
     */
    private float measureAxisWorldScale(Axis axis)
    {
        Vector3f local = new Vector3f();

        if (axis == Axis.X)
        {
            local.x = 1F;
        }
        else if (axis == Axis.Y)
        {
            local.y = 1F;
        }
        else
        {
            local.z = 1F;
        }

        Vector3f world = new Vector3f();

        this.rayGizmoMatrix.transformDirection(local, world);

        float length = world.length();

        return length > 1.0E-6F ? length : 1F;
    }

    private void cacheRayAxisWorldScales()
    {
        this.rayPrimaryAxisWorldScale = this.measureAxisWorldScale(this.axis);

        if (this.secondaryAxis != null)
        {
            this.raySecondaryAxisWorldScale = this.measureAxisWorldScale(this.secondaryAxis);
        }

        if (this.mode == 0 && this.freeTranslation)
        {
            this.rayAxisWorldScaleX = this.measureAxisWorldScale(Axis.X);
            this.rayAxisWorldScaleY = this.measureAxisWorldScale(Axis.Y);
            this.rayAxisWorldScaleZ = this.measureAxisWorldScale(Axis.Z);
        }
    }

    private float getRayAxisSensitivity(float axisWorldScale)
    {
        if (this.usesModelPixelTranslation())
        {
            return this.getEffectiveTranslationScale() / axisWorldScale;
        }

        return this.getEffectiveTranslationScale();
    }

    private void applyAbsoluteRayTranslate(Axis axis, float axisDelta)
    {
        Vector3f result = new Vector3f(this.rayDragStartTranslate);
        float delta = this.applyTranslateDelta(axis, axisDelta);

        if (this.isLocal() && !this.usesModelPixelTranslation())
        {
            Vector3f local = this.calculateLocalVector(delta, axis);

            this.setT(null,
                this.rayDragStartTranslate.x + local.x,
                this.rayDragStartTranslate.y + local.y,
                this.rayDragStartTranslate.z + local.z
            );
        }
        else
        {
            this.addAxisDelta(result, axis, delta);
            this.setT(null, result.x, result.y, result.z);
        }
    }

    private void applyAbsoluteRayTranslateFree(float dx, float dy, float dz)
    {
        Vector3f result = new Vector3f(this.rayDragStartTranslate);

        this.addAxisDelta(result, Axis.X, this.applyTranslateDelta(Axis.X, dx));
        this.addAxisDelta(result, Axis.Y, this.applyTranslateDelta(Axis.Y, dy));
        this.addAxisDelta(result, Axis.Z, this.applyTranslateDelta(Axis.Z, dz));
        this.setT(null, result.x, result.y, result.z);
    }

    private void applyAbsoluteRayTranslateFromPlaneOffset(Vector3d offset, float axisScale)
    {
        Vector3f worldX = new Vector3f();
        Vector3f worldY = new Vector3f();
        Vector3f worldZ = new Vector3f();

        this.extractAxisWorld(Axis.X, worldX);
        this.extractAxisWorld(Axis.Y, worldY);
        this.extractAxisWorld(Axis.Z, worldZ);

        float dx = (float) offset.dot(worldX.x, worldX.y, worldX.z) * axisScale;
        float dy = (float) offset.dot(worldY.x, worldY.y, worldY.z) * axisScale;
        float dz = (float) offset.dot(worldZ.x, worldZ.y, worldZ.z) * axisScale;

        if (this.isLocal() && !this.usesModelPixelTranslation())
        {
            Vector3f result = new Vector3f(this.rayDragStartTranslate);

            Vector3f local = new Vector3f();
            local.add(this.calculateLocalVector(this.applyTranslateDelta(Axis.X, dx), Axis.X));
            local.add(this.calculateLocalVector(this.applyTranslateDelta(Axis.Y, dy), Axis.Y));
            local.add(this.calculateLocalVector(this.applyTranslateDelta(Axis.Z, dz), Axis.Z));

            this.setT(null,
                this.rayDragStartTranslate.x + local.x,
                this.rayDragStartTranslate.y + local.y,
                this.rayDragStartTranslate.z + local.z
            );
        }
        else
        {
            this.applyAbsoluteRayTranslateFree(dx, dy, dz);
        }
    }

    private float computePlaneSweepAngleDegrees(Vector3d fromPoint, Vector3d toPoint)
    {
        Vector3f from = new Vector3f(
            (float) (fromPoint.x - this.rayGizmoOrigin.x),
            (float) (fromPoint.y - this.rayGizmoOrigin.y),
            (float) (fromPoint.z - this.rayGizmoOrigin.z)
        );
        Vector3f to = new Vector3f(
            (float) (toPoint.x - this.rayGizmoOrigin.x),
            (float) (toPoint.y - this.rayGizmoOrigin.y),
            (float) (toPoint.z - this.rayGizmoOrigin.z)
        );

        if (!this.normalizeSafe(from) || !this.normalizeSafe(to))
        {
            return 0F;
        }

        Vector3f cross = new Vector3f(from).cross(to);
        float sin = this.rayPlaneNormal.dot(cross);
        float cos = from.dot(to);

        return (float) Math.toDegrees(Math.atan2(sin, cos));
    }

    private float computePlaneSweepAngleRadians(Vector3d fromPoint, Vector3d toPoint)
    {
        return (float) Math.toRadians(this.computePlaneSweepAngleDegrees(fromPoint, toPoint));
    }

    private void resetPlaneSweepUnwrap()
    {
        this.rayPlaneSweepUnwrappedDeg = 0F;
        this.rayPlaneSweepUnwrappedInit = false;
    }

    private void resetViewSweepUnwrap()
    {
        this.rayViewSweepUnwrappedRad = 0F;
        this.rayViewSweepUnwrappedInit = false;
    }

    /**
     * Absolute atan2 from the grab point stays in (-180, 180]; unwrap so multi-turn drags
     * and the process bar keep a continuous signed sweep.
     */
    private float continuousPlaneSweepDegrees(Vector3d toPoint)
    {
        float wrapped = this.computePlaneSweepAngleDegrees(this.rayDragStartPoint, toPoint);

        if (!this.rayPlaneSweepUnwrappedInit)
        {
            this.rayPlaneSweepUnwrappedDeg = wrapped;
            this.rayPlaneSweepUnwrappedInit = true;
        }
        else
        {
            float delta = wrapped - this.rayPlaneSweepUnwrappedDeg;

            while (delta > 180F)
            {
                delta -= 360F;
            }

            while (delta < -180F)
            {
                delta += 360F;
            }

            this.rayPlaneSweepUnwrappedDeg += delta;
        }

        return this.rayDragPlaneAngleOffset + this.rayPlaneSweepUnwrappedDeg;
    }

    private float continuousViewSweepRadians(Vector3d toPoint)
    {
        float wrapped = this.computePlaneSweepAngleRadians(this.rayDragStartPoint, toPoint);

        if (!this.rayViewSweepUnwrappedInit)
        {
            this.rayViewSweepUnwrappedRad = wrapped;
            this.rayViewSweepUnwrappedInit = true;
        }
        else
        {
            float delta = wrapped - this.rayViewSweepUnwrappedRad;
            float pi = (float) Math.PI;

            while (delta > pi)
            {
                delta -= pi * 2F;
            }

            while (delta < -pi)
            {
                delta += pi * 2F;
            }

            this.rayViewSweepUnwrappedRad += delta;
        }

        return this.rayDragViewRingAngleOffsetRad + this.rayViewSweepUnwrappedRad;
    }

    public void enableMode(int mode)
    {
        this.enableMode(mode, null);
    }

    private void beginDragAnchor(UIContext context)
    {
        this.syncDragMouseFromContext(context);
        this.dragAnchorX = this.resolveDragMouseX(context);
        this.dragAnchorY = this.resolveDragMouseY(context);
        this.lastX = this.dragAnchorX;
        this.lastY = this.dragAnchorY;
        this.screenDragProgressEnd.zero();
        this.scaleProgressLength = 0F;
        this.rayDragPlaneAngleOffset = 0F;
        this.rayDragViewRingAngleOffsetRad = 0F;
        this.resetPlaneSweepUnwrap();
        this.resetViewSweepUnwrap();
    }

    private void syncDragMouseFromContext(UIContext context)
    {
        if (this.gizmoRayProvider == null || context == null)
        {
            return;
        }

        GLFW.glfwGetCursorPos(Window.getWindow(), CURSOR_X, CURSOR_Y);

        MinecraftClient mc = MinecraftClient.getInstance();
        double fx = Math.ceil(mc.getWindow().getWidth() / (double) context.menu.width);
        double fy = Math.ceil(mc.getWindow().getHeight() / (double) context.menu.height);

        this.updateRayDragMouse(fx, fy);
    }

    /** Re-read cursor position into ray/UI mouse fields without touching drag anchors. */
    private void refreshDragMousePosition(UIContext context)
    {
        if (context == null)
        {
            return;
        }

        GLFW.glfwGetCursorPos(Window.getWindow(), CURSOR_X, CURSOR_Y);

        MinecraftClient mc = MinecraftClient.getInstance();
        double fx = Math.ceil(mc.getWindow().getWidth() / (double) context.menu.width);
        double fy = Math.ceil(mc.getWindow().getHeight() / (double) context.menu.height);

        if (this.gizmoRayProvider != null)
        {
            this.updateRayDragMouse(fx, fy);
        }
    }

    /** Align frame-to-frame pointer deltas after a warp without disturbing the click anchor. */
    private void syncDragPointerLastAfterRayWarp(UIContext context)
    {
        this.refreshDragMousePosition(context);

        if (context == null)
        {
            return;
        }

        this.lastX = this.resolveDragMouseX(context);
        this.lastY = this.resolveDragMouseY(context);
    }

    /**
     * After a cursor warp, align legacy 2D drag references so no spurious screen delta is applied.
     * Ray-driven drags use {@link #refreshDragMousePosition(UIContext)} so the click anchor and
     * compensated ray baselines are not disturbed (see {@link #compensateRayDragForScreenWarp}).
     */
    private void syncDragPointerAfterWarp(UIContext context)
    {
        this.refreshDragMousePosition(context);

        if (context == null)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        double fx = Math.ceil(mc.getWindow().getWidth() / (double) context.menu.width);
        double fy = Math.ceil(mc.getWindow().getHeight() / (double) context.menu.height);

        int dragMouseX;
        int dragMouseY;

        if (this.gizmoRayProvider != null)
        {
            dragMouseX = this.resolveDragMouseX(context);
            dragMouseY = this.resolveDragMouseY(context);
        }
        else
        {
            dragMouseX = (int) Math.round(CURSOR_X[0] / fx);
            dragMouseY = (int) Math.round(CURSOR_Y[0] / fy);
        }

        this.dragAnchorX = dragMouseX;
        this.dragAnchorY = dragMouseY;
        this.lastX = dragMouseX;
        this.lastY = dragMouseY;
    }

    private int adjustPointerDeltaForScreenWrap(int delta, int screenSpan, boolean crossedEdge)
    {
        if (!crossedEdge)
        {
            return delta;
        }

        return delta > 0 ? delta - screenSpan : delta + screenSpan;
    }

    /**
     * After a screen-edge warp, resync the frozen plane grab for free-translation drags so the
     * ray pick does not jump when the cursor teleports.
     */
    private void syncRayDragLastPickAfterWarp(UIContext context)
    {
        if (!this.rayDragInitialized || this.gizmoRayProvider == null || context == null || !Gizmo.INSTANCE.isDragging())
        {
            return;
        }

        if (this.trackball || this.uniformScale || (this.mode == 2 && this.freeRotation))
        {
            return;
        }

        if (this.mode != 0 || !this.freeTranslation)
        {
            return;
        }

        if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
        {
            return;
        }

        if (!this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
        {
            return;
        }

        this.rayGizmoMatrix.getTranslation(this.rayGizmoOrigin);

        if (this.intersectCurrentRay(this.rayCurrentPoint))
        {
            this.rayLastPoint.set(this.rayCurrentPoint);
        }
    }

    private float computePrimaryScreenAxisRaw(int dx, int dy)
    {
        float factor = this.usesModelPixelTranslation()
            ? this.getRayAxisSensitivity(this.rayPrimaryAxisWorldScale)
            : this.getEffectiveTranslationScale();

        if (this.axis == Axis.X)
        {
            return factor * dx;
        }

        if (this.axis == Axis.Y)
        {
            return factor * (Gizmo.INSTANCE.isDragging() ? -dy : dx);
        }

        return factor * dx;
    }

    private float computePrimaryScreenAxisDelta(int dx, int dy)
    {
        return this.applyTranslateDelta(this.axis, this.computePrimaryScreenAxisRaw(dx, dy));
    }

    private float computeSecondaryScreenAxisDelta(int dx, int dy)
    {
        float factor = this.usesModelPixelTranslation()
            ? this.getRayAxisSensitivity(this.raySecondaryAxisWorldScale)
            : this.getEffectiveTranslationScale();

        return this.applyTranslateDelta(this.secondaryAxis, factor * dy);
    }

    private void applyIncrementalAxisTranslate(float signedDelta)
    {
        if (this.isLocal() && !this.usesModelPixelTranslation())
        {
            Vector3f vector = new Vector3f(this.getValue());
            Vector3f local = this.calculateLocalVector(signedDelta, this.axis);

            this.setT(null, vector.x + local.x, vector.y + local.y, vector.z + local.z);
        }
        else
        {
            Vector3f vector = new Vector3f(this.getValue());

            this.addAxisDelta(vector, this.axis, signedDelta);
            this.setT(null, vector.x, vector.y, vector.z);
        }
    }

    private void applyIncrementalAxisScale(float signedScreenDelta)
    {
        float signedDelta = this.applyScaleDelta(this.axis, signedScreenDelta);
        Vector3f vector = new Vector3f(this.getValue());

        this.addAxisDelta(vector, this.axis, signedDelta);
        this.setS(null, vector.x, vector.y, vector.z);
    }

    private boolean refreshRayDragFrame(UIContext context)
    {
        if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
        {
            return false;
        }

        if (!this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
        {
            return false;
        }

        this.rayGizmoMatrix.getTranslation(this.rayGizmoOrigin);
        this.extractAxisWorld(this.axis, this.rayPrimaryAxis);

        return true;
    }

    /**
     * On a screen-edge warp, only move the ray baseline to the warped cursor. Applying a
     * screen-space delta here fights the ray projection and produces a visible jump.
     */
    private void reanchorRayDragAfterScreenWarp(UIContext context)
    {
        if (!this.rayDragInitialized || this.gizmoRayProvider == null || context == null || !Gizmo.INSTANCE.isDragging())
        {
            return;
        }

        if (this.trackball || this.uniformScale || (this.mode == 2 && this.freeRotation))
        {
            return;
        }

        if (this.mode == 0 && !this.freeTranslation && this.secondaryAxis == null)
        {
            if (this.refreshRayDragFrame(context))
            {
                double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);

                if (Double.isFinite(axisValue))
                {
                    this.rayDragStartTranslate.set(this.getValue());
                    this.rayDragStartAxisValue = axisValue;
                }
            }
        }
        else if (this.mode == 0 && this.secondaryAxis != null)
        {
            if (this.refreshRayDragFrame(context))
            {
                this.extractAxisWorld(this.secondaryAxis, this.raySecondaryAxis);
                this.rayPlaneNormal.set(this.rayPrimaryAxis).cross(this.raySecondaryAxis);

                if (this.normalizeSafe(this.rayPlaneNormal) && this.intersectCurrentRay(this.rayCurrentPoint))
                {
                    this.rayDragStartTranslate.set(this.getValue());
                    this.rayDragStartPoint.set(this.rayCurrentPoint);
                    this.rayLastPoint.set(this.rayCurrentPoint);
                }
            }
        }
        else if (this.mode == 0 && this.freeTranslation)
        {
            if (this.refreshRayDragFrame(context))
            {
                this.rayPlaneNormal.set(this.rayDirection);

                if (this.normalizeSafe(this.rayPlaneNormal) && this.intersectCurrentRay(this.rayCurrentPoint))
                {
                    this.rayLastPoint.set(this.rayCurrentPoint);
                }
            }
        }
        else if (this.mode == 1 && !this.uniformScale)
        {
            if (this.refreshRayDragFrame(context))
            {
                double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);

                if (Double.isFinite(axisValue))
                {
                    this.rayDragStartTranslate.set(this.getValue());
                    this.rayDragStartAxisValue = axisValue;
                    this.scaleProgressLength = 0F;
                }
            }
        }
    }

    /**
     * Rotation rings use a frozen grab vector; preserve angular sweep across screen warps.
     */
    private void compensateRotationDragForScreenWarp(UIContext context)
    {
        if (!this.rayDragInitialized || this.gizmoRayProvider == null || context == null || !Gizmo.INSTANCE.isDragging())
        {
            return;
        }

        if (!this.viewRing && !(this.mode == 2 && !this.freeRotation))
        {
            return;
        }

        if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
        {
            return;
        }

        if (!this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
        {
            return;
        }

        this.rayGizmoMatrix.getTranslation(this.rayGizmoOrigin);

        if (!this.intersectCurrentRay(this.rayCurrentPoint))
        {
            return;
        }

        if (this.viewRing)
        {
            float segment = this.rayViewSweepUnwrappedInit ? this.rayViewSweepUnwrappedRad : this.computePlaneSweepAngleRadians(this.rayDragStartPoint, this.rayLastPoint);

            if (this.invertGizmoViewRing || this.invertGizmoViewRingTuning)
            {
                segment = -segment;
            }

            this.rayDragViewRingAngleOffsetRad += segment;
            this.resetViewSweepUnwrap();
        }
        else
        {
            float segment = this.rayPlaneSweepUnwrappedInit ? this.rayPlaneSweepUnwrappedDeg : this.computePlaneSweepAngleDegrees(this.rayDragStartPoint, this.rayLastPoint);

            if (this.shouldInvertRotationRing(this.axis))
            {
                segment = -segment;
            }

            this.rayDragPlaneAngleOffset += segment;
            this.resetPlaneSweepUnwrap();
        }

        this.rayDragStartPoint.set(this.rayCurrentPoint);
        this.rayLastPoint.set(this.rayCurrentPoint);
    }

    private boolean hasDragPointerMoved(UIContext context)
    {
        int dx = this.resolveDragMouseX(context) - this.dragAnchorX;
        int dy = this.resolveDragMouseY(context) - this.dragAnchorY;

        return dx * dx + dy * dy >= DRAG_APPLY_THRESHOLD_SQ;
    }

    /**
     * While the pointer is still inside the click deadzone, keep ray baselines on the current
     * pick so matrix/camera jitter between mouse-down and the first real drag cannot dump a
     * huge fake delta into the numbers.
     */
    private void reanchorRayDragBaseline(UIContext context)
    {
        if (!this.rayDragInitialized || this.gizmoRayProvider == null || context == null)
        {
            return;
        }

        if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
        {
            return;
        }

        if (!this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
        {
            return;
        }

        this.rayGizmoMatrix.getTranslation(this.rayGizmoOrigin);
        this.extractAxisWorld(this.axis, this.rayPrimaryAxis);

        if (this.viewRing)
        {
            this.rayPlaneNormal.set(
                (float) (this.rayOrigin.x - this.rayGizmoOrigin.x),
                (float) (this.rayOrigin.y - this.rayGizmoOrigin.y),
                (float) (this.rayOrigin.z - this.rayGizmoOrigin.z)
            );

            if (this.normalizeSafe(this.rayPlaneNormal) && this.intersectCurrentRay(this.rayCurrentPoint))
            {
                this.rayDragStartPoint.set(this.rayCurrentPoint);
                this.rayLastPoint.set(this.rayCurrentPoint);
                this.resetViewSweepUnwrap();
            }
        }
        else if (this.mode == 2 && !this.freeRotation)
        {
            this.rayPlaneNormal.set(this.rayPrimaryAxis);

            if (this.normalizeSafe(this.rayPlaneNormal) && this.intersectCurrentRay(this.rayCurrentPoint))
            {
                this.rayDragStartPoint.set(this.rayCurrentPoint);
                this.rayLastPoint.set(this.rayCurrentPoint);
                this.resetPlaneSweepUnwrap();
            }
        }
        else if (this.mode == 0 && (this.freeTranslation || this.secondaryAxis != null))
        {
            if (this.freeTranslation)
            {
                this.rayPlaneNormal.set(this.rayDirection);
            }
            else
            {
                this.extractAxisWorld(this.secondaryAxis, this.raySecondaryAxis);
                this.rayPlaneNormal.set(this.rayPrimaryAxis).cross(this.raySecondaryAxis);
            }

            if (this.normalizeSafe(this.rayPlaneNormal) && this.intersectCurrentRay(this.rayCurrentPoint))
            {
                this.rayDragStartPoint.set(this.rayCurrentPoint);
                this.rayLastPoint.set(this.rayCurrentPoint);
            }
        }
        else if ((this.mode == 0 || this.mode == 1) && !this.uniformScale)
        {
            double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);

            if (Double.isFinite(axisValue))
            {
                this.rayLastAxisValue = axisValue;
                this.rayDragStartAxisValue = axisValue;
                this.rayDragStartTranslate.set(this.getValue());
                this.scaleProgressLength = 0F;
            }
        }
    }

    public void enableMode(int mode, Axis axis)
    {
        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && axis == null)
        {
            return;
        }

        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        if (this.editing)
        {
            Axis[] values = Axis.values();

            this.axis = values[MathUtils.cycler(this.axis.ordinal() + 1, 0, values.length - 1)];
            this.secondaryAxis = null;
            this.freeRotation = false;

            this.restore(true);
        }
        else
        {
            this.axis = axis == null ? Axis.X : axis;
            this.secondaryAxis = null;
            this.freeRotation = false;
            this.beginDragAnchor(context);
        }

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.syncDragMouseFromContext(context);
        this.initializeRayDrag(context);
    }

    public void enablePlaneMode(int mode, Axis primary, Axis secondary)
    {
        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && primary == null)
        {
            return;
        }

        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        this.axis = primary == null ? Axis.X : primary;
        this.secondaryAxis = secondary;
        this.freeRotation = false;
        this.beginDragAnchor(context);

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.syncDragMouseFromContext(context);
        this.initializeRayDrag(context);
    }

    public void enableFreeRotation(int mode, Axis marker)
    {
        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && marker == null)
        {
            return;
        }

        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        if (this.editing)
        {
            this.freeRotation = true;
            this.secondaryAxis = null;

            this.restore(true);
        }
        else
        {
            this.axis = Axis.X;
            this.secondaryAxis = null;
            this.freeRotation = true;
            this.beginDragAnchor(context);
        }

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.syncDragMouseFromContext(context);
        this.initializeRayDrag(context);
    }

    public void enableFreeTranslation(int mode)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        this.axis = Axis.X;
        this.secondaryAxis = null;
        this.freeRotation = false;
        this.freeTranslation = true;
        this.beginDragAnchor(context);

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.syncDragMouseFromContext(context);
        this.initializeRayDrag(context);
    }

    /** Starts a uniform-scale drag from the gizmo's center handle (the small white cube):
     *  a single mouse-drag axis scales X, Y and Z together, exactly like holding Ctrl while
     *  dragging a single scale axis (see the {@code all} branch in {@link #render(UIContext)}). */
    public void enableUniformScale(int mode)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        this.axis = Axis.X;
        this.secondaryAxis = null;
        this.freeRotation = false;
        this.trackball = false;
        this.uniformScale = true;
        this.beginDragAnchor(context);

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }
    }

    /**
     * Same drag-start logic as {@link #enableMode(int, Axis)}, but never touches the global
     * {@link Gizmo} mode. Used by Gizmo's Combined mode so grabbing e.g. a scale cube while
     * the gizmo is displayed as Combined doesn't flip it back to solo Scale mode.
     */
    public void enableModeKeepGizmoMode(int mode, Axis axis)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        if (this.editing)
        {
            Axis[] values = Axis.values();

            this.axis = values[MathUtils.cycler(this.axis.ordinal() + 1, 0, values.length - 1)];
            this.secondaryAxis = null;
            this.freeRotation = false;
            this.trackball = false;

            this.restore(true);
        }
        else
        {
            this.axis = axis == null ? Axis.X : axis;
            this.secondaryAxis = null;
            this.freeRotation = false;
            this.trackball = false;
            this.beginDragAnchor(context);
        }

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.syncDragMouseFromContext(context);
        this.initializeRayDrag(context);
    }

    /**
     * Combined-mode counterpart of {@link #enablePlaneMode(int, Axis, Axis)} that skips the
     * {@link Gizmo#setMode(Gizmo.Mode)} call, for the same reason as {@link #enableModeKeepGizmoMode(int, Axis)}.
     */
    public void enablePlaneModeKeepGizmoMode(int mode, Axis primary, Axis secondary)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        this.axis = primary == null ? Axis.X : primary;
        this.secondaryAxis = secondary;
        this.freeRotation = false;
        this.trackball = false;
        this.beginDragAnchor(context);

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.syncDragMouseFromContext(context);
        this.initializeRayDrag(context);
    }

    /**
     * Starts a trackball (free-rotate sphere) drag: rotation is accumulated as a quaternion
     * from per-frame screen-space mouse deltas, so unlike {@link #enableFreeRotation(int, Axis)}
     * it never runs into gimbal lock regardless of the object's current orientation.
     *
     * Extension seam: a future "view" rotate ring or numeric input could plug in here by
     * adding another boolean sibling to {@link #trackball} following the same pattern.
     */
    public void enableTrackballRotate(int mode)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        if (this.editing)
        {
            this.trackball = true;
            this.viewRing = false;
            this.freeRotation = false;
            this.secondaryAxis = null;

            this.restore(true);
            this.beginDragAnchor(context);
        }
        else
        {
            this.axis = Axis.X;
            this.secondaryAxis = null;
            this.freeRotation = false;
            this.trackball = true;
            this.beginDragAnchor(context);
        }

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;
        this.raySphereDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.initializeTrackball();
    }

    /**
     * Starts a view/arcball ring drag: rotation happens around the camera-to-gizmo axis, and
     * the angle is tracked with the same ray/plane intersection method as the axis rings, so
     * the rotation follows the mouse around the ring exactly. The result is accumulated as a
     * quaternion (like the trackball) since the view axis rarely lines up with a euler axis.
     */
    public void enableViewRotate(int mode)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        this.axis = Axis.X;
        this.secondaryAxis = null;
        this.freeRotation = false;
        this.trackball = false;
        this.uniformScale = false;
        this.viewRing = true;
        this.beginDragAnchor(context);

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.initializeTrackball();
        this.syncDragMouseFromContext(context);
        this.initializeRayDrag(context);
    }

    private void initializeTrackball()
    {
        Vector3f rotation = this.getTrackballRotationValue();

        /* Mirrors Transform#createRotationMatrix's Rz * Ry * Rx composition order, so the
         * trackball starts exactly from the object's current orientation. */
        Quaternionf qx = new Quaternionf().fromAxisAngleRad(1F, 0F, 0F, rotation.x);
        Quaternionf qy = new Quaternionf().fromAxisAngleRad(0F, 1F, 0F, rotation.y);
        Quaternionf qz = new Quaternionf().fromAxisAngleRad(0F, 0F, 1F, rotation.z);

        this.trackballStart.set(qz).mul(qy).mul(qx);
        this.trackballAccum.identity();
    }

    private Vector3f getTrackballRotationValue()
    {
        /* Film pose arcball writes to rotate (same channel as the axis rings). */
        if (this.filmArcballTrackball)
        {
            return this.transform.rotate;
        }

        if (this.usesTrackballRotate2())
        {
            return this.transform.rotate2;
        }

        return this.transform.rotate;
    }

    private boolean usesTrackballRotate2()
    {
        return BBSSettings.gizmos.get() && (this.isLocal() || this.filmMatchPoseTrackball);
    }

    private void applyTrackballRotation(float ex, float ey, float ez)
    {
        if (this.filmArcballTrackball)
        {
            this.setR(null, ex, ey, ez);
        }
        else if (this.usesTrackballRotate2())
        {
            this.setR2(null, ex, ey, ez);
        }
        else
        {
            this.setR(null, ex, ey, ez);
        }
    }

    /** Applies decomposed trackball / view-ring euler to the transform, optionally flipping X/Y/Z for pose contexts. */
    private void finishTrackballEulerRotation(float ex, float ey, float ez)
    {
        if (this.invertTrackballRotationX)
        {
            ex = -ex;
        }

        if (this.invertTrackballRotationY)
        {
            ey = -ey;
        }

        if (this.invertTrackballRotationZ)
        {
            ez = -ez;
        }

        this.applyTrackballRotation(ex, ey, ez);
    }

    private Vector3f getValue()
    {
        if (this.mode == 1)
        {
            return this.transform.scale;
        }
        else if (this.mode == 2)
        {
            return this.isLocal() && BBSSettings.gizmos.get() ? this.transform.rotate2 : this.transform.rotate;
        }

        return this.transform.translate;
    }

    private void restore(boolean fully)
    {
        if (this.mode == 0 || fully) this.setT(null, this.cache.translate.x, this.cache.translate.y, this.cache.translate.z);
        if (this.mode == 1 || fully) this.setS(null, this.cache.scale.x, this.cache.scale.y, this.cache.scale.z);
        if (this.mode == 2 || fully)
        {
            this.setR(null, MathUtils.toDeg(this.cache.rotate.x), MathUtils.toDeg(this.cache.rotate.y), MathUtils.toDeg(this.cache.rotate.z));
            this.setR2(null, MathUtils.toDeg(this.cache.rotate2.x), MathUtils.toDeg(this.cache.rotate2.y), MathUtils.toDeg(this.cache.rotate2.z));
        }
    }

    private void disable()
    {
        this.editing = false;
        this.freeRotation = false;
        this.freeTranslation = false;
        this.trackball = false;
        this.uniformScale = false;
        this.viewRing = false;
        this.invertGizmoTrackball = false;
        this.invertGizmoViewRing = false;
        this.invertRotationArcSweep = false;
        this.invertRotationArcY = false;
        this.invertRotationArcZ = false;
        this.invertRotationArcViewRing = false;
        this.forceFrozenRotationArc = false;
        this.filmMatchPoseTrackball = false;
        this.filmArcballTrackball = false;
        this.invertFilmArcballDragY = false;
        this.invertFilmPoseGizmoAxes = false;
        this.rayDragInitialized = false;
        this.raySphereDragInitialized = false;
        this.rayDragPlaneAngleOffset = 0F;
        this.rayDragViewRingAngleOffsetRad = 0F;
        this.resetPlaneSweepUnwrap();
        this.resetViewSweepUnwrap();

        Gizmo.INSTANCE.clearRotationArc();
        Gizmo.INSTANCE.clearDragProgress();

        if (this.handler.hasParent())
        {
            this.handler.removeFromParent();
        }
    }

    public void acceptChanges()
    {
        this.disable();
        this.setTransform(this.transform);
    }

    public void rejectChanges()
    {
        this.disable();
        this.restore(true);
        this.setTransform(this.transform);
    }

    public String buildGizmoDragReadout()
    {
        if (this.transform == null)
        {
            return "";
        }

        if (this.mode == 1)
        {
            return this.formatVector(this.transform.scale, false);
        }

        if (this.mode == 2)
        {
            Vector3f rotation = this.isLocal() && BBSSettings.gizmos.get() ? this.transform.rotate2 : this.transform.rotate;

            return this.formatVector(rotation, true);
        }

        return this.formatVector(this.transform.translate, false);
    }

    private String formatVector(Vector3f vector, boolean degrees)
    {
        if (degrees)
        {
            return String.format(Locale.ENGLISH, "X %.1f\u00B0  Y %.1f\u00B0  Z %.1f\u00B0",
                MathUtils.toDeg(vector.x),
                MathUtils.toDeg(vector.y),
                MathUtils.toDeg(vector.z)
            );
        }

        return String.format(Locale.ENGLISH, "X %.3f  Y %.3f  Z %.3f", vector.x, vector.y, vector.z);
    }

    @Override
    protected void internalSetT(double x, Axis axis)
    {
        if (this.isLocal())
        {
            try
            {
                Vector3f vector3f = this.calculateLocalVector(x, axis);

                this.setT(null,
                    this.transform.translate.x + vector3f.x,
                    this.transform.translate.y + vector3f.y,
                    this.transform.translate.z + vector3f.z
                );
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            super.internalSetT(x, axis);
        }
    }

    @Override
    public void setT(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.translate.set((float) x, (float) y, (float) z);
        this.postCallback();
    }

    @Override
    public void setS(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.scale.set((float) x, (float) y, (float) z);
        this.postCallback();
    }

    @Override
    public void setR(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.rotate.set(MathUtils.toRad((float) x), MathUtils.toRad((float) y), MathUtils.toRad((float) z));
        this.postCallback();
    }

    @Override
    public void setR2(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.rotate2.set(MathUtils.toRad((float) x), MathUtils.toRad((float) y), MathUtils.toRad((float) z));
        this.postCallback();
    }

    @Override
    public void setP(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.pivot.set((float) x, (float) y, (float) z);
        this.postCallback();
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.editing)
        {
            if (context.isPressed(GLFW.GLFW_KEY_ENTER))
            {
                this.acceptChanges();

                return true;
            }
            else if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
            {
                this.rejectChanges();

                return true;
            }
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.editing)
        {
            this.processDragFrame(context);
        }

        super.render(context);

        if (this.editing)
        {
            String label = UIKeys.TRANSFORMS_EDITING.get();
            FontRenderer font = context.batcher.getFont();
            int x = this.area.mx(font.getWidth(label));
            int y = this.area.my(font.getHeight());

            context.batcher.textCard(label, x, y, Colors.WHITE, BBSSettings.primaryColor(Colors.A50));

            if (Gizmo.INSTANCE.isDragging() && this.mode == 1)
            {
                this.updateDragProgressVisual(context);
            }
        }
    }

    private void processDragFrame(UIContext context)
    {
        /* UIContext.mouseX can't be used because when cursor is outside of window
         * its position stops being updated. That's why it has to be queried manually
         * through GLFW...
         *
         * It gets updated outside the window only when one of mouse buttons is
         * being held! */
        GLFW.glfwGetCursorPos(Window.getWindow(), CURSOR_X, CURSOR_Y);

        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        double rawX = CURSOR_X[0];
        double rawY = CURSOR_Y[0];
        double fx = Math.ceil(w / (double) context.menu.width);
        double fy = Math.ceil(h / (double) context.menu.height);

        this.updateRayDragMouse(fx, fy);

        int border = 5;
        int borderPadding = border + 1;
        boolean wrapped = false;
        boolean wrappedLeft = false;
        boolean wrappedRight = false;
        boolean wrappedTop = false;
        boolean wrappedBottom = false;
        int cursorX = (int) mc.mouse.getX();
        int cursorY = (int) mc.mouse.getY();
        int prevLastX = this.lastX;
        int prevLastY = this.lastY;
        boolean rayDragAppliedBeforeWarp = false;

        if (rawX <= border)
        {
            cursorX = w - borderPadding;
            wrapped = true;
            wrappedLeft = true;
        }
        else if (rawX >= w - border)
        {
            cursorX = borderPadding;
            wrapped = true;
            wrappedRight = true;
        }

        if (rawY <= border)
        {
            cursorY = h - borderPadding;
            wrapped = true;
            wrappedTop = true;
        }
        else if (rawY >= h - border)
        {
            cursorY = borderPadding;
            wrapped = true;
            wrappedBottom = true;
        }

        if (wrapped)
        {
            if (this.gizmoRayProvider != null && Gizmo.INSTANCE.isDragging() && !this.trackball && (this.mode == 0 || (this.mode == 1 && !this.uniformScale)))
            {
                rayDragAppliedBeforeWarp = this.applyRayDrag(context);
            }

            Window.moveCursor(cursorX, cursorY);

            if (this.gizmoRayProvider != null)
            {
                this.syncDragPointerLastAfterRayWarp(context);
            }
            else
            {
                this.syncDragPointerAfterWarp(context);
            }

            if (this.filmArcballTrackball && this.trackball)
            {
                this.raySphereDragInitialized = false;
            }
        }

        int dragMouseX = this.resolveDragMouseX(context);
        int dragMouseY = this.resolveDragMouseY(context);
        int mouseDx = dragMouseX - prevLastX;
        int mouseDy = dragMouseY - prevLastY;
        boolean pointerWarped = wrapped;

        if (!wrapped && this.gizmoRayProvider == null && this.shouldReanchorMouseDrag(context, mouseDx, mouseDy))
        {
            this.syncDragPointerAfterWarp(context);

            dragMouseX = this.resolveDragMouseX(context);
            dragMouseY = this.resolveDragMouseY(context);
            pointerWarped = true;

            if (this.filmArcballTrackball && this.trackball)
            {
                this.raySphereDragInitialized = false;
            }
        }

        int warpDx = dragMouseX - prevLastX;
        int warpDy = dragMouseY - prevLastY;

        if (wrapped)
        {
            warpDx = this.adjustPointerDeltaForScreenWrap(warpDx, context.menu.width, wrappedLeft || wrappedRight);
            warpDy = this.adjustPointerDeltaForScreenWrap(warpDy, context.menu.height, wrappedTop || wrappedBottom);
        }

        if (pointerWarped)
        {
            if (wrapped && this.gizmoRayProvider != null)
            {
                this.reanchorRayDragAfterScreenWarp(context);
            }

            this.compensateRotationDragForScreenWarp(context);
            dragMouseX = this.resolveDragMouseX(context);
            dragMouseY = this.resolveDragMouseY(context);
        }

        boolean handledByRayDrag;

        if (rayDragAppliedBeforeWarp)
        {
            handledByRayDrag = true;
        }
        else if (this.trackball)
        {
            handledByRayDrag = this.applyTrackballDragDelta(context, warpDx, warpDy);
        }
        else
        {
            handledByRayDrag = this.applyRayDrag(context);
        }

        /* Interactions that are ray-driven must never fall back to the legacy 2D
         * screen-delta path below: when the ray is briefly unavailable (e.g. the very
         * first frame of a drag, before the baseline is anchored), that fallback
         * compares against a stale lastX/lastY and applies one huge arbitrary delta -
         * the "value suddenly jumps to 13/65/90 on click" bug. Skipping the frame is
         * always safe; the ray drag re-anchors and takes over on the next frame. */
        boolean rayDriven = !this.trackball
            && this.gizmoRayProvider != null
            && Gizmo.INSTANCE.isDragging()
            && !this.uniformScale
            && !(this.mode == 2 && this.freeRotation);

        if (!handledByRayDrag && !rayDriven && !(this.trackball && this.filmArcballTrackball))
        {
            int dx = warpDx;
            int dy = warpDy;
                    Vector3f vector = this.getValue();
                    boolean all = this.uniformScale || (this.mode == 1 && Window.isCtrlPressed());
                    UITrackpad reference = this.mode == 0 ? this.tx : (this.mode == 1 ? this.sx : this.rx);
                    float factor = (float) reference.getValueModifier();

                    if (this.isLocal() && this.mode == 0)
                    {
                        Vector3f local = new Vector3f();

                        if (this.secondaryAxis == null)
                        {
                            double delta;

                            if (this.axis == Axis.Y)
                            {
                                if (!Gizmo.INSTANCE.isDragging())
                                {
                                    delta = factor * dx;
                                }
                                else
                                {
                                    delta = factor * dy;
                                }
                            }
                            else
                            {
                                delta = factor * dx;
                            }

                            local.add(this.calculateLocalVector(delta, this.axis));
                        }
                        else
                        {
                            double primaryDelta = factor * dx;
                            double secondaryDelta = factor * dy;

                            local.add(this.calculateLocalVector(primaryDelta, this.axis));
                            local.add(this.calculateLocalVector(secondaryDelta, this.secondaryAxis));
                        }

                        this.setT(null, vector.x + local.x, vector.y + local.y, vector.z + local.z);
                    }
                    else
                    {
                        Vector3f vector3f = new Vector3f(vector);

                        if (this.mode == 2)
                        {
                            vector3f.mul(180F / MathUtils.PI);
                        }

                        if (this.mode == 2 && this.freeRotation)
                        {
                            vector3f.x += factor * dy;
                            vector3f.y -= factor * dx;
                        }
                        else if (this.mode == 0 && this.secondaryAxis != null)
                        {
                            if (this.axis == Axis.X)
                            {
                                vector3f.x += factor * dx;
                            }
                            else if (this.axis == Axis.Y)
                            {
                                vector3f.y += factor * dx;
                            }
                            else if (this.axis == Axis.Z)
                            {
                                vector3f.z += factor * dx;
                            }

                            float secondaryDelta = factor * dy;

                            if (this.secondaryAxis == Axis.X)
                            {
                                vector3f.x += secondaryDelta;
                            }
                            else if (this.secondaryAxis == Axis.Y)
                            {
                                vector3f.y -= secondaryDelta;
                            }
                            else if (this.secondaryAxis == Axis.Z)
                            {
                                vector3f.z -= secondaryDelta;
                            }
                        }
                        else
                        {
                            if (this.mode == 0 && !this.isLocal() && this.secondaryAxis == null && !all)
                            {
                                if (this.axis == Axis.X)
                                {
                                    vector3f.x += factor * dx;
                                }
                                else if (this.axis == Axis.Y)
                                {
                                    if (!Gizmo.INSTANCE.isDragging())
                                    {
                                        vector3f.y += factor * dx;
                                    }
                                    else
                                    {
                                        vector3f.y -= factor * dy;
                                    }
                                }
                                else if (this.axis == Axis.Z)
                                {
                                    vector3f.z += factor * dx;
                                }
                            }
                            else
                            {
                                /* Screen-delta fallback (used when no ray provider is
                                 * available, and always for uniform scale): mouse right/up is
                                 * positive, left/down is negative. Screen Y grows downward,
                                 * so "up" needs a negated dy contribution. */
                                float delta = this.mode == 2 ? factor * (dx - dy) : factor * dx;

                                if (this.mode == 1 && !all)
                                {
                                    float signedDelta = this.applyScaleDelta(this.axis, delta);

                                    if (this.axis == Axis.X) vector3f.x += signedDelta;
                                    else if (this.axis == Axis.Y) vector3f.y += signedDelta;
                                    else if (this.axis == Axis.Z) vector3f.z += signedDelta;

                                    this.scaleProgressLength += signedDelta;
                                }
                                else
                                {
                                    if (this.axis == Axis.X || all) vector3f.x += delta;
                                    if (this.axis == Axis.Y || all) vector3f.y += delta;
                                    if (this.axis == Axis.Z || all) vector3f.z += delta;

                                    if (this.mode == 1 && all)
                                    {
                                        this.scaleProgressLength += delta;
                                    }
                                }
                            }
                        }

                        if (this.mode == 0) this.setT(null, vector3f.x, vector3f.y, vector3f.z);
                        if (this.mode == 1) this.setS(null, vector3f.x, vector3f.y, vector3f.z);
                        if (this.mode == 2)
                        {
                            if (this.isLocal() && BBSSettings.gizmos.get()) this.setR2(null, vector3f.x, vector3f.y, vector3f.z);
                            else this.setR(null, vector3f.x, vector3f.y, vector3f.z);
                        }
                    }

            this.setTransform(this.transform);

            this.lastX = dragMouseX;
            this.lastY = dragMouseY;
        }

        if (Gizmo.INSTANCE.isDragging())
        {
            this.advanceDragPointer(context);
        }
    }

    private void advanceDragPointer(UIContext context)
    {
        int dragMouseX = this.resolveDragMouseX(context);
        int dragMouseY = this.resolveDragMouseY(context);

        this.lastX = dragMouseX;
        this.lastY = dragMouseY;
    }

    private void updateRayDragMouse(double fx, double fy)
    {
        this.rayDragMouseX = (int) Math.round(CURSOR_X[0] / fx);
        this.rayDragMouseY = (int) Math.round(CURSOR_Y[0] / fy);
    }

    private int resolveDragMouseX(UIContext context)
    {
        if (this.gizmoRayProvider != null)
        {
            return this.rayDragMouseX;
        }

        return context.mouseX;
    }

    private int resolveDragMouseY(UIContext context)
    {
        if (this.gizmoRayProvider != null)
        {
            return this.rayDragMouseY;
        }

        return context.mouseY;
    }

    private boolean initializeRayDrag(UIContext context)
    {
        if (!Gizmo.INSTANCE.isDragging())
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (this.gizmoRayProvider == null || context == null)
        {
            this.rayDragInitialized = false;
            return false;
        }

        /* Interactions that use the plain screen-delta fallback in render() instead of rays:
         * uniform scale (center cube, not tied to any world axis) and the legacy 2D free
         * rotation. Trackball never reaches this method (it goes through applyTrackballDrag). */
        if (this.uniformScale || (this.mode == 2 && this.freeRotation))
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (!this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
        {
            this.rayDragInitialized = false;
            return false;
        }

        this.rayGizmoMatrix.getTranslation(this.rayGizmoOrigin);
        this.extractAxisWorld(this.axis, this.rayPrimaryAxis);

        boolean needsPlanePoint = false;

        if (this.viewRing)
        {
            /* Rotation plane faces the camera; the plane normal (= rotation axis) points from
             * the gizmo toward the camera so the rotation follows the mouse's movement around
             * the ring (mouse moving right on the right side of the ring rotates rightward). */
            this.rayPlaneNormal.set(
                (float) (this.rayOrigin.x - this.rayGizmoOrigin.x),
                (float) (this.rayOrigin.y - this.rayGizmoOrigin.y),
                (float) (this.rayOrigin.z - this.rayGizmoOrigin.z)
            );

            if (!this.normalizeSafe(this.rayPlaneNormal))
            {
                this.rayDragInitialized = false;
                return false;
            }

            needsPlanePoint = true;
        }
        else if (this.mode == 0 && this.freeTranslation)
        {
            this.rayPlaneNormal.set(this.rayDirection);

            if (!this.normalizeSafe(this.rayPlaneNormal))
            {
                this.rayDragInitialized = false;
                return false;
            }

            needsPlanePoint = true;
        }
        else if (this.mode == 0 && this.secondaryAxis != null)
        {
            this.extractAxisWorld(this.secondaryAxis, this.raySecondaryAxis);
            this.rayPlaneNormal.set(this.rayPrimaryAxis).cross(this.raySecondaryAxis);

            if (!this.normalizeSafe(this.rayPlaneNormal))
            {
                this.rayDragInitialized = false;
                return false;
            }

            needsPlanePoint = true;
        }
        else if (this.mode == 0 || this.mode == 1)
        {
            /* Single-axis translate and single-axis scale both reduce to "where along this
             * world axis line does the mouse ray currently point", so they can share the
             * exact same grab-point-independent projection. */
            double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);

            if (!Double.isFinite(axisValue))
            {
                this.rayDragInitialized = false;
                return false;
            }

            this.rayLastAxisValue = axisValue;
        }
        else if (this.mode == 2)
        {
            /* Axis ring: the drag plane is the ring's own plane. The normal is intentionally
             * NOT flipped toward the camera: the per-frame angle between grab vectors then
             * rotates the value exactly as much (and in the same direction) as the mouse moved
             * around the ring, which is what makes the ring "follow the mouse". */
            this.rayPlaneNormal.set(this.rayPrimaryAxis);

            if (!this.normalizeSafe(this.rayPlaneNormal))
            {
                this.rayDragInitialized = false;
                return false;
            }

            needsPlanePoint = true;
        }
        else
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (needsPlanePoint && !this.intersectCurrentRay(this.rayLastPoint))
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (needsPlanePoint && (this.mode == 2 || this.viewRing))
        {
            Vector3f local = new Vector3f();

            this.toGizmoLocal(this.rayLastPoint, local);

            if (this.rayDragReanchor && Gizmo.INSTANCE.hasRotationArc())
            {
                Gizmo.INSTANCE.reanchorRotationArc(local);
            }
            else
            {
                if (this.useFrozenRotationArc())
                {
                    /* Freeze the last *drawn* gizmo matrix (camera × origin). The ray
                     * provider's bone-only matrix lacks the editor camera, so freezing
                     * that made Local process bars render off-screen in the model editor. */
                    Gizmo.INSTANCE.setRotationArcFrozen(Gizmo.INSTANCE.lastGizmoMatrix, this.viewRing);
                }

                Gizmo.INSTANCE.startRotationArc(local);
            }
        }

        this.rayDragReanchor = false;
        this.rayDragInitialized = true;

        if (this.mode == 0)
        {
            this.rayDragStartTranslate.set(this.getValue());
            this.rayDragStartAxisValue = this.rayLastAxisValue;
            this.cacheRayAxisWorldScales();

            if (needsPlanePoint)
            {
                this.rayDragStartPoint.set(this.rayLastPoint);
            }
        }
        else if (this.mode == 1)
        {
            if (needsPlanePoint)
            {
                if (!this.rayDragReanchor)
                {
                    this.rayDragStartPoint.set(this.rayLastPoint);
                }
            }
            else if (!this.uniformScale && !this.rayDragReanchor)
            {
                this.rayDragStartTranslate.set(this.getValue());
                this.rayDragStartAxisValue = this.rayLastAxisValue;
                this.scaleProgressLength = 0F;
            }

            this.updateDragProgressVisual(context);
        }
        else if (this.mode == 2 && !this.freeRotation && needsPlanePoint)
        {
            /* Axis and view rings: anchor the grab point on the ring plane so the first
             * drag frame measures from the click position, not a stale/zero vector. */
            this.rayDragStartPoint.set(this.rayLastPoint);
        }

        return true;
    }

    private void updateDragProgressVisual(UIContext context)
    {
        if (!Gizmo.INSTANCE.isDragging() || this.mode != 1)
        {
            Gizmo.INSTANCE.clearDragProgress();

            return;
        }

        boolean rayDriven = !this.trackball
            && this.gizmoRayProvider != null
            && !this.uniformScale
            && !(this.mode == 2 && this.freeRotation);

        if (!rayDriven || !this.rayDragInitialized)
        {
            this.updateScreenDragProgressVisual(context);

            return;
        }

        if (context == null || !this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
        {
            return;
        }

        if (this.secondaryAxis == null && !this.freeTranslation)
        {
            this.rayDragProgressStart.zero();
            this.setLocalAxisPoint(this.rayDragProgressEnd, this.axis, this.scaleProgressLength * this.getScaleProgressVisualSign(this.axis));
            Gizmo.INSTANCE.setDragProgress(this.rayDragProgressStart, this.rayDragProgressEnd);
        }
    }

    private void accumulateScaleProgress(float axisProjectionDelta)
    {
        this.scaleProgressLength += this.applyScaleDelta(this.axis, axisProjectionDelta);
    }

    private void updateScreenDragProgressVisual(UIContext context)
    {
        if (context == null)
        {
            Gizmo.INSTANCE.clearDragProgress();

            return;
        }

        this.rayDragProgressStart.zero();
        this.screenDragProgressEnd.zero();

        if (this.uniformScale || Window.isCtrlPressed())
        {
            this.screenDragProgressEnd.set(this.scaleProgressLength * Gizmo.INSTANCE.getFlipSign(Axis.X), 0F, 0F);
        }
        else
        {
            this.setLocalAxisPoint(this.screenDragProgressEnd, this.axis, this.scaleProgressLength * this.getScaleProgressVisualSign(this.axis));
        }

        Gizmo.INSTANCE.setDragProgress(this.rayDragProgressStart, this.screenDragProgressEnd);
    }

    private void setLocalAxisPoint(Vector3f dest, Axis axis, float value)
    {
        dest.zero();

        if (axis == Axis.X)
        {
            dest.x = value;
        }
        else if (axis == Axis.Y)
        {
            dest.y = value;
        }
        else
        {
            dest.z = value;
        }
    }

    /** Transforms a world-space point into the gizmo's local space (the space its handles are
     *  drawn in), used for the visual rotation sweep arc. */
    private void toGizmoLocal(Vector3d world, Vector3f dest)
    {
        Matrix4f inv = new Matrix4f(this.rayGizmoMatrix).invert();
        Vector4f point = new Vector4f((float) world.x, (float) world.y, (float) world.z, 1F).mul(inv);

        dest.set(point.x, point.y, point.z);
    }

    /** Local-mode gizmo drags rotate the live matrix, which makes geometric arc tracking drift.
     *  Film Anchor also freezes in Global so the process bar does not spin with the object. */
    private boolean useFrozenRotationArc()
    {
        return BBSSettings.gizmos.get() && (this.isLocal() || this.forceFrozenRotationArc);
    }

    private void updateRotationArcProgress(float deltaDegrees, Vector3d worldPoint)
    {
        float sweep = deltaDegrees;

        if (this.invertRotationArcSweep)
        {
            sweep = -sweep;
        }
        else if (this.invertRotationArcViewRing && this.viewRing)
        {
            /* Film Transform: white ring value follows the mouse; only the process bar is flipped. */
            sweep = -sweep;
        }
        else if (this.invertRotationArcY && !this.viewRing && this.axis == Axis.Y)
        {
            /* Model-editor General: Y process bar winds opposite the plane-sweep value delta. */
            sweep = -sweep;
        }
        else if (this.filmArcballTrackball && !this.viewRing && this.shouldInvertRotationRing(this.axis))
        {
            /* Pose cubic X/Z: value delta is flipped for mouse sense; undo that for arc3D so
             * the process bar follows the cursor. When invertRotationArcZ is also set (form /
             * model-block pose), Z's arc already winds with that flipped delta — only undo X. */
            if (this.axis != Axis.Z || !this.invertRotationArcZ)
            {
                sweep = -sweep;
            }
        }
        else if (this.invertRotationArcZ && !this.viewRing && this.axis == Axis.Z)
        {
            /* Film Anchor / form General: Z process bar winds opposite the plane-sweep value. */
            sweep = -sweep;
        }
        else if (!this.useFrozenRotationArc() && !this.viewRing && this.axis == Axis.Z
            && !this.filmArcballTrackball)
        {
            /* Film Transform Global Z-ring only. */
            sweep = -sweep;
        }

        Gizmo.INSTANCE.addRotationSweep(sweep);
    }

    private boolean applyRayDrag(UIContext context)
    {
        if (!Gizmo.INSTANCE.isDragging())
        {
            return false;
        }

        if (this.gizmoRayProvider == null || context == null)
        {
            return false;
        }

        if (!this.rayDragInitialized)
        {
            if (!this.initializeRayDrag(context))
            {
                return false;
            }

            /* The initialization frame only anchors the reference grab point/axis value.
             * No delta is applied until the mouse actually moves on a later frame, so
             * clicking a handle can never change the value by itself. */
            return true;
        }

        if (this.viewRing)
        {
            if (!this.hasDragPointerMoved(context))
            {
                this.reanchorRayDragBaseline(context);

                return true;
            }

            if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
            {
                return false;
            }

            if (!this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
            {
                return false;
            }

            this.rayGizmoMatrix.getTranslation(this.rayGizmoOrigin);

            return this.applyViewRingDrag();
        }

        if (this.mode == 0 && !this.hasDragPointerMoved(context))
        {
            this.reanchorRayDragBaseline(context);

            return true;
        }

        if (this.mode == 1 && !this.uniformScale && !this.hasDragPointerMoved(context))
        {
            this.reanchorRayDragBaseline(context);

            return true;
        }

        if (this.mode == 2 && !this.freeRotation && !this.trackball && !this.hasDragPointerMoved(context))
        {
            this.reanchorRayDragBaseline(context);

            return true;
        }

        if ((this.mode == 0 || (this.mode == 1 && !this.uniformScale))
            && !this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
        {
            return false;
        }

        if (this.mode == 0)
        {
            if (this.freeTranslation)
            {
                if (!this.intersectCurrentRay(this.rayCurrentPoint))
                {
                    return false;
                }

                Vector3d step = new Vector3d(this.rayCurrentPoint).sub(this.rayLastPoint);

                if (step.lengthSquared() <= 1.0E-12D)
                {
                    return true;
                }

                if (this.usesModelPixelTranslation())
                {
                    Vector3f worldX = new Vector3f();
                    Vector3f worldY = new Vector3f();
                    Vector3f worldZ = new Vector3f();
                    this.extractAxisWorld(Axis.X, worldX);
                    this.extractAxisWorld(Axis.Y, worldY);
                    this.extractAxisWorld(Axis.Z, worldZ);

                    float dx = (float) step.dot(worldX.x, worldX.y, worldX.z) * this.getRayAxisSensitivity(this.rayAxisWorldScaleX);
                    float dy = (float) step.dot(worldY.x, worldY.y, worldY.z) * this.getRayAxisSensitivity(this.rayAxisWorldScaleY);
                    float dz = (float) step.dot(worldZ.x, worldZ.y, worldZ.z) * this.getRayAxisSensitivity(this.rayAxisWorldScaleZ);
                    Vector3f result = new Vector3f(this.getValue());

                    this.addAxisDelta(result, Axis.X, this.applyTranslateDelta(Axis.X, dx));
                    this.addAxisDelta(result, Axis.Y, this.applyTranslateDelta(Axis.Y, dy));
                    this.addAxisDelta(result, Axis.Z, this.applyTranslateDelta(Axis.Z, dz));
                    this.setT(null, result.x, result.y, result.z);
                }
                else
                {
                    Vector3f worldX = new Vector3f();
                    Vector3f worldY = new Vector3f();
                    Vector3f worldZ = new Vector3f();
                    this.extractAxisWorld(Axis.X, worldX);
                    this.extractAxisWorld(Axis.Y, worldY);
                    this.extractAxisWorld(Axis.Z, worldZ);

                    float scale = this.getEffectiveTranslationScale();
                    Vector3f result = new Vector3f(this.getValue());

                    this.addAxisDelta(result, Axis.X, this.applyTranslateDelta(Axis.X, (float) step.dot(worldX.x, worldX.y, worldX.z) * scale));
                    this.addAxisDelta(result, Axis.Y, this.applyTranslateDelta(Axis.Y, (float) step.dot(worldY.x, worldY.y, worldY.z) * scale));
                    this.addAxisDelta(result, Axis.Z, this.applyTranslateDelta(Axis.Z, (float) step.dot(worldZ.x, worldZ.y, worldZ.z) * scale));
                    this.setT(null, result.x, result.y, result.z);
                }

                this.rayLastPoint.set(this.rayCurrentPoint);
            }
            else if (this.secondaryAxis == null)
            {
                double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);

                if (!Double.isFinite(axisValue))
                {
                    return false;
                }

                float sensitivity = this.usesModelPixelTranslation()
                    ? this.getRayAxisSensitivity(this.rayPrimaryAxisWorldScale)
                    : this.getEffectiveTranslationScale();
                float delta = (float) (axisValue - this.rayDragStartAxisValue) * sensitivity;

                this.applyAbsoluteRayTranslate(this.axis, delta);
            }
            else
            {
                if (!this.intersectCurrentRay(this.rayCurrentPoint))
                {
                    return false;
                }

                Vector3d offset = new Vector3d(this.rayCurrentPoint).sub(this.rayDragStartPoint);

                if (this.usesModelPixelTranslation())
                {
                    float primaryDelta = (float) offset.dot(this.rayPrimaryAxis.x, this.rayPrimaryAxis.y, this.rayPrimaryAxis.z) * this.getRayAxisSensitivity(this.rayPrimaryAxisWorldScale);
                    float secondaryDelta = (float) offset.dot(this.raySecondaryAxis.x, this.raySecondaryAxis.y, this.raySecondaryAxis.z) * this.getRayAxisSensitivity(this.raySecondaryAxisWorldScale);
                    Vector3f result = new Vector3f(this.rayDragStartTranslate);

                    this.addAxisDelta(result, this.axis, this.applyTranslateDelta(this.axis, primaryDelta));
                    this.addAxisDelta(result, this.secondaryAxis, this.applyTranslateDelta(this.secondaryAxis, secondaryDelta));
                    this.setT(null, result.x, result.y, result.z);
                }
                else
                {
                    float scale = this.getEffectiveTranslationScale();
                    float primaryDelta = (float) offset.dot(this.rayPrimaryAxis.x, this.rayPrimaryAxis.y, this.rayPrimaryAxis.z) * scale;
                    float secondaryDelta = (float) offset.dot(this.raySecondaryAxis.x, this.raySecondaryAxis.y, this.raySecondaryAxis.z) * scale;
                    Vector3f result = new Vector3f(this.rayDragStartTranslate);

                    this.addAxisDelta(result, this.axis, this.applyTranslateDelta(this.axis, primaryDelta));
                    this.addAxisDelta(result, this.secondaryAxis, this.applyTranslateDelta(this.secondaryAxis, secondaryDelta));
                    this.setT(null, result.x, result.y, result.z);
                }
            }
        }
        else if (this.mode == 1 && !this.uniformScale)
        {
            double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);

            if (!Double.isFinite(axisValue))
            {
                return false;
            }

            float axisProjection = (float) (axisValue - this.rayDragStartAxisValue);
            float primaryDelta = this.applyScaleDelta(this.axis, axisProjection * this.getBaseTranslationScale());
            Vector3f result = new Vector3f(this.rayDragStartTranslate);

            this.addAxisDelta(result, this.axis, primaryDelta);
            this.setS(null, result.x, result.y, result.z);
            this.scaleProgressLength = axisProjection;
        }
        else if (this.mode == 2 && !this.freeRotation)
        {
            /* Axis ring: total angle from the grab point on the ring to the current hit, applied
             * against the value captured at drag start so fast cursor movement never drops frames. */
            if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
            {
                return false;
            }

            if (!this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
            {
                return false;
            }

            this.rayGizmoMatrix.getTranslation(this.rayGizmoOrigin);

            if (!this.intersectCurrentRay(this.rayCurrentPoint))
            {
                return false;
            }

            float previousAngle = this.continuousPlaneSweepDegrees(this.rayLastPoint);
            float angle = this.continuousPlaneSweepDegrees(this.rayCurrentPoint);

            /* Model/bone-pose bodies are rendered through a matrix that (same as the "no
             * idea why but it works" 180-degree-about-X flip in calculateLocalVector() above)
             * mirrors the X and Z axes relative to how this class' own Euler storage rotates,
             * so the X and Z rings need their angle sign flipped to spin the same way the mouse
             * moves, exactly like the (already-correct) general Transform gizmo. Y is on the
             * flip's own axis and needs no correction. */
            if (this.shouldInvertRotationRing(this.axis))
            {
                angle = -angle;
                previousAngle = -previousAngle;
            }

            Vector3f value = this.isLocal() && BBSSettings.gizmos.get()
                ? new Vector3f(this.cache.rotate2)
                : new Vector3f(this.cache.rotate);

            value.mul(180F / MathUtils.PI);

            this.addAxisDelta(value, this.axis, angle);

            if (this.isLocal() && BBSSettings.gizmos.get())
            {
                this.setR2(null, value.x, value.y, value.z);
            }
            else
            {
                this.setR(null, value.x, value.y, value.z);
            }

            this.updateRotationArcProgress(angle - previousAngle, this.rayCurrentPoint);

            this.rayLastPoint.set(this.rayCurrentPoint);
        }
        else
        {
            return false;
        }

        return true;
    }

    /**
     * Applies one frame of view/arcball ring rotation: the angle the mouse moved around the
     * camera-facing plane is applied as a quaternion around the camera-to-gizmo axis, then
     * decomposed back to euler angles like the trackball.
     */
    private boolean applyViewRingDrag()
    {
        if (!this.intersectCurrentRay(this.rayCurrentPoint))
        {
            return false;
        }

        float previousAngleRad = this.continuousViewSweepRadians(this.rayLastPoint);
        float angleRad = this.continuousViewSweepRadians(this.rayCurrentPoint);

        if (this.invertGizmoViewRing || this.invertGizmoViewRingTuning)
        {
            angleRad = -angleRad;
            previousAngleRad = -previousAngleRad;
        }

        this.trackballAccum.identity().rotateAxis(angleRad, this.rayPlaneNormal.x, this.rayPlaneNormal.y, this.rayPlaneNormal.z);

        Quaternionf finalRotation = new Quaternionf(this.trackballStart).premul(this.trackballAccum);
        Vector3f euler = new Vector3f();

        this.eulerZYXFromQuaternion(finalRotation, euler);

        float ex = MathUtils.toDeg(euler.x);
        float ey = MathUtils.toDeg(euler.y);
        float ez = MathUtils.toDeg(euler.z);

        this.finishTrackballEulerRotation(ex, ey, ez);

        this.updateRotationArcProgress((float) Math.toDegrees(angleRad - previousAngleRad), this.rayCurrentPoint);

        this.rayLastPoint.set(this.rayCurrentPoint);

        return true;
    }

    /**
     * Applies one frame of trackball rotation from the accumulated screen-space mouse delta.
     * Unlike {@link #applyRayDrag(UIContext)}, this never needs the {@link #gizmoRayProvider}
     * to succeed (it falls back to world axes for its screen basis), so it always reports the
     * drag as handled and the caller's 2D-delta fallback branch is never reached for trackball.
     */
    private boolean applyTrackballDrag(UIContext context)
    {
        if (this.filmArcballTrackball)
        {
            return this.applyFilmArcballTrackballDrag(context);
        }

        int dx = this.resolveDragMouseX(context) - this.lastX;
        int dy = this.resolveDragMouseY(context) - this.lastY;

        return this.applyTrackballDragDelta(context, dx, dy);
    }

    private boolean applyTrackballDragDelta(UIContext context, int dx, int dy)
    {
        if (!this.hasDragPointerMoved(context))
        {
            return true;
        }

        if (dx != 0 || dy != 0)
        {
            Vector3f right = new Vector3f(1F, 0F, 0F);
            Vector3f up = new Vector3f(0F, 1F, 0F);

            if (this.gizmoRayProvider == null || !this.computeScreenBasis(context, right, up))
            {
                right.set(1F, 0F, 0F);
                up.set(0F, 1F, 0F);
            }

            float sensitivity = 0.012F;
            float angle = (float) Math.sqrt((double) dx * dx + (double) dy * dy) * sensitivity;

            if (this.invertGizmoTrackball || this.invertGizmoTrackballTuning)
            {
                angle = -angle;
            }

            if (angle > 1.0E-6F)
            {
                /* Screen Y grows downward; negate so dragging up rotates with the cursor. */
                float verticalDelta = -dy;

                if (this.invertTrackballDragY)
                {
                    verticalDelta = -verticalDelta;
                }

                Vector3f axis = new Vector3f(right).mul(verticalDelta).add(new Vector3f(up).mul(-dx));

                if (this.normalizeSafe(axis))
                {
                    Quaternionf delta = new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, angle);

                    this.trackballAccum.premul(delta);
                }
            }

            Quaternionf finalRotation = new Quaternionf(this.trackballStart).premul(this.trackballAccum);
            Vector3f euler = new Vector3f();

            this.eulerZYXFromQuaternion(finalRotation, euler);

            float ex = MathUtils.toDeg(euler.x);
            float ey = MathUtils.toDeg(euler.y);
            float ez = MathUtils.toDeg(euler.z);

            this.finishTrackballEulerRotation(ex, ey, ez);
        }

        return true;
    }

    /**
     * Film pose trackball: arcball drag on a unit sphere in gizmo-local space, driven by the
     * same view-space rays and captured pass matrix as the axis / view rings.
     */
    private boolean applyFilmArcballTrackballDrag(UIContext context)
    {
        if (this.gizmoRayProvider == null || context == null || !Gizmo.INSTANCE.isDragging())
        {
            return false;
        }

        if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
        {
            return true;
        }

        if (!this.gizmoRayProvider.getMouseRay(context, this.resolveDragMouseX(context), this.resolveDragMouseY(context), this.rayOrigin, this.rayDirection))
        {
            return true;
        }

        Vector3f current = new Vector3f();

        if (!this.intersectRayUnitSphereGizmoLocal(current))
        {
            return true;
        }

        if (this.invertFilmArcballDragY)
        {
            current.y = -current.y;
        }

        if (!this.raySphereDragInitialized)
        {
            this.rayLastSpherePoint.set(current);
            this.raySphereDragInitialized = true;

            return true;
        }

        if (!this.hasDragPointerMoved(context))
        {
            this.rayLastSpherePoint.set(current);

            return true;
        }

        Vector3f from = new Vector3f(this.rayLastSpherePoint);
        Vector3f to = new Vector3f(current);
        Vector3f axis = new Vector3f(from).cross(to);
        float sin = axis.length();
        float cos = from.dot(to);

        if (sin <= 1.0E-7F && cos >= 0.999999F)
        {
            this.rayLastSpherePoint.set(current);

            return true;
        }

        if (!this.normalizeSafe(axis))
        {
            return true;
        }

        float angleRad = (float) Math.atan2(sin, cos);

        if (this.invertGizmoTrackball)
        {
            angleRad = -angleRad;
        }

        if (Math.abs(angleRad) > MAX_RING_ANGLE_JUMP_RAD)
        {
            this.raySphereDragInitialized = false;

            return true;
        }

        Quaternionf delta = new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, angleRad);

        this.trackballAccum.premul(delta);

        Quaternionf finalRotation = new Quaternionf(this.trackballStart).premul(this.trackballAccum);
        Vector3f euler = new Vector3f();

        this.eulerZYXFromQuaternion(finalRotation, euler);

        float ex = MathUtils.toDeg(euler.x);
        float ey = MathUtils.toDeg(euler.y);
        float ez = MathUtils.toDeg(euler.z);

        this.finishTrackballEulerRotation(ex, ey, ez);

        this.rayLastSpherePoint.set(current);

        return true;
    }

    private boolean intersectRayUnitSphereGizmoLocal(Vector3f out)
    {
        Matrix4f inverse = new Matrix4f(this.rayGizmoMatrix).invert();
        Vector4f localOrigin4 = new Vector4f((float) this.rayOrigin.x, (float) this.rayOrigin.y, (float) this.rayOrigin.z, 1F).mul(inverse);
        Vector4f localDir4 = new Vector4f(this.rayDirection.x, this.rayDirection.y, this.rayDirection.z, 0F).mul(inverse);

        float ox = localOrigin4.x;
        float oy = localOrigin4.y;
        float oz = localOrigin4.z;
        float dx = localDir4.x;
        float dy = localDir4.y;
        float dz = localDir4.z;
        float dirLenSq = dx * dx + dy * dy + dz * dz;

        if (dirLenSq <= 1.0E-12F)
        {
            return false;
        }

        float invLen = (float) (1D / Math.sqrt(dirLenSq));

        dx *= invLen;
        dy *= invLen;
        dz *= invLen;

        float radius = 1F;
        float b = 2F * (ox * dx + oy * dy + oz * dz);
        float c = ox * ox + oy * oy + oz * oz - radius * radius;
        float disc = b * b - 4F * c;

        if (disc < 0F)
        {
            return this.projectArcballMissOntoUnitSphere(ox, oy, oz, dx, dy, dz, out);
        }

        float sqrtDisc = (float) Math.sqrt(disc);
        float t = (-b - sqrtDisc) * 0.5F;

        if (t <= 1.0E-6F)
        {
            t = (-b + sqrtDisc) * 0.5F;
        }

        if (t <= 1.0E-6F)
        {
            return false;
        }

        out.set(ox + dx * t, oy + dy * t, oz + dz * t);
        this.normalizeSafe(out);

        return true;
    }

    /**
     * GLU-style arcball projection when the view ray misses the unit sphere (for example the
     * cursor left the viewport but drag should keep rotating).
     */
    private boolean projectArcballMissOntoUnitSphere(float ox, float oy, float oz, float dx, float dy, float dz, Vector3f out)
    {
        float t = -(ox * dx + oy * dy + oz * dz);
        float px = ox + dx * t;
        float py = oy + dy * t;
        float d = (float) Math.sqrt(px * px + py * py);
        float z;

        if (d < 1F / 1.4142135F)
        {
            z = (float) Math.sqrt(Math.max(0F, 1F - d * d));
        }
        else
        {
            z = 0.5F / Math.max(d, 1.0E-6F);
        }

        if (d <= 1.0E-6F)
        {
            out.set(0F, 0F, 1F);
        }
        else
        {
            float scale = (float) Math.sqrt(Math.max(0F, 1F - z * z)) / d;

            out.set(px * scale, py * scale, z);
        }

        this.normalizeSafe(out);

        return true;
    }

    /**
     * Decomposes a unit quaternion into Euler angles (radians) matching the Rz * Ry * Rx
     * composition order used by {@link Transform#createRotationMatrix()} (and by
     * {@link #initializeTrackball()} above), using the standard closed-form quaternion-to-matrix
     * formula so it doesn't depend on any particular JOML matrix decomposition helper.
     */
    private void eulerZYXFromQuaternion(Quaternionf q, Vector3f dest)
    {
        float qx = q.x, qy = q.y, qz = q.z, qw = q.w;

        float m20 = 2F * (qx * qz - qy * qw);
        float m00 = 1F - 2F * (qy * qy + qz * qz);
        float m10 = 2F * (qx * qy + qz * qw);
        float m21 = 2F * (qy * qz + qx * qw);
        float m22 = 1F - 2F * (qx * qx + qy * qy);

        float y = (float) Math.asin(MathUtils.clamp(-m20, -1F, 1F));
        float z = (float) Math.atan2(m10, m00);
        float x = (float) Math.atan2(m21, m22);

        dest.set(x, y, z);
    }

    /**
     * Approximates the on-screen "right" and "up" directions in world/gizmo space at the
     * current mouse position by sampling the mouse ray at two neighbouring pixels. Works for
     * any {@link IGizmoRayProvider} implementation without needing camera basis vectors added
     * to that interface.
     */
    private boolean computeScreenBasis(UIContext context, Vector3f right, Vector3f up)
    {
        Vector3d originA = new Vector3d();
        Vector3f dirA = new Vector3f();
        Vector3d originB = new Vector3d();
        Vector3f dirB = new Vector3f();
        Vector3d originC = new Vector3d();
        Vector3f dirC = new Vector3f();

        int dragMouseX = this.resolveDragMouseX(context);
        int dragMouseY = this.resolveDragMouseY(context);

        if (!this.gizmoRayProvider.getMouseRay(context, dragMouseX, dragMouseY, originA, dirA)) return false;
        if (!this.gizmoRayProvider.getMouseRay(context, dragMouseX + 1, dragMouseY, originB, dirB)) return false;
        if (!this.gizmoRayProvider.getMouseRay(context, dragMouseX, dragMouseY + 1, originC, dirC)) return false;

        right.set(dirB).sub(dirA);
        up.set(dirC).sub(dirA);

        return this.normalizeSafe(right) && this.normalizeSafe(up);
    }

    private boolean intersectCurrentRay(Vector3d out)
    {
        this.planeOrigin.set(this.rayGizmoOrigin.x, this.rayGizmoOrigin.y, this.rayGizmoOrigin.z);
        this.planeNormal.set(this.rayPlaneNormal.x, this.rayPlaneNormal.y, this.rayPlaneNormal.z);
        this.rayDirectionD.set(this.rayDirection.x, this.rayDirection.y, this.rayDirection.z);

        if (this.planeNormal.dot(this.rayDirectionD) > 0)
        {
            this.planeNormal.negate();
        }

        double distance = Intersectiond.intersectRayPlane(this.rayOrigin, this.rayDirectionD, this.planeOrigin, this.planeNormal, 1.0E-6D);

        if (!Double.isFinite(distance) || distance < 0D)
        {
            return false;
        }

        out.set(this.rayOrigin).fma(distance, this.rayDirectionD);

        return true;
    }

    private void extractAxisWorld(Axis axis, Vector3f out)
    {
        if (axis == Axis.X)
        {
            out.set(1F, 0F, 0F);
        }
        else if (axis == Axis.Y)
        {
            out.set(0F, 1F, 0F);
        }
        else
        {
            out.set(0F, 0F, 1F);
        }

        this.rayGizmoMatrix.transformDirection(out);
        this.normalizeSafe(out);
    }

    /** Max world-space units a single-axis translate/scale ray value is allowed to change
     * in one frame; guards against grazing-angle ray/plane instability causing a sudden spike. */
    private static final float MAX_RAY_AXIS_JUMP = 8F;

    /** Max degrees a rotation ring is allowed to spin in a single frame, for the same reason. */
    private static final float MAX_RING_ANGLE_JUMP = 45F;

    private static final float MAX_RING_ANGLE_JUMP_RAD = (float) Math.toRadians(MAX_RING_ANGLE_JUMP);

    private static final double MAX_RAY_POINT_JUMP = 8D;

    private static final double MAX_RAY_POINT_JUMP_SQ = MAX_RAY_POINT_JUMP * MAX_RAY_POINT_JUMP;

    private void requestRayDragReanchor()
    {
        this.rayDragReanchor = true;
        this.rayDragInitialized = false;
        this.raySphereDragInitialized = false;
    }

    private boolean shouldReanchorMouseDrag(UIContext context, int dx, int dy)
    {
        int limit = Math.max(120, Math.min(context.menu.width, context.menu.height) / 3);

        return Math.abs(dx) > limit || Math.abs(dy) > limit;
    }

    private boolean isRayPointJumpTooLarge()
    {
        double dx = this.rayCurrentPoint.x - this.rayLastPoint.x;
        double dy = this.rayCurrentPoint.y - this.rayLastPoint.y;
        double dz = this.rayCurrentPoint.z - this.rayLastPoint.z;

        return dx * dx + dy * dy + dz * dz > MAX_RAY_POINT_JUMP_SQ;
    }

    private boolean normalizeSafe(Vector3f vector)
    {
        float lengthSquared = vector.lengthSquared();

        if (lengthSquared <= 1.0E-12F)
        {
            return false;
        }

        vector.mul((float) (1D / Math.sqrt(lengthSquared)));

        return true;
    }

    private void addAxisDelta(Vector3f vector, Axis axis, float delta)
    {
        if (axis == Axis.X)
        {
            vector.x += delta;
        }
        else if (axis == Axis.Y)
        {
            vector.y += delta;
        }
        else
        {
            vector.z += delta;
        }
    }

    private double computeAxisValue(Vector3d origin, Vector3f direction, Vector3f axisDirection)
    {
        double ux = direction.x;
        double uy = direction.y;
        double uz = direction.z;
        double vx = axisDirection.x;
        double vy = axisDirection.y;
        double vz = axisDirection.z;

        double wx = origin.x - this.rayGizmoOrigin.x;
        double wy = origin.y - this.rayGizmoOrigin.y;
        double wz = origin.z - this.rayGizmoOrigin.z;

        double b = ux * vx + uy * vy + uz * vz;
        double d = ux * wx + uy * wy + uz * wz;
        double e = vx * wx + vy * wy + vz * wz;
        double denom = 1D - b * b;

        if (Math.abs(denom) <= 1.0E-8D)
        {
            return e;
        }

        return (e - b * d) / denom;
    }

    public static class UITransformHandler extends UIElement
    {
        private UIPropTransform transform;

        public UITransformHandler(UIPropTransform transform)
        {
            this.transform = transform;
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (this.transform.editing)
            {
                if (context.mouseButton == 0)
                {
                    this.transform.acceptChanges();

                    return true;
                }
                else if (context.mouseButton == 1)
                {
                    this.transform.rejectChanges();

                    return true;
                }
            }
            
            return super.subMouseClicked(context);
        }

        @Override
        protected boolean subMouseScrolled(UIContext context)
        {
            UITrackpad.updateAmplifier(context);

            return true;
        }
    }
}
