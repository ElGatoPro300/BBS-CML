package mchorse.bbs_mod.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public enum StructurePickerAxis
{
    X,
    Y,
    Z;

    public int read(BlockPos pos)
    {
        return switch (this)
        {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    public BlockPos write(BlockPos pos, int value)
    {
        return switch (this)
        {
            case X -> new BlockPos(value, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), value, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), value);
        };
    }

    public double readLook(Vec3 look)
    {
        return switch (this)
        {
            case X -> look.x;
            case Y -> look.y;
            case Z -> look.z;
        };
    }

    public static StructurePickerAxis pickHorizontal(Vec3 look)
    {
        if (Math.abs(look.x) >= Math.abs(look.z))
        {
            return StructurePickerAxis.X;
        }

        return StructurePickerAxis.Z;
    }
}
