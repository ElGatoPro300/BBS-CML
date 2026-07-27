package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.class_1928;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

public class PermissionUtils
{
    public static boolean arePanelsAllowed(MinecraftServer server, class_3222 player)
    {
        class_1928.class_4310 rule = server.method_30002().method_8450().method_20746(BBSMod.BBS_EDITING_RULE);
        boolean allowed = rule.method_20753() || server.method_3760().method_14569(player.method_7334());

        return allowed;
    }
}