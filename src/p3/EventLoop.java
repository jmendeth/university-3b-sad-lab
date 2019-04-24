package p3;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple event loop.
 * @author Alba Mendez
 */
public class EventLoop implements Runnable {

    private class Handler {
        int ops;
        Runnable callback;

        public Handler(int ops, Runnable callback) {
            this.ops = ops;
            this.callback = callback;
        }
    }

    private static final ThreadLocal<EventLoop> CURRENT = new ThreadLocal<>();

    private final Map<SelectionKey, List<Handler>> handlers = new HashMap<>();
    private final List<Runnable> tickHandlers = new ArrayList<>();
    private Selector sel;

    /**
     * Constructs a new event loop.
     */
    public EventLoop() {
    }

    /**
     * Constructs a new event loop, and schedules the passed task
     * to be run when it starts.
     *
     * @param main Task to be run
     */
    public EventLoop(Runnable main) {
        this();
        nextTick(main);
    }

    /**
     * Register a handler that will be called whenever one of the passed
     * operations is ready on the specified channel.
     *
     * Important: Never register more than one handler for an operation on
     * the same channel.
     *
     * @param channel Channel to monitor for readiness.
     * @param ops Set of operations associated with this handler.
     * @param callback Function that will be called.
     */
    public void register(SelectableChannel channel, int ops, Runnable callback) {
        try {
            // Add ops to the interested set of the key
            SelectionKey key = channel.keyFor(sel);
            if (key == null)
                key = channel.register(sel, ops);
            else
                key.interestOps(key.interestOps() | ops);

            // Add handler to list
            List<Handler> hs = handlers.get(key);
            if (hs == null) {
                hs = new ArrayList<>(4);
                handlers.put(key, hs);
            }
            hs.add(new Handler(ops, callback));
        } catch (ClosedChannelException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Temporarily enable or disable listening for the passed operations on
     * the channel.
     *
     * If active is {@code false}, the handlers associated with the passed
     * operations will no longer be called, even if the channel is ready
     * for some of them. If active is {@code true}, the handlers will be called
     * when the operations are ready.
     *
     * This should only be called after handlers have been registered for
     * all of the passed operations.
     *
     * @param channel Channel on which to modify interest.
     * @param ops Operations to enable or disable listening for.
     * @param active Whether to start or stop listening.
     */
    public void setActive(SelectableChannel channel, int ops, boolean active) {
        SelectionKey key = channel.keyFor(sel);
        int kops = key.interestOps();
        key.interestOps(active ? (kops | ops) : (kops & ~ops));
    }

    /**
     * Register a handler that will be run once, when the next tick
     * of the event loop begins.
     *
     * This method can be called when the loop is not executing.
     *
     * @param callback Handler to run.
     */
    public void nextTick(Runnable callback) {
        tickHandlers.add(callback);
    }

    private boolean isDead() {
        return tickHandlers.isEmpty() && sel.keys().isEmpty();
    }

    private int getSelectTime() {
        if (!tickHandlers.isEmpty())
            return 1;
        return 0;
    }

    private void loop() throws IOException {
        while (!isDead()) {
            // Tick handlers
            List<Runnable> th = new ArrayList<>(tickHandlers);
            tickHandlers.clear();
            for (Runnable r : th) r.run();

            // Selection operation
            sel.select(getSelectTime());

            // Dispatch events
            List<Runnable> eh = new ArrayList<>();
            for (SelectionKey key : sel.selectedKeys()) {
                for (Handler handler : handlers.get(key)) {
                    if ((handler.ops & key.readyOps()) != 0)
                        eh.add(handler.callback);
                }
            }
            sel.selectedKeys().clear();
            for (Runnable r: eh) r.run();
        }
    }

    /**
     * Execute this event loop.
     */
    @Override
    public void run() {
        CURRENT.set(this);
        try (Selector sel = Selector.open()) {
            this.sel = sel;
            loop();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            CURRENT.remove();
        }
    }

    /**
     * Get the event loop associated with this thread.
     *
     * @throws IllegalStateException If not called from an event loop handler.
     * @return The current event loop.
     */
    public static EventLoop currentLoop() {
        EventLoop loop = CURRENT.get();
        if (loop == null)
            throw new IllegalStateException("Not called from an event loop handler");
        return loop;
    }

}
