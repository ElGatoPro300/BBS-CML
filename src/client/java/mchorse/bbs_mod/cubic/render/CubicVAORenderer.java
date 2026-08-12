package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;

import net.minecraft.client.renderer.LightTexture;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Map;
import java.util.function.Function;

public class CubicVAORenderer extends CubicCubeRenderer
{
    private RenderPipeline program;
    private ModelInstance model;
    private Function<String, Link> textureResolver;

    public CubicVAORenderer(RenderPipeline program, ModelInstance model, int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, Function<String, Link> textureResolver)
    {
        super(light, overlay, stencilMap, shapeKeys);

        this.program = program;
        this.model = model;
        this.textureResolver = textureResolver;
    }

    @Override
    public boolean renderGroup(BufferBuilder builder, PoseStack stack, ModelGroup group, Model model)
    {
        if (this.stencilMap != null && !this.stencilMap.isBoneAllowed(group.id))
        {
            return false;
        }

        Map<String, ModelVAO> groupVaos = this.model.getVaos().get(group);

        if (groupVaos != null && group.visible)
        {
            float effectiveGlowStrength = this.resolveEffectiveGlowStrength(group);
            float effectiveGlowR = this.resolveEffectiveGlowR(group);
            float effectiveGlowG = this.resolveEffectiveGlowG(group);
            float effectiveGlowB = this.resolveEffectiveGlowB(group);
            float effectivePaintStrength = this.resolveEffectivePaintStrength(group);
            float effectivePaintR = this.resolveEffectivePaintR(group);
            float effectivePaintG = this.resolveEffectivePaintG(group);
            float effectivePaintB = this.resolveEffectivePaintB(group);

            /* Set up lighting and colors */
            float r;
            float g;
            float b;
            float a;

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

            if (!ModelVAORenderer.isGlowingUniformActive())
            {
                if (effectiveGlowStrength != 0F)
                {
                    Color groupColor = new Color().set(r, g, b, a);
                    Color glowColor = new Color().set(effectiveGlowR, effectiveGlowG, effectiveGlowB, 1F);

                    FormColorEffects.blendBrighten(groupColor, glowColor, effectiveGlowStrength);

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
                int u = (int) Lerps.lerp(baseU, LightTexture.FULL_BLOCK, glowLightT);
                int v = groupLight >> 16 & '\uffff';

                groupLight = u | v << 16;
            }

            if (this.stencilMap != null)
            {
                groupLight = this.stencilMap.increment ? group.index : 0;
            }
            else
            {
                int u = (int) Lerps.lerp(groupLight & '\uffff', LightTexture.FULL_BLOCK, MathUtils.clamp(group.lighting, 0F, 1F));
                int v = groupLight >> 16 & '\uffff';

                groupLight = u | v << 16;
            }

            for (Map.Entry<String, ModelVAO> entry : groupVaos.entrySet())
            {
                String material = entry.getKey();
                ModelVAO modelVAO = entry.getValue();

                float currentPaintStrength = effectivePaintStrength;

                if (currentPaintStrength > 0F && !this.groupHasPaintableTexture(group, material))
                {
                    if (ModelVAORenderer.isPaintPass() && effectiveGlowStrength == 0F)
                    {
                        continue;
                    }

                    currentPaintStrength = 0F;
                }

                if (ModelVAORenderer.isPaintPass())
                {
                    if (currentPaintStrength == 0F && effectiveGlowStrength == 0F)
                    {
                        continue;
                    }
                }

                this.bindGroupTexture(group, material);

                ModelVAORenderer.setGroupPaint(effectivePaintR, effectivePaintG, effectivePaintB, currentPaintStrength);
                ModelVAORenderer.setGroupPaintEffectTransform(group.paintColor.transform);
                ModelVAORenderer.setGroupGlowing(effectiveGlowR, effectiveGlowG, effectiveGlowB, effectiveGlowStrength);
                ModelVAORenderer.setGroupGlowEffectTransform(group.glowingColor.transform);
                ModelVAORenderer.setGroupFormColorGrade(group.color);
                ModelVAORenderer.setGroupColorEffectTransform(group.color.transform);
                ModelVAORenderer.setGroupFormColorTint(group.color);

                ModelVAORenderer.render(this.program, modelVAO, stack, r, g, b, a, groupLight, this.overlay);
            }

            ModelVAORenderer.clearTextureBlend();
        }

        return false;
    }

    private void bindGroupTexture(ModelGroup group, String material)
    {
        Link defaultLink = this.textureResolver.apply(material);

        if (defaultLink == null)
        {
            defaultLink = this.model.texture;
        }

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
    private boolean groupHasPaintableTexture(ModelGroup group, String material)
    {
        if (group.textureOverride != null)
        {
            return true;
        }

        if (group.id.startsWith("armor_"))
        {
            return false;
        }

        Link defaultLink = this.textureResolver.apply(material);

        if (defaultLink == null)
        {
            defaultLink = this.model.texture;
        }

        return defaultLink != null;
    }
}