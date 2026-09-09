package mchorse.bbs_mod.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ServerPacketCrusher extends PacketCrusher
{
    private static CustomPacketPayload.Type<ServerNetwork.BufPayload> idFor(Identifier identifier)
    {
        return ServerNetwork.idFor(identifier);
    }

    @Override
    protected void sendBuffer(Player entity, Identifier identifier, FriendlyByteBuf buf)
    {
        ServerPlayNetworking.send((ServerPlayer) entity, ServerNetwork.BufPayload.from(buf, idFor(identifier)));
    }
}