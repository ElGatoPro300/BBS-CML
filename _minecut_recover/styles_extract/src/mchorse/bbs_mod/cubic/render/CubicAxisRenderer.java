package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import net.minecraft.class_287;
import net.minecraft.class_4587;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class CubicAxisRenderer implements ICubicRenderer
{
    private Vector4f vector = new Vector4f();

    @Override
    public boolean renderGroup(class_287 builder, class_4587 stack, ModelGroup group, Model model)
    {
        stack.method_22903();
        stack.method_46416(group.current.pivot.x / 16, group.current.pivot.y / 16, group.current.pivot.z / 16);

        Matrix4f matrix = stack.method_23760().method_23761();
        float f = 0.1F;

        matrix.transform(this.vector.set(0, 0, 0, 1));
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_1336(1, 0, 0, 1);

        matrix.transform(this.vector.set(f, 0, 0, 1));
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_1336(1, 0, 0, 1);

        matrix.transform(this.vector.set(0, 0, 0, 1));
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_1336(0, 1, 0, 1);

        matrix.transform(this.vector.set(0, f, 0, 1));
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_1336(0, 1, 0, 1);

        matrix.transform(this.vector.set(0, 0, 0, 1));
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_1336(0, 0, 1, 1);

        matrix.transform(this.vector.set(0, 0, f, 1));
        builder.method_22918(matrix, this.vector.x, this.vector.y, this.vector.z).method_1336(0, 0, 1, 1);

        stack.method_22909();

        return false;
    }
}
