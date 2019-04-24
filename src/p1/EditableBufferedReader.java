package p1;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditableBufferedReader extends BufferedReader {

    protected final Writer output;

    public EditableBufferedReader(Reader input) {
        super(input);
        this.output = new OutputStreamWriter(System.out);
    }

    protected boolean initialized = false;

    protected boolean interrupted;
    protected boolean lineEntered;
    protected boolean eofPressed;
    protected boolean submit;
    protected Line model;
    protected Console view;

    /**
     * Initializes state (view, model) to an empty line at the passed coordinates.
     * This method will be automatically called from {@link readLine} if not
     * manually called by the user before.
     *
     * The four integers describe the editor text area. startColumn and
     * startRow are 1-based.
     *
     * When submit is true, pressing ENTER submits the contents and Alt+ENTER
     * attempts to enter a linebreak. When submit is false, the behaviour is
     * reversed.
     */
    public void init(int width, int height, int startColumn, int startRow, boolean submit) {
        interrupted = false;
        lineEntered = false;
        eofPressed = false;
        this.submit = submit;
        model = new Line(width, height);
        view = new Console(output, model, startRow, startColumn);
        model.addObserver(view);

        initialized = true;
    }

    /**
     * Entry method. Puts the terminal in raw mode, starts the line editor
     * in single line mode, and blocks until a line is entered by the user.
     *
     * @return The entered line, or {@code null} if user pressed EOF.
     * @throws EOFException If EOF occurs when reading from terminal.
     * @throws InterruptedIOException If user interrupts by pressing Control+C.
     * @throws IOException Other I/O error.
     */
    @Override
    public String readLine() throws EOFException, InterruptedIOException, IOException {
        setRaw();
        try {
            if (!initialized) {
                Coordinates cursor = queryCursor();
                Coordinates bounds = queryWindowSize();
                int width = bounds.column - (cursor.column - 1);
                int height = 1;
                init(width, 1, cursor.column, cursor.row, true);
            }
            initialized = false;

            view.enableMouse();
            try {

                view.draw();
                // buffer = "";
                while (!lineEntered) {
                    if (interrupted)
                        throw new InterruptedIOException();
                    processInput();
                    view.draw();
                }
                model.moveCaret(model.getHeight(), 0);
                view.draw();
                output.write("\r\n");
                return eofPressed ? null : model.getContents();

            } finally {
                view.disableMouse();
            }
        } finally {
            unsetRaw();
        }
    }


    /**
     * Input subsystem. Reads input bytes into a temporary buffer and calls a
     * parsing function. If the parsing function requests more bytes, they're
     * read with a specified timeout, which helps separate ambiguous key
     * sequences (i.e. pressing ESC vs. actual control sequence) and the
     * function is called again. This method returns when the parsing function
     * correctly consumes bytes (which are removed from the buffer).
     *
     * The buffer is NOT guaranteed to be empty when this function returns; for
     * that, call repeatedly until buffer is empty. If EOF occurs while reading
     * this method throws EOFException immediately without calling the parsing
     * function [again].
     */
    protected String buffer = "";
    protected boolean bufferEnd;
    protected Integer readTimeout = 70;

    protected void processInput() throws EOFException, IOException {
        // If the buffer is empty, perform regular read
        if (buffer.isEmpty()) {
            int c = read();
            if (c == -1) throw new EOFException();
            buffer += String.valueOf((char)c);
            bufferEnd = false;
        }
        // Call parsing function until it succeeds
        int consumed;
        while ((consumed = parseInput(buffer, !bufferEnd, consumer)) == -1) {
            if (bufferEnd)
                throw new AssertionError("bufferEnd = true but input was NOT consumed");
            // Perform a read with timeout
            int c = (readTimeout != null) ? readWithTimeout(readTimeout) : read();
            if (c < 0) {
                bufferEnd = true;
                if (c == -1) throw new EOFException();
            } else {
                buffer += String.valueOf((char) c);
            }
        }
        // Consume bytes from buffer
        assert(consumed > 0);
        buffer = buffer.substring(consumed);
    }

    /**
     * Read the next character, with timeout.
     *
     * @param timeout Maximum time to wait for next character.
     * @return The result from {@link #read()}, or -2 if timeout reached.
     * @throws java.io.IOException
     */
    protected int readWithTimeout(int timeout) throws IOException {
        // A little bit hacky, but it's what we have...
        long current = System.currentTimeMillis();
        do {
            if (ready()) return read();
        } while (timeout > 0 && System.currentTimeMillis() < current + timeout);
        return -2;
    }


    /**
     * Parsing function. Given a (non-empty) input buffer, parse an input
     * sequence at the start of the buffer and call the appropriate function
     * on the consumer to handle it.
     *
     * The length of the consumed input sequence must be returned, which is
     * expected to be > 0. If {@code more == true} (more bytes are available)
     * the function can return -1 and it'll be called again with either more
     * bytes at the buffer, or {@code more} set to false.
     *
     * @param str Terminal input buffer.
     * @param more Indicates if more bytes can be requested.
     * @param c Consumer for the parsed entity.
     * @return Bytes consumed from the start of the buffer, or -1 if more bytes
     * are needed (only if {@code more == true}).
     */
    public static int parseInput(final String str, final boolean more, ParsingConsumer c) {
        // Begin by attempting to parse valid ECMA-48 control sequences in
        // a mostly compliant way.

        // -> try to parse C1 first
        if (str.charAt(0) == 0x1B && (1 < str.length() || more)) {
            if (1 >= str.length() && more) return -1;

            if (str.charAt(1) == '[') { // CSI
                // parse parameter chars
                int i = 2, paramStart = i;
                while (i < str.length() && (str.charAt(i) & ~0xF) == 0x30) i++;
                // parse intermediate chars
                int intermediateStart = i;
                while (i < str.length() && (str.charAt(i) & ~0xF) == 0x20) i++;
                // parse final byte
                if (i >= str.length() && more) return -1;
                if (i < str.length() && str.charAt(i) >= 0x40 && str.charAt(i) <= 0x7E) {
                    c.handleCSI(str.substring(paramStart, intermediateStart),
                            str.substring(intermediateStart, i + 1));
                    return i + 1;
                }
            } else if (str.charAt(1) == 'N' || str.charAt(1) == 'O') { // SS2 / SS3
                if (2 >= str.length() && more) return -1;
                if (2 < str.length()) {
                    c.handleSS(str.charAt(1) == 'N' ? 2 : 3, str.charAt(2));
                    return 3;
                }
            } else if (false) { // command string
                // TODO: section 5.6 of the spec
            }

            // at this point it could be another C1 (0x40-0x5F),
            // an independent function (0x70-0x4E), or a Meta modifier
            c.handleTwoByte(str.charAt(1));
            return 2;

        }

        // -> try to parse C0 and DEL (control characters)
        if (str.charAt(0) < 0x20 || str.charAt(0) == 0x7F) {
            c.handleOneByte(str.charAt(0));
            return 1;
        }

        // If there wasn't an ECMA-48 control sequence, check we have a valid,
        // full Unicode codepoint and process it.

        if (Character.isSurrogate(str.charAt(0))) {
            if (Character.isHighSurrogate(str.charAt(0))) {
                if (1 >= str.length() && more) return -1;
                if (1 < str.length() && Character.isLowSurrogate(str.charAt(1))) {
                    c.handleCodepoint(str.codePointAt(0));
                    return 2;
                }
            }
        } else { // non surrogate
            c.handleCodepoint(str.codePointAt(0));
            return 1;
        }

        // If nothing worked, ignore the byte.
        return 1;
    }

    /**
     * This interface defines callbacks that must be implemented
     * by the consumer to handle parsed entities.
     */
    public static interface ParsingConsumer {
        void handleCSI(String parameters, String function);
        void handleSS(int set, char c);
        void handleTwoByte(char c);
        void handleOneByte(char c);
        void handleCodepoint(int code);
    }

    private final ParsingConsumer consumer = new ParsingConsumer() {
        public void handleCSI(String parameters, String function) {
            EditableBufferedReader.this.handleCSI(parameters, function);
        }
        public void handleSS(int set, char c) {
            EditableBufferedReader.this.handleSS(set, c);
        }
        public void handleTwoByte(char c) {
            EditableBufferedReader.this.handleTwoByte(c);
        }
        public void handleOneByte(char c) {
            EditableBufferedReader.this.handleOneByte(c);
        }
        public void handleCodepoint(int code) {
            EditableBufferedReader.this.handleCodepoint(code);
        }
    };


    /**
     * User interface. Handles input sequences and updates model or view.
     */

    private final static Pattern MOUSE_CSI_PATTERN =
            Pattern.compile("^<?(\\d+);(\\d+);(\\d+)");

    protected void handleCSI(String parameters, String function) {
        if (function.equals("D") || function.equals("C")) { // left / right
            model.advanceCaret(function.equals("D") ? -1 : +1);
        } else if (function.equals("A") || function.equals("B")) { // up / down
            model.moveCaret(model.getRow() + (function.equals("A") ? -1 : +1), model.getColumn());
        } else if (function.equals("H") || function.equals("F")) { // home / end
            int column = function.equals("H") ? 0 : model.getLines().get(model.getRow()).size();
            model.moveCaret(model.getRow(), column);
        } else if (function.equals("~") && parameters.split(";")[0].equals("3")) { // delete
            model.delete();
        } else if (function.equals("~") && parameters.equals("2")) { // insert
            model.setInsertMode(!model.getInsertMode());
        } else if (function.equals("M")) { // mouse
            // FIXME: for backwards compatibility, parse X10-style CSIs
            // which include 3 bytes *after* the CSI..
            Matcher m = MOUSE_CSI_PATTERN.matcher(parameters);
            if (m.matches()) {
                int type = Integer.parseInt(m.group(1), 10),
                        col = Integer.parseInt(m.group(2), 10),
                        row = Integer.parseInt(m.group(3), 10);
                if ((type == 32 || type == 0) && col >= 1 && row >= 1) // FIXME
                    handleMousePress(row, col);
            }
        }
    }

    protected void handleSS(int set, char c) {
        // Old terminals send special keys as SS3 controls instead of CSIs
        if (set == 3 && "ABCDHF".indexOf(c) != -1) {
            handleCSI("", String.valueOf(c));
        }
    }

    protected void handleTwoByte(char c) {
        if (c == '\r') {
            if (submit)
                model.enterLine();
            else
                lineEntered = true;
        }
    }

    protected void handleOneByte(char c) {
        if (c == '\r') { // Enter
            if (submit)
                lineEntered = true;
            else
                model.enterLine();
        } else if (c == 0x04) { // Control+D
            if (model.getLines().size() == 1 && model.getLines().get(0).isEmpty()) {
                lineEntered = true;
                eofPressed = true;
            }
        } else if (c == 0x03) { // Control+C
            interrupted = true;
        } else if (c == 0x08 || c == 0x7F) { // BS or DEL -> backspace
            model.backspace();
        }
    }

    protected void handleCodepoint(int code) {
        // (For now, assume each codepoint is a glyph)
        model.enterGlyph(String.copyValueOf(Character.toChars(code)));
    }

    protected void handleMousePress(int row, int column) {
        model.moveCaret(row - view.getStartRow(), column - view.getStartColumn());
    }


    /**
     * Terminal query logic.
     */

    public static class Coordinates {
        int column;
        int row;
        public Coordinates(int column, int row) {
            this.column = column;
            this.row = row;
        }
    }
    private Map<String, Object> queries = new HashMap<>();
    private Integer queryTimeout = 300;

    private final static Pattern CPR_PATTERN =
            Pattern.compile("^(\\d+);(\\d+)$");
    private final static Pattern XTERM_SIZE_PATTERN =
            Pattern.compile("^8;(\\d+);(\\d+)$");

    private final ParsingConsumer queryConsumer = new ParsingConsumer() {
        public void handleCSI(String parameters, String function) {
            Matcher m;
            if (function.equals("R") && (m = CPR_PATTERN.matcher(parameters)).matches()) {
                int row = Integer.parseInt(m.group(1));
                int column = Integer.parseInt(m.group(2));
                queries.put("cursor", new Coordinates(column, row));
            } else if (function.equals("t") && (m = XTERM_SIZE_PATTERN.matcher(parameters)).matches()) {
                int rows = Integer.parseInt(m.group(1));
                int columns = Integer.parseInt(m.group(2));
                queries.put("windowSize", new Coordinates(columns, rows));
            }
        }
        public void handleSS(int set, char c) {}
        public void handleTwoByte(char c) {}
        public void handleOneByte(char c) {}
        public void handleCodepoint(int code) {}
    };

    public Coordinates queryCursor() throws IOException {
        output.write("\u001b[6n");
        output.flush();
        return (Coordinates) waitForQuery("cursor");
    }

    public Coordinates queryWindowSize() throws IOException {
        output.write("\u001b[18t");
        output.flush();
        return (Coordinates) waitForQuery("windowSize");
    }

    protected Object waitForQuery(String key) throws IOException {
        queries.remove(key);
        int bufferOffset = buffer.length();
        while (true) {
            int c = (queryTimeout != null) ? readWithTimeout(queryTimeout) : read();
            if (c < 0)
                throw new IOException("Didn't receive a reply from the terminal");
            buffer += String.valueOf((char)c);

            while (bufferOffset < buffer.length()) {
                int consumed = parseInput(buffer.substring(bufferOffset), true, queryConsumer);
                if (consumed == -1) break;
                if (queries.containsKey(key)) {
                    buffer = buffer.substring(0, bufferOffset);
                    return queries.get(key);
                }
                bufferOffset += consumed;
            }
        }
    }


    /**
     * Terminal mode change logic.
     */

    protected static void executeStty(String... args) {
        try {
            List<String> command = new ArrayList<>(Arrays.asList(args));
            command.add(0, "stty");
            final Process pr = new ProcessBuilder(command)
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();

            final int status = pr.waitFor();
            if (status != 0) {
                throw new RuntimeException("stty failed with exit code " + status);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed executing stty", ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException("Interrupted while waiting for stty", ex);
        }
    }

    public static void setRaw() {
        executeStty("-echo", "raw");
    }

    public static void unsetRaw() {
        executeStty("echo", "-raw");
    }

}
