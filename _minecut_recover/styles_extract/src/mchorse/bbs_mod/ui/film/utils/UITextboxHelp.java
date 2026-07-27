package mchorse.bbs_mod.ui.film.utils;

import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

public class UITextboxHelp extends UITextbox
{
    public UIIcon help;
    public String link = "";

    public UITextboxHelp(int maxLength, Consumer<String> callback)
    {
        super(maxLength, callback);

        this.setup();
    }

    public UITextboxHelp(Consumer<String> callback)
    {
        super(callback);

        this.setup();
    }

    protected void setup()
    {
        this.help = new UIIcon(Icons.HELP, (b) -> UIUtils.openWebLink(this.link));
        this.help.relative(this).x(1F, -1).y(1).wh(18, 18).anchorX(1F);
        this.help.hoverColor(Colors.GRAY).iconColor(Colors.LIGHTEST_GRAY);
        this.add(this.help);
    }

    public UITextboxHelp link(String link)
    {
        this.link = link;

        return this;
    }
}