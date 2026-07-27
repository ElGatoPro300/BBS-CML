package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelVertex;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_4587;
import net.minecraft.class_5944;
import net.minecraft.class_765;
import org.joml.Vector3f;

/**
 * Additive glow overlay for shape-key CPU meshes. Uses the same block/item overlay formula:
 * emissive vertex color, full bright lightmap, and SRC_ALPHA / ONE blending.
 */
public class CubicCpuGlowOverlayRenderer extends CubicCubeRenderer
{
    private final class_5944 shader;
    private final Link defaultTexture;
    private final Color glowLayerColor;
    private final boolean boneGlowOnly;
    private final float overlayIntensity;
    private final String targetGroupId;
    private final boolean skipBoneGlowGroups;

    public CubicCpuGlowOverlayRenderer(int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, class_5944 shader, Link defaultTexture, Color glowLayerColor, boolean boneGlowOnly, float overlayIntensity, String targetGroupId, boolean skipBoneGlowGroups)
    {
        super(light, overlay, stencilMap, shapeKeys);

        this.shader = shader;
        this.defaultTexture = defaultTexture;
        this.glowLayerColor = glowLayerColor;
        this.boneGlowOnly = boneGlowOnly;
        this.overlayIntensity = overlayIntensity;
        this.targetGroupId = targetGroupId;
        this.skipBoneGlowGroups = skipBoneGlowGroups;
    }

    @Override
    public boolean renderGroup(class_287 builder, class_4587 stack, ModelGroup group, Model model)
    {
        if (group.cubes.isEmpty() && group.meshes.isEmpty())
        {
            return false;
        }

        if (this.targetGroupId != null && !this.targetGroupId.equals(group.id))
        {
            return false;
        }

        if (this.skipBoneGlowGroups && group.glowIntensity > 0F)
        {
            return false;
        }

        if (this.boneGlowOnly)
        {
            if (group.glowIntensity <= 0F)
            {
                return false;
            }
        }
        else if (group.glowIntensity < 0F)
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
                /* Glow overlay does not use BBS paint/blend uniforms. */
            }
        }

        return false;
    }

    private void drawGroup(class_4587 stack, ModelGroup group, Model model, Link texture, float alpha)
    {
        if (texture != null)
        {
            BBSModClient.getTextures().bindTexture(texture);
        }

        this.setColor(1F, 1F, 1F, alpha);

        class_287 groupBuilder = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1580);

        super.renderGroup(groupBuilder, stack, group, model);

        try
        {
            this.shader.method_34586();
            class_286.method_43433(groupBuilder.method_60800());
            this.shader.method_34585();
        }
        catch (IllegalStateException e)
        {
            /* Empty or invalid buffer */
        }
    }

    @Override
    protected void writeVertex(class_287 builder, class_4587 stack, ModelGroup group, ModelVertex vertex, Vector3f normal)
    {
        float gr;
        float gg;
        float gb;
        float ga;

        if (!this.boneGlowOnly && group.glowIntensity <= 0F)
        {
            gr = this.glowLayerColor.r * group.color.r;
            gg = this.glowLayerColor.g * group.color.g;
            gb = this.glowLayerColor.b * group.color.b;
            ga = MathUtils.clamp(this.glowLayerColor.a * group.color.a * this.a, 0F, 1F);
        }
        else
        {
            gr = group.glowingColor.r * group.color.r;
            gg = group.glowingColor.g * group.color.g;
            gb = group.glowingColor.b * group.color.b;
            ga = MathUtils.clamp(group.color.a * this.a, 0F, 1F);
        }

        this.vertex.set(vertex.vertex.x, vertex.vertex.y, vertex.vertex.z, 1);
        stack.method_23760().method_23761().transform(this.vertex);

        builder.method_22912(this.vertex.x, this.vertex.y, this.vertex.z)
            .method_22915(gr, gg, gb, ga)
            .method_22913(vertex.uv.x, vertex.uv.y)
            .method_22922(this.overlay);

        if (this.stencilMap != null)
        {
            builder.method_22921(this.stencilMap.increment ? group.index : 0, 0);
        }
        else
        {
            builder.method_60803(class_765.field_32767);
        }

        builder.method_22914(normal.x, normal.y, normal.z);
    }
}
