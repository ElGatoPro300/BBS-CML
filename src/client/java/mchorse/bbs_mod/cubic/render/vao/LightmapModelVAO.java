package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.BBSShaders;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Static triangle mesh carrying a fixed, per-vertex baked lightmap value (e.g. a structure's block
 * light captured once when the structure form is built), instead of the single scalar light every
 * other {@link IModelVAO} draw applies uniformly.
 *
 * <p>Like {@link ModelVAO}, the 1.21.11 GPU pipeline rewrite removed the raw-GL VAO/VBO + ShaderProgram
 * bind this class used to hold; the geometry (with its baked per-vertex light) is kept on the CPU and
 * emitted into a BufferBuilder per draw, then drawn through
 * {@link BBSShaders#getModelLayer()} by {@link ModelVAORenderer}.</p>
 */
public class LightmapModelVAO implements IModelVAO
{
    private final ModelVAOData data;
    private final int[] lightData;

    public LightmapModelVAO(ModelVAOData data, int[] lightData)
    {
        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        this.upload(data, lightData);
        GL30.glBindVertexArray(currentVAO);
    }

    public void delete()
    {
        if (this.vao != 0)
        {
            GL30.glDeleteVertexArrays(this.vao);
            this.vao = 0;
        }
    }

    private void upload(ModelVAOData data, int[] lightData)
    {
        this.vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(this.vao);

        int vertexBuffer = GL30.glGenBuffers();
        int normalBuffer = GL30.glGenBuffers();
        int texCoordBuffer = GL30.glGenBuffers();
        int tangentsBuffer = GL30.glGenBuffers();
        int midTexCoordBuffer = GL30.glGenBuffers();
        int lightBuffer = GL30.glGenBuffers();

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vertexBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.vertices(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.POSITION, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, normalBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.normals(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.NORMAL, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, texCoordBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.texCoords(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.TEXTURE_UV, 2, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, tangentsBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.tangents(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.TANGENTS, 4, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, midTexCoordBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.texCoords(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.MID_TEXTURE_UV, 2, GL30.GL_FLOAT, false, 0, 0);

        short[] light = new short[lightData.length * 2];
        for (int i = 0; i < lightData.length; i++)
        {
            int packed = lightData[i];
            light[i * 2] = (short) (packed & 0xFFFF);
            light[i * 2 + 1] = (short) ((packed >> 16) & 0xFFFF);
        }

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, lightBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, light, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribIPointer(Attributes.LIGHTMAP_UV, 2, GL30.GL_SHORT, 0, 0);

        GL30.glEnableVertexAttribArray(Attributes.POSITION);
        GL30.glEnableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glEnableVertexAttribArray(Attributes.NORMAL);
        GL30.glEnableVertexAttribArray(Attributes.LIGHTMAP_UV);

        GL30.glDisableVertexAttribArray(Attributes.COLOR);
        GL30.glDisableVertexAttribArray(Attributes.OVERLAY_UV);

        this.count = data.vertices().length / 3;
    }

    @Override
    public void delete()
    {}

    @Override
    public void writeImmediate(BufferBuilder builder, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        if (this.vao == 0 || !GL30.glIsVertexArray(this.vao))
        {
            return;
        }

        boolean hasShaders = BBSRendering.isIrisShadersEnabled();

        float[] vertices = this.data.vertices();
        float[] normals = this.data.normals();
        float[] texCoords = this.data.texCoords();

        Vector4f vertex = new Vector4f();
        Vector3f normal = new Vector3f();

        int count = vertices.length / 3;

        for (int i = 0; i < count; i++)
        {
            vertex.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2], 1F);
            position.transform(vertex);

            normal.set(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
            normalMatrix.transform(normal);

            int packed = i < this.lightData.length ? this.lightData[i] : light;
            int lu = packed & 0xffff;
            int lv = packed >> 16 & 0xffff;

            builder.vertex(vertex.x, vertex.y, vertex.z)
                .color(r, g, b, a)
                .texture(texCoords[i * 2], texCoords[i * 2 + 1])
                .overlay(overlay)
                .light(lu, lv)
                .normal(normal.x, normal.y, normal.z);
        }
    }
}
