package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.resources.Link;

public class TextureBlend
{
    public Link from;
    public Link to;
    public float blend;

    public TextureBlend()
    {}

    public TextureBlend(Link from, Link to, float blend)
    {
        this.from = from;
        this.to = to;
        this.blend = blend;
    }
}
