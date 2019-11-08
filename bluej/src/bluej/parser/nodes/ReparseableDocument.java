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
package bluej.parser.nodes;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.Reader;

public interface ReparseableDocument
{
    /**
     * Schedule a reparse at a certain point within the document.
     * @param pos    The position to reparse at
     * @param size   The reparse size. This is a minimum, rather than a maximum; that is,
     *               the reparse when it occurs must parse at least this much.
     */
    public void scheduleReparse(int pos, int size);

    public Element getDefaultRootElement();
    
    public int getLength();
    
    public Reader makeReader(int startPos, int endPos);

    /**
     * Access the parsed node structure of this document.
     */
    public ParsedCUNode getParser();
    
    public void flushReparseQueue();
    
    /**
     * Mark a portion of the document as having been parsed. This removes any
     * scheduled re-parses as appropriate and repaints the appropriate area.
     */
    public void markSectionParsed(int pos, int size);

    @OnThread(Tag.FXPlatform)
    public static interface Element
    {
        public Element getElement(int index);
        public int getStartOffset();
        public int getEndOffset();
        public int getElementIndex(int offset);
        public int getElementCount();
    }
}
