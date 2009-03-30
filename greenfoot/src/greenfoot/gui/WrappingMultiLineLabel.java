/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import javax.swing.JLabel;

import bluej.utility.MultiLineLabel;

/**
 * A label that wraps lines when they exceed a specified length. It wraps on
 * word boundaries only.
 * 
 * @author Poul Henriksen
 * 
 */
public class WrappingMultiLineLabel extends MultiLineLabel
{
    private int cols;

    public WrappingMultiLineLabel(String text, int numCols)
    {
        super(null);
        alignment = LEFT_ALIGNMENT;
        cols = numCols;
        addText(text);
    }

    public void setText(String text)
    {
        addText(text);
    }

    public void addText(String text)
    {
        if (text != null) {
            // get user enforced line breaks
            String strs[] = text.split("\n");

            for (int i = 0; i < strs.length; i++) {
                int lastIndex = 0;
                int index = getLineBreakPoint(strs[i], lastIndex);
                while (index > lastIndex) {
                    addLabel(strs[i].substring(lastIndex, index).trim());
                    lastIndex = index;
                    index = getLineBreakPoint(strs[i], lastIndex);
                }
            }
        }
    }

    /**
     * Gets the next point at which to break the line, assuming that there was a
     * break at lastIndex.
     * 
     * @param str The string
     * @param lastIndex Only look at the string from this index onwards.
     * @return The new index to break the line at. The end has been reached when
     *         index==lastIndex
     */
    private int getLineBreakPoint(String str, int lastIndex)
    {
        int index = str.lastIndexOf(" ", lastIndex + cols);
        // no new whitespace found.
        if (index == lastIndex || index == -1) {
            index = lastIndex + cols;
        }
        // truncate to stay within string bounds
        if (index >= str.length()) {
            index = str.length();
        }
        // if the rest of the string will fit in, just add that
        int strLength = index - lastIndex;
        int rest = str.length() - index;
        if ((strLength + rest) < cols) {
            index = str.length();
        }
        return index;
    }

    private void addLabel(String str)
    {
        if (str.equals(""))
            str = " "; // To make empty lines
        JLabel label = new JLabel(str);
        label.setAlignmentX(alignment);
        add(label);
    }
}
