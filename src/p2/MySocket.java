package p2;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Class that encapsulates an instance of {@link Socket},
 * with the following enhancements:
 *
 * <ul>
 * <li> Thrown exceptions are encapsulated into runtime exceptions
 * <li> Methods are provided to send and receive basic types
 * <li> Input is buffered
 * </ul>
 *
 * @see Socket
 * @author Alba Mendez
 */
public class MySocket implements Closeable {
    
    private final Socket orig;
    private PrintStream send;
    private BufferedReader reader;
    private Scanner recv;
    
    MySocket(Socket orig) throws IOException {
        this.orig = orig;
        reader = new BufferedReader(new InputStreamReader(orig.getInputStream()));
        recv = new Scanner(reader);
        send = new PrintStream(orig.getOutputStream());
    }
    
    public String readLine() {
        try {
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public String next() {
        return recv.next();
    }

    public String next(String pattern) {
        return recv.next(pattern);
    }

    public String next(Pattern pattern) {
        return recv.next(pattern);
    }

    public String nextLine() {
        return recv.nextLine();
    }

    public boolean nextBoolean() {
        return recv.nextBoolean();
    }

    public byte nextByte() {
        return recv.nextByte();
    }

    public short nextShort() {
        return recv.nextShort();
    }

    public int nextInt() {
        return recv.nextInt();
    }

    public long nextLong() {
        return recv.nextLong();
    }

    public float nextFloat() {
        return recv.nextFloat();
    }

    public double nextDouble() {
        return recv.nextDouble();
    }
    
    public char nextChar() {
        return recv.next(".").charAt(0);
    }

    public void print(boolean b) {
        send.print(b);
    }

    public void print(char c) {
        send.print(c);
    }

    public void print(int i) {
        send.print(i);
    }

    public void print(long l) {
        send.print(l);
    }

    public void print(float f) {
        send.print(f);
    }

    public void print(double d) {
        send.print(d);
    }

    public void print(String s) {
        send.print(s);
    }
    
    // REEXPORTED METHODS

    /**
     * @see Socket#Socket(String, int)
     */
    public MySocket(String host, int port) throws UnknownHostException, IOException {
        this(new Socket(host, port));
    }
    
    /**
     * @see Socket#Socket(InetAddress, int)
     */
    public MySocket(InetAddress address, int port) throws IOException {
        this(new Socket(address, port));
    }
    
    /**
     * @see Socket#Socket(String, int, InetAddress, int)
     */
    public MySocket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        this(new Socket(host, port, localAddr, localPort));
    }
        
    /**
     * @see Socket#Socket(InetAddress, int, InetAddress, int)
     */
    public MySocket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        this(new Socket(address, port, localAddr, localPort));
    }
    
    /**
     * @see Socket#Socket(String, int, boolean)
     */
    @Deprecated
    public MySocket(String host, int port, boolean stream) throws IOException {
        this(new Socket(host, port, stream));
    }

    /**
     * @see Socket#Socket(InetAddress, int, boolean)
     */
    @Deprecated
    public MySocket(InetAddress host, int port, boolean stream) throws IOException {
        this(new Socket(host, port, stream));
    }

    /**
     * @see Socket#getInetAddress()
     */
    public InetAddress getInetAddress() {
        return orig.getInetAddress();
    }

    /**
     * @see Socket#getLocalAddress()
     */
    public InetAddress getLocalAddress() {
        return orig.getLocalAddress();
    }

    /**
     * @see Socket#getPort()
     */
    public int getPort() {
        return orig.getPort();
    }

    /**
     * @see Socket#getLocalPort()
     */
    public int getLocalPort() {
        return orig.getLocalPort();
    }

    /**
     * @see Socket#getRemoteSocketAddress()
     */
    public SocketAddress getRemoteSocketAddress() {
        return orig.getRemoteSocketAddress();
    }

    /**
     * @see Socket#getLocalSocketAddress()
     */
    public SocketAddress getLocalSocketAddress() {
        return orig.getLocalSocketAddress();
    }

