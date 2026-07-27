package mchorse.bbs_mod.network;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.class_1657;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
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

    public void receive(class_2540 buf, IBufferReceiver receiver)
    {
        int id = buf.readInt();
        int index = buf.readInt();
        int total = buf.readInt();
        int size = buf.readInt();
        byte[] bytes = new byte[size];

        buf.method_52979(bytes);

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

    public void send(class_1657 entity, class_2960 identifier, BaseType baseType, Consumer<class_2540> consumer)
    {
        this.send(Collections.singleton(entity), identifier, baseType, consumer);
    }

    public void send(class_1657 entity, class_2960 identifier, byte[] bytes, Consumer<class_2540> consumer)
    {
        this.send(Collections.singleton(entity), identifier, bytes, consumer);
    }

    public void send(Collection<class_1657> entities, class_2960 identifier, BaseType baseType, Consumer<class_2540> consumer)
    {
        this.send(entities, identifier, DataStorageUtils.writeToBytes(baseType), consumer);
    }

    public void send(Collection<class_1657> entities, class_2960 identifier, byte[] bytes, Consumer<class_2540> consumer)
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

            class_2540 buf = PacketByteBufs.create();
            int size = Math.min(BUFFER_SIZE, bytes.length - offset);

            buf.method_53002(counter);
            buf.method_53002(index);
            buf.method_53002(total);
            buf.method_53002(size);
            buf.method_52980(bytes, offset, size);

            if (consumer != null && index == total - 1)
            {
                consumer.accept(buf);
            }

            for (class_1657 playerEntity : entities)
            {
                this.sendBuffer(playerEntity, identifier, buf);
            }
        }

        this.counter += 1;
    }

    protected abstract void sendBuffer(class_1657 entity, class_2960 identifier, class_2540 buf);
}