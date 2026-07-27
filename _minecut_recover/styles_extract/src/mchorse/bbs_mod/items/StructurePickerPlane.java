package mchorse.bbs_mod.items;

import net.minecraft.class_2338;
import net.minecraft.class_243;

public enum StructurePickerPlane
{
    XZ,
    VERTICAL;

    public static StructurePickerPlane fromMouseDrag(double dx, double dy)
    {
        if (Math.abs(dy) > Math.abs(dx))
        {
            return StructurePickerPlane.VERTICAL;
        }

        return StructurePickerPlane.XZ;
    }

    public class_2338 clampSecond(class_2338 first, class_2338 hovered, StructurePickerAxis verticalPlaneAxis)
    {
        if (hovered == null)
        {
            return first;
        }

        return switch (this)
        {
            case XZ -> new class_2338(hovered.method_10263(), first.method_10264(), hovered.method_10260());
            case VERTICAL -> StructurePickerPlane.clampVertical(first, hovered, verticalPlaneAxis);
        };
    }

    private static class_2338 clampVertical(class_2338 first, class_2338 hovered, StructurePickerAxis lockedHorizontal)
    {
        if (lockedHorizontal == StructurePickerAxis.X)
        {
            return new class_2338(hovered.method_10263(), hovered.method_10264(), first.method_10260());
        }

        return new class_2338(first.method_10263(), hovered.method_10264(), hovered.method_10260());
    }

    public void applyDepth(class_2338 slabMin, class_2338 slabMax, StructurePickerAxis axis, int depth, class_2338[] outCorners)
    {
        int near = Math.min(axis.read(slabMin), axis.read(slabMax));
        int far = Math.max(axis.read(slabMin), axis.read(slabMax));

        if (depth >= near)
        {
            outCorners[0] = StructurePickerPlane.cornerAtDepth(slabMin, slabMax, axis, near, false);
            outCorners[1] = StructurePickerPlane.cornerAtDepth(slabMin, slabMax, axis, depth, true);
        }
        else
        {
            outCorners[0] = StructurePickerPlane.cornerAtDepth(slabMin, slabMax, axis, depth, false);
            outCorners[1] = StructurePickerPlane.cornerAtDepth(slabMin, slabMax, axis, far, true);
        }
    }

    private static class_2338 cornerAtDepth(class_2338 slabMin, class_2338 slabMax, StructurePickerAxis axis, int depth, boolean positive)
    {
        int x = positive ? slabMax.method_10263() : slabMin.method_10263();
        int y = positive ? slabMax.method_10264() : slabMin.method_10264();
        int z = positive ? slabMax.method_10260() : slabMin.method_10260();

        return axis.write(new class_2338(x, y, z), depth);
    }

    public StructurePickerAxis defaultDepthAxis(class_243 look)
    {
        return switch (this)
        {
            case XZ -> StructurePickerAxis.Y;
            case VERTICAL -> StructurePickerAxis.pickHorizontal(look);
        };
    }

    public float depthSensitivity(StructurePickerAxis axis)
    {
        if (this == StructurePickerPlane.XZ && axis == StructurePickerAxis.Y)
        {
            return 48F;
        }

        return 36F;
    }
}
