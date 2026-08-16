package mchorse.bbs_mod.network;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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

    public void receive(PacketByteBuf buf, IBufferReceiver receiver)
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

    public void send(PlayerEntity entity, Identifier identifier, BaseType baseType, Consumer<PacketByteBuf> consumer)
    {
        this.send(Collections.singleton(entity), identifier, baseType, consumer);
    }

    public void send(PlayerEntity entity, Identifier identifier, byte[] bytes, Consumer<PacketByteBuf> consumer)
    {
        this.send(Collections.singleton(entity), identifier, bytes, consumer);
    }

    public void send(Collection<PlayerEntity> entities, Identifier identifier, BaseType baseType, Consumer<PacketByteBuf> consumer)
    {
        this.send(entities, identifier, DataStorageUtils.writeToBytes(baseType), consumer);
    }

    public void send(Collection<PlayerEntity> entities, Identifier identifier, byte[] bytes, Consumer<PacketByteBuf> consumer)
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

            PacketByteBuf buf = PacketByteBufs.create();
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

            for (PlayerEntity playerEntity : entities)
            {
                this.sendBuffer(playerEntity, identifier, buf);
            }
        }

        this.counter += 1;
    }

    protected abstract void sendBuffer(PlayerEntity entity, Identifier identifier, PacketByteBuf buf);
}