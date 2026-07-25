package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

public class CubicVAORenderer extends CubicCubeRenderer
{
    private ShaderProgram program;
    private ModelInstance model;
    private Link defaultTexture;

    public CubicVAORenderer(ShaderProgram program, ModelInstance model, int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, Link defaultTexture)
    {
        super(light, overlay, stencilMap, shapeKeys);

        this.program = program;
        this.model = model;
        this.defaultTexture = defaultTexture;
    }

    @Override
    public boolean renderGroup(BufferBuilder builder, MatrixStack stack, ModelGroup group, Model model)
    {
        if (this.stencilMap != null && !this.stencilMap.isBoneAllowed(group.id))
        {
            return false;
        }

        ModelVAO modelVAO = this.model.getVaos().get(group);

        if (modelVAO != null && group.visible)
        {
            float r;
            float g;
            float b;
            float a;
            float effectiveGlowStrength = this.resolveEffectiveGlowStrength(group);
            float effectiveGlowR = this.resolveEffectiveGlowR(group);
            float effectiveGlowG = this.resolveEffectiveGlowG(group);
            float effectiveGlowB = this.resolveEffectiveGlowB(group);
            float effectivePaintStrength = this.resolveEffectivePaintStrength(group);
            float effectivePaintR = this.resolveEffectivePaintR(group);
            float effectivePaintG = this.resolveEffectivePaintG(group);
            float effectivePaintB = this.resolveEffectivePaintB(group);

            if (effectivePaintStrength > 0F && !this.groupHasPaintableTexture(group))
            {
                if (ModelVAORenderer.isPaintPass() && effectiveGlowStrength == 0F)
                {
                    return false;
                }

                effectivePaintStrength = 0F;
            }

            if (ModelVAORenderer.isPaintPass())
            {
                if (effectivePaintStrength == 0F && effectiveGlowStrength == 0F)
                {
                    return false;
                }

                ModelVAORenderer.setGroupPaint(effectivePaintR, effectivePaintG, effectivePaintB, effectivePaintStrength);
                ModelVAORenderer.setGroupPaintEffectTransform(group.paintColor.transform);
                ModelVAORenderer.setGroupGlowing(
                    effectiveGlowR,
                    effectiveGlowG,
                    effectiveGlowB,
                    effectiveGlowStrength);
                ModelVAORenderer.setGroupFormColorGrade(group.color);
                ModelVAORenderer.setGroupColorEffectTransform(group.color.transform);
                ModelVAORenderer.setGroupFormColorTint(group.color);

                this.bindGroupTexture(group);

                if (group.color.hasActiveTransform())
                {
                    r = this.r;
                    g = this.g;
                    b = this.b;
                    a = this.a;
                }
                else
                {
                    r = this.r * group.color.r;
                    g = this.g * group.color.g;
                    b = this.b * group.color.b;
                    a = this.a * group.color.a;
                }
            }
            else
            {
                this.bindGroupTexture(group);

                ModelVAORenderer.setGroupPaint(effectivePaintR, effectivePaintG, effectivePaintB, effectivePaintStrength);
                ModelVAORenderer.setGroupPaintEffectTransform(group.paintColor.transform);
                ModelVAORenderer.setGroupGlowing(effectiveGlowR, effectiveGlowG, effectiveGlowB, effectiveGlowStrength);
                ModelVAORenderer.setGroupFormColorGrade(group.color);
                ModelVAORenderer.setGroupColorEffectTransform(group.color.transform);
                ModelVAORenderer.setGroupFormColorTint(group.color);

                if (group.color.hasActiveTransform())
                {
                    r = this.r;
                    g = this.g;
                    b = this.b;
                    a = this.a;
                }
                else
                {
                    r = this.r * group.color.r;
                    g = this.g * group.color.g;
                    b = this.b * group.color.b;
                    a = this.a * group.color.a;
                }
            }

            if (!ModelVAORenderer.isGlowingUniformActive())
            {
                if (effectiveGlowStrength != 0F)
                {
                    Color groupColor = new Color().set(r, g, b, a);
                    Color glowColor = new Color().set(effectiveGlowR, effectiveGlowG, effectiveGlowB, 1F);

                    FormColorBlend.blendBrighten(groupColor, glowColor, effectiveGlowStrength);

                    r = groupColor.r;
                    g = groupColor.g;
                    b = groupColor.b;
                    a = groupColor.a;
                }
            }

            int groupLight = this.light;

            if (effectiveGlowStrength != 0F && !ModelVAORenderer.isGlowingUniformActive() && !ModelVAORenderer.isPaintOverlayPass())
            {
                float glowLightT = MathUtils.clamp(Math.abs(effectiveGlowStrength), 0F, 1F);
                int baseU = groupLight & '\uffff';
                int u = (int) Lerps.lerp(baseU, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, glowLightT);
                int v = groupLight >> 16 & '\uffff';

                groupLight = u | v << 16;
            }

            if (this.stencilMap != null)
            {
                groupLight = this.stencilMap.increment ? group.index : 0;
            }
            else
            {
                int u = (int) Lerps.lerp(groupLight & '\uffff', LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, MathUtils.clamp(group.lighting, 0F, 1F));
                int v = groupLight >> 16 & '\uffff';

                groupLight = u | v << 16;
            }

            ModelVAORenderer.render(this.program, modelVAO, stack, r, g, b, a, groupLight, this.overlay);
            ModelVAORenderer.clearTextureBlend();
        }

        return false;
    }

    private void bindGroupTexture(ModelGroup group)
    {
        Link defaultLink = this.defaultTexture != null ? this.defaultTexture : this.model.texture;

        if (group.textureOverride == null)
        {
            ModelVAORenderer.clearTextureBlend();
            BBSModClient.getTextures().bindTexture(defaultLink);

            return;
        }

        float blend = group.textureBlend;

        if (blend >= 1F)
        {
            ModelVAORenderer.clearTextureBlend();
            BBSModClient.getTextures().bindTexture(group.textureOverride);
        }
        else if (blend <= 0F)
        {
            ModelVAORenderer.clearTextureBlend();
            BBSModClient.getTextures().bindTexture(defaultLink);
        }
        else
        {
            BBSModClient.getTextures().bindTexture(defaultLink);
            ModelVAORenderer.setTextureBlend(group.textureOverride, blend);
        }
    }

    /**
     * Paint overlay should only touch groups that can sample a real texture.
     * Armor shell groups without a picked bone texture must not receive paint.
     */
    private boolean groupHasPaintableTexture(ModelGroup group)
    {
        if (group.textureOverride != null)
        {
            return true;
        }

        if (group.id.startsWith("armor_"))
        {
            return false;
        }

        Link defaultLink = this.defaultTexture != null ? this.defaultTexture : this.model.texture;

        return defaultLink != null;
    }
}