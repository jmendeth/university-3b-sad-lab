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
 *  - Move the caret (cursor) to an arbitrary position in the text.
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
    protected int caret;

    /** Insert mode (true -> insert, false -> replace) **/
    protected boolean insertMode;

    public List<List<String>> getLines() {
        return lines;
    }

    public int getCaret() {
        return caret;
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
        caret = 0;
        insertMode = true;
    }

    /** Convenience method to get the contents of the editor as a joined string **/
    public String getContents() {
        String result = "";
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0)
                result = result.concat("\n");
            for (String glyph : lines.get(i))
                result = result.concat(glyph);
        }
        return result;
    }

    /** Introduce a glyph, if possible, at the current caret position **/
    public boolean enterGlyph(String glyph) {
        
    }

    /** Introduce a linebreak, if possible **/
    public boolean enterLine() {
        
    }

    /** Move caret to the specified position **/
    public boolean setCaret(int caret) {
        
    }

    /** Remove the linebreak/glyph before the caret, if possible **/
    public boolean backspace() {
        
    }

    /** Delete the linebreak/glyph after the caret, if possible **/
    public boolean delete() {
        
    }

    /** Set the insert/replace mode **/
    public void setInsertMode(boolean insertMode) {
        this.insertMode = insertMode;
    }


    /**
     * Coordinate conversion utilities.
     */

    /** Represents a pair of (x,y) zero-based coordinates **/
    public static class Position {
        public int x;
        public int y;
        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Get the closest text position to the specified (x,y) coordinates **/
    public int positionOf(Position p) {
        
    }

    /** Get the closest text position to the specified (x,y) coordinates **/
    public int positionOf(int x, int y) {
        return positionOf(new Position(x, y));
    }

    /** Translate a text position into (x,y) coordinates **/
    public Position coordinatesOf(int p) {
        return new Position(x, y);
    }

}
