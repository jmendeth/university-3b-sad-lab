package p1;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditableBufferedReader extends BufferedReader {

    protected final Writer output;

    public EditableBufferedReader(Reader arg0) {
        super(arg0);
        this.output = new OutputStreamWriter(System.out);
        resetState();
    }

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

    /**
     * Entry method. Puts the terminal in raw mode, starts the line editor, and
     * blocks until a line is entered by the user.
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
            enableMouse();
            try {
                resetState();
                buffer = "";
                while (!lineEntered) {
                    if (interrupted) {
                        interrupted = false;
                        throw new InterruptedIOException();
                    }
                    processInput();
                }
                return eofPressed ? null : String.join("", lineContents);
            } finally {
                disableMouse();
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
        while ((consumed = parseInput(buffer, !bufferEnd)) == -1) {
            if (bufferEnd)
                throw new IllegalStateException("bufferEnd = true but input was NOT consumed");
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
     * Parsing function. Given a (non-empty) input buffer, parse an input
     * sequence at the start of the buffer and call the appropriate function to
     * handle it.
     *
     * The length of the consumed input sequence must be returned, which is
     * expected to be > 0. If {@code more == true} (more bytes are available)
     * the function can return -1 and it'll be called again with either more
     * bytes at the buffer, or {@code more} set to false.
     *
     * @param str Terminal input buffer.
     * @param more Indicates if more bytes can be requested.
     * @return Bytes consumed from the start of the buffer, or -1 if more bytes
     * are needed (only if {@code more == true}).
     */
    protected int parseInput(final String str, final boolean more) {
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
                    handleCSI(str.substring(paramStart, intermediateStart),
                            str.substring(intermediateStart, i + 1));
                    return i + 1;
                }
            } else if (str.charAt(1) == 'N' || str.charAt(1) == 'O') { // SS2 / SS3
                if (2 >= str.length() && more) return -1;
                if (2 < str.length()) {
                    handleSS(str.charAt(1) == 'N' ? 2 : 3, str.charAt(2));
                    return 3;
                }
            } else if (false) { // command string
                // TODO: section 5.6 of the spec
            }

            // at this point it could be another C1 (0x40-0x5F),
            // an independent function (0x70-0x4E), or a Meta modifier
            handleTwoByte(str.charAt(1));
            return 2;

        }

        // -> try to parse C0 and DEL (control characters)
        if (str.charAt(0) < 0x20 || str.charAt(0) == 0x7F) {
            handleOneByte(str.charAt(0));
            return 1;
        }

        // If there wasn't an ECMA-48 control sequence, check we have a valid,
        // full Unicode codepoint and process it.

        if (Character.isSurrogate(str.charAt(0))) {
            if (Character.isHighSurrogate(str.charAt(0))) {
                if (1 >= str.length() && more) return -1;
                if (1 < str.length() && Character.isLowSurrogate(str.charAt(1))) {
                    handleCodepoint(str.codePointAt(0));
                    return 2;
                }
            }
        } else { // non surrogate
            handleCodepoint(str.codePointAt(0));
            return 1;
        }

        // If nothing worked, ignore the byte.
        return 1;
    }


    /**
     * State and user interface. Handles input sequences and updates state and
     * terminal.
     */
    protected boolean interrupted;
    protected boolean lineEntered;
    protected boolean eofPressed;
    protected List<String> lineContents;
    protected boolean insertMode;
    protected int caret;
    protected int offsetX;
    protected int offsetY;
    protected int width;
    protected int height;

    protected final void resetState() {
        interrupted = false;
        lineEntered = false;
        eofPressed = false;
        lineContents = new ArrayList<>();
        insertMode = true;
        caret = 0;
        offsetX = 0;
        offsetY = 0;
        width = 20;
        height = 1;
    }

    private final static Pattern MOUSE_CSI_PATTERN =
            Pattern.compile("^<?(\\d+);(\\d+);(\\d+)");

    protected void handleCSI(String parameters, String function) {
        if (function.equals("D") || function.equals("C")) { // left / right
            int newCaret = caret + (function.equals("D") ? -1 : +1);
            if (newCaret < 0) newCaret = 0;
            if (newCaret > lineContents.size()) newCaret = lineContents.size();

            if (caret != newCaret) {
                caret = newCaret;
                writeOutput("\u001B[" + function);
            }
        } else if (function.equals("H") || function.equals("F")) { // home / end
            moveCaret(function.equals("H") ? 0 : lineContents.size(), true);
        } else if (function.equals("~") && parameters.split(";")[0].equals("3")) { // delete
            if (caret < lineContents.size()) {
                lineContents.remove(caret);
                writeOutput(setCursor(offsetX + caret) + insertColumns(-1));
            }
        } else if (function.equals("~") && parameters.equals("2")) { // insert
            insertMode = !insertMode;
        } else if (function.equals("M")) { // mouse
            // FIXME: for backwards compatibility, parse X10-style CSIs
            // which include 3 bytes *after* the CSI..
            Matcher m = MOUSE_CSI_PATTERN.matcher(parameters);
            if (m.matches()) {
                int type = Integer.parseInt(m.group(1), 10),
                        col = Integer.parseInt(m.group(2), 10),
                        row = Integer.parseInt(m.group(3), 10);
                if ((type == 32 || type == 0) && col >= 1 && row >= 1) // FIXME
                    handleMousePress(col - 1, row - 1);
            }
        }
    }

    protected void handleSS(int set, char c) {
    }

    protected void handleTwoByte(char c) {
    }

    protected void handleOneByte(char c) {
        if (c == '\r') { // Enter
            lineEntered = true;
        } else if (c == 0x04) { // Control+D
            if (lineContents.isEmpty()) {
                lineEntered = true;
                eofPressed = true;
            }
        } else if (c == 0x03) { // Control+C
            interrupted = true;
        } else if (c == 0x08 || c == 0x7F) { // BS or DEL -> backspace
            if (caret > 0) {
                caret--;
                if (insertMode) {
                    lineContents.remove(caret);
                    writeOutput(setCursor(offsetX + caret) + insertColumns(-1));
                } else {
                    writeOutput(setCursor(offsetX + caret));
                }
            }
        }
    }

    protected void handleCodepoint(int code) {
        // (For now, assume each codepoint is a glyph)
        // Render to screen
        String out = "";
        if (insertMode && caret < lineContents.size())
            out += insertColumns(1);
        out += fromCodepoint(code);
        // Store glyph at caret position, increment caret
        if (!insertMode && caret < lineContents.size())
            lineContents.remove(caret);
        lineContents.add(caret, fromCodepoint(code));
        caret++;
        // Position at the caret, to make sure
        out += setCursor(offsetX + caret);
        writeOutput(out);
    }

    protected void moveCaret(int newCaret, boolean force) {
        if (caret != newCaret || force) {
            caret = newCaret;
            writeOutput(setCursor(offsetX + caret));
        }
    }

    protected void handleMousePress(int x, int y) {
        // For now (since we don't know vertical position of the editor yet)
        // simply move caret according to x
        int newCaret = x - offsetX;
        if (newCaret > lineContents.size())
            newCaret = lineContents.size();
        else if (newCaret < 0)
            newCaret = 0;
        moveCaret(newCaret, true);
    }


    /**
     * Terminal output / query functions.
     */

    protected static String setCursor(int x) {
        return String.format("\u001b[%dG", x + 1);
    }

    protected static String insertColumns(int n) {
        if (n == 0) return "";

        char command = '@';
        if (n < 0) {
            command = 'P';
            n = -n;
        }
        if (n == 1)
            return String.format("\u001b[%c", command);
        return String.format("\u001b[%d%c", n, command);
    }

    protected static String fromCodepoint(int code) {
        return String.copyValueOf(Character.toChars(code));
    }

    protected void writeOutput(String out) {
        try {
            output.write(out);
            output.flush();
        } catch (IOException ex) {
            throw new RuntimeException("IOException while flushing output:", ex);
        }
    }

    protected void enableMouse() {
        // Enable mousepress-only reporting (9) with extension 1006.
        // Extension 1015 is used as a fallback only.
        writeOutput("\u001b[?9h" + "\u001b[?1015h" + "\u001b[?1006h");
    }

    protected void disableMouse() {
        writeOutput("\u001b[?9l");
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

}
