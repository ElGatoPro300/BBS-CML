package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.render.picker.BBSPickerRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.joml.Matrices;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Skinned BOBJ mesh. Bone skinning stays on the CPU (unchanged by the render migration); the 1.21.11
 * GPU pipeline rewrite removed the raw-GL VAO/VBO + ShaderProgram bind this class previously drew
 * with, so the skinned result (tmpVertices/tmpNormals/tmpLight) is instead emitted into one or more
 * BufferBuilders per draw and submitted through {@link BBSShaders#getModelLayer()} (per-bone texture
 * overrides still split into separate draws/buffers, one per bound texture, matching the previous
 * {@code glDrawArrays} range-splitting behaviour).
 */
public class BOBJModelVAO
{
    public BOBJLoader.CompiledData data;
    public BOBJArmature armature;

    protected int vao;
    protected int count;

    /* GL buffers */
    public int vertexBuffer;
    public int normalBuffer;
    public int lightBuffer;
    public int texCoordBuffer;
    public int tangentBuffer;
    public int midTextureBuffer;

    protected float[] tmpVertices;
    protected float[] tmpNormals;
    protected int[] tmpLight;
    protected float[] tmpTangents;
    protected int[] dominantBonePerTriangle;

    private final Map<Integer, Link> fullOverrides = new HashMap<>();
    private final Map<Integer, Float> partialOverrides = new HashMap<>();
    private final Set<Integer> overridden = new HashSet<>();

    public BOBJModelVAO(BOBJLoader.CompiledData data, BOBJArmature armature)
    {
        this.data = data;
        this.armature = armature;

        this.initBuffers();
    }

    /**
     * Initiate buffers. This method is responsible for allocating 
     * buffers for the data to be passed to VBOs and also generating the 
     * VBOs themselves. 
     */
    protected void initBuffers()
    {
        this.vao = GL30.glGenVertexArrays();

        GL30.glBindVertexArray(this.vao);

        this.vertexBuffer = GL30.glGenBuffers();
        this.normalBuffer = GL30.glGenBuffers();
        this.lightBuffer = GL30.glGenBuffers();
        this.texCoordBuffer = GL30.glGenBuffers();
        this.tangentBuffer = GL30.glGenBuffers();
        this.midTextureBuffer = GL30.glGenBuffers();

        this.count = this.data.normData.length / 3;
        this.tmpVertices = new float[this.data.posData.length];
        this.tmpNormals = new float[this.data.normData.length];
        this.tmpLight = new int[this.count * 2];
        this.dominantBonePerTriangle = new int[this.count / 3];

        this.buildDominantBones();
    }

    /**
     * Clean up resources which were used by this
     */
    public void delete()
    {}

    /**
     * Update this mesh. This method is responsible for applying
     * matrix transformations to vertices and normals according to its
     * bone owners and these bone influences. The skinned result is kept on the CPU
     * (tmpVertices/tmpNormals/tmpLight) and emitted into a BufferBuilder in {@link #render}.
     */
    public void updateMesh(StencilMap stencilMap)
    {
        Vector4f sum = new Vector4f();
        Vector4f result = new Vector4f(0F, 0F, 0F, 0F);
        Vector3f sumNormal = new Vector3f();
        Vector3f resultNormal = new Vector3f();

        float[] oldVertices = this.data.posData;
        float[] newVertices = this.tmpVertices;
        float[] oldNormals = this.data.normData;
        float[] newNormals = this.tmpNormals;

        Matrix4f[] matrices = this.armature.matrices;

        for (int i = 0, c = this.count; i < c; i++)
        {
            int count = 0;
            float maxWeight = -1;
            int lightBone = -1;

            for (int w = 0; w < 4; w++)
            {
                float weight = this.data.weightData[i * 4 + w];

                if (weight > 0)
                {
                    int index = this.data.boneIndexData[i * 4 + w];

                    sum.set(oldVertices[i * 3], oldVertices[i * 3 + 1], oldVertices[i * 3 + 2], 1F);
                    matrices[index].transform(sum);
                    result.add(sum.mul(weight));

                    sumNormal.set(oldNormals[i * 3], oldNormals[i * 3 + 1], oldNormals[i * 3 + 2]);
                    Matrices.TEMP_3F.set(matrices[index]).transform(sumNormal);
                    resultNormal.add(sumNormal.mul(weight));

                    count++;

                    if (weight > maxWeight)
                    {
                        lightBone = index;
                        maxWeight = weight;
                    }
                }
            }

            if (count == 0)
            {
                result.set(oldVertices[i * 3], oldVertices[i * 3 + 1], oldVertices[i * 3 + 2], 1F);
                resultNormal.set(oldNormals[i * 3], oldNormals[i * 3 + 1], oldNormals[i * 3 + 2]);
            }

            result.x /= result.w;
            result.y /= result.w;
            result.z /= result.w;

            newVertices[i * 3] = result.x;
            newVertices[i * 3 + 1] = result.y;
            newVertices[i * 3 + 2] = result.z;

            newNormals[i * 3] = resultNormal.x;
            newNormals[i * 3 + 1] = resultNormal.y;
            newNormals[i * 3 + 2] = resultNormal.z;

            result.set(0F, 0F, 0F, 0F);
            resultNormal.set(0F, 0F, 0F);

            boolean allowBone = true;
            if (stencilMap != null && stencilMap.allowedBones != null && lightBone >= 0)
            {
                BOBJBone bone = this.getBoneByIndex(lightBone);
                allowBone = bone != null && stencilMap.allowedBones.contains(bone.name);
            }

            if (stencilMap != null)
            {
                this.tmpLight[i * 2] = Math.max(0, stencilMap.increment ? (allowBone ? lightBone : 0) : 0);
                this.tmpLight[i * 2 + 1] = 0;
            }
        }

        this.processData(newVertices, newNormals);
    }

