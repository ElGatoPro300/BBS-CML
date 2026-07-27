package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import net.minecraft.class_287;
import net.minecraft.class_4587;
import net.minecraft.class_7833;
import org.joml.Vector3f;

public interface ICubicRenderer
{
    public static void offsetGroup(class_4587 stack, ModelGroup group)
    {
        Vector3f offset = group.offset;

        if (offset != null)
        {
            stack.method_46416(offset.x, offset.y, offset.z);
        }
    }

    public static void translateGroup(class_4587 stack, ModelGroup group)
    {
        Vector3f translate = group.current.translate;
        Vector3f pivot = group.current.pivot;

        stack.method_46416(-(translate.x - pivot.x) / 16F, (translate.y - pivot.y) / 16F, (translate.z - pivot.z) / 16F);
    }

    public static void moveToGroupPivot(class_4587 stack, ModelGroup group)
    {
        Vector3f pivot = group.current.pivot;

        stack.method_46416(pivot.x / 16F, pivot.y / 16F, pivot.z / 16F);
    }

    public static void rotateGroup(class_4587 stack, ModelGroup group)
    {
        if (group.orient != null)
        {
            stack.method_22907(group.orient);

            return;
        }

        if (group.current.rotate.z != 0F) stack.method_22907(class_7833.field_40718.rotation(MathUtils.toRad(group.current.rotate.z)));
        if (group.current.rotate.y != 0F) stack.method_22907(class_7833.field_40716.rotation(MathUtils.toRad(group.current.rotate.y)));
        if (group.current.rotate.x != 0F) stack.method_22907(class_7833.field_40714.rotation(MathUtils.toRad(group.current.rotate.x)));

        if (group.current.rotate2.z != 0F) stack.method_22907(class_7833.field_40718.rotation(MathUtils.toRad(group.current.rotate2.z)));
        if (group.current.rotate2.y != 0F) stack.method_22907(class_7833.field_40716.rotation(MathUtils.toRad(group.current.rotate2.y)));
        if (group.current.rotate2.x != 0F) stack.method_22907(class_7833.field_40714.rotation(MathUtils.toRad(group.current.rotate2.x)));
    }

    public static void scaleGroup(class_4587 stack, ModelGroup group)
    {
        Vector3f scale = group.current.scale;

        MatrixStackUtils.scaleStack(stack, scale.x, scale.y, scale.z);
    }

    public static void moveBackFromGroupPivot(class_4587 stack, ModelGroup group)
    {
        Vector3f pivot = group.current.pivot;

        stack.method_46416(-pivot.x / 16F, -pivot.y / 16F, -pivot.z / 16F);
    }

    public default void applyGroupTransformations(class_4587 stack, ModelGroup group)
    {
        offsetGroup(stack, group);
        translateGroup(stack, group);
        moveToGroupPivot(stack, group);
        rotateGroup(stack, group);
        scaleGroup(stack, group);
        moveBackFromGroupPivot(stack, group);
    }

    public boolean renderGroup(class_287 builder, class_4587 stack, ModelGroup group, Model model);
}
