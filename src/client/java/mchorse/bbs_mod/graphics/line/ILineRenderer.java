package mchorse.bbs_mod.graphics.line;

import org.joml.Matrix3x2fc;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface ILineRenderer <T>
{
    public void render(VertexConsumer builder, Matrix3x2fc matrix, LinePoint<T> point);
}