package mchorse.bbs_mod.ui.utils.presets;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIPresetsOverlayPanel extends UIListOverlayPanel
{
    private final UICopyPasteController controller;
    private String hoveredPreset = "";

    public UIPresetsOverlayPanel(UICopyPasteController controller, int mouseX, int mouseY)
    {
        super(UIKeys.PRESETS_TITLE, null);
        this.controller = controller;

        this.list.removeFromParent();
        this.list = new PresetsSearchList();
        this.list.relative(this.content).xy(6, 6).w(1F, -12).h(1F, -6);
        this.content.add(this.list);

        this.callback = (l) ->
        {
            MapType load = controller.manager.load(l.get(0));

            if (load != null)
            {
                controller.getConsumer().paste(load, mouseX, mouseY);
                this.close();
            }
        };

        this.addValues(controller.manager.getKeys());

        UIIcon save = new UIIcon(Icons.SAVED, (b) ->
        {
            MapType type = controller.getSupplier().get();

            if (type != null)
            {
                UIPromptOverlayPanel pane = new UIPromptOverlayPanel(UIKeys.PRESETS_SAVE_TITLE, UIKeys.PRESETS_SAVE_DESCRIPTION, (t) ->
                {
                    controller.manager.save(t, type);
                    this.list.list.clear();
                    this.addValues(controller.manager.getKeys());
                });

                pane.text.filename();
                UIOverlay.addOverlay(this.getContext(), pane);
            }
        });

        save.setEnabled(controller.canCopy());

        UIIcon folder = new UIIcon(Icons.FOLDER, (b) ->
        {
            UIUtils.openFolder(controller.manager.getFolder());
        });

        save.tooltip(UIKeys.PRESETS_SAVE, Direction.LEFT);
        folder.tooltip(UIKeys.PRESETS_OPEN, Direction.LEFT);
        this.icons.add(save, folder);
    }

    private static String[] getPresetTooltip(String preset, MapType data)
    {
        if (data == null)
        {
            return null;
        }

        String resolution = "-";
        String fps = "-";
        String motionBlur = "-";

        if (data.has("video_frame_width") && data.has("video_frame_height"))
        {
            resolution = data.getInt("video_frame_width") + "x" + data.getInt("video_frame_height");
        }

        if (data.has("video_frame_rate"))
        {
            fps = String.valueOf(data.getInt("video_frame_rate"));
        }

        if (data.has("video_motion_blur"))
        {
            motionBlur = String.valueOf(data.getInt("video_motion_blur"));
        }

        return new String[]
        {
            preset,
            "Resolution: " + resolution,
            "FPS: " + fps,
            "Motion Blur: " + motionBlur
        };
    }

    private void renderPresetTooltip(UIContext context, String[] lines, int x, int y)
    {
        if (lines == null || lines.length == 0)
        {
            return;
        }

        int offset = 3;
        int lineGap = 4;
        int lineHeight = context.batcher.getFont().getHeight() + lineGap;
        int width = 0;

        for (String line : lines)
        {
            width = Math.max(width, context.batcher.getFont().getWidth(line));
        }

        int height = (lineHeight * (lines.length - 1)) + context.batcher.getFont().getHeight();

        context.batcher.box(x - offset, y - offset, x + width + offset - 1, y + height + offset, Colors.A75);

        for (int i = 0; i < lines.length; i++)
        {
            context.batcher.text(lines[i], x, y + i * lineHeight, Colors.WHITE, true);
        }
    }

    @Override
    public void render(UIContext context)
    {
        this.hoveredPreset = "";
        super.render(context);

        if (!this.hoveredPreset.isEmpty())
        {
            String[] tooltip = getPresetTooltip(this.hoveredPreset, this.controller.manager.load(this.hoveredPreset));

            if (tooltip != null)
            {
                this.renderPresetTooltip(context, tooltip, context.mouseX + 12, context.mouseY + 8);
            }
        }
    }

    private class PresetsSearchList extends UISearchList<String>
    {
        public PresetsSearchList()
        {
            super(new PresetsStringList());
            this.list.callback = (l) ->
            {
                if (UIPresetsOverlayPanel.this.callback != null)
                {
                    UIPresetsOverlayPanel.this.callback.accept(l);
                }
            };
        }
    }

    private class PresetsStringList extends UIStringList
    {
        public PresetsStringList()
        {
            super(null);
        }

        @Override
        protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
        {
            super.renderElementPart(context, element, i, x, y, hover, selected);

            if (hover)
            {
                UIPresetsOverlayPanel.this.hoveredPreset = element;
            }
        }
    }
}
