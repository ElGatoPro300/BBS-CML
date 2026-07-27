package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.list.UILabelList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.utils.Label;

import java.util.Collection;
import java.util.function.Consumer;

public class UILabelListOverlayPanel extends UIOverlayPanel
{
    public UISearchList<Label<String>> strings;

    private Consumer<String> callback;

    public UILabelListOverlayPanel(IKey title, Collection<Label<String>> strings, Consumer<String> callback)
    {
        super(title);

        this.callback = callback;

        this.strings = new UISearchList<>(new UILabelList<>((list) -> this.accept(list.get(0).value)));
        this.strings.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);

        this.strings.list.add(strings);
        this.strings.list.sort();
        this.strings.list.scroll.scrollSpeed *= 2;

        this.content.add(this.strings);
    }

    public UILabelListOverlayPanel callback(Consumer<String> callback)
    {
        this.callback = callback;

        return this;
    }

    protected void accept(String string)
    {
        if (this.callback != null)
        {
            this.callback.accept(string);
        }
    }
}
