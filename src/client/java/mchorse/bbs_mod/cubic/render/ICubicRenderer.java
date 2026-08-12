package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public interface ICubicRenderer
{
    public static void offsetGroup(PoseStack stack, ModelGroup group)
    {
        Vector3f offset = group.offset;

        if (offset != null)
        {
            stack.translate(offset.x, offset.y, offset.z);
        }
    }

    public static void translateGroup(PoseStack stack, ModelGroup group)
    {
        Vector3f translate = group.current.translate;
        Vector3f pivot = group.current.pivot;

        stack.translate(-(translate.x - pivot.x) / 16F, (translate.y - pivot.y) / 16F, (translate.z - pivot.z) / 16F);
    }

    public static void moveToGroupPivot(PoseStack stack, ModelGroup group)
    {
        Vector3f pivot = group.current.pivot;

        stack.translate(pivot.x / 16F, pivot.y / 16F, pivot.z / 16F);
    }

    public static void rotateGroup(PoseStack stack, ModelGroup group)
    {
        if (group.orient != null)
        {
            stack.mulPose(group.orient);

            return;
        }

        if (group.current.rotate.z != 0F) stack.mulPose(Axis.ZP.rotation(MathUtils.toRad(group.current.rotate.z)));
        if (group.current.rotate.y != 0F) stack.mulPose(Axis.YP.rotation(MathUtils.toRad(group.current.rotate.y)));
        if (group.current.rotate.x != 0F) stack.mulPose(Axis.XP.rotation(MathUtils.toRad(group.current.rotate.x)));

        if (group.current.rotate2.z != 0F) stack.mulPose(Axis.ZP.rotation(MathUtils.toRad(group.current.rotate2.z)));
        if (group.current.rotate2.y != 0F) stack.mulPose(Axis.YP.rotation(MathUtils.toRad(group.current.rotate2.y)));
        if (group.current.rotate2.x != 0F) stack.mulPose(Axis.XP.rotation(MathUtils.toRad(group.current.rotate2.x)));
    }

    public static void scaleGroup(PoseStack stack, ModelGroup group)
    {
        Vector3f scale = group.current.scale;

        MatrixStackUtils.scaleStack(stack, scale.x, scale.y, scale.z);
    }

    public static void moveBackFromGroupPivot(PoseStack stack, ModelGroup group)
    {
        Vector3f pivot = group.current.pivot;

        stack.translate(-pivot.x / 16F, -pivot.y / 16F, -pivot.z / 16F);
    }

    public default void applyGroupTransformations(PoseStack stack, ModelGroup group)
    {
        offsetGroup(stack, group);
        translateGroup(stack, group);
        moveToGroupPivot(stack, group);
        rotateGroup(stack, group);
        scaleGroup(stack, group);
        moveBackFromGroupPivot(stack, group);
    }

    public boolean renderGroup(BufferBuilder builder, PoseStack stack, ModelGroup group, Model model);
}
