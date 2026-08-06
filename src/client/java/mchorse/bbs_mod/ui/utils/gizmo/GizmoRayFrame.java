package mchorse.bbs_mod.ui.utils.gizmo;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.ui.film.replays.FilmPoseGizmoDrag;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.MathUtils;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.Supplier;

/**
 * Builds the common "perspective camera + viewport area -> mouse ray" gizmo ray provider shared
 * by the Model and Form editors, replacing their two byte-for-byte identical anonymous
 * {@link UIPropTransform.IGizmoRayProvider} implementations.
 *
 * Film/Replays editors use {@link #fromFilmWorld(Camera, Area, Supplier)} so the ray origin
 * is the real camera position (not the origin) and the gizmo matrix is expressed in world space.
 */
public final class GizmoRayFrame
{
    private GizmoRayFrame()
    {}

    public static UIPropTransform.IGizmoRayProvider fromCamera(Camera camera, Area area, Supplier<Matrix4f> gizmoMatrix)
    {
        return new UIPropTransform.IGizmoRayProvider()
        {
            @Override
            public boolean getMouseRay(UIContext context, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
            {
                if (area.w <= 0 || area.h <= 0)
                {
                    return false;
                }

                int vx = context.globalX(area.x);
                int vy = context.globalY(area.y);
                int clampedX = MathUtils.clamp(mouseX, vx, vx + area.w - 1);
                int clampedY = MathUtils.clamp(mouseY, vy, vy + area.h - 1);

                Vector3f direction = camera.getMouseDirection(
                    clampedX,
                    clampedY,
                    vx,
                    vy,
                    area.w,
                    area.h
                );

                if (direction.lengthSquared() <= 1.0E-12F)
                {
                    return false;
                }

                rayDirection.set(direction).normalize();
                rayOrigin.set(camera.position.x, camera.position.y, camera.position.z);

                return true;
            }

            @Override
            public boolean getGizmoMatrix(Matrix4f matrix)
            {
                Matrix4f source = gizmoMatrix.get();

                if (source == null)
                {
                    return false;
                }

                matrix.set(source);

                return true;
            }
        };
    }

    /**
     * Film/replay world gizmos: same mouse ray as {@link #fromCamera(Camera, Area, Supplier)},
     * but converts the captured camera-relative gizmo matrix into world space for drag math.
     */
    public static UIPropTransform.IGizmoRayProvider fromFilmWorld(Camera camera, Area area, Supplier<Matrix4f> cameraRelativeMatrix)
    {
        return fromCamera(camera, area, () ->
        {
            Matrix4f relative = cameraRelativeMatrix.get();

            if (relative == null)
            {
                return null;
            }

            return GizmoMatrixUtils.cameraRelativeToWorld(camera.view, relative);
        });
    }

    /**
     * Film pose view-ring / axis-handle drag for editors that capture a model-space gizmo matrix.
     * Premultiplies {@code camera.view} so view-space rays line up with
     * {@link FilmPoseGizmoDrag} handle math.
     */
    public static UIPropTransform.IGizmoRayProvider fromFilmPoseStyle(Camera camera, Area area, Supplier<Matrix4f> gizmoMatrix)
    {
        return fromFilmStyle(camera, area, () ->
        {
            Matrix4f source = gizmoMatrix.get();

            if (source == null)
            {
                return null;
            }

            return new Matrix4f(camera.view).mul(source);
        });
    }

    /**
     * Matches film replay gizmo drag ({@link FilmPoseGizmoDrag}): view-space rays through the
     * origin for every handle, including the trackball. The supplied matrix must already be in
     * the same view space as the drawn gizmo (full editor/world MV), not bone-local alone.
     */
    public static UIPropTransform.IGizmoRayProvider fromFilmStyle(Camera camera, Area area, Supplier<Matrix4f> gizmoMatrix)
    {
        return new UIPropTransform.IGizmoRayProvider()
        {
            @Override
            public boolean getMouseRay(UIContext context, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
            {
                if (area.w <= 0 || area.h <= 0)
                {
                    return false;
                }

                int vx = context.globalX(area.x);
                int vy = context.globalY(area.y);

                return GizmoRayFrame.fillViewSpaceMouseRay(camera.projection, mouseX, mouseY, vx, vy, area.w, area.h, rayOrigin, rayDirection);
            }

            @Override
            public boolean getGizmoMatrix(Matrix4f matrix)
            {
                Matrix4f source = gizmoMatrix.get();

                if (source == null)
                {
                    return false;
                }

                matrix.set(source);

                return true;
            }
        };
    }

    private static boolean fillViewSpaceMouseRay(Matrix4f projection, int mouseX, int mouseY, int vx, int vy, int vw, int vh, Vector3d rayOrigin, Vector3f rayDirection)
    {
        mouseX -= vx;
        mouseY -= vy;

        float w2 = vw / 2F;
        float h2 = vh / 2F;
        float x = (mouseX - w2) / w2;
        float y = (-mouseY + h2) / h2;

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

        if (direction.lengthSquared() <= 1.0E-12F)
        {
            return false;
        }

        direction.normalize();
        rayDirection.set(direction);
        rayOrigin.set(0D, 0D, 0D);

        return true;
    }
}
