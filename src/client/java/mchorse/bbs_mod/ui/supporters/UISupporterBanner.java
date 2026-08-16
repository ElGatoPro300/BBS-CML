package mchorse.bbs_mod.ui.supporters;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.opengl.GL11;

public class UISupporterBanner extends UIElement
{
    public static final String LINK_BILIBILI = "space.bilibili.com";
    public static final String LINK_TIKTOK = "douyin.com";
    public static final String LINK_TIKTOK2 = "tiktok.com";
    public static final String LINK_YOUTUBE = "youtube.com";
    public static final String LINK_TWITTER = "twitter.com";
    public static final String LINK_TWITTER2 = "x.com";
    public static final String LINK_TWITCH = "twitch.com";

    private UIElement placeholder;

    private Supporter supporter;
    private int randomColor;

    public static UIElement createLinkIcon(Supporter supporter)
    {
        return new UIIcon(getFromLink(supporter.link), (b) ->
        {
            if (!supporter.link.isEmpty() && !supporter.link.equals("..."))
            {
                UIUtils.openWebLink(supporter.link);
            }
        });
    }

    public static UIElement createLinkEntry(Supporter supporter)
    {
        return UI.row(
            UI.label(IKey.constant(supporter.name), 20).labelAnchor(0F, 0.5F),
            UISupporterBanner.createLinkIcon(supporter)
        );
    }

    public static Icon getFromLink(String link)
    {
        if (link.contains(LINK_BILIBILI))
        {
            return Icons.BILIBILI;
        }
        else if (link.contains(LINK_TIKTOK) || link.contains(LINK_TIKTOK2))
        {
            return Icons.TIKTOK;
        }
        else if (link.contains(LINK_YOUTUBE))
        {
            return Icons.YOUTUBE;
        }
        else if (link.contains(LINK_TWITTER) || link.contains(LINK_TWITTER2))
        {
            return Icons.TWITTER;
        }
        else if (link.contains(LINK_TWITCH))
        {
            return Icons.TWITCH;
        }
        else if (link.isEmpty() || link.equals("..."))
        {
            return Icons.NONE;
        }

        return Icons.LINK;
    }

    public UISupporterBanner(Supporter supporter)
    {
        super();

        this.supporter = supporter;

        this.column(0).vertical().stretch();

        UIElement row = createLinkEntry(supporter);

        this.placeholder = new UIElement();
        this.placeholder.relative(this).w(1F).h(70);
        this.randomColor = Colors.HSVtoRGB((float) Math.random(), 1F, 1F).getARGBColor();

        this.add(row, this.placeholder);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        Area a = this.placeholder.area;
        int color1 = this.randomColor | Colors.A100;
        int color2 = Colors.mulRGB(color1, 0.75F);

        if (this.supporter.banner != null && !this.supporter.banner.path.equals("..."))
        {
            Texture texture = BBSModClient.getTextures().getTexture(this.supporter.banner, GL11.GL_LINEAR);

            context.batcher.fullTexturedBox(texture, a.x, a.y, a.w, a.h);
        }
        else
        {
            context.batcher.box(a.x, a.y, a.w, a.h, color2, color1, color1, color2);
        }
    }
}