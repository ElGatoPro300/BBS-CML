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
    private final int count;

    public LightmapModelVAO(ModelVAOData data, int[] lightData)
    {
        this.data = data;
        this.lightData = lightData;
        this.count = data.vertices().length / 3;
    }

    public void delete()
    {}

    @Override
    public void writeImmediate(BufferBuilder builder, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        Matrix4f position = stack.peek().getPositionMatrix();
        Matrix3f normalMatrix = stack.peek().getNormalMatrix();

        float[] vertices = this.data.vertices();
        float[] normals = this.data.normals();
        float[] texCoords = this.data.texCoords();

        Vector4f vertex = new Vector4f();
        Vector3f normal = new Vector3f();

        for (int i = 0; i < this.count; i++)
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
