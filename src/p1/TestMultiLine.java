package p1;

import java.io.IOException;
import java.io.InputStreamReader;
import p1.EditableBufferedReader.Coordinates;

/**
 * Class to test multi-line input.
 * @author Alba Mendez
 */
public class TestMultiLine {

    public static void main(String[] args) {
        EditableBufferedReader in = new EditableBufferedReader(
                new InputStreamReader(System.in));
        Coordinates bounds, cursor;
        try {
            System.out.println("Enter text, submit with Alt + ENTER:");

            // Get window size
            EditableBufferedReader.setRaw();
            try {
                bounds = in.queryWindowSize();
            } finally {
                EditableBufferedReader.unsetRaw();
            }
            int width = bounds.column - 6, height = 5;

            // Print the editor's decoration
            System.out.print("\n  ┌" + repeat("─", width) + "┐\n"
                + repeat("  │" + repeat(" ", width) + "│\n", height)
                + "  └" + repeat("─", width) + "┘");

            // Get cursor position (this flushes the output)
            EditableBufferedReader.setRaw();
            try {
                cursor = in.queryCursor();
            } finally {
                EditableBufferedReader.unsetRaw();
            }

            // Start the editor
            in.init(width, height, 3 + 1, cursor.row - height, false);
            in.readLine();

            // Clear rest of screen, 'cut' decoration
            System.out.println("\u001b[J" + "  └" + repeat("─", width) + "┘");
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String repeat(String s, int times) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < times; i++)
            r.append(s);
        return r.toString();
    }

}
