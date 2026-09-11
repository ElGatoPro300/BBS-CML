package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.render.special.BbsFormGuiElementRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin
{
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static List<PictureInPictureRenderer<?>> bbs$addBbsRenderers(List<PictureInPictureRenderer<?>> original)
    {
        List<PictureInPictureRenderer<?>> list = new ArrayList<>(original);

        list.add(new BbsFormGuiElementRenderer());

        return list;
    }
}
