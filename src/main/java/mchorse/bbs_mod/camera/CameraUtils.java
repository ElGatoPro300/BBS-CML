package mchorse.bbs_mod.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class CameraUtils
{
    public static Vector3f getMouseDirection(Matrix4f projection, Matrix4f view, int mx, int my, int vx, int vy, int vw, int vh)
    {
        mx -= vx;
        my -= vy;

        float w2 = vw / 2F;
        float h2 = vh / 2F;

        float x = (mx - w2) / w2;
        float y = (-my + h2) / h2;

        return getMouseDirection(projection, view, x, y);
    }

    public static Vector3f getMouseDirection(Matrix4f projection, Matrix4f view, float mx, float my)
    {
        Matrix4f matrix4f = new Matrix4f(projection);

        matrix4f.mul(view);
        matrix4f.invert();

        Vector4f forward = new Vector4f(mx, my, 0, 1);

        matrix4f.transform(forward);

        return new Vector3f(forward.x, forward.y, forward.z);
    }
}
