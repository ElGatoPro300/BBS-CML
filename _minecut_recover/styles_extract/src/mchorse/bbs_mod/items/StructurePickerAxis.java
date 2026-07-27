package mchorse.bbs_mod.items;

import net.minecraft.class_2338;
import net.minecraft.class_243;

public enum StructurePickerAxis
{
    X,
    Y,
    Z;

    public int read(class_2338 pos)
    {
        return switch (this)
        {
            case X -> pos.method_10263();
            case Y -> pos.method_10264();
            case Z -> pos.method_10260();
        };
    }

    public class_2338 write(class_2338 pos, int value)
    {
        return switch (this)
        {
            case X -> new class_2338(value, pos.method_10264(), pos.method_10260());
            case Y -> new class_2338(pos.method_10263(), value, pos.method_10260());
            case Z -> new class_2338(pos.method_10263(), pos.method_10264(), value);
        };
    }

    public double readLook(class_243 look)
    {
        return switch (this)
        {
            case X -> look.field_1352;
            case Y -> look.field_1351;
            case Z -> look.field_1350;
        };
    }

    public static StructurePickerAxis pickHorizontal(class_243 look)
    {
        if (Math.abs(look.field_1352) >= Math.abs(look.field_1350))
        {
            return StructurePickerAxis.X;
        }

        return StructurePickerAxis.Z;
    }
}
