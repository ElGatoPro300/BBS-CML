package mchorse.bbs_mod.audio.wav;

import mchorse.bbs_mod.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class WaveList
{
    public String type;
    public List<Pair<String, String>> entries = new ArrayList<>();

    public WaveList(String type)
    {
        this.type = type;
    }
}