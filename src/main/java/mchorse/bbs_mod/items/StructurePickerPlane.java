package mchorse.bbs_mod.items;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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

    public BlockPos clampSecond(BlockPos first, BlockPos hovered, StructurePickerAxis verticalPlaneAxis)
    {
        if (hovered == null)
        {
            return first;
        }

        return switch (this)
        {
            case XZ -> new BlockPos(hovered.getX(), first.getY(), hovered.getZ());
            case VERTICAL -> StructurePickerPlane.clampVertical(first, hovered, verticalPlaneAxis);
        };
    }

    private static BlockPos clampVertical(BlockPos first, BlockPos hovered, StructurePickerAxis lockedHorizontal)
    {
        if (lockedHorizontal == StructurePickerAxis.X)
        {
            return new BlockPos(hovered.getX(), hovered.getY(), first.getZ());
        }

        return new BlockPos(first.getX(), hovered.getY(), hovered.getZ());
    }

    public void applyDepth(BlockPos slabMin, BlockPos slabMax, StructurePickerAxis axis, int depth, BlockPos[] outCorners)
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

    private static BlockPos cornerAtDepth(BlockPos slabMin, BlockPos slabMax, StructurePickerAxis axis, int depth, boolean positive)
    {
        int x = positive ? slabMax.getX() : slabMin.getX();
        int y = positive ? slabMax.getY() : slabMin.getY();
        int z = positive ? slabMax.getZ() : slabMin.getZ();

        return axis.write(new BlockPos(x, y, z), depth);
    }

    public StructurePickerAxis defaultDepthAxis(Vec3d look)
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
