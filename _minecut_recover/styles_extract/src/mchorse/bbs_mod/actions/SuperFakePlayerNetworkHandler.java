package mchorse.bbs_mod.actions;

import javax.annotation.Nullable;
import net.minecraft.class_2535;
import net.minecraft.class_2547;
import net.minecraft.class_2596;
import net.minecraft.class_2598;
import net.minecraft.class_3222;
import net.minecraft.class_3244;
import net.minecraft.class_8792;

public class SuperFakePlayerNetworkHandler extends class_3244
{
    private static final class_2535 FAKE_CONNECTION = new FakeClientConnection();

    public SuperFakePlayerNetworkHandler(class_3222 player)
    {
        super(player.method_5682(), FAKE_CONNECTION, player, class_8792.method_53824(player.method_7334(), false));
    }

    public void send(class_2596<?> packet)
    {}

    private static final class FakeClientConnection extends class_2535
    {
        private FakeClientConnection()
        {
            super(class_2598.field_11942);
        }

        public void setPacketListener(class_2547 packetListener)
        {}
    }
}