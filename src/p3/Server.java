package p3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import static java.nio.channels.SelectionKey.*;
import static p3.EventLoop.currentLoop;

/**
 * Chat server.
 *
 * @author Alba Mendez
 */
public class Server implements Runnable {

    private final int port;

    public Server(int port) {
        this.port = port;
    }

    /**
     * Main server code.
     */
    @Override
    public void run() {

        final Map<String, NetSocket> peers = new HashMap<>();
        final BiConsumer<String, String> broadcast = (origin, data) ->
                peers.forEach((nick, socket) ->
                    { if (!nick.equals(origin)) socket.write(data); });

        NetServer server = createServer((socket) -> {
            socket.setKeepAlive(true);

            final Readline rl = new Readline(socket);
            rl.once("line", (String nick) -> {
                if (!nick.matches("^[a-zA-Z0-9 _.@-]+$")) {
                    socket.end("Error: Invalid characters in nickname\n");
                    return;
                }
                if (peers.containsKey(nick)) {
                    socket.end("Error: Nickname '" + nick + "' already in use\n");
                    return;
                }

                broadcast.accept(nick, "[" + nick + " joined the room]\n");
                peers.put(nick, socket);
                socket.write("[current participants: " + String.join(", ", peers.keySet()) + "]\n");
                rl.on("line", (message) -> broadcast.accept(nick, nick + ": " + message + "\n"));
                rl.on("close", () -> {
                  peers.remove(nick);
                  broadcast.accept(nick, "[" + nick + " left the room]\n");
                });
            });

            socket.on("error", () -> socket.destroy());
        });
        server.listen(port, () -> System.out.println("Server listening."));

    }

    public static void main(String[] args) {
        Server server = new Server(3500);
        new EventLoop(server).run();
    }



    // HIGH-LEVEL API

    public static class EventEmitter {
        private final Map<String, List<Consumer>> handlers = new HashMap<>();

        public boolean emit(String event, Object value) {
            if (handlers.containsKey(event)) {
                new ArrayList<>(handlers.get(event))
                        .forEach(handler -> handler.accept(value));
                return true;
            }
            if (event.equals("error"))
                throw new RuntimeException((Throwable) value);
            return false;
        }

        public boolean emit(String event) {
            return emit(event, null);
        }

        public <T> void on(String event, Consumer<T> handler) {
            if (!handlers.containsKey(event))
                handlers.put(event, new ArrayList<>());
            handlers.get(event).add(handler);
        }

        public <T> void once(String event, Consumer<T> handler) {
            on(event, new Consumer<T>() {
                public void accept(T t) {
                    handler.accept(t);
                    handlers.get(event).remove(this);
                }
            });
        }

        public <T> void on(String event, Runnable handler) {
            on(event, (x) -> handler.run());
        }

        public <T> void once(String event, Runnable handler) {
            once(event, (x) -> handler.run());
        }
    }

    public static class NetSocket extends EventEmitter {
        private final SocketChannel s;
        private final Queue<ByteBuffer> sendQueue = new LinkedList<>();
        private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(2048);
        private boolean inputEnd = false, outputEnd = false;

