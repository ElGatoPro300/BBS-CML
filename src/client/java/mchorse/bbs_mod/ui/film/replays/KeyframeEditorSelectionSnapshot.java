package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.utils.gizmo.TransformOrientation;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

/**
 * Survives a replay keyframe-editor rebuild (e.g. Actor toggle) so the selected
 * sheet / keyframe / limb can be restored. Keyframe instances stay in channels;
 * only the UI graph is recreated.
 */
public final class KeyframeEditorSelectionSnapshot
{
    private final Keyframe<?> keyframe;
    private final String sheetId;
    private final float tick;
    private final String bone;

    private KeyframeEditorSelectionSnapshot(Keyframe<?> keyframe, String sheetId, float tick, String bone)
    {
        this.keyframe = keyframe;
        this.sheetId = sheetId;
        this.tick = tick;
        this.bone = bone;
    }

    public static KeyframeEditorSelectionSnapshot capture(UIKeyframeEditor editor)
    {
        if (editor == null || editor.view == null)
        {
            return null;
        }

        IUIKeyframeGraph graph = editor.view.getGraph();
        Keyframe<?> selected = graph.getSelected();
        UIKeyframeSheet sheet = selected != null ? graph.getSheet(selected) : null;
        Pair<String, TransformOrientation> bonePair = editor.getBone();
        String bone = bonePair != null ? bonePair.a : null;

        if (sheet == null && (bone == null || bone.isEmpty()))
        {
            return null;
        }

        String sheetId = sheet != null ? sheet.id : null;
        float tick = selected != null ? selected.getTick() : Float.NaN;

        /* Pose / limb sheet id from bone when only the pose factory has a limb. */
        if (sheetId == null && bone != null && !bone.isEmpty() && editor.editor != null)
        {
            UIKeyframeSheet factorySheet = editor.getSheet(editor.editor.getKeyframe());

            if (factorySheet != null)
            {
                sheetId = factorySheet.id;
                selected = editor.editor.getKeyframe();
                tick = selected != null ? selected.getTick() : tick;
            }
        }

        if (sheetId == null)
        {
            return null;
        }

        return new KeyframeEditorSelectionSnapshot(selected, sheetId, tick, bone);
    }

    public boolean isEmpty()
    {
        return this.sheetId == null || this.sheetId.isEmpty();
    }

    public Keyframe<?> getKeyframe()
    {
        return this.keyframe;
    }

    public String getSheetId()
    {
        return this.sheetId;
    }

    public float getTick()
    {
        return this.tick;
    }

    public String getBone()
    {
        return this.bone;
    }

    public Keyframe<?> findKeyframe(UIKeyframeSheet sheet)
    {
        if (sheet == null)
        {
            return null;
        }

        if (this.keyframe != null && sheet.channel == this.keyframe.getParent())
        {
            return this.keyframe;
        }

        if (Float.isNaN(this.tick))
        {
            return null;
        }

        for (Object object : sheet.channel.getKeyframes())
        {
            Keyframe<?> keyframe = (Keyframe<?>) object;

            if (Math.abs(keyframe.getTick() - this.tick) < 1.0E-3F)
            {
                return keyframe;
            }
        }

        return null;
    }
}
