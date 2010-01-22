/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.terminal;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import bluej.utility.Debug;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * A customised text area for use in the BlueJ text terminal.
 *
 * @author  Michael Kolling
 * @version $Id: TermTextArea.java 7046 2010-01-22 14:56:08Z plcs $
 */
public final class TermTextArea extends JTextArea
{
    private static final int BUFFER_LINES = 48;

    private boolean unlimitedBuffer = false;

    private static StringBuffer pasteBuffer = new StringBuffer();

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

    @Override
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

    /*
     * Overrides the default method to stop it append to the JTextArea straight
     * away, instead we add the resultant string to our pasteBuffer for use
     * elsewhere
     * @see Terminal.keyTyped(KeyEvent event)
     */
    @Override
    public void paste()
    {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            String result = null;
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException ex) {
                Debug.message(ex.getMessage());
            } catch (IOException ex) {
                Debug.message(ex.getMessage());
            }

            // replace the contents of the pasteBuffer with this
            // it will resize the buffer to be the correct size for the
            // input string
            if (result != null) {
                pasteBuffer.replace(0, pasteBuffer.length(), result);
            }
        } else {
            // if it isn't a string, let the usual paint method handle it.
            super.paste();
        }
    }

    /*
     * Returns true only if the pasteBuffer contains any characters
     */
    protected synchronized boolean pasteBufferEmpty()
    {
        return 0 == pasteBuffer.length();
    }

    /*
     * Returns a char array of the contents of the pasteBuffer and then
     * deletes the contents of the pasteBuffer.
     */
    protected synchronized char[] takePasteBuffer()
    {
        char[] temp = pasteBuffer.toString().toCharArray();
        // setLength means subsequent additions to the buffer will require
        // it to resize itself, whereas delete should not.
        // pasteBuffer.setLength(0)
        pasteBuffer.delete(0, pasteBuffer.length());
        return temp;
    }
}
