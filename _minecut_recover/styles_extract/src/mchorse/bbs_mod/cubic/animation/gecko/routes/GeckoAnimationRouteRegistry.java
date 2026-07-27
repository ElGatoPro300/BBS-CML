package mchorse.bbs_mod.cubic.animation.gecko.routes;

public class GeckoAnimationRouteRegistry
{
    public GeckoLimbRole resolve(String limbId)
    {
        String key = limbId == null ? "" : limbId.toLowerCase();

        if (key.equals("head") || key.equals("helmet") || key.contains("head"))
        {
            return GeckoLimbRole.HEAD;
        }

        if (key.equals("right_arm") || key.equals("arm_right") || key.contains("rarm") || key.contains("rightarm"))
        {
            return GeckoLimbRole.RIGHT_ARM;
        }

        if (key.equals("left_arm") || key.equals("arm_left") || key.contains("larm") || key.contains("leftarm"))
        {
            return GeckoLimbRole.LEFT_ARM;
        }

        if (key.equals("right_leg") || key.equals("leg_right") || key.contains("rleg") || key.contains("rightleg"))
        {
            return GeckoLimbRole.RIGHT_LEG;
        }

        if (key.equals("left_leg") || key.equals("leg_left") || key.contains("lleg") || key.contains("leftleg"))
        {
            return GeckoLimbRole.LEFT_LEG;
        }

        if (key.equals("torso") || key.equals("body") || key.equals("chest"))
        {
            return GeckoLimbRole.TORSO;
        }

        return GeckoLimbRole.OTHER;
    }
}
