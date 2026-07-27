package mchorse.bbs_mod.data;

import mchorse.bbs_mod.data.types.BaseType;

public interface IDataSerializable <T extends BaseType>
{
    public T toData();

    public void fromData(T data);
}