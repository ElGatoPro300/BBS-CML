package mchorse.bbs_mod.ui.film.clips.renderer;

import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.clips.Clip;

public interface IUIClipRenderer <T extends Clip>
{
    public void renderClip(UIContext context, UIClips clips, T clip, Area area, boolean selected, boolean current);
}