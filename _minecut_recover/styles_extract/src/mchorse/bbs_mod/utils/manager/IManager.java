package mchorse.bbs_mod.utils.manager;

import mchorse.bbs_mod.data.IDataSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.Collection;

public interface IManager <T extends IDataSerializable>
{
    boolean exists(String name);

    public default T create(String id)
    {
        return this.create(id, null);
    }

    public T create(String id, MapType data);

    public T load(String id);

    public boolean save(String name, MapType mapType);

    public boolean rename(String from, String to);

    public boolean delete(String name);

    public Collection<String> getKeys();
}