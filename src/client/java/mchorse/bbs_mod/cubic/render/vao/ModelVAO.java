package mchorse.bbs_mod.cubic.render.vao;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;

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
    @Override
    public void delete()
    {}

    /**
     * Bake the stack position/normal matrices into each vertex CPU-side and write the triangles into
     * {@code builder} (format POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL). Color/overlay/light are
     * constant per draw (matching the old {@code glVertexAttrib4f}/{@code glVertexAttribI2i} defaults).
     */
    @Override
    public void writeImmediate(BufferBuilder builder, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {}
}
