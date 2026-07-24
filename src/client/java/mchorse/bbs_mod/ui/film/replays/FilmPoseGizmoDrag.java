package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Film pose gizmo drag frame. Axis rings / translate / scale handles and the trackball sphere
 * all use view-space rays plus the captured pass matrix so drag math matches the drawn gizmo.
 */
public final class FilmPoseGizmoDrag
{
    /* Orbit (and any live camera that follows the edited transform) must not update the
     * drag frame mid-stroke — otherwise rotate → camera/gizmo move → huge ray deltas. */
    private static final Matrix4f FROZEN_DRAG_GIZMO = new Matrix4f();
    private static final Matrix4f FROZEN_DRAG_VIEW = new Matrix4f();
    private static boolean hasFrozenDragFrame;

    private FilmPoseGizmoDrag()
    {}

    public static void prepare(UIFilmPanel panel, Area area, UIPropTransform transform)
    {
        if (panel == null || area == null || transform == null)
        {
            return;
        }

        if (!Gizmo.INSTANCE.isDragging())
        {
            FilmPoseGizmoDrag.hasFrozenDragFrame = false;
        }

        final Camera camera = panel.getCamera();

        if (camera == null)
        {
            return;
        }

        UIKeyframeEditor keyframeEditor = panel.replayEditor.keyframeEditor;
        boolean poseGizmo = keyframeEditor != null && keyframeEditor.editor instanceof UIPoseKeyframeFactory;
        boolean poseLimbGizmo = false;
        boolean replayTransformGizmo = false;
        boolean anchorTrackGizmo = false;

        if (keyframeEditor != null && keyframeEditor.editor instanceof UITransformKeyframeFactory transformFactory)
        {
            UIKeyframeSheet sheet = keyframeEditor.getSheet(transformFactory.getKeyframe());

            poseLimbGizmo = UITransformKeyframeFactory.isPoseLimbTrack(sheet);
            replayTransformGizmo = !poseLimbGizmo;
        }
        else if (keyframeEditor != null && keyframeEditor.editor instanceof UIAnchorKeyframeFactory)
        {
            anchorTrackGizmo = true;
        }

        boolean filmTransformGizmo = replayTransformGizmo || anchorTrackGizmo;
        boolean bobjModel = FilmPoseGizmoDrag.isBobjReplay(panel);

        if (poseGizmo)
        {
            transform.configurePoseRingTuning(bobjModel);

            /* White ring + sphere: always use .bbs.json (cubic) sense. BOBJ lacks the
             * bone-local Ry(180°) mirror, so it also gets the cubic X/Z trackball euler flip. */
            transform.setInvertGizmoViewRing(true);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.clearTrackballEulerInverts();

            if (bobjModel)
            {
                transform.invertModelPoseTrackballXZ();
            }

            transform.setInvertTrackballDragY(false);
            transform.setInvertFilmArcballDragY(false);
            transform.setFilmArcballTrackball(true);
            transform.setFilmMatchPoseTrackball(false);
            transform.setInvertRotationArcSweep(false);
            transform.setInvertRotationArcViewRing(false);
            /* Y/Z process bars wind opposite arc3D with filmArcball (X keeps the X/Z undo). */
            transform.setInvertRotationArcY(true);
            transform.setInvertRotationArcZ(true);
            transform.setForceFrozenRotationArc(false);

            /* Pose editor may be built before the model is known (scale defaults to 16). */
            transform.translationScale(bobjModel ? 1F : 16F);
            transform.setAxisProjectedTranslation(bobjModel);
        }
        else if (poseLimbGizmo)
        {
            transform.configurePoseLimbRingTuning(bobjModel);

            transform.setInvertGizmoViewRing(false);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.setFilmArcballTrackball(false);
            transform.clearTrackballEulerInverts();
            transform.setInvertTrackballDragY(false);
            transform.setInvertRotationArcViewRing(false);
            transform.setInvertRotationArcY(false);
            transform.setInvertRotationArcZ(false);
            transform.setForceFrozenRotationArc(false);

            if (!bobjModel)
            {
                transform.invertModelPoseTrackballXZ();
            }

            /* Limb sheets have null formProperty, so the factory often kept cubic's /16 scale
             * on BOBJ — force block units + axis-projected rays so drag tracks the cursor. */
            transform.translationScale(bobjModel ? 1F : 16F);
            transform.setAxisProjectedTranslation(bobjModel);
        }
        else if (anchorTrackGizmo)
        {
            transform.anchorGizmoTuning();
            transform.setInvertGizmoViewRing(false);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.setFilmArcballTrackball(false);
            transform.clearTrackballEulerInverts();
            transform.setInvertTrackballDragY(true);
            transform.setInvertFilmArcballDragY(false);
            transform.setInvertRotationArcSweep(false);
            /* White ring value is correct; only its process bar winds the wrong way. */
            transform.setInvertRotationArcViewRing(true);
            /* Y value follows the mouse; only the process bar winds the wrong way without this. */
            transform.setInvertRotationArcY(true);
            /* Z value follows the mouse; only the process bar winds the wrong way without this. */
            transform.setInvertRotationArcZ(true);
            /* Global Anchor still spins the live gizmo matrix — freeze the process bar like Local. */
            transform.setForceFrozenRotationArc(true);
        }
        else if (replayTransformGizmo)
        {
            transform.setInvertGizmoViewRing(false);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.setFilmArcballTrackball(false);
            transform.clearTrackballEulerInverts();
            /* Film view-space screen basis: without this, vertical trackball opposes the mouse. */
            transform.setInvertTrackballDragY(true);
            transform.setInvertFilmArcballDragY(false);
            transform.setInvertRotationArcSweep(false);
            /* White ring value is correct; only its process bar winds the wrong way. */
            transform.setInvertRotationArcViewRing(true);
            /* Y ring value follows the mouse; only the green process bar winds the wrong way. */
            transform.setInvertRotationArcY(true);
            transform.setInvertRotationArcZ(false);
            transform.setForceFrozenRotationArc(false);
            transform.configurePoseRingTuning(true);
        }
        else
        {
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.setFilmArcballTrackball(false);
            transform.clearTrackballEulerInverts();
            transform.setInvertTrackballDragY(filmTransformGizmo);
            transform.setInvertFilmArcballDragY(false);
            transform.setInvertRotationArcY(false);
            transform.setInvertRotationArcZ(false);
            transform.setInvertRotationArcViewRing(false);
        }

        transform.setFilmMatchPoseTrackball(filmTransformGizmo);

        transform.setGizmoRayProvider(new UIPropTransform.IGizmoRayProvider()
        {
            @Override
            public boolean getMouseRay(UIContext context, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
            {
                if (area.w <= 0 || area.h <= 0)
                {
                    return false;
                }

                FilmPoseGizmoDrag.syncDragCamera(panel, camera);

                int vx = context.globalX(area.x);
                int vy = context.globalY(area.y);

                return FilmPoseGizmoDrag.fillHandleMouseRay(camera, vx, vy, area.w, area.h, mouseX, mouseY, rayOrigin, rayDirection);
            }

            @Override
            public boolean getGizmoMatrix(Matrix4f matrix)
            {
                return FilmPoseGizmoDrag.fillHandleGizmoMatrix(panel, matrix);
            }
        });
    }

    private static boolean fillHandleMouseRay(Camera camera, int vx, int vy, int vw, int vh, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
    {
        Vector3f direction = getViewSpaceMouseDirection(
            camera.projection,
            mouseX,
            mouseY,
            vx,
            vy,
            vw,
            vh
        );

        if (direction.lengthSquared() <= 1.0E-12F)
        {
            return false;
        }

        rayDirection.set(direction).normalize();
        rayOrigin.set(0D, 0D, 0D);

        return true;
    }

    private static boolean fillHandleGizmoMatrix(UIFilmPanel panel, Matrix4f matrix)
    {
        if (panel == null || !panel.hasLastGizmoMatrix)
        {
            return false;
        }

        if (Gizmo.INSTANCE.isDragging())
        {
            FilmPoseGizmoDrag.ensureFrozenDragFrame(panel);
            matrix.set(FilmPoseGizmoDrag.FROZEN_DRAG_GIZMO);

            return FilmPoseGizmoDrag.hasFrozenDragFrame;
        }

        /* Same depth-based composition as the visual pass, so drags grab the
         * handle exactly where it is drawn regardless of the shader path. */
        Gizmo.composeVisualMatrix(panel.lastGizmoMatrix, BBSRendering.camera, panel.lastProjection, matrix);

        return true;
    }

    private static void ensureFrozenDragFrame(UIFilmPanel panel)
    {
        if (FilmPoseGizmoDrag.hasFrozenDragFrame || panel == null || !panel.hasLastGizmoMatrix)
        {
            return;
        }

        /* Same depth-based composition as the visual pass, snapshotted once at drag
         * start so orbit / transform feedback cannot spin the ray frame. */
        Gizmo.composeVisualMatrix(panel.lastGizmoMatrix, BBSRendering.camera, panel.lastProjection, FilmPoseGizmoDrag.FROZEN_DRAG_GIZMO);
        FilmPoseGizmoDrag.FROZEN_DRAG_VIEW.set(panel.lastView);
        FilmPoseGizmoDrag.hasFrozenDragFrame = true;
    }

    private static Vector3f getViewSpaceMouseDirection(Matrix4f projection, int mx, int my, int vx, int vy, int vw, int vh)
    {
        mx -= vx;
        my -= vy;

        float w2 = vw / 2F;
        float h2 = vh / 2F;
        float x = (mx - w2) / w2;
        float y = (-my + h2) / h2;

        Matrix4f inverseProjection = new Matrix4f(projection).invert();
        Vector4f forward = new Vector4f(x, y, 0F, 1F).mul(inverseProjection);

        if (Math.abs(forward.w) > 1.0E-6F)
        {
            float invW = 1F / forward.w;

            forward.x *= invW;
            forward.y *= invW;
            forward.z *= invW;
        }

        Vector3f direction = new Vector3f(forward.x, forward.y, forward.z);

        direction.normalize();

        return direction;
    }

    public static void syncDragCamera(UIFilmPanel panel, Camera camera)
    {
        camera.copy(panel.getWorldCamera());

        if (Gizmo.INSTANCE.isDragging())
        {
            FilmPoseGizmoDrag.ensureFrozenDragFrame(panel);
        }

        if (FilmPoseGizmoDrag.hasFrozenDragFrame && Gizmo.INSTANCE.isDragging())
        {
            camera.view.set(FilmPoseGizmoDrag.FROZEN_DRAG_VIEW);
        }
        else
        {
            camera.view.set(panel.lastView);
        }

        camera.projection.set(panel.lastProjection);
    }

    private static boolean isBobjReplay(UIFilmPanel panel)
    {
        if (panel == null || panel.replayEditor == null)
        {
            return false;
        }

        Replay replay = panel.replayEditor.getReplay();

        if (replay == null)
        {
            return false;
        }

        Form form = FormUtils.getRoot(replay.form.get());

        return form instanceof ModelForm modelForm && ModelFormRenderer.isBobjModel(modelForm);
    }
}
