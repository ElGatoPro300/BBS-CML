package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.bridge.IRenderLayerBridge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.renderpearl.backend.opengl.GlTexture;

import java.util.Map;

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
            Map<String, RenderSetup.TextureBinding> textures = this.state.textures;

            if (textures != null)
            {
                for (RenderSetup.TextureBinding texture : textures.values())
                {
                    if (texture != null && texture.location() != null)
                    {
                        AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(texture.location());

                        if (abstractTexture != null && abstractTexture.getTexture() instanceof GlTexture glTexture)
                        {
                            return glTexture.glId();
                        }
                    }
                }
            }
        }

        return 0;
    }
}
