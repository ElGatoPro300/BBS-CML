package mchorse.bbs_mod.graphics.line;

import net.minecraft.class_287;
import org.joml.Matrix4f;

public interface ILineRenderer <T>
{
    public void render(class_287 builder, Matrix4f matrix, LinePoint<T> point);
}