package p2;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImplFactory;
import java.nio.channels.ServerSocketChannel;

/**
 * Class that encapsulates an instance of {@link Socket},
 * with the following enhancements:
 *
 * <ul>
 * <li> Thrown exceptions are encapsulated into runtime exceptions
 * <li> Returned sockets are wrapped in {@link MySocket} instances
 * </ul>
 *
 * @see ServerSocket
 * @author Alba Mendez
 */
public class MyServerSocket implements Closeable {
    
    private final ServerSocket orig;
    
    private MyServerSocket(ServerSocket orig) {
        this.orig = orig;
    }
    
    // REEXPORTED METHODS

    /**
     * @see ServerSocket#ServerSocket()
     */
    public MyServerSocket() throws IOException {
        this(new ServerSocket());
    }

    /**
     * @see ServerSocket#ServerSocket(int)
     */
    public MyServerSocket(int port) throws IOException {
        this(new ServerSocket(port));
    }
    
    /**
     * @see ServerSocket#ServerSocket(int, int)
     */
    public MyServerSocket(int port, int backlog) throws IOException {
        this(new ServerSocket(port, backlog));
    }
    
    /**
     * @see ServerSocket#ServerSocket(int, int, InetAddress)
     */
    public MyServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        this(new ServerSocket(port, backlog, bindAddr));
    }

    /**
     * @see ServerSocket#bind(SocketAddress)
     */
    public void bind(SocketAddress endpoint) {
        try {
            orig.bind(endpoint);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#bind(SocketAddress, int)
     */
    public void bind(SocketAddress endpoint, int backlog) {
        try {
            orig.bind(endpoint, backlog);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#getInetAddress()
     */
    public InetAddress getInetAddress() {
        return orig.getInetAddress();
    }

    /**
     * @see ServerSocket#getLocalPort()
     */
    public int getLocalPort() {
        return orig.getLocalPort();
    }

    /**
     * @see ServerSocket#getLocalSocketAddress()
     */
    public SocketAddress getLocalSocketAddress() {
        return orig.getLocalSocketAddress();
    }

    /**
     * Like the original method, except the returned socket is wrapped
     * within a {@link MySocket} instance.
     *
     * @see ServerSocket#accept()
     */
    public MySocket accept() {
        try {
            return new MySocket(orig.accept());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#close()
     */
    public void close() {
        try {
            orig.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#getChannel()
     */
    public ServerSocketChannel getChannel() {
        return orig.getChannel();
    }

    /**
     * @see ServerSocket#isBound()
     */
    public boolean isBound() {
        return orig.isBound();
    }

    /**
     * @see ServerSocket#isClosed()
     */
    public boolean isClosed() {
        return orig.isClosed();
    }

    /**
     * @see ServerSocket#setSoTimeout(int)
     */
    public synchronized void setSoTimeout(int timeout) {
        try {
            orig.setSoTimeout(timeout);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#getSoTimeout()
     */
    public synchronized int getSoTimeout() {
        try {
            return orig.getSoTimeout();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#setReuseAddress(boolean)
     */
    public void setReuseAddress(boolean on) {
        try {
            orig.setReuseAddress(on);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#getReuseAddress()
     */
    public boolean getReuseAddress() {
        try {
            return orig.getReuseAddress();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#toString()
     */
    public String toString() {
        return orig.toString();
    }

    public static synchronized void setSocketFactory(SocketImplFactory fac) {
        try {
            ServerSocket.setSocketFactory(fac);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#setReceiveBufferSize(int)
     */
    public synchronized void setReceiveBufferSize(int size) {
        try {
            orig.setReceiveBufferSize(size);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#getReceiveBufferSize()
     */
    public synchronized int getReceiveBufferSize() {
        try {
            return orig.getReceiveBufferSize();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see ServerSocket#setPerformancePreferences(int, int, int)
     */
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        orig.setPerformancePreferences(connectionTime, latency, bandwidth);
    }
    
}
