package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelVertex;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Vector3f;

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

        float savedA = this.a;

        this.setColor(this.r, this.g, this.b, alpha);

        BufferBuilder groupBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        ModelVAORenderer.beginCpuGeometry(this.shader);
        super.renderGroup(groupBuilder, stack, group, model);

        try
        {
            RenderSystem.setShaderColor(this.r, this.g, this.b, alpha);
            this.shader.bind();

            if (this.shader.colorModulator != null)
            {
                this.shader.colorModulator.set(this.r, this.g, this.b, alpha);
            }

            ModelVAORenderer.setupUniformsCpuPretransformed(this.shader);
            BufferRenderer.drawWithGlobalProgram(groupBuilder.end());
            this.shader.unbind();
        }
        catch (IllegalStateException e)
        {
            /* Empty or invalid buffer */
        }
        finally
        {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            if (this.shader.colorModulator != null)
            {
                this.shader.colorModulator.set(1F, 1F, 1F, 1F);
            }
        }

        this.setColor(this.r, this.g, this.b, savedA);
    }

    @Override
    protected void writeVertex(BufferBuilder builder, MatrixStack stack, ModelGroup group, ModelVertex vertex, Vector3f normal)
    {
        this.vertex.set(vertex.vertex.x, vertex.vertex.y, vertex.vertex.z, 1);
        stack.peek().getPositionMatrix().transform(this.vertex);

        float vr = 1F;
        float vg = 1F;
        float vb = 1F;
        float va = 1F;

        if (!group.color.hasActiveTransform())
        {
            vr = group.color.r;
            vg = group.color.g;
            vb = group.color.b;
            va = group.color.a;
        }

        builder.vertex(this.vertex.x, this.vertex.y, this.vertex.z)
            .color(
                MathUtils.clamp(vr, 0F, 1F),
                MathUtils.clamp(vg, 0F, 1F),
                MathUtils.clamp(vb, 0F, 1F),
                MathUtils.clamp(va, 0F, 1F)
            )
            .texture(vertex.uv.x, vertex.uv.y)
            .overlay(this.overlay);

        if (this.stencilMap != null)
        {
            builder.light(this.stencilMap.increment ? group.index : 0, 0);
        }
        else
        {
            int u = (int) Lerps.lerp(this.light & '\uffff', LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, MathUtils.clamp(group.lighting, 0F, 1F));
            int v = this.light >> 16 & '\uffff';

            builder.light(u, v);
        }

        builder.normal(normal.x, normal.y, normal.z);
    }
}
