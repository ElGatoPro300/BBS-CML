package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.colors.Color;

public class VideoForm extends Form
{
    public final ValueString video = new ValueString("video", "");
    public final ValueBoolean billboard = new ValueBoolean("billboard", false);
    public final ValueBoolean linear = new ValueBoolean("linear", true);
    public final ValueBoolean loop = new ValueBoolean("loop", true);
    public final ValueFloat speed = new ValueFloat("speed", 1F, 0.01F, 8F);
    public final ValueInt offset = new ValueInt("offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    public final ValueColor color = new ValueColor("color", new Color(1F, 1F, 1F, 0F));
    public final ValueFloat offsetX = new ValueFloat("offsetX", 0F);
    public final ValueFloat offsetY = new ValueFloat("offsetY", 0F);
    public final ValueFloat rotation = new ValueFloat("rotation", 0F);
    public final ValueBoolean shading = new ValueBoolean("shading", true);

    public VideoForm()
    {
        super();

        this.shading.invisible();

        this.add(this.video);
        this.add(this.billboard);
        this.add(this.linear);
        this.add(this.loop);
        this.add(this.speed);
        this.add(this.offset);
        this.add(this.color);
        this.registerColorOverlays();
        this.add(this.offsetX);
        this.add(this.offsetY);
        this.add(this.rotation);
        this.add(this.shading);
    }

    @Override
    public String getDefaultDisplayName()
    {
        String path = this.video.get();

        return path == null || path.isEmpty() ? "none" : path;
    }
}
