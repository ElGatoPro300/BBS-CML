package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_4587;
import net.minecraft.class_5944;

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
    private final class_5944 shader;
    private final Link defaultTexture;

    public CubicCpuGroupDrawRenderer(int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, class_5944 shader, Link defaultTexture)
    {
        super(light, overlay, stencilMap, shapeKeys);

        this.shader = shader;
        this.defaultTexture = defaultTexture;
    }

    @Override
    public boolean renderGroup(class_287 builder, class_4587 stack, ModelGroup group, Model model)
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

    private void drawGroup(class_4587 stack, ModelGroup group, Model model, Link texture, float alpha)
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
        ModelVAORenderer.setGroupGlowing(
            this.resolveEffectiveGlowR(group),
            this.resolveEffectiveGlowG(group),
            this.resolveEffectiveGlowB(group),
            effectiveGlowStrength
        );
        ModelVAORenderer.setGroupFormColorGrade(group.color);

        float cr = this.r;
        float cg = this.g;
        float cb = this.b;

        this.setColor(cr, cg, cb, alpha);

        class_287 groupBuilder = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1580);

        ModelVAORenderer.beginCpuGeometry(this.shader);
        super.renderGroup(groupBuilder, stack, group, model);

        try
        {
            this.shader.method_34586();
            ModelVAORenderer.setupUniformsCpuPretransformed(this.shader);
            class_286.method_43433(groupBuilder.method_60800());
            this.shader.method_34585();
        }
        catch (IllegalStateException e)
        {
            /* Empty or invalid buffer */
        }
    }
}
