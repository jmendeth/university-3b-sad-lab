package p1;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * This class keeps track of rendering an editor state ({@link Line} class)
 * into a terminal that understands ANSI codes.
 *
 * @author Alba Mendez
 */
public class Console implements Observer {

    private final Writer output;
    private final Line editor;
    private final int startRow;
    private final int startColumn;

    /** Constructs a new renderer for the specified terminal (output) and line editor */
    public Console(Writer output, Line editor, int startRow, int startColumn) {
        this.output = output;
        this.editor = editor;
        this.startRow = startRow;
        this.startColumn = startColumn;
    }

    public Writer getOutput() {
        return output;
    }

    public Line getEditor() {
        return editor;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartColumn() {
        return startColumn;
    }


    /**
     * Initialization and update logic.
     */

    @Override
    public void update(Observable obs, Object event) {
        if (obs != editor) return;
        
        draw(); // TODO
    }

    /** Draw the editor contents from scratch at its bounds in the screen */
    public void draw() {
        StringBuilder output = new StringBuilder();
        List<List<String>> lines = editor.getLines();
        for (int row = 0; row < editor.getHeight(); row++) {
            output.append(moveCursor(row, 0));
            output.append(eraseColumns(editor.getWidth()));
            if (row >= lines.size()) continue;
            List<String> line = lines.get(row);
            for (int column = 0; column < line.size(); column++) {
                if (column > 0) output.append(moveCursor(row, column));
                output.append(line.get(column));
            }
        }
        output.append(moveCursor(editor.getRow(), editor.getColumn()));
        writeOutput(output.toString());
    }

    /** (Re)position the cursor at the editor caret */
    public void refreshCursor() {
        writeOutput(moveCursor(editor.getRow(), editor.getColumn()));
    }


    /**
     * Terminal manipulation functions.
     */

    public String moveCursor(int row, int column) {
        return String.format("\u001b[%d;%dH", startRow + row, startColumn + column);
    }

    protected String insertColumns(int n) {
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

    protected String eraseColumns(int n) {
        if (n == 1) return "\u001b[X";
        return String.format("\u001b[%dX", n);
    }

    protected void writeOutput(String out) {
        try {
            output.write(out);
            output.flush();
        } catch (IOException ex) {
            throw new RuntimeException("IOException while flushing output:", ex);
        }
    }

    public void enableMouse() {
        // Enable mousepress-only reporting (9) with extension 1006.
        // Extension 1015 is used as a fallback only.
        writeOutput("\u001b[?9h" + "\u001b[?1015h" + "\u001b[?1006h");
    }

    public void disableMouse() {
        writeOutput("\u001b[?9l");
    }

}
