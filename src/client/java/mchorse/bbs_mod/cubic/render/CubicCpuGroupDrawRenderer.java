package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Shape-key CPU geometry must draw one model group per call so PaintColor, GlowingColor, and
 * per-bone texture crossfade uniforms match the group that was just meshed.
 * <p>
 * Positions and normals are written already transformed by the render stack. Uniforms must use
 * {@link ModelVAORenderer#setupUniformsCpuPretransformed} so {@code ModelViewMat} / {@code NormalMat}
 * are not applied a second time (second ModelView hides composites; second NormalMat inverts lighting).
 */
public class CubicCpuGroupDrawRenderer extends CubicCubeRenderer
{
    private final ShaderProgram shader;
    private final Link defaultTexture;

    public CubicCpuGroupDrawRenderer(int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, ShaderProgram shader, Link defaultTexture)
    {
        super(light, overlay, stencilMap, shapeKeys);

        this.shader = shader;
        this.defaultTexture = defaultTexture;
    }

    @Override
    public boolean renderGroup(BufferBuilder builder, MatrixStack stack, ModelGroup group, Model model)
    {
        if (group.cubes.isEmpty() && group.meshes.isEmpty())
        {
            return false;
        }

        CubicGroupTextureBlend textureBlend = CubicGroupTextureBlend.resolve(group, this.defaultTexture);

        if (textureBlend != null && textureBlend.isPartial() && !CubicGroupTextureBlend.supportsShader(this.shader))
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
            CubicGroupTextureBlend.bindForDraw(this.shader, textureBlend, this.defaultTexture);

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

    private void drawGroup(MatrixStack stack, ModelGroup group, Model model, Link texture, float alpha)
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

        float savedR = this.r;
        float savedG = this.g;
        float savedB = this.b;
        float savedA = this.a;

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

        BufferBuilder groupBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        ModelVAORenderer.beginCpuGeometry(this.shader);
        super.renderGroup(groupBuilder, stack, group, model);

        try
        {
            this.shader.bind();
            ModelVAORenderer.setupUniformsCpuPretransformed(this.shader);
            BufferRenderer.drawWithGlobalProgram(groupBuilder.end());
            this.shader.unbind();
        }
        catch (IllegalStateException e)
        {
            /* Empty or invalid buffer */
        }

        this.setColor(savedR, savedG, savedB, savedA);
    }
}
