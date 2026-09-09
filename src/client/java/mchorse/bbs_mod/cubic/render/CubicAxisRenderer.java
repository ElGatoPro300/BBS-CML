package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

public class CubicAxisRenderer implements ICubicRenderer
{
    private Vector4f vector = new Vector4f();

    @Override
    public boolean renderGroup(BufferBuilder builder, PoseStack stack, ModelGroup group, Model model)
    {
        stack.pushPose();
        stack.translate(group.current.pivot.x / 16, group.current.pivot.y / 16, group.current.pivot.z / 16);

        Matrix4f matrix = stack.last().pose();
        float f = 0.1F;

        matrix.transform(this.vector.set(0, 0, 0, 1));
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(1, 0, 0, 1);

        matrix.transform(this.vector.set(f, 0, 0, 1));
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(1, 0, 0, 1);

        matrix.transform(this.vector.set(0, 0, 0, 1));
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 1, 0, 1);

        matrix.transform(this.vector.set(0, f, 0, 1));
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 1, 0, 1);

        matrix.transform(this.vector.set(0, 0, 0, 1));
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 0, 1, 1);

        matrix.transform(this.vector.set(0, 0, f, 1));
        builder.addVertex(matrix, this.vector.x, this.vector.y, this.vector.z).setColor(0, 0, 1, 1);

        stack.popPose();

        return false;
    }
}
