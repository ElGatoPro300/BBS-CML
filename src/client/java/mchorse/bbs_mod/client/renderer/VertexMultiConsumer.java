package mchorse.bbs_mod.client.renderer;

import org.joml.Matrix4fc;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class VertexMultiConsumer
{
    public static VertexConsumer create(VertexConsumer first, VertexConsumer second)
    {
        if (first == null)
        {
            return second;
        }

        if (second == null)
        {
            return first;
        }

        return new Dual(first, second);
    }

    private static class Dual implements VertexConsumer
    {
        private final VertexConsumer first;
        private final VertexConsumer second;

        public Dual(VertexConsumer first, VertexConsumer second)
        {
            this.first = first;
            this.second = second;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z)
        {
            this.first.addVertex(x, y, z);
            this.second.addVertex(x, y, z);

            return this;
        }

        @Override
        public VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z)
        {
            this.first.addVertex(matrix, x, y, z);
            this.second.addVertex(matrix, x, y, z);

            return this;
        }

        @Override
        public VertexConsumer setColor(int color)
        {
            this.first.setColor(color);
            this.second.setColor(color);

            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha)
        {
            this.first.setColor(red, green, blue, alpha);
            this.second.setColor(red, green, blue, alpha);

            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v)
        {
            this.first.setUv(u, v);
            this.second.setUv(u, v);

            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v)
        {
            this.first.setUv1(u, v);
            this.second.setUv1(u, v);

            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v)
        {
            this.first.setUv2(u, v);
            this.second.setUv2(u, v);

            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ)
        {
            this.first.setNormal(normalX, normalY, normalZ);
            this.second.setNormal(normalX, normalY, normalZ);

            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width)
        {
            this.first.setLineWidth(width);
            this.second.setLineWidth(width);

            return this;
        }
    }
}
