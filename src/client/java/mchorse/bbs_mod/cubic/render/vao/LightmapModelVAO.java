package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.render.VertexFormat;

import org.lwjgl.opengl.GL30;

/**
 * Structure VAO with per-vertex lightmap. When {@link ModelVAOData#colors()} is present,
 * foliage / glass vertex colors are stored as a base buffer and multiplied by the form tint
 * each draw (Iris reads the COLOR attribute; ColorModulator alone is unreliable in gbuffer).
 */
public class LightmapModelVAO implements IModelVAO
{
    private int vao;
    private int count;
    private int colorBuffer;
    private boolean hasVertexColors;
    private float[] baseColors;
    private float[] tintedColors;
    private float lastR = Float.NaN;
    private float lastG = Float.NaN;
    private float lastB = Float.NaN;
    private float lastA = Float.NaN;

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

        if (this.colorBuffer != 0)
        {
            GL30.glDeleteBuffers(this.colorBuffer);
            this.colorBuffer = 0;
        }

        this.baseColors = null;
        this.tintedColors = null;
        this.hasVertexColors = false;
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

        this.count = data.vertices().length / 3;
        float[] colors = data.colors();

        if (colors != null && colors.length >= this.count * 4)
        {
            this.hasVertexColors = true;
            this.baseColors = colors;
            this.tintedColors = new float[colors.length];
            this.colorBuffer = GL30.glGenBuffers();

            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.colorBuffer);
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, colors, GL30.GL_DYNAMIC_DRAW);
            GL30.glVertexAttribPointer(Attributes.COLOR, 4, GL30.GL_FLOAT, false, 0, 0);
            GL30.glEnableVertexAttribArray(Attributes.COLOR);
        }
        else
        {
            this.hasVertexColors = false;
            GL30.glDisableVertexAttribArray(Attributes.COLOR);
        }

        GL30.glEnableVertexAttribArray(Attributes.POSITION);
        GL30.glEnableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glEnableVertexAttribArray(Attributes.NORMAL);
        GL30.glEnableVertexAttribArray(Attributes.LIGHTMAP_UV);

        GL30.glDisableVertexAttribArray(Attributes.OVERLAY_UV);
    }

    private void ensureTintedColors(float r, float g, float b, float a)
    {
        if (!this.hasVertexColors || this.colorBuffer == 0)
        {
            return;
        }

        if (r == this.lastR && g == this.lastG && b == this.lastB && a == this.lastA)
        {
            return;
        }

        this.lastR = r;
        this.lastG = g;
        this.lastB = b;
        this.lastA = a;

        for (int i = 0; i < this.baseColors.length; i += 4)
        {
            this.tintedColors[i] = this.baseColors[i] * r;
            this.tintedColors[i + 1] = this.baseColors[i + 1] * g;
            this.tintedColors[i + 2] = this.baseColors[i + 2] * b;
            this.tintedColors[i + 3] = this.baseColors[i + 3] * a;
        }

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.colorBuffer);
        GL30.glBufferSubData(GL30.GL_ARRAY_BUFFER, 0, this.tintedColors);
    }

    @Override
    public void render(VertexFormat format, float r, float g, float b, float a, int light, int overlay)
    {
        if (this.vao == 0 || !GL30.glIsVertexArray(this.vao))
        {
            return;
        }

        boolean hasShaders = BBSRendering.isIrisShadersEnabled();

        GL30.glBindVertexArray(this.vao);

        GL30.glDisableVertexAttribArray(Attributes.OVERLAY_UV);
        GL30.glVertexAttribI2i(Attributes.OVERLAY_UV, overlay & 0xFFFF, overlay >> 16 & 0xFFFF);

        if (this.hasVertexColors)
        {
            this.ensureTintedColors(r, g, b, a);
            GL30.glEnableVertexAttribArray(Attributes.COLOR);
        }
        else
        {
            GL30.glDisableVertexAttribArray(Attributes.COLOR);
            GL30.glVertexAttrib4f(Attributes.COLOR, r, g, b, a);
        }

        if (hasShaders)
        {
            GL30.glEnableVertexAttribArray(Attributes.MID_TEXTURE_UV);
            GL30.glEnableVertexAttribArray(Attributes.TANGENTS);
        }
        else
        {
            GL30.glDisableVertexAttribArray(Attributes.MID_TEXTURE_UV);
            GL30.glDisableVertexAttribArray(Attributes.TANGENTS);
        }

        GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, this.count);
        GL30.glBindVertexArray(0);
    }
}
