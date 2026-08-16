package mchorse.bbs_mod.camera;

import mchorse.bbs_mod.utils.Factor;
import mchorse.bbs_mod.utils.MathUtils;
import org.joml.Matrix3f;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class OrbitDistanceCamera extends OrbitCamera
{
    public final Factor distance = new Factor();

    @Override
    public Vector3d getFinalPosition()
    {
        return this.finalPosition.set(this.position).add(this.rotateVector(0, 0, -1).mul((float) this.distance.getValue()));
    }

    @Override
    protected Vector3f rotateVector(float x, float y, float z)
    {
        Matrix3f rotation = new Matrix3f();
        Vector3f rotate = new Vector3f(x, y, z);

        rotation.rotateY(MathUtils.PI - this.rotation.y);
        rotation.rotateX(this.rotation.x);
        rotation.transform(rotate);

        return rotate;
    }

    @Override
    public boolean scroll(int scroll)
    {
        if (this.dragging >= 0)
        {
            return false;
        }

        int factor = this.distance.getX();

        this.distance.addX(scroll);

        return this.distance.getX() != factor;
    }
}