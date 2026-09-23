package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.film.clips.renderer.IUIClipRenderer;
import mchorse.bbs_mod.ui.film.clips.renderer.UIClipRenderers;
import mchorse.bbs_mod.utils.clips.Clip;

/**
 * Posted on the client once BBS has registered how its built-in clips draw on a timeline.
 */
public class RegisterClipRenderersEvent
{
    public void register(Class<? extends Clip> clazz, IUIClipRenderer renderer)
    {
        UIClipRenderers.registerExtra(clazz, renderer);
    }
}
