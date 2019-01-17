/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

public class HoleDocument implements Document
{
    private static final int GROWTH_MARGIN = 256;
    // Array which always contains, in order:
    //  content, up to holeStart (exclusive) -- may be empty if holeStart == 0
    //  a hole in the middle, from holeStart (inclusive) to holeEnd (exclusive) -- may be empty if holeStart == holeEnd
    //  more content, from holeEnd (inclusive) to end of array -- may be empty if holdEnd == content.length
    private char[] content;
    private int holeStart; // Index of first character in the hole.
    private int holeEnd; // Index of first character in array after the hole
    
    public HoleDocument()
    {
        content = new char[128];
        holeStart = 0;
        holeEnd = content.length;
    }

    @Override
    public void replaceText(int startCharIncl, int endCharExcl, String text)
    {
        // Start by moving the hole to the modification location:
        if (holeStart < startCharIncl)
        {
            // Hole is too early, shuffle content backwards
            System.arraycopy(content, holeEnd, content, holeStart, startCharIncl - holeStart);
            holeEnd += (startCharIncl - holeStart);
            holeStart = startCharIncl;
        }
        else if (holeStart > startCharIncl)
        {
            // Hole is too late, shuffle content forwards
            int amountToMove = holeStart - startCharIncl;
            System.arraycopy(content, startCharIncl, content, holeEnd - amountToMove, amountToMove);
            holeEnd -= amountToMove;
            holeStart = startCharIncl;
        }

        // Now hole is at the right position.

        // Remove existing content by deleting at end of hole
        holeEnd += (endCharExcl - startCharIncl);
        
        // Check if we have enough space for the new content:
        int additionAmount = text.length();
        if (holeEnd - holeStart < additionAmount)
        {
            // Hole not big enough, need to enlarge:
            int extraLength = additionAmount + GROWTH_MARGIN;
            char[] newContent = new char[content.length + extraLength];
            System.arraycopy(content, 0, newContent, 0, holeStart);
            System.arraycopy(content, holeEnd, newContent, holeEnd + extraLength, content.length - holeEnd);
            content = newContent;
            holeEnd += extraLength;
        }
        
        // Add new content by copying into hole
        System.arraycopy(text.toCharArray(), 0, content, holeStart, text.length());
        holeStart += text.length();
    }

    @Override
    public String getFullContent()
    {
        return new String(content, 0, holeStart) + new String(content, holeEnd, content.length - holeEnd);
    }

    @Override
    public int getLength()
    {
        return content.length - (holeEnd - holeStart);
    }
}
