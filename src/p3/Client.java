package p3;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import p2.MySocket;

/**
 * Graphical chat client.
 * @author Alba Mendez
 */
public class Client {

    public Client() {
        initUI();
    }
    
    // UI
    
    private static final String CARD_CONNECT = "connect";
    private static final String CARD_CHAT = "chat";
    
    private static final String ABOUT_TEXT =
            "Chat client v1.0.0-pre\n" +
            "\n" +
            "SAD, QP2018\n" +
            "Alba Mendez";
        
    private JFrame frame;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JMenuItem disconnectMenuItem;

    private JTextField hostnameField;
    private JFormattedTextField portField;
    private JTextField nicknameField;
    private JButton connectButton;

    private JList<String> usersList;
    private DefaultListModel<String> usersListModel;
    private JTextField messageField;
    private JEditorPane messagesPane;
    private boolean scrollToBottom;

    private HTMLDocument messagesDoc;
    private Element messagesElem;
    
    private void initUI() {
        // FIXME: don't use open sans directly
        // FIXME: intantiate fillers correctly
        // FIXME: enable button when nickname is filled
        
        // CONNECTION PANEL

        JPanel connectionFormPanel = new JPanel(new GridLayout(4, 2));

        ActionListener connectAction = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                connect();
            }
        };

        nicknameField = new JTextField();
        nicknameField.setColumns(8);
        nicknameField.addActionListener(connectAction);

        JLabel nicknameLabel = new JLabel();
        nicknameLabel.setLabelFor(nicknameField);
        nicknameLabel.setText("Nickname:");
        nicknameLabel.setFont(nicknameLabel.getFont().deriveFont(Font.BOLD));
        connectionFormPanel.add(nicknameLabel);
        connectionFormPanel.add(nicknameField);

        hostnameField = new JTextField();
        hostnameField.setColumns(8);
        hostnameField.setText("localhost");
        hostnameField.addActionListener(connectAction);

        JLabel hostnameLabel = new JLabel();
        hostnameLabel.setLabelFor(hostnameField);
        hostnameLabel.setText("Hostname:");
        connectionFormPanel.add(hostnameLabel);
        connectionFormPanel.add(hostnameField);

        portField = new JFormattedTextField();
        portField.setColumns(4);
        portField.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(new DecimalFormat("#0"))));
        portField.setText("3500");
        portField.addActionListener(connectAction);
        
        JLabel portLabel = new JLabel();
        portLabel.setLabelFor(portField);
        portLabel.setText("Port:");
        connectionFormPanel.add(portLabel);
        connectionFormPanel.add(portField);

        connectButton = new JButton();
        connectButton.setFont(new Font("Open Sans", 0, 14));
        connectButton.setIcon(new ImageIcon(getClass().getResource("/p3/assets/connect.png")));
        connectButton.setText("Connect");
        connectButton.addActionListener(connectAction);
        connectionFormPanel.add(connectButton);

        JPanel connectionPanel = new JPanel();
        connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.PAGE_AXIS));
        connectionPanel.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 31333), new Dimension(0, 32767)));
        connectionPanel.add(connectionFormPanel);
        connectionPanel.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 31333), new Dimension(0, 31333)));

        // CHAT PANEL

        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.LINE_AXIS));

        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.setMinimumSize(new Dimension(50, 100));
        usersPanel.setPreferredSize(new Dimension(100, 100));

        usersList = new JList<>();
        usersListModel = new DefaultListModel<>();
        usersList.setModel(usersListModel);

        JScrollPane usersScrollPane = new JScrollPane();
        usersScrollPane.setViewportView(usersList);
        usersPanel.add(usersScrollPane, BorderLayout.CENTER);

        JLabel usersLabel = new JLabel();
        usersLabel.setText("Users:");
        usersLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0)); //FIXME: replace with gap
        usersPanel.add(usersLabel, BorderLayout.PAGE_START);

        chatPanel.add(usersPanel);
        chatPanel.add(new Box.Filler(new Dimension(10, 0), new Dimension(10, 0), new Dimension(10, 32767)));

        JPanel messagePanel = new JPanel(new BorderLayout(0, 2));
        messagePanel.setMinimumSize(new Dimension(200, 200));
        messagePanel.setPreferredSize(new Dimension(400, 400));

        messagesPane = new JEditorPane();
        messagesPane.setEditable(false);
        messagesPane.setContentType("text/html");

        JScrollPane messagesScrollPane = new JScrollPane();
        messagesScrollPane.setViewportView(messagesPane);
        final JScrollBar vsb = messagesScrollPane.getVerticalScrollBar();
        vsb.addAdjustmentListener(new AdjustmentListener() {
            int value = vsb.getValue();
            public void adjustmentValueChanged(AdjustmentEvent e) {
                int max = vsb.getMaximum() - vsb.getVisibleAmount();
                if (vsb.getValue() != value) {
                    value = vsb.getValue();
                    scrollToBottom = (value >= max);
                } else {
                    if (scrollToBottom) {
                        value = max;
                        vsb.setValue(max);
                    }
                }
            }
        });
        messagePanel.add(messagesScrollPane, BorderLayout.CENTER);

        JPanel sendPanel = new JPanel(new BorderLayout());

        messageField = new JTextField();
        messageField.setFont(new Font("Open Sans", 0, 13));
        messageField.setMargin(new Insets(4, 4, 4, 4));
        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        sendPanel.add(messageField, BorderLayout.CENTER);

        JButton sendButton = new JButton();
        sendButton.setIcon(new ImageIcon(getClass().getResource("/p3/assets/send.png")));
        sendButton.setBorderPainted(false);
        sendButton.setFocusable(false);
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                sendMessage();
            }
        });
        sendPanel.add(sendButton, BorderLayout.LINE_END);

        messagePanel.add(sendPanel, BorderLayout.SOUTH);

        chatPanel.add(messagePanel);

        // STATUS PANEL

        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 1, 2, 1));
        statusPanel.setLayout(new BorderLayout());

        statusLabel = new JLabel();
        statusLabel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        // MENUS
        
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu();
        fileMenu.setText("File");

        disconnectMenuItem = new JMenuItem();
        disconnectMenuItem.setText("Disconnect");
        disconnectMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                disconnect();
            }
        });
        fileMenu.add(disconnectMenuItem);
        fileMenu.add(new JPopupMenu.Separator());

        JMenuItem exitMenuItem = new JMenuItem();
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitApplication();
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu();
        helpMenu.setText("Help");

        JMenuItem aboutMenuItem = new JMenuItem();
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                about();
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        // GLOBAL

        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        mainPanel.setLayout(new CardLayout());
        mainPanel.add(connectionPanel, CARD_CONNECT);
        mainPanel.add(chatPanel, CARD_CHAT);

        frame = new JFrame("Chat client");
        frame.setJMenuBar(menuBar);
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setPreferredSize(new Dimension(700, 600));
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        frame.pack();
        
        renderStatus();
        resetMessages();
    }
    
    private void renderStatus() {
        if (!isActive()) {
            statusLabel.setIcon(new ImageIcon(getClass().getResource("/p3/assets/server-error.png")));
            if (failReason != null) {
                statusLabel.setText("Connection failed: " + failReason);
            } else {
                statusLabel.setText("Disconnected");
            }
        } else if (socket == null) {
            statusLabel.setIcon(new ImageIcon(getClass().getResource("/p3/assets/server-connecting.png")));
            statusLabel.setText("Connecting...");
        } else {
            String hostname = socket.getInetAddress().getHostName();
            int port = socket.getPort();
            statusLabel.setIcon(new ImageIcon(getClass().getResource("/p3/assets/server-ok.png")));
            statusLabel.setText("Connected to " + hostname + ":" + port);
        }
        
        boolean enabled = !isActive();
        for (Component c : new Component[]{ hostnameField, portField, nicknameField, connectButton })
            c.setEnabled(enabled);
        disconnectMenuItem.setEnabled(isActive());
    }

    public static String escapeHTML(String s) {
        // By Bruno Eberhard - https://stackoverflow.com/a/25228492/710951
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private void appendMessage(String nick, String content) {
        StringBuilder bf = new StringBuilder();
        bf.append("<div class=\"box\">");
        bf.append("<span class=\"user\">").append(escapeHTML(nick)).append("</span> ");
        bf.append("<span class=\"message\">").append(escapeHTML(content)).append("</span></div>");
        try {
            messagesDoc.insertBeforeEnd(messagesElem, bf.toString());
        } catch (BadLocationException | IOException ex) {
            throw new RuntimeException(ex);
        }
        System.out.println(messagesPane.getText());
    }
    
    private void appendNotification(String content) {
        StringBuilder bf = new StringBuilder();
        bf.append("<div class=\"notification\">").append(escapeHTML(content)).append("</div>");
        try {
            messagesDoc.insertBeforeEnd(messagesElem, bf.toString());
        } catch (BadLocationException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void resetMessages() {
        messagesPane.setText("<html>\n  <head>\n<style type=\"text/css\">\n\nbody {\n  font-name: Roboto, 'Open Sans', Ubuntu, sans-serif;\n  font-size: 12px;\n  background-color: #ffffff;\n  color: #000000;\n  padding: 1px 3px;\n}\n.box {\n  padding-top: 1px;\n  padding-bottom: 1px;\n}\n.box .user {\n  font-weight: bold;\n  font-size: 10px;\n}\n.notification {\n  font-weight: normal;\n  text-align: center;\n  color: #555555;\n  font-size: 10px;\n  padding-top: 2px;\n  padding-bottom: 2px;\n}\n\n\n</style>\n  </head>\n  <body><div id=\"messages\">" + "<div class=\"notification\">You have joined the room</div></div>\n  </body>\n</html>\n");
        messagesDoc = (HTMLDocument) messagesPane.getDocument();
        messagesElem = messagesDoc.getElement("messages");
        scrollToBottom = true;
    }
    
    public void start() {
        frame.setVisible(true);
        nicknameField.requestFocus();
    }
    
    // UI CALLBACKS
    
    private void exitApplication() {
        if (isActive() && socket != null) {
            int option = JOptionPane.showConfirmDialog(frame, "Do you want to exit the application?\nYou will be disconnected from the server.",
                    "Disconnection", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (option != JOptionPane.YES_OPTION)
                return;
        }
        frame.setVisible(false);
        frame.dispose();
        // Try to shutdown the receiver thread (FIXME)
        if (isActive()) {
            if (socket != null)
                socket.close();
            this.receiver.interrupt();
        }
        System.exit(0);
    }
    
    private void about() {
        JOptionPane.showMessageDialog(frame, ABOUT_TEXT, "About", JOptionPane.PLAIN_MESSAGE);
    }
    
    private void connect() {
        failReason = null;
        String hostname = hostnameField.getText();
        int port = Integer.parseInt(portField.getText());
        String nick = nicknameField.getText();
        startConnection(hostname, port, nick);
        renderStatus();
    }
    
    private void disconnect() {
        if (socket != null) {
            // FIXME: handle case where socket isn't connected yet
            socket.shutdownOutput();
            ((CardLayout)(mainPanel.getLayout())).show(mainPanel, CARD_CONNECT);
        }
    }
    
    private void sendMessage() {
        String line = messageField.getText();
        if (line.trim().length() > 0) {
            socket.print(line + "\n");
            scrollToBottom = true;
            appendMessage(nick, line);
        }
        messageField.setText("");
    }
    
    // NETWORK CALLBACKS
    
    private void connected(String[] participants) {
        renderStatus();
        
        // Prepare users pane
        usersListModel.clear();
        for (String p : participants)
            usersListModel.addElement(p);
        // Show card
        resetMessages();
        ((CardLayout)(mainPanel.getLayout())).show(mainPanel, CARD_CHAT);
        messageField.requestFocus();
    }
    
    private void receivedMessage(String line) {
        Matcher m;
        if ((m = PATTERN_JOINED.matcher(line)).matches()) {
            usersListModel.addElement(m.group(1));
            appendNotification(m.group(1) + " has joined the room");
        } else if ((m = PATTERN_LEFT.matcher(line)).matches()) {
            usersListModel.removeElement(m.group(1));
            appendNotification(m.group(1) + " has left the room");
        } else {
            String[] parts = line.split(": ", 2);
            appendMessage(parts[0], parts[1]);
        }
    }
    
    private void disconnected() {
        renderStatus();
        ((CardLayout)(mainPanel.getLayout())).show(mainPanel, CARD_CONNECT);
    }

    // NETWORK LOGIC
    
    private Thread receiver;
    private MySocket socket;
    private String nick;
    private String failReason;
    
    private boolean isActive() {
        return receiver != null && receiver.isAlive();
    }
    
    private void startConnection(final String hostname, final int port, final String nick) {
        if (isActive())
            return;
        socket = null;
        receiver = new Thread(new Runnable() {
            public void run() {
                networkThread(hostname, port, nick);
            }
        });
        receiver.start();
    }
    
    // NETWORK THREAD
    
    private final Pattern PATTERN_PARTICIPANTS =
            Pattern.compile("^\\[current participants: (.+)\\]$");
    private final Pattern PATTERN_JOINED =
            Pattern.compile("^\\[(.+) joined the room\\]$");
    private final Pattern PATTERN_LEFT =
            Pattern.compile("^\\[(.+) left the room\\]$");
    
    private void networkThread(String hostname, int port, final String nick) {
        // Try to connect
        try (final MySocket socket = new MySocket(hostname, port)) {
            // Send nick
            socket.print(nick + "\n");
            String line = socket.readLine();
            if (line == null)
                throw new IOException("Connection closed unexpectedly");
            
            // Read participants or error from server
            Matcher m;
            if (!(m = PATTERN_PARTICIPANTS.matcher(line)).matches())
                throw new IOException(line);
            final String[] participants = m.group(1).split(", ");
            
            // Send connected event
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Client.this.socket = socket;
                    Client.this.nick = nick;
                    connected(participants);
                }
            });
            
            // Main loop
            while ((line = socket.readLine()) != null) {
                final String _line = line;
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        receivedMessage(_line);
                    }
                });
            }
        } catch (final IOException | RuntimeException ex) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    failReason = ex.getMessage();
                }
            });
        } finally {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    socket = null;
                    receiver = null;
                    disconnected();
                }
            });
        }
    }



    public static boolean useSystemLAF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            return true;
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException ex) {
        }
        return false;
    }
    
    public static boolean useNimbusLAF() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return true;
                }
            }
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException ex) {
        }
        return false;
    }
    
    public static void main(String args[]) {
        // Try to set Nimbus look & feel
        if (!useSystemLAF()) {
            if (!useNimbusLAF()) {
                System.out.println("Warning: Couldn't set LAF");
            }
        }
        
        // Start the application
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                Client client = new Client();
                client.frame.setVisible(true);
            }
        });
    }
    
}
