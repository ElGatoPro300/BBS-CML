package mchorse.bbs_mod.client.renderer;

import net.minecraft.util.LightCoordsUtil;

public class LightTexture
{
    public static final int FULL_BRIGHT = LightCoordsUtil.FULL_BRIGHT;
    public static final int FULL_SKY = LightCoordsUtil.FULL_SKY;
    public static final int FULL_BLOCK = LightCoordsUtil.pack(0, 0);

    public static int pack(int block, int sky)
    {
        return LightCoordsUtil.pack(block, sky);
    }

    public static int block(int light)
    {
        return LightCoordsUtil.block(light);
    }

    public static int sky(int light)
    {
        return LightCoordsUtil.sky(light);
    }
}
