package mchorse.bbs_mod.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ServerPacketCrusher extends PacketCrusher
{
    private static CustomPayload.Id<ServerNetwork.BufPayload> idFor(Identifier identifier)
    {
        return ServerNetwork.idFor(identifier);
    }

    @Override
    protected void sendBuffer(PlayerEntity entity, Identifier identifier, PacketByteBuf buf)
    {
        ServerPlayNetworking.send((ServerPlayerEntity) entity, ServerNetwork.BufPayload.from(buf, idFor(identifier)));
    }
}