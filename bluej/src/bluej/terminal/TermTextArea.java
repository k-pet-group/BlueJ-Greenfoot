package bluej.terminal;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import bluej.utility.Debug;

/**
 * A customised text area for use in the BlueJ text terminal.
 *
 * @author  Michael Kolling
 * @version $Id: TermTextArea.java 2612 2004-06-14 20:36:28Z mik $
 */
public final class TermTextArea extends JTextArea
{
    private static final int BUFFER_LINES = 48;

    private boolean unlimitedBuffer = false;

    /**
     * Create a new text area with given size.
     */
    public TermTextArea(int rows, int columns)
    {
        super(rows, columns);
    }

    public void setUnlimitedBuffering(boolean arg)
    {
        unlimitedBuffer = arg;
    }

    public void append(String s)
    {
        super.append(s);

        if(!unlimitedBuffer) {             // possibly remove top line
            int lines = getLineCount();
            if(lines > BUFFER_LINES) {
                try {
                    int linePos = getLineStartOffset(lines-BUFFER_LINES);
                    replaceRange(null, 0, linePos);
                }
                catch(BadLocationException exc) {
                    Debug.reportError("bad location in terminal operation");
                }
            }
        }
    }
}
