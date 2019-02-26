package p1;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

public class EditableBufferedReader extends BufferedReader {

    public EditableBufferedReader(Reader arg0) {
        super(arg0);
        setRaw();
    }

    protected static void executeStty(String args) {
        try {
            final Process pr = Runtime.getRuntime().exec("stty " + args);
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
        executeStty("-echo raw");
    }
    
    public static void unsetRaw() {
        executeStty("echo -raw");
    }
    
    /** Buffer/search subsystem **/
    protected String buffer = "";
    protected void readNextChar() throws IOException {
        int c = read();
        if (c != -1) buffer += String.valueOf((char)c);
        if (buffer.length() > 0) {
            if (processInput(buffer))
                buffer = "";
        }
        if (c == -1) throw new EOFException();
    }
    
    /** 
     * Process the input buffer by calling appropriate methods.
     * buffer contains at least one char.
     * @return true if the buffer has been consumed, false otherwise.
     */
    
    
    protected static CSI processInput(String buffer) {
        if (buffer.charAt(0) == 0x1B) { // Escape sequence
            
            if (buffer.length() <= 1) return false;
            if (buffer.charAt(1) == '[') { // CSI
                if (buffer.length() <= 2) return false;
                // parse parameter chars
                int i = 2, paramStart = i;
                while (i < buffer.length() && (buffer.charAt(i) & 0xF0) == 0x30) i++;
                // parse intermediate chars
                int intermediateStart = i;
                while (i < buffer.length() && (buffer.charAt(i) & 0xF0) == 0x20) i++;
                // parse final byte
                if (i >= buffer.length()) return false;
                if (buffer.charAt(i) >= 0x40 && buffer.charAt(i) <= 0x7E) {
                    processCSI(
                            buffer.substring(paramStart, intermediateStart),
                            buffer.substring(intermediateStart, i),
                            buffer.charAt(i));
                    return true;
                }
            } else if (buffer.charAt(1) == 'O') {
                
            }

        }
        return processText(buffer);
    }
    
    protected void processCSI(String parameters, String intermediate, char f) {
        if (f >= 'A' && f <= 'D') {
            if (parameters.length() == 0 && intermediate.length() == 0)
                processArrow(f);
        }
    }
    
    protected boolean processText(String buffer) {
        
    }
    
    @Override
    public String readLine() throws IOException {
        while (true) {
            // Read character and aggregate to buffer, call everything
            readNextChar();
        }
    }
    
    
}
