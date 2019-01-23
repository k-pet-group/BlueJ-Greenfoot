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

import java.util.ArrayList;
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
        content = content.substring(0, startCharIncl) + text + content.substring(endCharExcl);
        
        // Update tracked positions:
        for (TrackedPosition trackedPosition : trackedPositions)
        {
            trackedPosition.updateTrackedPosition(startCharIncl, endCharExcl, text.length());
        }
        
        listeners.forEach(DocumentListener::documentChanged);
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
    public Stream<CharSequence> getLines()
    {
        Stream<String> streamedLines = Pattern.compile("\n").splitAsStream(content);
        if (content.endsWith("\n"))
            streamedLines = Stream.concat(streamedLines, Stream.of(""));
        return streamedLines.map(s -> s);
    }
    
    @Override
    public TrackedPosition trackPosition(int position, Bias bias)
    {
        TrackedPosition trackedPosition = new TrackedPosition(this, position, bias);
        trackedPositions.add(trackedPosition);
        return trackedPosition;
    }

    @Override
    public void addListener(DocumentListener listener)
    {
        listeners.add(listener);
    }
}
