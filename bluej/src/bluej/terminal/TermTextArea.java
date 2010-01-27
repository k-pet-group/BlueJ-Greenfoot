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
 * @version $Id: TermTextArea.java 7054 2010-01-27 03:58:25Z davmac $
 */
public final class TermTextArea extends JTextArea
{
    private static final int BUFFER_LINES = 48;

    private boolean unlimitedBuffer = false;

    private InputBuffer buffer;
    private Terminal terminal;

    /**
     * Create a new text area with given size.
     */
    public TermTextArea(int rows, int columns, InputBuffer buffer, Terminal terminal)
    {
        super(rows, columns);
        this.buffer = buffer;
        this.terminal = terminal;
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
        if (! terminal.checkActive()) {
            return;
        }
        
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
            
            if (result != null) {
                for (char ch : result.toCharArray()) {
                    if (buffer.putChar(ch)) {
                        terminal.writeToTerminal(String.valueOf(ch));
                    }
                }
            }
        } else {
            // if it isn't a string, let the usual paint method handle it.
            super.paste();
        }
    }
}
