package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/**
 * Shared rules for which keyframe tracks accept a given toolbar interaction.
 * Mirrors the context-menu availability on each track type.
 */
public final class TimelineTrackEligibility
{
    public static boolean canRename(UIKeyframeSheet sheet)
    {
        return sheet != null && !sheet.groupHeader;
    }

    public static boolean canEditTrack(UIKeyframes keyframes, UIKeyframeSheet sheet)
    {
        if (keyframes.isSingleSheet() || keyframes.isEditing() || sheet == null || sheet.groupHeader)
        {
            return false;
        }

        return canPickEditTrack(sheet);
    }

    /**
     * Track eligibility during pick-track interaction (does not capture the
     * {@link UIKeyframes} instance, so it stays valid after editor rebuilds).
     */
    public static boolean canPickEditTrack(UIKeyframeSheet sheet)
    {
        if (sheet == null || sheet.groupHeader)
        {
            return false;
        }

        return KeyframeFactories.isNumeric(sheet.channel.getFactory());
    }

    public static boolean isPoseTrack(UIKeyframeSheet sheet)
    {
        if (sheet == null || sheet.channel.getFactory() != KeyframeFactories.POSE)
        {
            return false;
        }

        String trackName = StringUtils.fileName(sheet.id);

        return trackName.equals("pose") || trackName.startsWith("pose_overlay");
    }

    public static boolean canAnimationToPose(UIReplaysEditor editor, UIKeyframeSheet sheet)
    {
        if (!isPoseTrack(sheet))
        {
            return false;
        }

        Replay replay = editor.getReplay();

        if (replay == null)
        {
            return false;
        }

        Form form = sheet.property != null ? FormUtils.getForm(sheet.property) : replay.form.get();

        return form instanceof ModelForm;
    }

    public static boolean canPoseToLimbs(UIReplaysEditor editor, UIKeyframeSheet sheet)
    {
        return isPoseTrack(sheet) && editor.getReplay() != null;
    }

    public static boolean hasRenameableTrack(UIKeyframes keyframes)
    {
        return keyframes.anyTrackMatches(TimelineTrackEligibility::canRename);
    }

    public static boolean hasEditableTrack(UIKeyframes keyframes)
    {
        return keyframes.anyTrackMatches(sheet -> canEditTrack(keyframes, sheet));
    }

    public static boolean hasAnimationToPoseTrack(UIReplaysEditor editor)
    {
        if (editor.keyframeEditor == null)
        {
            return false;
        }

        return editor.keyframeEditor.view.anyTrackMatches(sheet -> canAnimationToPose(editor, sheet));
    }

    public static boolean hasPoseToLimbsTrack(UIReplaysEditor editor)
    {
        if (editor.keyframeEditor == null)
        {
            return false;
        }

        return editor.keyframeEditor.view.anyTrackMatches(sheet -> canPoseToLimbs(editor, sheet));
    }

    private TimelineTrackEligibility()
    {}
}
