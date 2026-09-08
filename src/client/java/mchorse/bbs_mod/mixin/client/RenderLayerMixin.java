package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.bridge.IRenderLayerBridge;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.renderers.utils.ModelEffectPass;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.vertex.MeshData;

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

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    public void onDraw(MeshData buffer, CallbackInfo info)
    {
        ModelEffectPass.bound(null);
        CustomVertexConsumerProvider.drawLayer((RenderType) (Object) this);

        if (ModelEffectPass.hasBinding())
        {
            RenderSetup.TextureAndSampler texture = this.state.getTextures().get("Sampler0");

            if (texture != null && texture.textureView().texture() instanceof GlTexture glTexture
                && ModelEffectPass.drawBound(buffer, AdoptedTexture.identifier(glTexture.glId(), glTexture.getWidth(0), glTexture.getHeight(0), false)))
            {
                info.cancel();
            }
        }
    }

    @Override
    public int bbs$getTextureId()
    {
        if (this.state != null)
        {
            Map<String, RenderSetup.TextureAndSampler> textures = this.state.getTextures();

            if (textures != null)
            {
                for (RenderSetup.TextureAndSampler texture : textures.values())
                {
                    if (texture != null && texture.textureView() != null && texture.textureView().texture() instanceof GlTexture glTexture)
                    {
                        return glTexture.glId();
                    }
                }
            }
        }

        return 0;
    }
}
