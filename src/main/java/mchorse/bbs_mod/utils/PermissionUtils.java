package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class PermissionUtils
{
    public static boolean arePanelsAllowed(MinecraftServer server, ServerPlayer player)
    {
        boolean rule = server.overworld().getGameRules().get(BBSMod.BBS_EDITING_RULE);
        boolean allowed = rule || server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));

        return allowed;
    }
}
