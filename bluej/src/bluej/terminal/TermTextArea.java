package bluej.terminal;

import bluej.Config;
import bluej.utility.Debug;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 ** A customised text area for use in the BlueJ text terminal.
 **
 ** @author Michael Kolling
 **/

public final class TermTextArea extends JTextArea
{
    private static final int BUFFER_LINES = 40;

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
                    int linePos = getLineStartOffset(lines-40);
                    replaceRange(null, 0, linePos);
                }
                catch(BadLocationException exc) {
                    Debug.reportError("bad location in terminal operation");
                }
            }
        }
    }
}
