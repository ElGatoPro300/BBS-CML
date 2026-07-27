package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.HotbarClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class UIHotbarClip extends UIClip<HotbarClip>
{
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    public UIHotbarClip(HotbarClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("hotbar_keyframes");

        this.edit = new UIButton(UIKeys.CAMERA_PANELS_EDIT_KEYFRAMES, (b) ->
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

        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_HOTBAR), this.edit).marginTop(12));
    }

    @Override
    public void fillData()
    {
        super.fillData();
        this.ensureHardcoreIsBoolean();

        this.keyframes.setChannels(this.clip.channels);

        for (UIKeyframeSheet sheet : this.keyframes.view.getGraph().getSheets())
        {
            sheet.title = this.getTrackTitle(sheet.id);
        }
    }

    @Override
    protected UIKeyframeEditor resolveClipEmbeddableView(String undoId)
    {
        return undoId.equals(this.keyframes.getUndoId()) ? this.keyframes : null;
    }

    private IKey getTrackTitle(String id)
    {
        return switch (id)
        {
            case "selected_slot" -> UIKeys.C_CLIP.get("bbs:selected_slot");
            case "slot_0" -> UIKeys.C_CLIP.get("bbs:slot_0");
            case "slot_1" -> UIKeys.C_CLIP.get("bbs:slot_1");
            case "slot_2" -> UIKeys.C_CLIP.get("bbs:slot_2");
            case "slot_3" -> UIKeys.C_CLIP.get("bbs:slot_3");
            case "slot_4" -> UIKeys.C_CLIP.get("bbs:slot_4");
            case "slot_5" -> UIKeys.C_CLIP.get("bbs:slot_5");
            case "slot_6" -> UIKeys.C_CLIP.get("bbs:slot_6");
            case "slot_7" -> UIKeys.C_CLIP.get("bbs:slot_7");
            case "slot_8" -> UIKeys.C_CLIP.get("bbs:slot_8");
            case "offhand_slot" -> UIKeys.C_CLIP.get("bbs:offhand_slot");
            case "health" -> UIKeys.C_CLIP.get("bbs:health");
            case "health_container" -> UIKeys.C_CLIP.get("bbs:health_container");
            case "absorption" -> UIKeys.C_CLIP.get("bbs:absorption");
            case "absorption_container" -> UIKeys.C_CLIP.get("bbs:absorption_container");
            case "heart_type" -> UIKeys.C_CLIP.get("bbs:heart_type");
            case "hardcore" -> UIKeys.C_CLIP.get("bbs:hardcore");
            case "heart_regeneration" -> UIKeys.C_CLIP.get("bbs:heart_regeneration");
            case "hunger_effect" -> UIKeys.C_CLIP.get("bbs:hunger_effect");
            case "armor" -> UIKeys.C_CLIP.get("bbs:armor");
            case "hunger" -> UIKeys.C_CLIP.get("bbs:hunger");
            case "air" -> UIKeys.C_CLIP.get("bbs:air");
            case "experience" -> UIKeys.C_CLIP.get("bbs:experience");
            case "experience_level" -> UIKeys.C_CLIP.get("bbs:experience_level");
            case "layout" -> UIKeys.C_CLIP.get("bbs:layout");
            default -> IKey.constant(id);
        };
    }

    private void ensureHardcoreIsBoolean()
    {
        if (this.clip.hardcore.getFactory() == KeyframeFactories.BOOLEAN)
        {
            return;
        }

        MapType data = this.clip.hardcore.toData().asMap();

        data.putString("type", "boolean");
        this.clip.hardcore.fromData(data);
    }
}
