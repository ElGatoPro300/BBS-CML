package mchorse.bbs_mod.ui.utils.presets;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.utils.presets.PresetManager;

import java.util.function.Supplier;

public class UICopyPasteController
{
    public final PresetManager manager;
    public final String copyPrefix;
    private Supplier<MapType> supplier;
    private IPaste consumer;

    private Supplier<Boolean> canCopy;
    private Supplier<Boolean> canPaste;

    public UICopyPasteController(PresetManager manager, String copyPrefix)
    {
        this.manager = manager;
        this.copyPrefix = copyPrefix;
    }

    public UICopyPasteController supplier(Supplier<MapType> supplier)
    {
        this.supplier = supplier;

        return this;
    }

    public UICopyPasteController consumer(IPaste consumer)
    {
        this.consumer = consumer;

        return this;
    }

    public UICopyPasteController canCopy(Supplier<Boolean> canCopy)
    {
        this.canCopy = canCopy;

        return this;
    }

    public UICopyPasteController canPaste(Supplier<Boolean> canPaste)
    {
        this.canPaste = canPaste;

        return this;
    }

    public IPaste getConsumer()
    {
        return this.consumer;
    }

    public Supplier<MapType> getSupplier()
    {
        return this.supplier;
    }

    public boolean copy()
    {
        MapType type = this.supplier.get();

        if (type != null)
        {
            Window.setClipboard(type, this.copyPrefix);
        }

        return type != null;
    }

    public boolean paste(int mouseX, int mouseY)
    {
        MapType type = Window.getClipboardMap(this.copyPrefix);

        if (type != null)
        {
            this.consumer.paste(type, mouseX, mouseY);
        }

        return type != null;
    }

    public void openPresets(UIContext context, int mouseX, int mouseY)
    {
        UIOverlay.addOverlay(context, new UIPresetsOverlayPanel(this, mouseX, mouseY), 240, 0.5F);
    }

    public boolean canCopy()
    {
        if (this.canCopy != null && !this.canCopy.get())
        {
            return false;
        }

        return true;
    }

    public boolean canPaste()
    {
        if (!this.canPreviewPresets())
        {
            return false;
        }

        return Window.getClipboardMap(this.copyPrefix) != null;
    }

    public boolean canPreviewPresets()
    {
        return !(this.canPaste != null && !this.canPaste.get());
    }

    public static interface IPaste
    {
        public void paste(MapType mapType, int mouseX, int mouseY);
    }
}