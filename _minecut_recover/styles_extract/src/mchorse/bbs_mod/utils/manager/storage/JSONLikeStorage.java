package mchorse.bbs_mod.utils.manager.storage;

import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;

import java.io.File;
import java.io.IOException;

public class JSONLikeStorage implements IDataStorage
{
    private boolean json;

    public JSONLikeStorage json()
    {
        this.json = true;

        return this;
    }

    @Override
    public MapType load(File file) throws IOException
    {
        return (MapType) DataToString.read(file);
    }

    @Override
    public void save(File file, MapType data) throws IOException
    {
        DataToString.write(file, data, this.json);
    }
}