package mchorse.bbs_mod.graphics;

import mchorse.bbs_mod.ui.framework.BbsGuiScale;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.List;
import java.util.function.Consumer;

/**
 * Owns the immediate film overlay batch independently of the extracted screen.
 */
public final class WorldOverlayRenderer implements AutoCloseable
{
    private final GuiRenderState state = new GuiRenderState();
    private final GuiRenderer renderer;
    private boolean active;

    public static WorldOverlayRenderer get()
    {
        return ((Provider) Minecraft.getInstance().gameRenderer).bbs$getWorldOverlays();
    }

    public WorldOverlayRenderer(SubmitNodeStorage storage, FeatureRenderDispatcher dispatcher)
    {
        this.renderer = new GuiRenderer(this.state, dispatcher, List.of());
    }

    public void render(Consumer<Batcher2D> draw)
    {
        BbsGuiScale.withBbsWindowScale(() ->
        {
            Minecraft client = Minecraft.getInstance();
            WindowRenderState window = client.gameRenderer.gameRenderState().windowRenderState;
            int previousWidth = window.width;
            int previousHeight = window.height;
            int previousScale = window.guiScale;

            this.state.reset();
            this.active = true;

            try
            {
                window.width = client.gameRenderer.mainRenderTarget().width;
                window.height = client.gameRenderer.mainRenderTarget().height;
                window.guiScale = client.getWindow().getGuiScale();
                draw.accept(new Batcher2D(new GuiGraphicsExtractor(client, this.state,
                    client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight())));
                this.flush();
            }
            finally
            {
                this.active = false;
                this.state.reset();
                window.width = previousWidth;
                window.height = previousHeight;
                window.guiScale = previousScale;
                this.renderer.endFrame();
            }
        });
    }

    public void flush()
    {
        if (this.active)
        {
            this.renderer.render();
        }
    }

    @Override
    public void close()
    {
        this.renderer.close();
    }

    public interface Provider
    {
        WorldOverlayRenderer bbs$getWorldOverlays();
    }
}
