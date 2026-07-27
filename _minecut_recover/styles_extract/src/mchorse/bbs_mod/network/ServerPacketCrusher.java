package mchorse.bbs_mod.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_1657;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_8710;

public class ServerPacketCrusher extends PacketCrusher
{
    private static class_8710.class_9154<ServerNetwork.BufPayload> idFor(class_2960 identifier)
    {
        return ServerNetwork.idFor(identifier);
    }

    @Override
    protected void sendBuffer(class_1657 entity, class_2960 identifier, class_2540 buf)
    {
        ServerPlayNetworking.send((class_3222) entity, ServerNetwork.BufPayload.from(buf, idFor(identifier)));
    }
}