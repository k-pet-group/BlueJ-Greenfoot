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

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class HoleDocument implements Document
{
    // How much extra should we grow the array by when needed?
    private static final int GROWTH_MARGIN = 256;
    // Array which always contains, in order:
    //  content, up to holeStart (exclusive) -- may be empty if holeStart == 0
    //  a hole in the middle, from holeStart (inclusive) to holeEnd (exclusive) -- may be empty if holeStart == holeEnd
    //  more content, from holeEnd (inclusive) to end of array -- may be empty if holdEnd == content.length
    private char[] content;
    private int holeStart; // Index of first character in the hole.
    private int holeEnd; // Index of first character in array after the hole
    
    // Not including the first line, which always begins at zero.  So lineStartPositions.get(0)
    // is the beginning of the second line...
    private final ArrayList<TrackedPosition> lineStartPositions = new ArrayList<>();

    /**
     * We need to know all the positions so we can update them all.  But we don't want
     * to retain them and cause a memory leak.  Rather than having a deregistration system,
     * we just keep weak references and thus let them fall out of memory once the caller
     * of trackPosition no longer keeps track of them.
     */
    private final ArrayList<WeakReference<TrackedPosition>> trackedPositions = new ArrayList<>();
    
    public HoleDocument()
    {
        content = new char[128];
        holeStart = 0;
        holeEnd = content.length;
    }

    @Override
    public void replaceText(int startCharIncl, int endCharExcl, String text)
    {
        // Get rid of any new line positions that were in removed region:
        int indexToInsertNewNewlines = 0;
        for (Iterator<TrackedPosition> iterator = lineStartPositions.iterator(); iterator.hasNext(); )
        {
            TrackedPosition lineStartPosition = iterator.next();
            if (lineStartPosition.position <= startCharIncl)
            {
                indexToInsertNewNewlines++;
            }
            else if (lineStartPosition.position > startCharIncl && lineStartPosition.position <= endCharExcl)
            {
                iterator.remove();
            }
            
        }
        
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

        for (Iterator<WeakReference<TrackedPosition>> iterator = trackedPositions.iterator(); iterator.hasNext(); )
        {
            WeakReference<TrackedPosition> trackedPositionRef = iterator.next();
            TrackedPosition trackedPosition = trackedPositionRef.get();
            if (trackedPosition == null)
            {
                iterator.remove();
            }
            else
            {
                trackedPosition.updateTrackedPosition(startCharIncl, endCharExcl, text.length());
            }
        }
        
        // Now add new positions for the added newlines:
        for (int i = 0; i < text.length(); i++)
        {
            if (text.charAt(i) == '\n')
            {
                lineStartPositions.add(indexToInsertNewNewlines, trackPosition(i + 1 + startCharIncl, Bias.BACK));
                indexToInsertNewNewlines += 1;
            }
        }
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

    @Override
    public int getLineFromPosition(int position)
    {
        int index = Collections.binarySearch(getLineStartPositions(), position);
        if (index >= 0)
        {
            // Line begins there exactly.  Answer is that index + 1, for the initial empty line:
            return index + 1;
        }
        else
        {
            return -1 - index;
        }
    }

    // A read-only list of the current line start positions in the document
    private List<Integer> getLineStartPositions()
    {
        // No need to copy when we are read-only, just make a dummy list object: 
        return new AbstractList<Integer>() {

            @Override
            public int size()
            {
                return lineStartPositions.size();
            }

            @Override
            public Integer get(int index)
            {
                return lineStartPositions.get(index).position;
            }
        };
    }

    @Override
    public int getColumnFromPosition(int position)
    {
        int lineStartIndex = getLineFromPosition(position) - 1;
        if (lineStartIndex < 0)
        {
            // First line:
            return position;
        }
        else
        {
            return position - lineStartPositions.get(lineStartIndex).position;
        }
    }

    @Override
    public TrackedPosition trackPosition(int position, Bias bias)
    {
        TrackedPosition trackedPosition = new TrackedPosition(this, position, bias);
        trackedPositions.add(new WeakReference<>(trackedPosition));
        return trackedPosition;
    }
    
    public Stream<CharSequence> getLines()
    {
        List<Integer> lineStarts = getLineStartPositions();
        
        return IntStream.range(0, lineStarts.size() + 1).mapToObj(lineIndex -> {
            int startChar = lineIndex == 0 ? 0 : lineStarts.get(lineIndex - 1);
            int endChar = lineIndex == lineStarts.size() ? getLength() : (lineStarts.get(lineIndex) - 1);
            return subSequence(startChar, endChar);
        });
    }

    // Only valid while document content doesn't change!
    private CharSequence subSequence(int startChar, int endChar)
    {
        return new CharSequence()
        {
            @Override
            public int length()
            {
                return endChar - startChar;
            }

            @Override
            public char charAt(int index)
            {
                if (index + startChar < holeStart)
                    return content[index + startChar];
                else
                    return content[index + startChar + holeEnd - holeStart];
            }

            @Override
            public CharSequence subSequence(int start, int end)
            {
                return HoleDocument.this.subSequence(startChar + start, startChar + end);
            }

            @Override
            public String toString()
            {
                String beforeHole = startChar < holeStart ? new String(content, startChar, Math.min(endChar, holeStart) - startChar) : "";
                String afterHole = endChar > holeStart ? new String(content, Math.max(startChar + holeEnd - holeStart, holeEnd), endChar + holeEnd - holeStart - Math.max(startChar + holeEnd - holeStart, holeEnd)) : "";
                return beforeHole + afterHole;
            }
        };
    }

    @Override
    public int getLineStart(int lineNumber)
    {
        if (lineNumber == 0)
        {
            return 0;
        }
        else
        {
            return getLineStartPositions().get(lineNumber - 1);
        }
    }
}
