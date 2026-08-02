package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.BBSShaders;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Static (non-skinned) triangle mesh used by cubic Models (VAO path) and extruded forms.
 *
 * <p>The 1.21.11 GPU pipeline rewrite removed the raw-GL VAO + ShaderProgram.bind()/unbind() draw this
 * class used to hold (there is no supported way to bind a raw GL vertex array against the new
 * RenderPipeline/RenderPass abstraction). The geometry is now kept on the CPU and emitted into a
 * BufferBuilder per draw ({@link #writeImmediate}), exactly like the cubic immediate path, then drawn
 * through {@link BBSShaders#getModelLayer()} by {@link ModelVAORenderer}.</p>
 */
public class ModelVAO implements IModelVAO
{
    private final ModelVAOData data;

    public ModelVAO(ModelVAOData data)
    {
        this.data = data;
    }

    /**
     * Previously freed the raw-GL VAOs. Geometry now lives on the CPU, so there is nothing to free.
     */
    public void delete()
    {}

    /**
     * Bake the stack position/normal matrices into each vertex CPU-side and write the triangles into
     * {@code builder} (format POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL). Color/overlay/light are
     * constant per draw (matching the old {@code glVertexAttrib4f}/{@code glVertexAttribI2i} defaults).
     */
    public void writeImmediate(BufferBuilder builder, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        if (this.vao != 0)
        {
            GL30.glDeleteVertexArrays(this.vao);
            this.vao = 0;
        }

        if (this.vao2 != 0)
        {
            GL30.glDeleteVertexArrays(this.vao2);
            this.vao2 = 0;
        }
    }

        float[] vertices = this.data.vertices();
        float[] normals = this.data.normals();
        float[] texCoords = this.data.texCoords();

        Vector4f vertex = new Vector4f();
        Vector3f normal = new Vector3f();

        int lu = light & 0xffff;
        int lv = light >> 16 & 0xffff;

        int count = vertices.length / 3;

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

        GL30.glEnableVertexAttribArray(Attributes.POSITION);
        GL30.glEnableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glEnableVertexAttribArray(Attributes.NORMAL);

        GL30.glDisableVertexAttribArray(Attributes.COLOR);
        GL30.glDisableVertexAttribArray(Attributes.OVERLAY_UV);
        GL30.glDisableVertexAttribArray(Attributes.LIGHTMAP_UV);
        GL30.glDisableVertexAttribArray(Attributes.TANGENTS);
        GL30.glDisableVertexAttribArray(Attributes.MID_TEXTURE_UV);

        /* VertexFormats.POSITION_TEXTURE_LIGHT_COLOR */
        GL30.glBindVertexArray(this.vao2);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vertexBuffer);
        GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, texCoordBuffer);
        GL30.glVertexAttribPointer(1, 2, GL30.GL_FLOAT, false, 0, 0);

        GL30.glEnableVertexAttribArray(0);
        GL30.glEnableVertexAttribArray(1);
        GL30.glDisableVertexAttribArray(2);
        GL30.glDisableVertexAttribArray(3);

        this.count = data.vertices().length / 3;
    }

    @Override
    public void render(VertexFormat format, float r, float g, float b, float a, int light, int overlay)
    {
        boolean hasShaders = isShadersEnabled();
        int vao = hasShaders || format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL ? this.vao : this.vao2;

        if (vao == 0 || !GL30.glIsVertexArray(vao))
        {
            return;
        }

        GL30.glBindVertexArray(vao);

        if (vao == this.vao)
        {
            vertex.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2], 1F);
            position.transform(vertex);

            normal.set(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
            normalMatrix.transform(normal);

            builder.vertex(vertex.x, vertex.y, vertex.z)
                .color(r, g, b, a)
                .texture(texCoords[i * 2], texCoords[i * 2 + 1])
                .overlay(overlay)
                .light(lu, lv)
                .normal(normal.x, normal.y, normal.z);
        }
    }
}
