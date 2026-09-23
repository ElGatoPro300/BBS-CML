package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;

import java.util.function.BiConsumer;

public class RegisterReplayListContextMenuEvent
{
    public void register(BiConsumer<UIReplayList, ContextMenuManager> consumer)
    {
        UIReplayList.extensions.add(consumer);
    }
}
