package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAOData;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelData;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelMesh;
import mchorse.bbs_mod.cubic.data.model.ModelQuad;
import mchorse.bbs_mod.cubic.data.model.ModelVertex;
import mchorse.bbs_mod.utils.CollectionUtils;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CubicVAOBuilderRenderer implements ICubicRenderer
{
    private final static Vector3f v1 = new Vector3f();
    private final static Vector3f v2 = new Vector3f();
    private final static Vector3f v3 = new Vector3f();

    private final static Vector3f n1 = new Vector3f();
    private final static Vector3f n2 = new Vector3f();
    private final static Vector3f n3 = new Vector3f();

    private final static Vector2f u1 = new Vector2f();
    private final static Vector2f u2 = new Vector2f();
    private final static Vector2f u3 = new Vector2f();

    private Map<ModelGroup, ModelVAO> model;

    /* Temporary variables to avoid allocating and GC vectors */
    private ModelVertex modelVertex = new ModelVertex();
    private Vector3f normal = new Vector3f();
    private Vector4f vertex = new Vector4f();

    public CubicVAOBuilderRenderer(Map<ModelGroup, ModelVAO> model)
    {
        this.model = model;
    }

    @Override
    public void applyGroupTransformations(MatrixStack stack, ModelGroup group)
    {}

    @Override
    public boolean renderGroup(BufferBuilder builder, MatrixStack stack, ModelGroup group, Model model)
    {
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();

        for (ModelCube cube : group.cubes)
        {
            this.renderCube(vertices, normals, uvs, stack, group, cube);
        }

        for (ModelMesh mesh : group.meshes)
        {
            this.renderMesh(vertices, normals, uvs, stack, model, group, mesh);
        }

        if (!vertices.isEmpty())
        {
            float[] v = CollectionUtils.toArray(vertices);
            float[] n = CollectionUtils.toArray(normals);
            float[] u = CollectionUtils.toArray(uvs);
            float[] t = BBSRendering.calculateTangents(v, n, u);

            this.model.put(group, new ModelVAO(new ModelVAOData(v, n, t, u)));
        }

        return false;
    }

    private void renderCube(List<Float> vertices, List<Float> normals, List<Float> uvs, MatrixStack stack, ModelGroup group, ModelCube cube)
    {
        stack.push();
        CubicCubeRenderer.moveToPivot(stack, cube.pivot);
        CubicCubeRenderer.rotate(stack, cube.rotate);
        CubicCubeRenderer.moveBackFromPivot(stack, cube.pivot);

        for (ModelQuad quad : cube.quads)
        {
            this.normal.set(quad.normal.x, quad.normal.y, quad.normal.z);
            stack.peek().getNormalMatrix().transform(this.normal);

            if (quad.vertices.size() == 4)
            {
                this.writeVertex(vertices, normals, uvs, stack, group, quad.vertices.get(0), this.normal);
                this.writeVertex(vertices, normals, uvs, stack, group, quad.vertices.get(1), this.normal);
                this.writeVertex(vertices, normals, uvs, stack, group, quad.vertices.get(2), this.normal);
                this.writeVertex(vertices, normals, uvs, stack, group, quad.vertices.get(0), this.normal);
                this.writeVertex(vertices, normals, uvs, stack, group, quad.vertices.get(2), this.normal);
                this.writeVertex(vertices, normals, uvs, stack, group, quad.vertices.get(3), this.normal);
            }
        }

        stack.pop();
    }

    private void renderMesh(List<Float> vertices, List<Float> normals, List<Float> uvs, MatrixStack stack, Model model, ModelGroup group, ModelMesh mesh)
    {
        stack.push();
        CubicCubeRenderer.moveToPivot(stack, mesh.origin);
        CubicCubeRenderer.rotate(stack, mesh.rotate);
        CubicCubeRenderer.moveBackFromPivot(stack, mesh.origin);

        ModelData baseData = mesh.baseData;

        for (int i = 0, c = baseData.vertices.size() / 3; i < c; i++)
        {
            v1.set(baseData.vertices.get(i * 3));
            v2.set(baseData.vertices.get(i * 3 + 1));
            v3.set(baseData.vertices.get(i * 3 + 2));

            n1.set(baseData.normals.get(i * 3));
            n2.set(baseData.normals.get(i * 3 + 1));
            n3.set(baseData.normals.get(i * 3 + 2));

            u1.set(baseData.uvs.get(i * 3));
            u2.set(baseData.uvs.get(i * 3 + 1));
            u3.set(baseData.uvs.get(i * 3 + 2));

            /* Write vertices */
            this.normal.set(n1.x, n1.y, n1.z);
            stack.peek().getNormalMatrix().transform(this.normal);
            this.modelVertex.set(v1, u1, model);
            this.writeVertex(vertices, normals, uvs, stack, group, this.modelVertex, this.normal);

            this.normal.set(n2.x, n2.y, n2.z);
            stack.peek().getNormalMatrix().transform(this.normal);
            this.modelVertex.set(v2, u2, model);
            this.writeVertex(vertices, normals, uvs, stack, group, this.modelVertex, this.normal);

            this.normal.set(n3.x, n3.y, n3.z);
            stack.peek().getNormalMatrix().transform(this.normal);
            this.modelVertex.set(v3, u3, model);
            this.writeVertex(vertices, normals, uvs, stack, group, this.modelVertex, this.normal);
        }

        stack.pop();
    }

    private void writeVertex(List<Float> vertices, List<Float> normals, List<Float> uvs, MatrixStack stack, ModelGroup group, ModelVertex vertex, Vector3f normal)
    {
        this.vertex.set(vertex.vertex.x, vertex.vertex.y, vertex.vertex.z, 1);
        stack.peek().getPositionMatrix().transform(this.vertex);

        vertices.add(this.vertex.x);
        vertices.add(this.vertex.y);
        vertices.add(this.vertex.z);
        normals.add(normal.x);
        normals.add(normal.y);
        normals.add(normal.z);
        uvs.add(vertex.uv.x);
        uvs.add(vertex.uv.y);
    }
}