    protected void processData(float[] newVertices, float[] newNormals)
    {}

    protected void buildDominantBones()
    {
        for (int triangle = 0, triCount = this.dominantBonePerTriangle.length; triangle < triCount; triangle++)
        {
            int base = triangle * 3;
            int a = this.getDominantBoneForVertex(base);
            int b = this.getDominantBoneForVertex(base + 1);
            int c = this.getDominantBoneForVertex(base + 2);

            if (a == b || a == c)
            {
                this.dominantBonePerTriangle[triangle] = a;
            }
            else if (b == c)
            {
                this.dominantBonePerTriangle[triangle] = b;
            }
            else
            {
                this.dominantBonePerTriangle[triangle] = a;
            }
        }
    }

    protected int getDominantBoneForVertex(int vertex)
    {
        int base = vertex * 4;
        float max = -1F;
        int bone = -1;

        for (int i = 0; i < 4; i++)
        {
            float weight = this.data.weightData[base + i];
            int boneIndex = this.data.boneIndexData[base + i];

            if (boneIndex >= 0 && weight > max)
            {
                max = weight;
                bone = boneIndex;
            }
        }

        return bone;
    }

    protected BOBJBone getBoneByIndex(int index)
    {
        for (BOBJBone bone : this.armature.orderedBones)
        {
            if (bone.index == index)
            {
                return bone;
            }
        }

        return null;
    }

