package mchorse.bbs_mod.utils.resources;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.resources.Link;

public class LinkUtils
{
    public static Link create(String path)
    {
        return path.isEmpty() ? null : Link.create(path);
    }

    public static Link create(String domain, String path)
    {
        return new Link(domain, path);
    }

    public static Link create(BaseType data)
    {
        Link location = MultiLink.from(data);

        if (location != null)
        {
            return location;
        }

        if (BaseType.isString(data))
        {
            return create(data.asString());
        }

        return null;
    }

    public static BaseType toData(Link link)
    {
        if (link instanceof IWritableLink)
        {
            return ((IWritableLink) link).toData();
        }
        else if (link != null)
        {
            return new StringType(link.toString());
        }

        return null;
    }

    public static Link copy(Link link)
    {
        if (link instanceof IWritableLink)
        {
            return ((IWritableLink) link).copy();
        }
        else if (link != null)
        {
            return create(link.toString());
        }

        return null;
    }
}