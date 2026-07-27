package mchorse.bbs_mod.data.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class DataURLStorage extends DataStorage
{
    protected URL url;

    public DataURLStorage(URL url)
    {
        this.url = url;
    }

    public URL getURL()
    {
        return this.url;
    }

    @Override
    protected InputStream getInputStream() throws IOException
    {
        return this.url.openStream();
    }

    @Override
    protected OutputStream getOutputStream() throws IOException
    {
        throw new UnsupportedOperationException("URLs can't be written to!");
    }
}