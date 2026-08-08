package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.network.ClientNetwork;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;

public class PlayerUtils
{
    public static void teleport(double x, double y, double z, float yaw, float pitch)
    {
        teleport(x, y, z, yaw, yaw, pitch);
    }

    public static void teleport(double x, double y, double z, float yaw, float bodyYaw, float pitch)
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (!ClientNetwork.isIsBBSModOnServer())
        {
            String command = "tp " + player.getGameProfile().name() + " " + x + " " + y + " " + z + " " + yaw + " " + pitch;

            player.networkHandler.sendChatCommand(command);
        }
        else
        {
            ClientNetwork.sendTeleport(x, y, z, yaw, bodyYaw, pitch);
            player.setYaw(yaw);
            player.setHeadYaw(yaw);
            player.setBodyYaw(bodyYaw);
            player.setPitch(pitch);
        }
    }

    public static void teleport(double x, double y, double z)
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (!ClientNetwork.isIsBBSModOnServer())
        {
            player.networkHandler.sendChatCommand("tp " + player.getGameProfile().name() + " " + x + " " + y + " " + z);
        }
        else
        {
            ClientNetwork.sendTeleport(player, x, y, z);
        }
    }

    public static class ProtectedAccess extends PlayerEntity
    {
        public static TrackedData<Byte> getModelParts()
        {
            /* TODO(1.21.11 render): PlayerEntity.PLAYER_MODEL_PARTS tracked-data was removed; the
             * cosmetic model-parts (cape/jacket/sleeves) byte is no longer exposed via the data
             * tracker. This accessor is currently unused; returns null until the new player-model/
             * skin-config path is ported (see MobFormRenderer for the matching TODO). */
            return null;
        }

        public ProtectedAccess(World world, GameProfile gameProfile)
        {
            super(world, gameProfile);
        }

        @Override
        public GameMode getGameMode()
        {
            return GameMode.SURVIVAL;
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