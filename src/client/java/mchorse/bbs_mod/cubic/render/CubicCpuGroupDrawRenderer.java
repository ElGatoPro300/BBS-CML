package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Shape-key CPU geometry is submitted one model group per call so each group's texture and
 * vertex color are resolved before its buffer is handed to the model layer.
 * <p>
 * Positions and normals are written already transformed by the render stack; the model layer
 * supplies the active camera transform when it submits the built buffer.
 */
public class CubicCpuGroupDrawRenderer extends CubicCubeRenderer
{
    private final RenderPipeline pipeline;
    private final Link defaultTexture;

    public CubicCpuGroupDrawRenderer(int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, RenderPipeline pipeline, Link defaultTexture)
    {
        super(light, overlay, stencilMap, shapeKeys);

        this.pipeline = pipeline;
        this.defaultTexture = defaultTexture;
    }

    @Override
    public boolean renderGroup(BufferBuilder builder, PoseStack stack, ModelGroup group, Model model)
    {
        if (group.cubes.isEmpty() && group.meshes.isEmpty())
        {
            return false;
        }

        CubicGroupTextureBlend textureBlend = CubicGroupTextureBlend.resolve(group, this.defaultTexture);

        if (textureBlend != null && textureBlend.isPartial())
        {
            float fromA = this.a * (1F - textureBlend.blend);
            float toA = this.a * textureBlend.blend;

            CubicGroupTextureBlend.drawTwoPass(
                () -> this.drawGroup(stack, group, model, textureBlend.from, fromA),
                () -> this.drawGroup(stack, group, model, textureBlend.to, toA),
                textureBlend.blend
            );
        }
        else
        {
            CubicGroupTextureBlend.bindForDraw(this.pipeline, textureBlend, this.defaultTexture);

            try
            {
                this.drawGroup(stack, group, model, CubicGroupTextureBlend.resolveDrawTexture(textureBlend, this.defaultTexture), this.a);
            }
            finally
            {
                ModelVAORenderer.clearTextureBlend();
            }
        }

        return false;
    }

    private void drawGroup(PoseStack stack, ModelGroup group, Model model, Link texture, float alpha)
    {
        if (texture != null)
        {
            ModelVAORenderer.clearTextureBlend();
            BBSModClient.getTextures().bindTexture(texture);
        }

        float effectivePaintStrength = this.resolveEffectivePaintStrength(group);
        float effectiveGlowStrength = this.resolveEffectiveGlowStrength(group);

        if (ModelVAORenderer.isSuppressShapeKeyMainPassGlow() && effectiveGlowStrength > 0F)
        {
            effectiveGlowStrength = 0F;
        }

        ModelVAORenderer.setGroupPaint(
            this.resolveEffectivePaintR(group),
            this.resolveEffectivePaintG(group),
            this.resolveEffectivePaintB(group),
            effectivePaintStrength
        );
        ModelVAORenderer.setGroupPaintEffectTransform(group.paintColor.transform);
        ModelVAORenderer.setGroupGlowing(
            this.resolveEffectiveGlowR(group),
            this.resolveEffectiveGlowG(group),
            this.resolveEffectiveGlowB(group),
            effectiveGlowStrength
        );
        ModelVAORenderer.setGroupGlowEffectTransform(group.glowingColor.transform);
        ModelVAORenderer.setGroupFormColorGrade(group.color);
        ModelVAORenderer.setGroupColorEffectTransform(group.color.transform);
        ModelVAORenderer.setGroupFormColorTint(group.color);

        float cr = this.r;
        float cg = this.g;
        float cb = this.b;

        if (!group.color.hasActiveTransform())
        {
            cr *= group.color.r;
            cg *= group.color.g;
            cb *= group.color.b;
            alpha *= group.color.a;
        }

        this.setColor(cr, cg, cb, alpha);

        BufferBuilder groupBuilder = Tesselator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, this.pipeline.getVertexFormat());

        super.renderGroup(groupBuilder, stack, group, model);

        MeshData built = groupBuilder.build();

        if (built != null)
        {
            built.close();
        }
    }
}
