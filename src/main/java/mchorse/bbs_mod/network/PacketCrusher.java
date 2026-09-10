package mchorse.bbs_mod.network;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.netty.buffer.Unpooled;

public abstract class PacketCrusher
{
    public static final int BUFFER_SIZE = 30_000;

    private Map<Integer, ByteArrayOutputStream> chunks = new HashMap<>();
    private int counter;

    public void reset()
    {
        this.chunks.clear();
        this.counter = 0;
    }

    public void receive(FriendlyByteBuf buf, IBufferReceiver receiver)
    {
        int id = buf.readInt();
        int index = buf.readInt();
        int total = buf.readInt();
        int size = buf.readInt();
        byte[] bytes = new byte[size];

        buf.readBytes(bytes);

        ByteArrayOutputStream map = this.chunks.computeIfAbsent(id, (k) -> new ByteArrayOutputStream(total * BUFFER_SIZE));

        map.writeBytes(bytes);

        if (index == total - 1)
        {
            byte[] finalBytes = map.toByteArray();

            if (finalBytes.length == 1 && finalBytes[0] == 69)
            {
                finalBytes = null;
            }

            receiver.receiveBuffer(finalBytes, buf);
            this.chunks.remove(id);
        }
    }

    public void send(Player entity, Identifier identifier, BaseType baseType, Consumer<FriendlyByteBuf> consumer)
    {
        this.send(Collections.singleton(entity), identifier, baseType, consumer);
    }

    public void send(Player entity, Identifier identifier, byte[] bytes, Consumer<FriendlyByteBuf> consumer)
    {
        this.send(Collections.singleton(entity), identifier, bytes, consumer);
    }

    public void send(Collection<Player> entities, Identifier identifier, BaseType baseType, Consumer<FriendlyByteBuf> consumer)
    {
        this.send(entities, identifier, DataStorageUtils.writeToBytes(baseType), consumer);
    }

    public void send(Collection<Player> entities, Identifier identifier, byte[] bytes, Consumer<FriendlyByteBuf> consumer)
    {
        if (bytes.length == 0)
        {
            bytes = new byte[]{69};
        }

        int total = Math.max((int) Math.ceil(bytes.length / (float) BUFFER_SIZE), 1);
        int counter = this.counter;

        for (int index = 0; index < total; index++)
        {
            int offset = index * BUFFER_SIZE;

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            int size = Math.min(BUFFER_SIZE, bytes.length - offset);

            buf.writeInt(counter);
            buf.writeInt(index);
            buf.writeInt(total);
            buf.writeInt(size);
            buf.writeBytes(bytes, offset, size);

            if (consumer != null && index == total - 1)
            {
                consumer.accept(buf);
            }

            for (Player playerEntity : entities)
            {
                this.sendBuffer(playerEntity, identifier, buf);
            }
        }

        this.counter += 1;
    }

    protected abstract void sendBuffer(Player entity, Identifier identifier, FriendlyByteBuf buf);
}