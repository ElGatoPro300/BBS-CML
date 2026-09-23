package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.utils.clips.Clip;

/**
 * Posted on the client once BBS has registered the editor panels of its built-in clips.
 */
public class RegisterClipPanelsEvent
{
    public <T extends Clip> void register(Class<T> clazz, UIClip.IUIClipFactory<T> factory)
    {
        UIClip.register(clazz, factory);
    }
}
