package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.forms.FormUtils;

public class PerLimbService
{
    public static final String POSE_BONES = "pose.bones.";

    public static record PoseBonePath(String formPath, String bone)
    {}

    public static boolean isPoseBoneChannel(String id)
    {
        return id != null && id.contains(POSE_BONES);
    }

    public static PoseBonePath parsePoseBonePath(String id)
    {
        if (id == null)
        {
            return null;
        }

        int index = id.indexOf(POSE_BONES);

        if (index < 0)
        {
            return null;
        }

        String bone = id.substring(index + POSE_BONES.length());
        String formPath = id.substring(0, index);

        if (formPath.endsWith(FormUtils.PATH_SEPARATOR))
        {
            formPath = formPath.substring(0, formPath.length() - 1);
        }

        return new PoseBonePath(formPath, bone);
    }

    public static String toPoseBoneKey(String formPath, String bone)
    {
        if (formPath == null || formPath.isEmpty())
        {
            return POSE_BONES + bone;
        }

        return formPath + FormUtils.PATH_SEPARATOR + POSE_BONES + bone;
    }
}
