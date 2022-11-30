/*
 This file is part of the BlueJ program. 
 Copyright (C) 2021  Michael Kolling and John Rosenberg

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

import bluej.editor.base.TextLine.StyledSegment;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class stores information about a line of content in a terminal text pane;
 * its text content and its styling information.  It is mutable; it allows appending new content.
 * This should not be done while iterating over the ContentLine.
 */
class ContentLine implements Iterable<StyledSegment>
{
    // The core data: an ordered list of text+styles.  Each item itself is immutable, but the list is mutable
    private final ArrayList<StyledSegment> segments;
    // A cached copy of the joined text of all segments above.
    private String cachedText;

    // Makes an instance with the given initial content (will be copied)
    public ContentLine(List<StyledSegment> segments)
    {
        this.segments = new ArrayList<>(segments);
        this.cachedText = segments.stream().map(s -> s.getText()).collect(Collectors.joining());
    }

    /**
     * Appends the given styled segment to the end of this content line.
     */
    public void append(StyledSegment segment)
    {
        this.segments.add(segment);
        this.cachedText += segment.getText();
    }

    /**
     * An iterator over the segments, that merges any adjacent segments with identical styles.
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public Iterator<StyledSegment> iterator()
    {
        // If no content, we need to present at least one item for the display to be correct:
        if (segments.isEmpty())
            return Collections.singletonList(new StyledSegment(Collections.emptyList(), "")).iterator();
        return StyledSegment.mergeAdjacentIdentical(segments).iterator();
    }

    /**
     * Gets the text content of this line (without any trailing newline)
     */
    public String getText()
    {
        return cachedText;
    }

    /**
     * Given a column index of a character (i.e. not a caret position between characters, but an
     * actual character index), gets the custom data from the StyledSegment at that position.
     * 
     * @param column The column of interest, 0 = first character in the line
     * @return Custom style data (may be null if null in the StyledSegment) or null if the position is invalid.
     */
    public Object getCustomStyleDataAtColumn(int column)
    {
        if (column < 0)
            return null;
        int toSkip = column;
        for (StyledSegment segment : segments)
        {
            int segmentLength = segment.getText().length();
            if (toSkip < segmentLength)
                return segment.getCustomData();
            toSkip -= segmentLength;
        }
        return null;
    }
}
