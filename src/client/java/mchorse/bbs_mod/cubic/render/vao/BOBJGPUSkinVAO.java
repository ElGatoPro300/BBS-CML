package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class BOBJGPUSkinVAO extends BOBJModelVAO
{
    private static int program = -1;
    private static int ubo = -1;
    private static final int MAX_BONES = 256;

    private int staticVao;
    private int staticPosBuffer;
    private int staticNormBuffer;
    private int staticTangentBuffer;
    private int weightBuffer;
    private int boneIndexBuffer;

    private FloatBuffer uboBuffer;
    private float[] baseTangents;

    public BOBJGPUSkinVAO(BOBJLoader.CompiledData data, BOBJArmature armature)
    {
        super(data, armature);
        this.uboBuffer = MemoryUtil.memAllocFloat(MAX_BONES * 16);
    }

    public static void setupShader()
    {
        if (program != -1) return;

        String vs = "#version 150\n" +
                "in vec3 inPos;\n" +
                "in vec3 inNorm;\n" +
                "in vec4 inTangent;\n" +
                "in vec4 inWeights;\n" +
                "in ivec4 inIndices;\n" +
                "layout(std140) uniform BoneData {\n" +
                "    mat4 bones[" + MAX_BONES + "];\n" +
                "};\n" +
                "out vec3 outPos;\n" +
                "out vec3 outNorm;\n" +
                "out vec4 outTangent;\n" +
                "void main() {\n" +
                "    mat4 boneMat = mat4(0.0);\n" +
                "    if(inIndices.x >= 0 && inWeights.x > 0.0) boneMat += bones[inIndices.x] * inWeights.x;\n" +
                "    if(inIndices.y >= 0 && inWeights.y > 0.0) boneMat += bones[inIndices.y] * inWeights.y;\n" +
                "    if(inIndices.z >= 0 && inWeights.z > 0.0) boneMat += bones[inIndices.z] * inWeights.z;\n" +
                "    if(inIndices.w >= 0 && inWeights.w > 0.0) boneMat += bones[inIndices.w] * inWeights.w;\n" +
                "    if(boneMat == mat4(0.0)) boneMat = mat4(1.0);\n" +
                "    vec4 pos = boneMat * vec4(inPos, 1.0);\n" +
                "    outPos = pos.xyz / pos.w;\n" +
                "    outNorm = mat3(boneMat) * inNorm;\n" +
                "    outTangent = vec4(mat3(boneMat) * inTangent.xyz, inTangent.w);\n" +
                "}\n";

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vs);
        GL20.glCompileShader(vertexShader);

        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
        {
            System.err.println("Failed to compile BOBJ Skinning Shader: " + GL20.glGetShaderInfoLog(vertexShader));
        }

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);

        GL20.glBindAttribLocation(program, 0, "inPos");
        GL20.glBindAttribLocation(program, 1, "inNorm");
        GL20.glBindAttribLocation(program, 2, "inTangent");
        GL20.glBindAttribLocation(program, 3, "inWeights");
        GL20.glBindAttribLocation(program, 4, "inIndices");

        CharSequence[] varyings = new CharSequence[]{"outPos", "outNorm", "outTangent"};
        GL30.glTransformFeedbackVaryings(program, varyings, GL30.GL_SEPARATE_ATTRIBS);

        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
        {
            System.err.println("Failed to link BOBJ Skinning Program: " + GL20.glGetProgramInfoLog(program));
        }

        ubo = GL31.glGenBuffers();
        GL31.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo);
        GL31.glBufferData(GL31.GL_UNIFORM_BUFFER, MAX_BONES * 64, GL31.GL_DYNAMIC_DRAW);
        GL31.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

        int blockIndex = GL31.glGetUniformBlockIndex(program, "BoneData");

        GL31.glUniformBlockBinding(program, blockIndex, 7);
    }

    @Override
    protected void initBuffers()
    {
        super.initBuffers();

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.vertexBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, (long) this.count * 3 * 4, GL30.GL_STREAM_DRAW);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.normalBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, (long) this.count * 3 * 4, GL30.GL_STREAM_DRAW);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.tangentBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, (long) this.count * 4 * 4, GL30.GL_STREAM_DRAW);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);

        setupShader();

        this.baseTangents = new float[this.count * 4];
        if (BBSRendering.isIrisShadersEnabled())
        {
            BBSRendering.calculateTangents(this.baseTangents, this.data.posData, this.data.normData, this.data.texData);
        }

        int previousVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        this.staticVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(this.staticVao);

        this.staticPosBuffer = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.staticPosBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.posData, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 0, 0);
        GL30.glEnableVertexAttribArray(0);

        this.staticNormBuffer = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.staticNormBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.normData, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 0, 0);
        GL30.glEnableVertexAttribArray(1);

        this.staticTangentBuffer = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.staticTangentBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.baseTangents, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(2, 4, GL30.GL_FLOAT, false, 0, 0);
        GL30.glEnableVertexAttribArray(2);

        this.weightBuffer = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.weightBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.weightData, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(3, 4, GL30.GL_FLOAT, false, 0, 0);
        GL30.glEnableVertexAttribArray(3);

        this.boneIndexBuffer = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.boneIndexBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.boneIndexData, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribIPointer(4, 4, GL30.GL_INT, 0, 0);
        GL30.glEnableVertexAttribArray(4);

        GL30.glBindVertexArray(previousVAO);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void delete()
    {
        super.delete();
        GL30.glDeleteVertexArrays(this.staticVao);
        GL15.glDeleteBuffers(this.staticPosBuffer);
        GL15.glDeleteBuffers(this.staticNormBuffer);
        GL15.glDeleteBuffers(this.staticTangentBuffer);
        GL15.glDeleteBuffers(this.weightBuffer);
        GL15.glDeleteBuffers(this.boneIndexBuffer);

        if (this.uboBuffer != null)
        {
            MemoryUtil.memFree(this.uboBuffer);
            this.uboBuffer = null;
        }
    }

    @Override
    public void updateMesh(StencilMap stencilMap)
    {
        if (program == -1)
        {
            return;
        }


        int savedProgram = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int savedVao = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        /* Upload bone matrices to UBO */
        this.uboBuffer.clear();
        for (int i = 0; i < this.armature.matrices.length && i < MAX_BONES; i++)
        {
            this.armature.matrices[i].get(i * 16, this.uboBuffer);
        }

        GL31.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo);
        GL31.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, this.uboBuffer);
        GL31.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

        /* Bind to slot 7 */
        GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 7, ubo);

        GL20.glUseProgram(program);
        GL30.glBindVertexArray(this.staticVao);

        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, this.vertexBuffer);
        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 1, this.normalBuffer);
        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 2, this.tangentBuffer);

        GL30.glEnable(GL30.GL_RASTERIZER_DISCARD);
        GL30.glBeginTransformFeedback(GL30.GL_POINTS);

        GL30.glDrawArrays(GL30.GL_POINTS, 0, this.count);

        GL30.glEndTransformFeedback();
        GL30.glDisable(GL30.GL_RASTERIZER_DISCARD);

        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 1, 0);
        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 2, 0);
        GL30.glBindVertexArray(savedVao);

        GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 7, 0);

        /* Restore whatever program was active before */
        GL20.glUseProgram(savedProgram);


        if (stencilMap != null)
        {
            for (int i = 0; i < this.count; i++)
            {
                float maxWeight = -1F;
                int lightBone = -1;

                for (int w = 0; w < 4; w++)
                {
                    float weight = this.data.weightData[i * 4 + w];
                    int boneIndex = this.data.boneIndexData[i * 4 + w];

                    if (boneIndex >= 0 && weight > 0F && weight > maxWeight)
                    {
                        maxWeight = weight;
                        lightBone = boneIndex;
                    }
                }

                boolean allowBone = true;

                if (stencilMap.allowedBones != null && lightBone >= 0)
                {
                    BOBJBone bone = this.getBoneByIndex(lightBone);
                    allowBone = bone != null && stencilMap.allowedBones.contains(bone.name);
                }

                this.tmpLight[i * 2] = Math.max(0, stencilMap.increment ? (allowBone ? lightBone : 0) : 0);
                this.tmpLight[i * 2 + 1] = 0;
            }

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.lightBuffer);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, this.tmpLight);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }


        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
}