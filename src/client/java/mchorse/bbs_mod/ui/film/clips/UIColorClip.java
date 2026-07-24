package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Color clip editor: classic overlay color with alpha only
 * (no grade / lift / gamma / gain tracks in the UI).
 */
public class UIColorClip extends UIClip<ColorClip>
{
    private static final int COLOR_OVERLAY = Colors.YELLOW;

    public UIColor overlayColor;
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    public UIColorClip(ColorClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.overlayColor = new UIColor((c) ->
        {
            this.editor.editMultiple(this.clip.overlayColor, c);

            /* Keep opacity keyframe in sync with picker alpha (0–4 scale like before). */
            float a = Colors.getA(c);
            int tick = Math.max(0, this.editor.getCursor() - this.clip.tick.get());

            this.editor.editMultiple(this.clip.overlayAlpha, (channel) ->
            {
                if (channel.isEmpty())
                {
                    channel.insert(0, (double) (a * 4F));
                }
                else
                {
                    channel.insert(tick, (double) (a * 4F));
                }
            });
        }).withAlpha();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.backgroundRenderer((context) ->
        {
            UIReplaysEditor.renderBackground(context, this.keyframes.view, (Clips) this.clip.getParent(), this.clip.tick.get(), this.clip);
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("color_keyframes");

        this.edit = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.edit.keys().register(Keys.FORMS_EDIT, () -> this.edit.clickItself());
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_OVERLAY_COLOR), this.overlayColor).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_KEYFRAMES), this.edit).marginTop(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        int rgb = this.clip.overlayColor.get();
        float alpha = this.clip.overlayAlpha.isEmpty()
            ? Colors.getA(rgb)
            : (float) (double) this.clip.overlayAlpha.interpolate(Math.max(0, this.editor.getCursor() - this.clip.tick.get())) * 0.25F;

        this.overlayColor.setColor(Colors.setA(rgb, alpha));
        this.rebuildChannels();
    }

    private void rebuildChannels()
    {
        this.keyframes.view.removeAllSheets();

        UIKeyframeSheet sheet = new UIKeyframeSheet(COLOR_OVERLAY, false, this.clip.overlayAlpha, null);

        sheet.title = UIKeys.SCREEN_PANELS_OVERLAY_COLOR;
        sheet.limit(0D, 4D);
        this.keyframes.view.addSheet(sheet);
        this.keyframes.view.getGraph().clearSelection();
    }

    @Override
    protected UIKeyframeEditor resolveClipEmbeddableView(String undoId)
    {
        return undoId.equals(this.keyframes.getUndoId()) ? this.keyframes : null;
    }
}
