package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.settings.ui.UISettingsOverlayPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterSettingsUISectionEvent
{
    private final List<Consumer<UISettingsOverlayPanel>> sectionBuilders = new ArrayList<>();

    public void registerSection(Consumer<UISettingsOverlayPanel> builder)
    {
        if (builder != null)
        {
            this.sectionBuilders.add(builder);
        }
    }

    public List<Consumer<UISettingsOverlayPanel>> getSectionBuilders()
    {
        return this.sectionBuilders;
    }
}
