package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelData;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelMesh;
import mchorse.bbs_mod.cubic.data.model.ModelQuad;
import mchorse.bbs_mod.cubic.data.model.ModelVertex;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.class_287;
import net.minecraft.class_4587;
import net.minecraft.class_765;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Map;

public class CubicCubeRenderer implements ICubicRenderer
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

    private static Matrix4f modelM = new Matrix4f();
    private static Matrix3f normalM = new Matrix3f();

    protected float r = 1F;
    protected float g = 1F;
    protected float b = 1F;
    protected float a = 1F;
    protected int light;
    protected int overlay;
    protected StencilMap stencilMap;

    /* Temporary variables to avoid allocating and GC vectors */
    protected Vector3f normal = new Vector3f();
    protected Vector4f vertex = new Vector4f();

    private ModelVertex modelVertex = new ModelVertex();
    private ShapeKeys shapeKeys;

    public static void moveToPivot(class_4587 stack, Vector3f pivot)
    {
        stack.method_46416(pivot.x / 16F, pivot.y / 16F, pivot.z / 16F);
    }

    public static void rotate(class_4587 stack, Vector3f rotation)
    {
        if (rotation.x == 0 && rotation.y == 0 && rotation.z == 0)
        {
            return;
        }

        Matrix4f matrix4f = new Matrix4f();
        Matrix3f matrix3f = new Matrix3f();

        modelM.identity();
        matrix4f.identity().rotateZ(MathUtils.toRad(rotation.z));
        modelM.mul(matrix4f);

        matrix4f.identity().rotateY(MathUtils.toRad(rotation.y));
        modelM.mul(matrix4f);

        matrix4f.identity().rotateX(MathUtils.toRad(rotation.x));
        modelM.mul(matrix4f);

        normalM.identity();
        matrix3f.identity().rotateZ(MathUtils.toRad(rotation.z));
        normalM.mul(matrix3f);

        matrix3f.identity().rotateY(MathUtils.toRad(rotation.y));
        normalM.mul(matrix3f);

        matrix3f.identity().rotateX(MathUtils.toRad(rotation.x));
        normalM.mul(matrix3f);

        stack.method_23760().method_23761().mul(modelM);
        stack.method_23760().method_23762().mul(normalM);
    }

    public static void moveBackFromPivot(class_4587 stack, Vector3f pivot)
    {
        stack.method_46416(-pivot.x / 16F, -pivot.y / 16F, -pivot.z / 16F);
    }

    public CubicCubeRenderer(int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys)
    {
        this.light = light;
        this.overlay = overlay;
        this.stencilMap = stencilMap;
        this.shapeKeys = shapeKeys;
    }

    public void setColor(float r, float g, float b, float a)
    {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public boolean renderGroup(class_287 builder, class_4587 stack, ModelGroup group, Model model)
    {
        for (ModelCube cube : group.cubes)
        {
            this.renderCube(builder, stack, group, cube);
        }

        for (ModelMesh mesh : group.meshes)
        {
            this.renderMesh(builder, stack, model, group, mesh);
        }

        return false;
    }

    protected void renderCube(class_287 builder, class_4587 stack, ModelGroup group, ModelCube cube)
    {
        stack.method_22903();
        moveToPivot(stack, cube.pivot);
        rotate(stack, cube.rotate);
        moveBackFromPivot(stack, cube.pivot);

        for (ModelQuad quad : cube.quads)
        {
            this.normal.set(quad.normal.x, quad.normal.y, quad.normal.z);
            stack.method_23760().method_23762().transform(this.normal);

            if (quad.vertices.size() == 4)
            {
                this.writeVertex(builder, stack, group, quad.vertices.get(0), this.normal);
                this.writeVertex(builder, stack, group, quad.vertices.get(1), this.normal);
                this.writeVertex(builder, stack, group, quad.vertices.get(2), this.normal);
                this.writeVertex(builder, stack, group, quad.vertices.get(0), this.normal);
                this.writeVertex(builder, stack, group, quad.vertices.get(2), this.normal);
                this.writeVertex(builder, stack, group, quad.vertices.get(3), this.normal);
            }
        }

        stack.method_22909();
    }

    protected void renderMesh(class_287 builder, class_4587 stack, Model model, ModelGroup group, ModelMesh mesh)
    {
        stack.method_22903();
        moveToPivot(stack, mesh.origin);
        rotate(stack, mesh.rotate);
        moveBackFromPivot(stack, mesh.origin);

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

            /* Apply shape keys */
            for (Map.Entry<String, Float> entry : this.shapeKeys.shapeKeys.entrySet())
            {
                ModelData data = mesh.data.get(entry.getKey());
                float value = entry.getValue();

                if (data != null)
                {
                    /* final = temporary + lerp(initial, current, x) - initial */
                    this.relativeShift(v1, baseData.vertices.get(i * 3), data.vertices.get(i * 3), value);
                    this.relativeShift(v2, baseData.vertices.get(i * 3 + 1), data.vertices.get(i * 3 + 1), value);
                    this.relativeShift(v3, baseData.vertices.get(i * 3 + 2), data.vertices.get(i * 3 + 2), value);

                    this.relativeShift(n1, baseData.normals.get(i * 3), data.normals.get(i * 3), value);
                    this.relativeShift(n2, baseData.normals.get(i * 3 + 1), data.normals.get(i * 3 + 1), value);
                    this.relativeShift(n3, baseData.normals.get(i * 3 + 2), data.normals.get(i * 3 + 2), value);

                    this.relativeShift(u1, baseData.uvs.get(i * 3), data.uvs.get(i * 3), value);
                    this.relativeShift(u2, baseData.uvs.get(i * 3 + 1), data.uvs.get(i * 3 + 1), value);
                    this.relativeShift(u3, baseData.uvs.get(i * 3 + 2), data.uvs.get(i * 3 + 2), value);
                }
            }

            /* Write vertices */
            this.normal.set(n1.x, n1.y, n1.z);
            stack.method_23760().method_23762().transform(this.normal);
            this.modelVertex.set(v1, u1, model);
            this.writeVertex(builder, stack, group, this.modelVertex, this.normal);

            this.normal.set(n2.x, n2.y, n2.z);
            stack.method_23760().method_23762().transform(this.normal);
            this.modelVertex.set(v2, u2, model);
            this.writeVertex(builder, stack, group, this.modelVertex, this.normal);

            this.normal.set(n3.x, n3.y, n3.z);
            stack.method_23760().method_23762().transform(this.normal);
            this.modelVertex.set(v3, u3, model);
            this.writeVertex(builder, stack, group, this.modelVertex, this.normal);
        }

        stack.method_22909();
    }

    private void relativeShift(Vector3f temp, Vector3f initial, Vector3f current, float x)
    {
        temp.x = temp.x + Lerps.lerp(initial.x, current.x, x) - initial.x;
        temp.y = temp.y + Lerps.lerp(initial.y, current.y, x) - initial.y;
        temp.z = temp.z + Lerps.lerp(initial.z, current.z, x) - initial.z;
    }

    private void relativeShift(Vector2f temp, Vector2f initial, Vector2f current, float x)
    {
        temp.x = temp.x + Lerps.lerp(initial.x, current.x, x) - initial.x;
        temp.y = temp.y + Lerps.lerp(initial.y, current.y, x) - initial.y;
    }

    protected void writeVertex(class_287 builder, class_4587 stack, ModelGroup group, ModelVertex vertex, Vector3f normal)
    {
        this.vertex.set(vertex.vertex.x, vertex.vertex.y, vertex.vertex.z, 1);
        stack.method_23760().method_23761().transform(this.vertex);

        builder.method_22912(this.vertex.x, this.vertex.y, this.vertex.z)
            .method_22915(this.r * group.color.r, this.g * group.color.g, this.b * group.color.b, this.a * group.color.a)
            .method_22913(vertex.uv.x, vertex.uv.y)
            .method_22922(this.overlay);

        if (this.stencilMap != null)
        {
            builder.method_22921(stencilMap.increment ? group.index : 0, 0);
        }
        else
        {
            int u = (int) Lerps.lerp(this.light & '\uffff', class_765.field_32769, MathUtils.clamp(group.lighting, 0F, 1F));
            int v = this.light >> 16 & '\uffff';

            builder.method_22921(u, v);
        }

        builder.method_22914(normal.x, normal.y, normal.z);
    }

    protected float resolveEffectiveGlowStrength(ModelGroup group)
    {
        if (group.glowIntensity != 0F)
        {
            return group.glowIntensity;
        }

        return ModelVAORenderer.getBaseGlowingStrength();
    }

    protected float resolveEffectiveGlowR(ModelGroup group)
    {
        if (group.glowIntensity != 0F)
        {
            return group.glowingColor.r;
        }

        return ModelVAORenderer.getBaseGlowingR();
    }

    protected float resolveEffectiveGlowG(ModelGroup group)
    {
        if (group.glowIntensity != 0F)
        {
            return group.glowingColor.g;
        }

        return ModelVAORenderer.getBaseGlowingG();
    }

    protected float resolveEffectiveGlowB(ModelGroup group)
    {
        if (group.glowIntensity != 0F)
        {
            return group.glowingColor.b;
        }

        return ModelVAORenderer.getBaseGlowingB();
    }

    protected float resolveEffectivePaintStrength(ModelGroup group)
    {
        if (group.paintColor.a != 0F)
        {
            return group.paintColor.a;
        }

        return ModelVAORenderer.getBasePaintStrength();
    }

    protected float resolveEffectivePaintR(ModelGroup group)
    {
        if (group.paintColor.a != 0F)
        {
            return group.paintColor.r;
        }

        return ModelVAORenderer.getBasePaintR();
    }

    protected float resolveEffectivePaintG(ModelGroup group)
    {
        if (group.paintColor.a != 0F)
        {
            return group.paintColor.g;
        }

        return ModelVAORenderer.getBasePaintG();
    }

    protected float resolveEffectivePaintB(ModelGroup group)
    {
        if (group.paintColor.a != 0F)
        {
            return group.paintColor.b;
        }

        return ModelVAORenderer.getBasePaintB();
    }
}