package mchorse.bbs_mod.network;

import net.minecraft.network.PacketByteBuf;

public interface IBufferReceiver
{
    public void receiveBuffer(byte[] bytes, PacketByteBuf buf);
}