    /**
     * @see Socket#getChannel()
     */
    public SocketChannel getChannel() {
        return orig.getChannel();
    }

    /**
     * @see Socket#getInputStream()
     */
    public InputStream getInputStream() {
        try {
            return orig.getInputStream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getOutputStream()
     */
    public OutputStream getOutputStream() {
        try {
            return orig.getOutputStream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setTcpNoDelay(boolean)
     */
    public void setTcpNoDelay(boolean on) {
        try {
            orig.setTcpNoDelay(on);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getTcpNoDelay()
     */
    public boolean getTcpNoDelay() {
        try {
            return orig.getTcpNoDelay();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setSoLinger(boolean, int)
     */
    public void setSoLinger(boolean on, int linger) {
        try {
            orig.setSoLinger(on, linger);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getSoLinger()
     */
    public int getSoLinger() {
        try {
            return orig.getSoLinger();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#sendUrgentData(int)
     */
    public void sendUrgentData(int data) {
        try {
            orig.sendUrgentData(data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setOOBInline(boolean)
     */
    public void setOOBInline(boolean on) {
        try {
            orig.setOOBInline(on);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getOOBInline()
     */
    public boolean getOOBInline() {
        try {
            return orig.getOOBInline();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setSoTimeout(int)
     */
    public synchronized void setSoTimeout(int timeout) {
        try {
            orig.setSoTimeout(timeout);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getSoTimeout()
     */
    public synchronized int getSoTimeout() {
        try {
            return orig.getSoTimeout();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setSendBufferSize(int)
     */
    public synchronized void setSendBufferSize(int size) {
        try {
            orig.setSendBufferSize(size);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getSendBufferSize()
     */
    public synchronized int getSendBufferSize() {
        try {
            return orig.getSendBufferSize();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setReceiveBufferSize(int)
     */
    public synchronized void setReceiveBufferSize(int size) {
        try {
            orig.setReceiveBufferSize(size);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getReceiveBufferSize()
     */
    public synchronized int getReceiveBufferSize() {
        try {
            return orig.getReceiveBufferSize();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setKeepAlive(boolean)
     */
    public void setKeepAlive(boolean on) {
        try {
            orig.setKeepAlive(on);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getKeepAlive()
     */
    public boolean getKeepAlive() {
        try {
            return orig.getKeepAlive();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setTrafficClass(int)
     */
    public void setTrafficClass(int tc) {
        try {
            orig.setTrafficClass(tc);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getTrafficClass()
     */
    public int getTrafficClass() {
        try {
            return orig.getTrafficClass();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#setReuseAddress(boolean)
     */
    public void setReuseAddress(boolean on) {
        try {
            orig.setReuseAddress(on);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#getReuseAddress()
     */
    public boolean getReuseAddress() {
        try {
            return orig.getReuseAddress();
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#close()
     */
    public synchronized void close() {
        try {
            orig.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#shutdownInput()
     */
    public void shutdownInput() {
        try {
            orig.shutdownInput();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#shutdownOutput()
     */
    public void shutdownOutput() {
        try {
            orig.shutdownOutput();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @see Socket#toString()
     */
    public String toString() {
        return orig.toString();
    }

    /**
     * @see Socket#isConnected()
     */
    public boolean isConnected() {
        return orig.isConnected();
    }

    /**
     * @see Socket#isBound()
     */
    public boolean isBound() {
        return orig.isBound();
    }

    /**
     * @see Socket#isClosed()
     */
    public boolean isClosed() {
        return orig.isClosed();
    }

    /**
     * @see Socket#isInputShutdown()
     */
    public boolean isInputShutdown() {
        return orig.isInputShutdown();
    }

    /**
     * @see Socket#isOutputShutdown()
     */
    public boolean isOutputShutdown() {
        return orig.isOutputShutdown();
    }

    /**
     * @see Socket#setPerformancePreferences(int, int, int)
     */
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        orig.setPerformancePreferences(connectionTime, latency, bandwidth);
    }
    
}
