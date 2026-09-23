package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;

import java.util.function.Consumer;

public class RegisterReplayPanelEvent
{
    public void register(Consumer<UIReplaysOverlayPanel> consumer)
    {
        UIReplaysOverlayPanel.extensions.add(consumer);
    }
}
