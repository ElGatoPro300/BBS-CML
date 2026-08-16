package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

public class UILoader extends UIElement
{
    private IKey label = UIKeys.NEWS_IMAGE_LOADING;
    private float scale = 2.25F;

    public static void draw(UIContext context, float cx, float cy, float scale, IKey label)
    {
        long frame = System.currentTimeMillis() / 200L;
        int index = (int) (frame % 3L);
        Icon icon;

        if (index == 0)
        {
            icon = Icons.LOADING_BBS_1;
        }
        else if (index == 1)
        {
            icon = Icons.LOADING_BBS_2;
        }
        else
        {
            icon = Icons.LOADING_BBS_3;
        }

        float iw = icon.w * scale;
        float ih = icon.h * scale;

        Texture atlas = BBSModClient.getTextures().getTexture(icon.texture);
        context.batcher.texturedBox(
            atlas,
            Colors.WHITE,
            cx - iw / 2F,
            cy - ih / 2F,
            iw,
            ih,
            icon.x,
            icon.y,
            icon.x + icon.w,
            icon.y + icon.h,
            icon.textureW,
            icon.textureH
        );

        if (label != null)
        {
            String text = label.get();
            int lw = context.batcher.getFont().getWidth(text);
            context.batcher.textShadow(text, cx - lw / 2F, cy + ih / 2F + 4, Colors.LIGHTER_GRAY);
        }
    }

    public UILoader()
    {
        super();
    }

    public UILoader(IKey label)
    {
        super();
        this.label = label;
    }

    public UILoader label(IKey label)
    {
        this.label = label;
        return this;
    }

    public UILoader scale(float scale)
    {
        this.scale = scale;
        return this;
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);
        draw(context, this.area.mx(), this.area.my() - 12, this.scale, this.label);
    }
}
