package mchorse.bbs_mod.ui.film.clips.renderer;

import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.camera.clips.misc.VideoClip;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.client.video.VideoWaveformCache;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIVideoClipRenderer extends UIClipRenderer<VideoClip>
{
    @Override
    protected void renderBackground(UIContext context, int color, VideoClip clip, Area area, boolean selected, boolean current)
    {
        SoundBuffer buffer = VideoWaveformCache.get(clip.video.get());

        if (buffer != null && buffer.getWaveform() != null)
        {
            int offset = clip.offset.get();

            context.batcher.box(area.x, area.y, area.ex(), area.ey(), Colors.mulRGB(color, 0.6F));
            buffer.getWaveform().render(context.batcher, Colors.WHITE, area.x, area.y, area.w, area.h,
                TimeUtils.toSeconds(offset), TimeUtils.toSeconds(offset + clip.duration.get()));
        }
        else
        {
            super.renderBackground(context, color, clip, area, selected, current);
        }
    }
}
