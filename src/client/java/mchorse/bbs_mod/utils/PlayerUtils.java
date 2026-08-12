package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.network.ClientNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import com.mojang.authlib.GameProfile;

public class PlayerUtils
{
    public static void teleport(double x, double y, double z, float yaw, float pitch)
    {
        teleport(x, y, z, yaw, yaw, pitch);
    }

    public static void teleport(double x, double y, double z, float yaw, float bodyYaw, float pitch)
    {
        LocalPlayer player = Minecraft.getInstance().player;

        if (!ClientNetwork.isIsBBSModOnServer())
        {
            String command = "tp " + player.getGameProfile().name() + " " + x + " " + y + " " + z + " " + yaw + " " + pitch;

            player.connection.sendCommand(command);
        }
        else
        {
            ClientNetwork.sendTeleport(x, y, z, yaw, bodyYaw, pitch);
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            player.setYBodyRot(bodyYaw);
            player.setXRot(pitch);
        }
    }

    public static void teleport(double x, double y, double z)
    {
        LocalPlayer player = Minecraft.getInstance().player;

        if (!ClientNetwork.isIsBBSModOnServer())
        {
            player.connection.sendCommand("tp " + player.getGameProfile().name() + " " + x + " " + y + " " + z);
        }
        else
        {
            ClientNetwork.sendTeleport(player, x, y, z);
        }
    }

    public static class ProtectedAccess extends Player
    {
        public static EntityDataAccessor<Byte> getModelParts()
        {
            /* TODO(1.21.11 render): PlayerEntity.PLAYER_MODEL_PARTS tracked-data was removed; the
             * cosmetic model-parts (cape/jacket/sleeves) byte is no longer exposed via the data
             * tracker. This accessor is currently unused; returns null until the new player-model/
             * skin-config path is ported (see MobFormRenderer for the matching TODO). */
            return null;
        }

        public ProtectedAccess(Level world, GameProfile gameProfile)
        {
            super(world, gameProfile);
        }

        @Override
        public GameType gameMode()
        {
            return GameType.SURVIVAL;
        }

        @Override
        public boolean isSpectator()
        {
            return false;
        }

        @Override
        public boolean isCreative()
        {
            return false;
        }
    }
}