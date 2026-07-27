package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.network.ClientNetwork;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2940;
import net.minecraft.class_310;
import net.minecraft.class_746;
import com.mojang.authlib.GameProfile;

public class PlayerUtils
{
    public static void teleport(double x, double y, double z, float yaw, float pitch)
    {
        teleport(x, y, z, yaw, yaw, pitch);
    }

    public static void teleport(double x, double y, double z, float yaw, float bodyYaw, float pitch)
    {
        class_746 player = class_310.method_1551().field_1724;

        if (!ClientNetwork.isIsBBSModOnServer())
        {
            String command = "tp " + player.method_7334().getName() + " " + x + " " + y + " " + z + " " + yaw + " " + pitch;

            player.field_3944.method_45731(command);
        }
        else
        {
            ClientNetwork.sendTeleport(x, y, z, yaw, bodyYaw, pitch);
            player.method_36456(yaw);
            player.method_5847(yaw);
            player.method_5636(bodyYaw);
            player.method_36457(pitch);
        }
    }

    public static void teleport(double x, double y, double z)
    {
        class_746 player = class_310.method_1551().field_1724;

        if (!ClientNetwork.isIsBBSModOnServer())
        {
            player.field_3944.method_45731("tp " + player.method_7334().getName() + " " + x + " " + y + " " + z);
        }
        else
        {
            ClientNetwork.sendTeleport(player, x, y, z);
        }
    }

    public static class ProtectedAccess extends class_1657
    {
        public static class_2940<Byte> getModelParts()
        {
            return field_7518;
        }

        public ProtectedAccess(class_1937 world, class_2338 pos, float yaw, GameProfile gameProfile)
        {
            super(world, pos, yaw, gameProfile);
        }

        @Override
        public boolean method_7325()
        {
            return false;
        }

        @Override
        public boolean method_7337()
        {
            return false;
        }
    }
}