    protected BOBJBone getBoneByName(String name)
    {
        if (stencilMap != null)
        {
            BuiltBuffer built = this.writeBuffer(stack, r, g, b, a, stencilMap, light, overlay, null);

            if (built != null)
            {
                BBSPickerRenderer.draw(BBSShaders.getPickerModelsProgram(), built, RenderSystem.getModelViewMatrix());
            }

        return null;
    }

    protected void renderStencilPickPriority(StencilMap stencilMap)
    {
        if (stencilMap == null || !stencilMap.increment)
        {
            return;
        }

        Map<Integer, Link> overrides = new HashMap<>();

        for (BOBJBone bone : this.armature.orderedBones)
        {
            if (bone.texture != null)
            {
                this.drawTriangles((boneIndex) -> boneIndex == bone.index);
            }
        }
    }

    protected void drawTriangles(IntPredicate predicate)
    {
        int start = -1;

        for (int i = 0; i < this.dominantBonePerTriangle.length; i++)
        {
            boolean draw = predicate.test(this.dominantBonePerTriangle[i]);

            if (draw && start == -1)
            {
                start = i;
            }
            else if (!draw && start != -1)
            {
                GL30.glDrawArrays(GL30.GL_TRIANGLES, start * 3, (i - start) * 3);
                start = -1;
            }
        }

        if (overrides.isEmpty())
        {
            if (defaultTexture != null)
            {
                BBSModClient.getTextures().bindTexture(defaultTexture);
            }

            this.drawGroup(stack, r, g, b, a, light, overlay, null);

            return;
        }
    }

    /**
     * BBS {@link ShaderProgram#bind()} snapshots Sampler* from {@link RenderSystem} at
     * {@link ModelVAORenderer#setupUniforms}. Skin must be bound before that — binding after
     * leaves Sampler0 on whatever Iris left (featureless tinted silhouette, no skin).
     */
    protected void bindDrawTexture(Link texture)
    {
        if (texture != null)
        {
            BBSModClient.getTextures().bindTexture(texture);
        }
    }

    protected void rebindShaderSamplers(ShaderProgram shader, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        ModelVAORenderer.setupUniforms(stack, shader);
        RenderSystem.setShader(() -> shader);
        shader.bind();
        GL30.glBindVertexArray(this.vao);

        GL30.glDisableVertexAttribArray(Attributes.COLOR);
        GL30.glDisableVertexAttribArray(Attributes.OVERLAY_UV);
        GL30.glDisableVertexAttribArray(Attributes.LIGHTMAP_UV);

        GL30.glVertexAttrib4f(Attributes.COLOR, r, g, b, a);
        GL30.glVertexAttribI2i(Attributes.OVERLAY_UV, overlay & '\uffff', overlay >> 16 & '\uffff');
        GL30.glVertexAttribI2i(Attributes.LIGHTMAP_UV, light & '\uffff', light >> 16 & '\uffff');
    }

    public void render(ShaderProgram shader, MatrixStack stack, float r, float g, float b, float a, StencilMap stencilMap, int light, int overlay, Link defaultTexture)
    {
        boolean hasShaders = BBSRendering.isIrisShadersEnabled();

        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int currentElementArrayBuffer = GL30.glGetInteger(GL30.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        if (defaultTexture != null)
        {
            BBSModClient.getTextures().bindTexture(defaultTexture);
        }

        this.drawGroup(stack, r, g, b, a, light, overlay, (bone) -> bone < 0 || !overrides.containsKey(bone));

        for (Map.Entry<Integer, Link> entry : overrides.entrySet())
        {
            BBSModClient.getTextures().bindTexture(entry.getValue());
            this.drawGroup(stack, r, g, b, a, light, overlay, (bone) -> bone == entry.getKey());
        }
    }

    private void drawGroup(MatrixStack stack, float r, float g, float b, float a, int light, int overlay, IntPredicate predicate)
    {
        BuiltBuffer built = this.writeBuffer(stack, r, g, b, a, null, light, overlay, predicate);

        if (built != null)
        {
            BBSShaders.getModelLayer().draw(built);
        }
    }

    private BuiltBuffer writeBuffer(MatrixStack stack, float r, float g, float b, float a, StencilMap stencilMap, int light, int overlay, IntPredicate predicate)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        Matrix4f position = stack.peek().getPositionMatrix();
        Matrix3f normalMatrix = stack.peek().getNormalMatrix();

        float[] vertices = this.tmpVertices;
        float[] normals = this.tmpNormals;
        float[] texData = this.data.texData;

        Vector4f vertex = new Vector4f();
        Vector3f normal = new Vector3f();

        int lu = light & 0xffff;
        int lv = light >> 16 & 0xffff;

        for (int triangle = 0, triCount = this.dominantBonePerTriangle.length; triangle < triCount; triangle++)
        {
            if (predicate != null && !predicate.test(this.dominantBonePerTriangle[triangle]))
            {
                continue;
            }

            for (int k = 0; k < 3; k++)
            {
                int i = triangle * 3 + k;

                vertex.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2], 1F);
                position.transform(vertex);

                normal.set(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
                normalMatrix.transform(normal);

                int u = lu;
                int v = lv;

                if (stencilMap != null)
                {
                    u = this.tmpLight[i * 2];
                    v = this.tmpLight[i * 2 + 1];
                }

                builder.vertex(vertex.x, vertex.y, vertex.z)
                    .color(r, g, b, a)
                    .texture(texData[i * 2], texData[i * 2 + 1])
                    .overlay(overlay)
                    .light(u, v)
                    .normal(normal.x, normal.y, normal.z);
            }
        }

        return builder.endNullable();
    }
}
