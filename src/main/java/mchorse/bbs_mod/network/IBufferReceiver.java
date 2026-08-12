package mchorse.bbs_mod.network;

import net.minecraft.network.FriendlyByteBuf;

public interface IBufferReceiver
{
    public void receiveBuffer(byte[] bytes, FriendlyByteBuf buf);
}