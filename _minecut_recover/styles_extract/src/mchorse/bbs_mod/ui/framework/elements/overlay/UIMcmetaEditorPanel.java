package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;

import java.io.File;

public class UIMcmetaEditorPanel extends UIOverlayPanel
{
    private File file;
    private MapType data;

    private UITrackpad frametime;
    private UIButton resize;
    private UITrackpad width;
    private UITrackpad height;
    private boolean resizeVisible = false;

    public UIMcmetaEditorPanel(File file)
    {
        super(UIKeys.TEXTURES_MCMETA_EDIT);

        this.file = file;
        this.data = new MapType();

        try
        {
            BaseType type = DataToString.read(file);

            if (type instanceof MapType)
            {
                this.data = (MapType) type;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.frametime = new UITrackpad((v) -> this.updateData());
        this.frametime.integer().limit(1, Float.POSITIVE_INFINITY).tooltip(UIKeys.TEXTURES_MCMETA_FRAMETIME);

        this.resize = new UIButton(UIKeys.TEXTURES_MCMETA_RESIZE, (b) -> this.toggleResize());

        this.width = new UITrackpad((v) -> this.updateData());
        this.width.integer().limit(1, Float.POSITIVE_INFINITY).tooltip(UIKeys.TEXTURES_MCMETA_WIDTH);

        this.height = new UITrackpad((v) -> this.updateData());
        this.height.integer().limit(1, Float.POSITIVE_INFINITY).tooltip(UIKeys.TEXTURES_MCMETA_HEIGHT);

        this.content.add(new UILabel(UIKeys.TEXTURES_MCMETA_FRAMETIME).relative(this.content).x(10).y(20).anchor(0, 1));
        this.frametime.relative(this.content).x(10).y(20).w(1F, -20);
        this.content.add(this.frametime);

        this.resize.relative(this.content).x(10).y(60).w(1F, -20);
        this.content.add(this.resize);

        this.loadData();
    }

    private void toggleResize()
    {
        this.resizeVisible = true;
        this.resize.removeFromParent();
        
        this.content.add(new UILabel(UIKeys.TEXTURES_MCMETA_WIDTH).relative(this.content).x(10).y(60).anchor(0, 1));
        this.width.relative(this.content).x(10).y(60).w(0.5F, -15);
        this.content.add(this.width);
        
        this.content.add(new UILabel(UIKeys.TEXTURES_MCMETA_HEIGHT).relative(this.content).x(0.5F, 5).y(60).anchor(0, 1));
        this.height.relative(this.content).x(0.5F, 5).y(60).w(0.5F, -15);
        this.content.add(this.height);
        
        this.resize();
    }

    private void loadData()
    {
        MapType animation = this.data.getMap("animation");

        if (animation != null)
        {
            if (animation.has("frametime")) this.frametime.setValue(animation.getInt("frametime"));
            
            if (animation.has("width") || animation.has("height"))
            {
                this.toggleResize();
                
                if (animation.has("width")) this.width.setValue(animation.getInt("width"));
                if (animation.has("height")) this.height.setValue(animation.getInt("height"));
            }
        }
        else
        {
            this.frametime.setValue(1);
        }
    }

    private void updateData()
    {
        MapType animation = new MapType();

        animation.putInt("frametime", (int) this.frametime.getValue());

        if (this.resizeVisible)
        {
            if (this.width.getValue() > 0) animation.putInt("width", (int) this.width.getValue());
            if (this.height.getValue() > 0) animation.putInt("height", (int) this.height.getValue());
        }

        this.data.put("animation", animation);

        try
        {
            DataToString.writeSilently(this.file, this.data, true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
