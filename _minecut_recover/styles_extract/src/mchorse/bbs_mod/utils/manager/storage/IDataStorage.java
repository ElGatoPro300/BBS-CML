package mchorse.bbs_mod.utils.manager.storage;

import mchorse.bbs_mod.data.types.MapType;

import java.io.File;
import java.io.IOException;

public interface IDataStorage
{
    public MapType load(File file) throws IOException;

    public void save(File file, MapType data) throws IOException;
}