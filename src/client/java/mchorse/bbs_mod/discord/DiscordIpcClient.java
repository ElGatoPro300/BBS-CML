package mchorse.bbs_mod.discord;

import com.mojang.logging.LogUtils;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

/**
 * Minimal Discord IPC client using the legacy Rich Presence pipe protocol.
 * No external dependencies beyond Gson (provided by Minecraft).
 */
public class DiscordIpcClient
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int OP_PING = 3;
    private static final int OP_PONG = 4;

    private static final AtomicInteger NONCE = new AtomicInteger();

    private final String applicationId;

    private RandomAccessFile windowsPipe;
    private SocketChannel unixChannel;
    private Thread readerThread;
    private final Object writeLock = new Object();

    private volatile boolean connected;
    private volatile boolean running;

    public DiscordIpcClient(String applicationId)
    {
        this.applicationId = applicationId;
    }

    public boolean isConnected()
    {
        return this.connected;
    }

    public boolean connect()
    {
        if (this.connected)
        {
            return true;
        }

        if (!this.openPipe())
        {
            LOGGER.error("Discord Rich Presence: could not open IPC pipe (is Discord running?)");

            return false;
        }

        try
        {
            String handshakeError = this.handshake();

            if (handshakeError != null)
            {
                LOGGER.error("Discord Rich Presence handshake failed: {}", handshakeError);
                this.closePipe();

                return false;
            }

            this.connected = true;
            this.running = true;
            this.readerThread = new Thread(this::readLoop, "BBS-Discord-IPC");
            this.readerThread.setDaemon(true);
            this.readerThread.start();

            LOGGER.info("Discord Rich Presence connected (application ID: {})", this.applicationId);

            return true;
        }
        catch (IOException e)
        {
            LOGGER.error("Discord Rich Presence connection failed", e);
            this.closePipe();

            return false;
        }
    }

    public void disconnect()
    {
        this.running = false;
        this.connected = false;

        if (this.readerThread != null)
        {
            this.readerThread.interrupt();
            this.readerThread = null;
        }

        this.closePipe();
    }

    public void setActivity(String details, String state, long startTimestamp)
    {
        if (!this.connected)
        {
            return;
        }

        JsonObject activity = new JsonObject();

        activity.addProperty("type", 0);

        if (details != null && !details.isEmpty())
        {
            activity.addProperty("details", details);
        }

        if (state != null && !state.isEmpty())
        {
            activity.addProperty("state", state);
        }

        if (startTimestamp > 0L)
        {
            JsonObject timestamps = new JsonObject();

            timestamps.addProperty("start", startTimestamp);
            activity.add("timestamps", timestamps);
        }

        JsonObject args = new JsonObject();

        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);

        this.sendCommand("SET_ACTIVITY", args);
    }

    public void clearActivity()
    {
        if (!this.connected)
        {
            return;
        }

        JsonObject args = new JsonObject();

        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", JsonNull.INSTANCE);

        this.sendCommand("SET_ACTIVITY", args);
    }

    private boolean openPipe()
    {
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win"))
        {
            return this.openWindowsPipe();
        }

        return this.openUnixPipe();
    }

    private boolean openWindowsPipe()
    {
        for (int i = 0; i < 10; i++)
        {
            String pipeName = "\\\\.\\pipe\\discord-ipc-" + i;

            try
            {
                this.windowsPipe = new RandomAccessFile(pipeName, "rw");

                LOGGER.info("Discord Rich Presence opened IPC pipe {}", pipeName);

                return true;
            }
            catch (IOException e)
            {
                LOGGER.debug("Discord Rich Presence could not open IPC pipe {}: {}", pipeName, e.getMessage());
            }
        }

        return false;
    }

    private boolean openUnixPipe()
    {
        List<String> basePaths = new ArrayList<>();
        String runtimeDir = System.getenv("XDG_RUNTIME_DIR");
        String tmpDir = System.getenv("TMPDIR");

        if (runtimeDir != null && !runtimeDir.isEmpty())
        {
            basePaths.add(runtimeDir);
        }

        if (tmpDir != null && !tmpDir.isEmpty())
        {
            basePaths.add(tmpDir);
        }

        basePaths.add("/tmp");

        for (String basePath : basePaths)
        {
            for (int i = 0; i < 10; i++)
            {
                Path path = Path.of(basePath, "discord-ipc-" + i);

                try
                {
                    this.unixChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    this.unixChannel.connect(UnixDomainSocketAddress.of(path));

                    LOGGER.info("Discord Rich Presence opened IPC socket {}", path);

                    return true;
                }
                catch (IOException e)
                {
                    LOGGER.debug("Discord Rich Presence could not open IPC socket {}: {}", path, e.getMessage());
                }
            }
        }

        return false;
    }

    private void closePipe()
    {
        if (this.windowsPipe != null)
        {
            try
            {
                this.windowsPipe.close();
            }
            catch (IOException e)
            {
                LOGGER.debug("Discord Rich Presence failed to close Windows IPC pipe", e);
            }

            this.windowsPipe = null;
        }

        if (this.unixChannel != null)
        {
            try
            {
                this.unixChannel.close();
            }
            catch (IOException e)
            {
                LOGGER.debug("Discord Rich Presence failed to close Unix IPC socket", e);
            }

            this.unixChannel = null;
        }
    }

    private String handshake() throws IOException
    {
        JsonObject payload = new JsonObject();

        payload.addProperty("v", 1);
        payload.addProperty("client_id", this.applicationId);

        this.writeFrame(OP_HANDSHAKE, payload.toString().getBytes(StandardCharsets.UTF_8));

        int opcode = this.readInt();
        int length = this.readInt();

        if (opcode == OP_CLOSE)
        {
            return "Discord closed the IPC connection during handshake";
        }

        if (length <= 0 || length > 65536)
        {
            return "Invalid handshake response length: " + length;
        }

        byte[] data = this.readBytes(length);

        if (opcode != OP_FRAME || data.length == 0)
        {
            return "Unexpected handshake opcode: " + opcode;
        }

        JsonObject response = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();

        if (response.has("evt"))
        {
            String event = response.get("evt").getAsString();

            if ("READY".equals(event))
            {
                return null;
            }

            if ("ERROR".equals(event) && response.has("data") && response.get("data").isJsonObject())
            {
                JsonObject errorData = response.getAsJsonObject("data");
                String message = errorData.has("message") ? errorData.get("message").getAsString() : "unknown error";
                int code = errorData.has("code") ? errorData.get("code").getAsInt() : -1;

                return "Discord returned ERROR (code " + code + "): " + message;
            }

            return "Discord returned unexpected handshake event: " + event;
        }

        return "Discord handshake response did not contain an event";
    }

    private void sendCommand(String command, JsonObject args)
    {
        JsonObject frame = new JsonObject();

        frame.addProperty("cmd", command);
        frame.add("args", args);
        frame.addProperty("nonce", String.valueOf(NONCE.incrementAndGet()));

        try
        {
            synchronized (this.writeLock)
            {
                this.writeFrame(OP_FRAME, frame.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Discord Rich Presence failed to send {} command", command, e);
            this.connected = false;
            this.closePipe();
        }
    }

    private void readLoop()
    {
        while (this.running && this.connected)
        {
            try
            {
                int opcode = this.readInt();
                int length = this.readInt();

                if (length <= 0 || length > 65536)
                {
                    break;
                }

                byte[] data = this.readBytes(length);

                if (opcode == OP_PING)
                {
                    synchronized (this.writeLock)
                    {
                        this.writeFrame(OP_PONG, data);
                    }
                }
                else if (opcode == OP_CLOSE)
                {
                    break;
                }
                else if (opcode == OP_FRAME && data.length > 0)
                {
                    this.handleFrame(data);
                }
            }
            catch (IOException | RuntimeException e)
            {
                if (this.running)
                {
                    LOGGER.debug("Discord Rich Presence IPC reader stopped", e);
                }

                break;
            }
        }

        this.connected = false;
        this.closePipe();
    }

    private void handleFrame(byte[] data)
    {
        JsonObject frame = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();

        if (!frame.has("evt"))
        {
            return;
        }

        String event = frame.get("evt").getAsString();

        if ("ERROR".equals(event) && frame.has("data") && frame.get("data").isJsonObject())
        {
            JsonObject errorData = frame.getAsJsonObject("data");
            String message = errorData.has("message") ? errorData.get("message").getAsString() : "unknown error";
            int code = errorData.has("code") ? errorData.get("code").getAsInt() : -1;

            LOGGER.error("Discord Rich Presence IPC error (code {}): {}", code, message);
        }
    }

    private void writeFrame(int opcode, byte[] data) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(opcode);
        buffer.putInt(data.length);
        buffer.put(data);

        byte[] bytes = buffer.array();

        if (this.windowsPipe != null)
        {
            this.windowsPipe.write(bytes);
        }
        else if (this.unixChannel != null)
        {
            ByteBuffer writeBuffer = ByteBuffer.wrap(bytes);

            while (writeBuffer.hasRemaining())
            {
                this.unixChannel.write(writeBuffer);
            }
        }
        else
        {
            throw new IOException("Discord IPC pipe is not open");
        }
    }

    private int readInt() throws IOException
    {
        byte[] bytes = this.readBytes(4);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        return buffer.getInt();
    }

    private byte[] readBytes(int length) throws IOException
    {
        byte[] buffer = new byte[length];
        int read = 0;

        while (read < length)
        {
            int chunk;

            if (this.windowsPipe != null)
            {
                chunk = this.windowsPipe.read(buffer, read, length - read);
            }
            else if (this.unixChannel != null)
            {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, read, length - read);

                chunk = this.unixChannel.read(byteBuffer);
            }
            else
            {
                throw new IOException("Discord IPC pipe is not open");
            }

            if (chunk < 0)
            {
                throw new IOException("Discord IPC pipe closed");
            }

            read += chunk;
        }

        return buffer;
    }
}
