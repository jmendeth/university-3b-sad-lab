package p2;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chat server.
 * @author Alba Mendez
 */
public class Server implements Runnable {

    private class Message {
        String data;
        Message(String data) {
            this.data = data;
        }
    }
    
    private final MyServerSocket serverSocket;
    private final ConcurrentHashMap<String, Connection> peers = new ConcurrentHashMap<>();

    public Server(int port) {
        try {
            this.serverSocket = new MyServerSocket(port);
        } catch (IOException ex) {
            throw new RuntimeException("Couldn't bind to specified port");
        }
    }
    
    private void broadcast(String origin, Message msg) {
        for (Entry<String, Connection> e : peers.entrySet()) {
            if (!e.getKey().equals(origin)) {
                e.getValue().sendQueue.add(msg);
            }
        }
    }
    
    @Override
    public void run() {
        try {
            System.out.println("Server listening.");
            while (true) {
                final MySocket socket = this.serverSocket.accept();
                final Connection connection = new Connection(socket);
                new Thread(connection).start();
            }
        } finally {
            serverSocket.close();
        }
    }
    
    private class Connection implements Runnable {
        
        MySocket socket;
        LinkedBlockingQueue<Message> sendQueue = new LinkedBlockingQueue<>();

        Connection(MySocket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                socket.setKeepAlive(true);

                // Read nickname from client
                String nick = socket.readLine();
                if (nick == null)
                    return;
                if (!nick.matches("^[a-zA-Z0-9 _.@-]+$")) {
                    socket.print("Error: Invalid characters in nickname\n");
                    return;
                }
                
                // Register peer
                if (peers.putIfAbsent(nick, this) != null) {
                    socket.print("Error: Nickname '" + nick + "' already in use\n");
                    return;
                }
                broadcast(nick, new Message("[" + nick + " joined the room]\n"));
                
                // Send participants (FIXME: not atomic)
                String participants = String.join(", ", peers.keySet());
                socket.print("[current participants: " + participants + "]\n");
                
                // Start sender thread
                Thread sender = new Thread(new Runnable() {
                    public void run() {
                        sendThread();
                    }
                });
                sender.start();
                
                // Main loop until EOF
                String line;
                while ((line = socket.readLine()) != null) {
                    broadcast(nick, new Message(nick + ": " + line + "\n"));
                }
                
                // Deregister peer
                peers.remove(nick);
                broadcast(nick, new Message("[" + nick + " left the room]\n"));
                
                // Send EOF to sender, and wait for it to end
                sendQueue.add(new Message(null));
                sender.join();
            } catch (RuntimeException | InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE,
                        "Unexpected exception at connection thread", ex);
            } finally {
                socket.close();
            }
        }
        
        void sendThread() {
            try {
                Message msg;
                while ((msg = sendQueue.take()).data != null) {
                    socket.print(msg.data);
                }
            } catch (RuntimeException | InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE,
                        "Unexpected exception at connection sendThread", ex);
            }
        }
        
    }

    public static void main(String[] args) {
        Server server = new Server(3500);
        server.run();
    }
    
}
