package mchorse.bbs_mod.camera;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.joml.Matrices;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Camera
{
    public Matrix4f projection = new Matrix4f();
    public Matrix4f view = new Matrix4f();
    public float fov;
    public float near = 0.01F;
    public float far = 300F;

    public Vector3d position = new Vector3d();
    public Vector3f rotation = new Vector3f();

    private Vector3f relative = new Vector3f();

    public Camera()
    {
        this.setFov(70);
    }

    public void setFov(float degrees)
    {
        this.fov = MathUtils.toRad(degrees);
    }

    public void setFarNear(float near, float far)
    {
        this.near = near;
        this.far = far;
    }

    public Vector3f getLookDirection()
    {
        return Matrices.rotation(this.rotation.x, MathUtils.PI - this.rotation.y);
    }

    public Vector3f getMouseDirection(int mx, int my, int vx, int vy, int w, int h)
    {
        return CameraUtils.getMouseDirection(this.projection, this.view, mx, my, vx, vy, w, h);
    }

    public Vector3f getMouseDirectionFov(int mx, int my, int vx, int vy, int w, int h)
    {
        float nx = ((mx - vx) - w / 2F) / (w / 2F);
        float ny = (-(my - vy) + h / 2F) / (h / 2F);

        float aspect = w / (float) h;
        float tanHalfFov = (float) Math.tan(this.fov / 2F);

        Vector3f forward = Matrices.rotation(this.rotation.x, MathUtils.PI - this.rotation.y).normalize();
        Vector3f upWorld = new Vector3f(0F, 1F, 0F);

        Vector3f right = new Vector3f(forward).cross(upWorld).normalize();
        if (right.lengthSquared() < 1e-6f)
        {
            right.set(1F, 0F, 0F);
        }

        Vector3f upCam = new Vector3f(right).cross(forward).normalize();

        Vector3f dir = new Vector3f(forward)
            .add(new Vector3f(right).mul(nx * tanHalfFov * aspect))
            .add(new Vector3f(upCam).mul(ny * tanHalfFov))
            .normalize();

        return dir;
    }

    public Vector3f getMouseDirectionNormalized(float mx, float my)
    {
        return CameraUtils.getMouseDirection(this.projection, this.view, mx, my);
    }

    public Vector3f getRelative(Vector3d vector)
    {
        return this.getRelative(vector.x, vector.y, vector.z);
    }

    public Vector3f getRelative(double x, double y, double z)
    {
        return this.relative.set((float) (x - this.position.x), (float) (y - this.position.y), (float) (z - this.position.z));
    }

    public void updatePerspectiveProjection(int width, int height)
    {
        this.projection.identity().perspective(this.fov, width / (float) height, this.near, this.far);
    }

    public void updateOrthoProjection(int width, int height)
    {
        this.projection.identity().ortho(-width, width, -height, height, this.near, this.far);
    }

    public Matrix4f updateView()
    {
        return this.view.identity()
            .rotateZ(this.rotation.z)
            .rotateX(this.rotation.x)
            .rotateY(this.rotation.y);
    }

    public void copy(Camera camera)
    {
        this.projection.set(camera.projection);
        this.view.set(camera.view);
        this.fov = camera.fov;
        this.near = camera.near;
        this.far = camera.far;
        this.position.set(camera.position);
        this.rotation.set(camera.rotation);
    }

    public void set(Entity cameraEntity, float fov)
    {
        Vec3d eyePos = cameraEntity.getEyePos();

        this.position.set(eyePos.x, eyePos.y, eyePos.z);
        this.rotation.set(MathUtils.toRad(cameraEntity.getPitch()), MathUtils.toRad(cameraEntity.getHeadYaw() + 180F), 0);
        this.fov = fov;
    }
}
