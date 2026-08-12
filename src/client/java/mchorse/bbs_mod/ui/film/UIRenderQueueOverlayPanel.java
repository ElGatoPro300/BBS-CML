package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.FFMpegUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;

import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class UIRenderQueueOverlayPanel extends UIOverlayPanel
{
    private final UIFilmPanel panel;

    public UIRenderQueueList queueList;
    public UIIcon addEntry;
    public UIIcon removeEntry;
    public UIElement previewArea;
    public UIButton renderButton;

    public UIRenderQueueOverlayPanel(UIFilmPanel panel)
    {
        super(UIKeys.RENDER_QUEUE_TITLE);

        this.panel = panel;

        /* Queue list */
        this.queueList = new UIRenderQueueList((list) ->
        {});
        this.queueList.background();
        this.queueList.relative(this.content).xy(0, 20).w(180).h(1F, -20);

        /* Add / remove icon buttons above the list */
        this.addEntry = new UIIcon(Icons.ADD, (b) -> this.openFilmPicker());
        this.addEntry.tooltip(UIKeys.RENDER_QUEUE_ADD, Direction.TOP);
        this.addEntry.relative(this.content).xy(0, 0).wh(20, 20);

        this.removeEntry = new UIIcon(Icons.REMOVE, (b) -> this.removeSelectedEntry());
        this.removeEntry.tooltip(UIKeys.RENDER_QUEUE_REMOVE, Direction.TOP);
        this.removeEntry.relative(this.content).xy(20, 0).wh(20, 20);

        /* Preview area to the right of the list */
        this.previewArea = new UIElement();
        this.previewArea.relative(this.content).x(186).y(0).w(1F, -186).h(1F, -26);

        /* Render button bottom-right */
        this.renderButton = new UIButton(UIKeys.RENDER_QUEUE_RENDER, (b) -> this.startRender());
        this.renderButton.relative(this.content).x(1F, -86).y(1F, -22).wh(82, 20);

        this.content.add(this.addEntry, this.removeEntry, this.queueList, this.previewArea, this.renderButton);
    }

    private void openFilmPicker()
    {
        UIFilmPickerOverlayPanel picker = new UIFilmPickerOverlayPanel((name) ->
        {
            if (!this.queueList.getList().contains(name))
            {
                this.queueList.add(name);
            }
        });

        UIOverlay.addOverlay(this.getContext(), picker, 240, 0.7F);
    }

    private void removeSelectedEntry()
    {
        String selected = this.queueList.getCurrentFirst();

        if (selected != null)
        {
            this.queueList.remove(selected);
        }
    }

    private void startRender()
    {
        List<String> queue = this.queueList.getList();

        if (queue.isEmpty())
        {
            return;
        }

        if (!FFMpegUtils.checkFFMPEG())
        {
            UIOverlay.addOverlay(this.getContext(), new UIMessageOverlayPanel(UIKeys.GENERAL_WARNING, UIKeys.GENERAL_FFMPEG_ERROR_DESCRIPTION));

            return;
        }

        List<String> remaining = new ArrayList<>(queue);

        this.close();
        this.renderNext(remaining);
    }

    private void renderNext(List<String> remaining)
    {
        if (remaining.isEmpty())
        {
            return;
        }

        String filmName = remaining.remove(0);

        ContentType.FILMS.getRepository().load(filmName, (data) ->
        {
            Film film = (Film) data;

            this.panel.fill(film);
            this.panel.recorder.onStop = () -> this.renderNext(remaining);
            this.panel.recorder.startRecordingAfterLoad(film.camera.calculateDuration(), BBSRendering.getTexture(), 60);
        });
    }

    private Area calcPreviewViewport()
    {
        int width = BBSRendering.getVideoWidth();
        int height = BBSRendering.getVideoHeight();

        if (width <= 0 || height <= 0)
        {
            return this.previewArea.area;
        }

        Vector2i size = Vectors.resize(width / (float) height, this.previewArea.area.w, this.previewArea.area.h);
        Area area = new Area();

        area.setSize(size.x, size.y);
        area.setPos(
            this.previewArea.area.mx() - area.w / 2,
            this.previewArea.area.my() - area.h / 2
        );

        return area;
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        /* Dark background for the whole preview area */
        context.batcher.box(
            this.previewArea.area.x,
            this.previewArea.area.y,
            this.previewArea.area.ex(),
            this.previewArea.area.ey(),
            Colors.A100
        );

        /* Render the current film render texture in the preview */
        Texture texture = BBSRendering.getTexture();

        if (texture != null && texture.id > 0)
        {
            Area vp = this.calcPreviewViewport();

            context.batcher.flush();
            context.batcher.texturedBox(
                texture.id, Colors.WHITE,
                vp.x, vp.y, vp.w, vp.h,
                0, texture.height, texture.width, 0,
                texture.width, texture.height
            );
        }
        else
        {
            String label = UIKeys.RENDER_QUEUE_NO_PREVIEW.get();
            int tw = context.batcher.getFont().getWidth(label);
            int th = context.batcher.getFont().getHeight();

            context.batcher.text(
                label,
                this.previewArea.area.mx() - tw / 2,
                this.previewArea.area.my() - th / 2,
                Colors.GRAY
            );
        }
    }
}
