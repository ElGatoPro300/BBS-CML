package mchorse.bbs_mod.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ClientPacketCrusher extends PacketCrusher
{
    private static CustomPayload.Id<ServerNetwork.BufPayload> idFor(Identifier identifier)
    {
        return ServerNetwork.idFor(identifier);
    }

    @Override
    protected void sendBuffer(PlayerEntity entity, Identifier identifier, PacketByteBuf buf)
    {
        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, idFor(identifier)));
    }
}