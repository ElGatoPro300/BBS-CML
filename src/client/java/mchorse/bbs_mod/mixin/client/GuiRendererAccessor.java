package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor
{
    @Accessor("renderState")
    GuiRenderState bbs$getRenderState();

    @Mutable
    @Accessor("renderState")
    void bbs$setRenderState(GuiRenderState state);
}