        public NetSocket(SocketChannel s) {
            try {
                this.s = s;
                s.configureBlocking(false);
                currentLoop().register(s, OP_READ, () -> readHandler());
                currentLoop().register(s, OP_WRITE, () -> writeHandler());
                currentLoop().setActive(s, OP_WRITE, false);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void destroy() {
            try {
                s.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void readHandler() {
            try {
                while (true) {
                    readBuffer.clear();
                    int r = s.read(readBuffer);
                    if (r == -1) {
                        emit("end");
                        inputEnd = true;
                        if (outputEnd) destroy();
                        end();
                    }
                    if (r <= 0) return;

                    readBuffer.flip();
                    byte[] chunk = new byte[readBuffer.limit()];
                    readBuffer.get(chunk);
                    emit("data", chunk);
                }
            } catch (IOException ex) {
                emit("error", ex);
            }
            currentLoop().setActive(s, OP_READ, false);
        }

        private boolean doWrite(ByteBuffer buf) throws IOException {
            if (buf != null) {
                s.write(buf);
                return buf.remaining() == 0;
            }
            s.shutdownOutput(); //FIXME: does this block?
            outputEnd = true;
            if (inputEnd) destroy();
            return true;
        }

        private void writeHandler() {
            try {
                while (!sendQueue.isEmpty()) {
                    if (!doWrite(sendQueue.peek()))
                        return;
                    sendQueue.remove();
                }
            } catch (IOException ex) {
                emit("error", ex);
            }
            currentLoop().setActive(s, OP_WRITE, false);
        }

        public boolean write(ByteBuffer buf) {
            if (outputEnd)
                throw new IllegalArgumentException("Socket closed for output");
            if (sendQueue.isEmpty()) {
                try {
                    if (doWrite(buf))
                        return true;
                } catch (IOException ex) {
                    currentLoop().nextTick(() -> emit("error", ex));
                }
                currentLoop().setActive(s, OP_WRITE, true);
            }
            sendQueue.add(buf);
            return false;
        }

        public boolean write(String data) {
            return write(ByteBuffer.wrap(data.getBytes()));
        }

        public boolean end() {
            if (outputEnd)
                return true;
            return write((ByteBuffer) null);
        }

        public boolean end(ByteBuffer data) {
            write(data);
            return end();
        }

        public boolean end(String data) {
            return end(ByteBuffer.wrap(data.getBytes()));
        }

        public void setKeepAlive(boolean enabled) {
            try {
                s.setOption(StandardSocketOptions.SO_KEEPALIVE, enabled); // FIXME: does this block?
            } catch (IOException ex) {
                currentLoop().nextTick(() -> emit("error", ex));
            }
        }
    }

    public static class NetServer extends EventEmitter {
        private final ServerSocketChannel ss;

        public NetServer() {
            try {
                ss = ServerSocketChannel.open();
                ss.configureBlocking(false);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void acceptHandler() {
            try {
                SocketChannel s = ss.accept();
                if (s != null)
                    emit("connection", new NetSocket(s));
            } catch (IOException ex) {
                emit("error", ex);
            }
        }

        public void listen(int port) {
            try {
                ss.bind(new InetSocketAddress(port)); // FIXME: blocking method: should be performed outside of the loop
                currentLoop().register(ss, OP_ACCEPT, () -> acceptHandler());
            } catch (IOException ex) {
                currentLoop().nextTick(() -> emit("error", ex));
            }
        }

        public void listen(int port, Runnable callback) {
            listen(port);
            currentLoop().nextTick(callback);
        }
    }

    public static class Readline extends EventEmitter {
        private final EventEmitter source;
        private final Charset charset = StandardCharsets.UTF_8;
        private byte[] last;

        public Readline(EventEmitter source) {
            this.source = source;
            source.on("data", (chunk) -> dataHandler((byte[]) chunk));
            source.on("end", () -> endHandler());
        }

        private void dataHandler(byte[] data) {
            int start = 0, end = 0;
            if (last != null) {
                end = last.length;
                last = Arrays.copyOf(last, last.length + data.length);
                System.arraycopy(data, 0, last, end, data.length);
                data = last;
            }

            while (true) {
                // Optimization: detect newline byte and decode afterwards
                while (end < data.length && data[end] != '\n') end++;
                if (end >= data.length) break;
                emit("line", new String(data, start, end - start, charset));
                start = end = end + 1;
            }

            last = (start < data.length) ? Arrays.copyOfRange(data, start, data.length) : null;
        }

        private void endHandler() {
            if (last != null)
                emit("line", new String(last, charset));
            emit("close");
        }

    }

    public static NetServer createServer(Consumer<NetSocket> connectionCallback) {
        NetServer result = new NetServer();
        result.on("connection", connectionCallback);
        return result;
    }

}
