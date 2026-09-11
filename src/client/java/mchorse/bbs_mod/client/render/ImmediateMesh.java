package mchorse.bbs_mod.client.render;

import net.minecraft.client.renderer.rendertype.PreparedRenderType;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;

/** Owns immediate GPU uploads, but never the shared sequential index buffer. */
public final class ImmediateMesh implements AutoCloseable
{
    private final GpuBuffer vertices;
    private final GpuBuffer indices;
    private final IndexType indexType;
    private final int indexCount;
    private final boolean ownsIndices;

    public ImmediateMesh(MeshData mesh)
    {
        MeshData.DrawState state = mesh.drawState();

        this.indexCount = state.indexCount();
        this.ownsIndices = mesh.indexBuffer() != null;
        this.vertices = RenderSystem.getDevice().createBuffer(() -> "BBS immediate vertices", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());

        try
        {
            if (this.ownsIndices)
            {
                this.indices = RenderSystem.getDevice().createBuffer(() -> "BBS immediate indices", GpuBuffer.USAGE_INDEX, mesh.indexBuffer());
                this.indexType = state.indexType();
            }
            else
            {
                RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(state.primitiveTopology());

                this.indices = sequential.getBuffer(this.indexCount);
                this.indexType = sequential.type();
            }
        }
        catch (RuntimeException | Error exception)
        {
            this.vertices.close();

            throw exception;
        }
    }

    public void draw(PreparedRenderType prepared)
    {
        prepared.drawFromBuffer(this.vertices, this.indices, this.indexType, 0, 0, this.indexCount);
    }

    public void draw(RenderPass pass)
    {
        pass.setVertexBuffer(0, this.vertices.slice());
        pass.setIndexBuffer(this.indices, this.indexType);
        pass.drawIndexed(this.indexCount, 1, 0, 0, 0);
    }

    @Override
    public void close()
    {
        try
        {
            if (this.ownsIndices)
            {
                this.indices.close();
            }
        }
        finally
        {
            this.vertices.close();
        }
    }
}
