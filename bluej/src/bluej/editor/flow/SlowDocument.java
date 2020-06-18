/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020  Michael Kolling and John Rosenberg

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

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A simplistic implementation of the Document interface.  Useful for sanity checking
 * during testing.  Do not use for real!  Use HoleDocument instead.
 */
public class SlowDocument implements Document
{
    private String content = "";
    // We keep a strong reference, since this class is just for testing:
    private final List<TrackedPosition> trackedPositions = new ArrayList<>();
    private final List<DocumentListener> listeners = new ArrayList<>();

    @Override
    public void replaceText(int startCharIncl, int endCharExcl, String text)
    {
        int linesRemoved = getLineFromPosition(endCharExcl) - getLineFromPosition(startCharIncl);
        String prev = content.substring(startCharIncl, endCharExcl);
        content = content.substring(0, startCharIncl) + text + content.substring(endCharExcl);
        
        // Update tracked positions:
        for (TrackedPosition trackedPosition : trackedPositions)
        {
            trackedPosition.updateTrackedPosition(startCharIncl, endCharExcl, text.length());
        }

        int linesAdded = getLineFromPosition(startCharIncl + text.length()) - getLineFromPosition(startCharIncl);
        for (DocumentListener listener : listeners)
        {
            listener.textReplaced(startCharIncl, prev, text, linesRemoved, linesAdded);
        }
    }

    @Override
    public String getFullContent()
    {
        return content;
    }

    @Override
    public int getLength()
    {
        return content.length();
    }

    @Override
    public int getLineFromPosition(int position)
    {
        int line = 0;
        for (int i = 0; i < position; i++)
        {
            if (content.charAt(i) == '\n')
            {
                line++;
            }
        }
        return line;
    }

    @Override
    public int getColumnFromPosition(int position)
    {
        int column = 0;
        for (int i = 0; i < position; i++)
        {
            if (content.charAt(i) == '\n')
            {
                column = 0;
            }
            else
            {
                column++;
            }
        }
        return column;
    }

    @Override
    public int getLineStart(int lineNumber)
    {
        int line = 0;
        for (int i = 0; i < content.length(); i++)
        {
            if (line == lineNumber)
                return i;
            if (content.charAt(i) == '\n')
            {
                line++;
            }
        }
        return content.length();
    }

    @Override
    public int getLineEnd(int lineNumber)
    {
        int start = getLineStart(lineNumber);
        int nextNewLine = content.indexOf('\n', start);
        return nextNewLine == -1 ? getLength() : nextNewLine;
    }

    @Override
    public List<CharSequence> getLines()
    {
        return Arrays.asList(content.split("\n", -1));
    }
    
    @Override
    public TrackedPosition trackPosition(int position, Bias bias)
    {
        TrackedPosition trackedPosition = new TrackedPosition(this, position, bias);
        trackedPositions.add(trackedPosition);
        return trackedPosition;
    }

    @Override
    public void addListener(boolean atStart, DocumentListener listener)
    {
        if (atStart)
            listeners.add(0, listener);
        else
            listeners.add(listener);
    }

    @Override
    public String getContent(int startCharIncl, int endCharExcl)
    {
        return content.substring(startCharIncl, endCharExcl);
    }
    
    @Override
    public Reader makeReader(int startPos, int endPos)
    {
        return new StringReader(getContent(startPos, endPos));
    }
}
