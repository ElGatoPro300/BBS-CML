package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.bridge.IRenderLayerBridge;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.renderers.utils.ModelEffectPass;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderType.class)
public class RenderLayerMixin implements IRenderLayerBridge
{
    @Shadow
    private RenderSetup state;

    @Override
    public int bbs$getTextureId()
    {
        if (this.state != null)
        {
            try
            {
                List<PreparedRenderType.Texture> textures =
                    this.state.prepareTextures(
                        Minecraft.getInstance().getTextureManager(),
                        RenderSystem.getSamplerCache(),
                        null, null);

                if (textures != null)
                {
                    for (PreparedRenderType.Texture texture : textures)
                    {
                        if (texture != null && texture.textureView() != null && texture.textureView().texture() instanceof GlTexture glTexture)
                        {
                            return glTexture.glId();
                        }
                    }
                }
            }
            catch (Throwable ignored)
            {
            }
        }

        return 0;
    }
}
