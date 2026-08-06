package mchorse.bbs_mod.actions;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class SuperFakePlayerNetworkHandler extends ServerPlayNetworkHandler
{
    private static final ClientConnection FAKE_CONNECTION = new ClientConnection(NetworkSide.CLIENTBOUND);

    public SuperFakePlayerNetworkHandler(ServerPlayerEntity player)
    {
        super(player.getServer(), FAKE_CONNECTION, player);
    }

    @Override
    public void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks)
    {}
}