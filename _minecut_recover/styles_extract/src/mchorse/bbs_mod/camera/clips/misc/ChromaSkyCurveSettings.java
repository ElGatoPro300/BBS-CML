package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

public class ChromaSkyCurveSettings
{
    public boolean enabled;
    public Color color = Color.rgb(Colors.A75);
    public boolean terrain = true;
    public boolean clouds = true;
    public float billboard;

    public ChromaSkyCurveSettings copy()
    {
        ChromaSkyCurveSettings output = new ChromaSkyCurveSettings();

        output.enabled = this.enabled;
        output.color = this.color.copy();
        output.terrain = this.terrain;
        output.clouds = this.clouds;
        output.billboard = this.billboard;

        return output;
    }

    public MapType toData()
    {
        MapType data = new MapType();

        data.putBool("enabled", this.enabled);
        data.putInt("color", this.color.getRGBColor());
        data.putBool("terrain", this.terrain);
        data.putBool("clouds", this.clouds);
        data.putFloat("billboard", this.billboard);

        return data;
    }

    public void fromData(MapType data)
    {
        if (data == null)
        {
            return;
        }

        if (data.has("enabled")) this.enabled = data.getBool("enabled");
        if (data.has("color")) this.color.set(data.getInt("color"), false);
        if (data.has("terrain")) this.terrain = data.getBool("terrain");
        if (data.has("clouds")) this.clouds = data.getBool("clouds");
        if (data.has("billboard")) this.billboard = data.getFloat("billboard");
    }
}
