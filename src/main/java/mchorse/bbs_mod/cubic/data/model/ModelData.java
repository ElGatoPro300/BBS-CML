package mchorse.bbs_mod.cubic.data.model;

import mchorse.bbs_mod.obj.MeshOBJ;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ModelData
{
    public List<Vector3f> vertices = new ArrayList<>();
    public List<Vector3f> normals = new ArrayList<>();
    public List<Vector2f> uvs = new ArrayList<>();

    public void clear()
    {
        this.vertices.clear();
        this.normals.clear();
        this.uvs.clear();
    }

    public void fill(MeshOBJ mesh, int tx, int ty)
    {
        for (int i = 0, c = mesh.triangles; i < c; i++)
        {
            this.vertices.add(new Vector3f(mesh.posData[i * 3] * 16F, mesh.posData[i * 3 + 1] * 16F, mesh.posData[i * 3 + 2] * 16F));
            this.normals.add(new Vector3f(mesh.normData[i * 3], mesh.normData[i * 3 + 1], mesh.normData[i * 3 + 2]));
            this.uvs.add(new Vector2f(mesh.texData[i * 2] * tx, mesh.texData[i * 2 + 1] * ty));
        }
    }
}