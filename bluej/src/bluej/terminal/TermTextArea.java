/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import bluej.utility.Debug;

/**
 * A customised text area for use in the BlueJ text terminal.
 *
 * @author  Michael Kolling
 * @version $Id: TermTextArea.java 6164 2009-02-19 18:11:32Z polle $
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
