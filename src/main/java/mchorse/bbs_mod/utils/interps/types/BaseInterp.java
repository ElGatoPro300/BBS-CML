package mchorse.bbs_mod.utils.interps.types;

import mchorse.bbs_mod.utils.interps.IInterp;

public abstract class BaseInterp implements IInterp
{
    public final String key;
    public final int keybind;

    public BaseInterp(String key, int keybind)
    {
        this.key = key;
        this.keybind = keybind;
    }

    @Override
    public String getKey()
    {
        return this.key;
    }

    @Override
    public int getKeyCode()
    {
        return this.keybind;
    }
}
