package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.utils.PNGEncoder;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.resources.Pixels;

import java.io.File;
import java.io.IOException;

public class UITextureManagerPanel extends UIDashboardPanel
{
    public UITexturePicker picker;

    public static void extractTexture(Link link, Pixels pixels, int frames, int w, int h, int x, int y)
    {
        if (pixels == null)
        {
            /* TODO: throw error */

            return;
        }

        int endX = w + x * (frames - 1);
        int endY = h + y * (frames - 1);

        if (endX > pixels.width || endY > pixels.height)
        {
            /* TODO: throw error */

            return;
        }

        for (int i = 0; i < frames; i++)
        {
            Link texture = new Link(link.source, StringUtils.removeExtension(link.path) + "_" + (i + 1) + ".png");
            File file = BBSMod.getProvider().getFile(texture);

            if (file != null)
            {
                Pixels newPixels = Pixels.fromSize(w, h);
                int sx1 = x * i;
                int sy1 = y * i;

                newPixels.drawPixels(pixels, 0, 0, w, h, sx1, sy1, sx1 + w, sy1 + h);

                try
                {
                    PNGEncoder.writeToFile(newPixels, file);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                newPixels.delete();
            }
        }
    }

    public UITextureManagerPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.picker = new UITexturePicker(null).cantBeClosed();
        this.picker.full(this);
        this.picker.fill(null);

        this.add(this.picker);
    }

    public Link getLink()
    {
        return this.picker.current;
    }
}