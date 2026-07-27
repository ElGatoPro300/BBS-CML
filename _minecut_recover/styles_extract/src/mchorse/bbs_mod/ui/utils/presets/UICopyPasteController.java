package mchorse.bbs_mod.ui.utils.presets;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
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
    private IPresetPreview preview;

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

    public UICopyPasteController preview(IPresetPreview preview)
    {
        this.preview = preview;

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

    public IPresetPreview getPreview()
    {
        return this.preview;
    }

    public boolean copy()
    {
        MapType type = this.supplier.get();

        if (type != null)
        {
            Window.setInMemoryClipboard(type, this.copyPrefix);
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
        if (BBSSettings.presetsGridPanel != null && BBSSettings.presetsGridPanel.get() && this.preview != null)
        {
            UIOverlay.addOverlay(context, new UIPresetsGridOverlayPanel(this, mouseX, mouseY), 420, 0.7F);
        }
        else
        {
            UIOverlay.addOverlay(context, new UIPresetsOverlayPanel(this, mouseX, mouseY), 240, 0.5F);
        }
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

    public static interface IPresetPreview
    {
        public UIElement createElement();

        public void preview(String presetId, MapType presetData);

        public void reset();

        public default IPresetPreview fork()
        {
            return null;
        }
    }
}
