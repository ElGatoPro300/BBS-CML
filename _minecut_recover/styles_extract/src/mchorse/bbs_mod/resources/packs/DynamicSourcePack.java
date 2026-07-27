package mchorse.bbs_mod.resources.packs;

import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class DynamicSourcePack implements ISourcePack
{
    private ISourcePack main;
    private ISourcePack secondary;

    public DynamicSourcePack(ISourcePack main)
    {
        this.main = main;
    }

    public void setSecondary(ISourcePack secondary)
    {
        this.secondary = secondary;
    }

    public ISourcePack getSourcePack()
    {
        return this.secondary == null ? this.main : this.secondary;
    }

    @Override
    public String getPrefix()
    {
        return this.getSourcePack().getPrefix();
    }

    @Override
    public boolean hasAsset(Link link)
    {
        return this.getSourcePack().hasAsset(link);
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        return this.getSourcePack().getAsset(link);
    }

    @Override
    public File getFile(Link link)
    {
        return this.getSourcePack().getFile(link);
    }

    @Override
    public Link getLink(File file)
    {
        return this.getSourcePack().getLink(file);
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
        this.getSourcePack().getLinksFromPath(links, link, recursive);
    }
}