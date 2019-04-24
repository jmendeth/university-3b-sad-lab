package p1;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * This class keeps the state of a multi-line editor with maximum line width.
 *
 * Editor lines are made of glyphs (Strings). No line can have
 * more than `width` glyphs, and no more than `height` lines can be
 * introduced. There are functions to:
 *
 *  - Move the caret (cursor) to an arbitrary position in the text, if possible.
 *  - Advance or recede the cursor, if possible.
 *  - Introduce a glyph at the caret position (if possible).
 *  - Introduce a linebreak (if possible).
 *  - Remove the linebreak/glyph before the caret (if possible).
 *  - Delete the linebreak/glyph after the caret (if possible).
 *  - Switch between 'insert' or 'replace' mode for subsequent glyphs.
 *
 * @author Alba Mendez
 */
public class Line extends Observable {

    protected final int width;
    protected final int height;

    /** Construct an empty editor, with the specified width and height **/
    public Line(int width, int height) {
        this.width = width;
        this.height = height;
        reset();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


    /**
     * Editor state.
     */

    /** List of lines, each line is a list of glyphs (Strings) **/
    protected final List<List<String>> lines = new ArrayList<>();

    /** Caret position **/
    protected int row;
    protected int column;

    /** Insert mode (true -> insert, false -> replace) **/
    protected boolean insertMode;

    public List<List<String>> getLines() {
        return lines;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public boolean getInsertMode() {
        return insertMode;
    }


    /**
     * State manipulation functions.
     */

    /** Reset the editor to an empty one in insert mode **/
    public final void reset() {
        lines.clear();
        lines.add(new ArrayList<String>());
        row = column = 0;
        insertMode = true;
    }

    /** Convenience method to get the contents of the editor as a joined string **/
    public String getContents() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0)
                result = result.append("\n");
            for (String glyph : lines.get(i))
                result = result.append(glyph);
        }
        return result.toString();
    }

    /** Introduce a glyph, if possible, at the current caret position **/
    public boolean enterGlyph(String glyph) {
        if (insertMode || column >= lines.get(row).size()) {
            if (lines.get(row).size() >= width)
                return false;
            lines.get(row).add(column, glyph);
        } else {
            lines.get(row).set(column, glyph);
        }
        column++;
        setChanged();
        return true;
    }

    /** Introduce a linebreak, if possible **/
    public boolean enterLine() {
        if (lines.size() >= height) return false;

        List<String> line = lines.get(row);
        lines.add(row + 1, new ArrayList<>(line.subList(column, line.size())));
        while (line.size() > column) line.remove(line.size() - 1);

        row++;
        column = 0;
        setChanged();
        return true;
    }

    /** Move caret to the specified position, or the closest valid one **/
    public boolean moveCaret(int row, int column) {
        // First ensure row and column are in bounds
        if (row < 0) {
            row = 0;
            column = 0;
        } else if (row >= lines.size()) {
            row = lines.size() - 1;
            column = lines.get(row).size();
        } else {
            // Then, ensure column is in bounds
            if (column < 0) {
                column = 0;
            } else if (column > lines.get(row).size()) {
                column = lines.get(row).size();
            }
        }
        // Move the caret if needed
        if (this.row == row && this.column == column) return false;
        this.row = row;
        this.column = column;
        setChanged();
        return true;
    }

    /** Advance (or recede if negative) the caret by the number of passed positions **/
    public boolean advanceCaret(int positions) {
        int row = this.row;
        int column = this.column;
        if (positions < 0) {
            for (; positions != 0; positions++) {
                // Recede cursor
                if (column > 0) {
                    column--;
                } else {
                    if (row <= 0) break;
                    row--;
                    column = lines.get(row).size();
                }
            }
        } else {
            for (; positions != 0; positions--) {
                // Advance cursor
                if (column < lines.get(row).size()) {
                    column++;
                } else {
                    if (row >= lines.size() - 1) break;
                    row++;
                    column = 0;
                }
            }
        }
        // Move the caret if needed
        if (this.row == row && this.column == column) return false;
        this.row = row;
        this.column = column;
        setChanged();
        return true;
    }

    /** Remove the linebreak/glyph before the caret, if possible **/
    public boolean backspace() {
        if (column > 0) {
            column--;
            this.lines.get(row).remove(column);
        } else {
            if (row <= 0 || this.lines.get(row-1).size() + this.lines.get(row).size() > width)
                return false;
            row--;
            column = this.lines.get(row).size();
            this.lines.get(row).addAll(this.lines.get(row+1));
            this.lines.remove(row+1);
        }
        setChanged();
        return true;
    }

    /** Delete the linebreak/glyph after the caret, if possible **/
    public boolean delete() {
        if (column < this.lines.get(row).size()) {
            this.lines.get(row).remove(column);
        } else {
            if (row >= this.lines.size() - 1 || this.lines.get(row).size() + this.lines.get(row+1).size() > width)
                return false;
            this.lines.get(row).addAll(this.lines.get(row+1));
            this.lines.remove(row+1);
        }
        setChanged();
        return true;
    }

    /** Set the insert/replace mode **/
    public void setInsertMode(boolean insertMode) {
        this.insertMode = insertMode;
        setChanged();
    }

}
