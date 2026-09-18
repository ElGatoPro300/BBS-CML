package mchorse.bbs_mod.client.renderer;

import net.minecraft.client.resources.model.geometry.BakedQuad;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class VertexMultiConsumer implements VertexConsumer
{
    private final VertexConsumer first;
    private final VertexConsumer second;

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

        return new VertexMultiConsumer(first, second);
    }

    public VertexMultiConsumer(VertexConsumer first, VertexConsumer second)
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
    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        this.first.setColor(red, green, blue, alpha);
        this.second.setColor(red, green, blue, alpha);

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

    @Override
    public VertexConsumer setLight(int light)
    {
        this.first.setLight(light);
        this.second.setLight(light);

        return this;
    }

    @Override
    public VertexConsumer setOverlay(int overlay)
    {
        this.first.setOverlay(overlay);
        this.second.setOverlay(overlay);

        return this;
    }

    @Override
    public VertexConsumer setUv3(float u, float v)
    {
        this.first.setUv3(u, v);
        this.second.setUv3(u, v);

        return this;
    }

    @Override
    public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance quadInstance)
    {
        this.first.putBakedQuad(pose, quad, quadInstance);
        this.second.putBakedQuad(pose, quad, quadInstance);
    }
}
