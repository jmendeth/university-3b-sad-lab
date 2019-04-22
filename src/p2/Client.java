package p2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chat client.
 *
 * @author Alba Mendez
 */
public class Client implements Runnable {

    private final MySocket socket;
    private final BufferedReader input;
    private final PrintStream output;
    private volatile boolean ended = false;

    public Client(MySocket socket, BufferedReader input, PrintStream output) {
        this.socket = socket;
        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        try {
            // Read nickname
            output.print("Enter your nickname: ");
            output.flush();
            String nick = input.readLine();
            if (nick == null)
                return;
            socket.print(nick + "\n");

            // Start receiver thread
            Thread receiver = new Thread(new Runnable() {
                public void run() {
                    receiveThread();
                }
            });
            receiver.start();

            // Main loop
            output.println("Type :q to exit");
            while (true) {
                output.print("\033[1m> ");
                output.flush();
                String line = input.readLine();
                if (line == null || line.equals(":q"))
                    break;
                if (line.length() > 0)
                    socket.print(line + "\n");
            }

            // Send EOF to server, wait for receiver thread to finish
            ended = true;
            socket.shutdownOutput();
            receiver.join();
        } catch (RuntimeException | InterruptedException | IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE,
                    "Unexpected exception in client thread", ex);
        } finally {
            socket.close();
        }
    }

    public void receiveThread() {
        String line;
        while ((line = socket.readLine()) != null) {
            output.print("\033[s\033[m\n\033[A\033[L" + line + "\033[u\033[B\033[1m");
            output.flush();
        }

        output.println("\n\033[mConnection ended by server.");
        if (!ended) {
            // Currently no good way to interrupt the main thread loop,
            // so just exit
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: client.js <hostname> [<port>]");
            System.exit(1);
        }

        MySocket socket = null;
        try {
            socket = new MySocket(args[0], args.length > 1 ? Integer.parseInt(args[1]) : 3500);
        } catch (IOException ex) {
            System.err.println("Failed connecting to server: " + ex.getMessage());
            System.exit(1);
        }
        System.out.println("Connected to server.");

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        Client client = new Client(socket, input, System.out);
        client.run();
    }

}
