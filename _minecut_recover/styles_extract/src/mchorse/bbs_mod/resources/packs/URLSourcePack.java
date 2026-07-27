package mchorse.bbs_mod.resources.packs;

import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Set;

public class URLSourcePack implements ISourcePack
{
    /**
     * Look, MA! I'm Google Chrome on OS X!!! 😂
     */
    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36";

    private final String prefix;
    private final URLRepository repository;

    public static InputStream downloadImage(final Link url) throws IOException
    {
        URLConnection connection = new URL(url.toString()).openConnection();

        connection.setRequestProperty("User-Agent", "curl/8.9.0");
        connection.setRequestProperty("Accept", "*/*");

        String type = connection.getHeaderField("Content-Type");

        if (type == null || !type.startsWith("image/"))
        {
            return null;
        }

        return connection.getInputStream();
    }

    public URLSourcePack(String prefix, URLRepository repository)
    {
        this.prefix = prefix;
        this.repository = repository;
    }

    @Override
    public String getPrefix()
    {
        return this.prefix;
    }

    @Override
    public boolean hasAsset(Link link)
    {
        return link.source.equals(this.prefix);
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        String url = link.toString();
        File file = this.repository.getFile(url);

        if (file != null)
        {
            return new FileInputStream(file);
        }

        try
        {
            InputStream inputStream = downloadImage(link);
            File outFile = this.repository.convertInputStream(url, inputStream);

            return outFile == null ? null : new FileInputStream(outFile);
        }
        catch (Exception e)
        {
            URLTextureErrorCallback.EVENT.invoker().onError(url, URLError.HTTP_ERROR);
        }

        return null;
    }

    @Override
    public File getFile(Link link)
    {
        return null;
    }

    @Override
    public Link getLink(File file)
    {
        return null;
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
        Set<String> strings = this.repository.getCache().keySet();

        for (String string : strings)
        {
            if (!string.startsWith(link.source + ":"))
            {
                continue;
            }

            if (recursive)
            {
                links.add(Link.create(string));
            }
            else
            {
                String toString = link.toString();

                if (string.length() > toString.length())
                {
                    String newString = string.endsWith("/") ? string : string + "/";
                    String suffix = newString.substring(toString.length());

                    if (suffix.contains("/"))
                    {
                        links.add(Link.create(string));
                    }
                }
            }
        }
    }
}