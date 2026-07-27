package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.resources.LinkUtils;
import mchorse.bbs_mod.utils.resources.MultiLink;

public class LinkKeyframeFactory implements IKeyframeFactory<Link>
{
    @Override
    public Link fromData(BaseType data)
    {
        return LinkUtils.create(data);
    }

    @Override
    public BaseType toData(Link value)
    {
        return LinkUtils.toData(value);
    }

    @Override
    public Link createEmpty()
    {
        return Link.create("bbs:textures/error.png");
    }

    @Override
    public Link copy(Link value)
    {
        return LinkUtils.copy(value);
    }

    @Override
    public Link interpolate(Link preA, Link a, Link b, Link postB, IInterp interpolation, float x)
    {
        if (!this.canAnimate(a, b))
        {
            return a;
        }

        Integer lastFrame = this.extractFrame(a.path);
        Integer currentFrame = this.extractFrame(b.path);

        if (lastFrame != null && currentFrame != null)
        {
            int frame = Math.round(interpolation.interpolate(lastFrame, currentFrame, x));

            return new Link(b.source, this.replaceFrame(b.path, frame));
        }

        return a;
    }

    private boolean canAnimate(Link a, Link b)
    {
        if (b == null || a == null)
        {
            return false;
        }

        if (b instanceof MultiLink || a instanceof MultiLink)
        {
            return false;
        }

        return b.source.equals(a.source);
    }

    private Integer extractFrame(String path)
    {
        int lastUnderscore = path.lastIndexOf('_');
        int lastDot = path.lastIndexOf('.');

        if (lastUnderscore < 0 || lastDot < 0)
        {
            return null;
        }

        try
        {
            return Integer.parseInt(path.substring(lastUnderscore + 1, lastDot));
        }
        catch (Exception e)
        {}

        return null;
    }

    private String replaceFrame(String path, int frame)
    {
        int lastUnderscore = path.lastIndexOf('_');
        int lastDot = path.lastIndexOf('.');

        if (lastUnderscore < 0 || lastDot < 0)
        {
            return null;
        }

        return path.substring(0, lastUnderscore + 1) + frame + path.substring(lastDot);
    }
}