package mchorse.bbs_mod.cubic.render.vao;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface IModelVAO
{
    public void render(VertexFormat format, float r, float g, float b, float a, int light, int overlay);
}
