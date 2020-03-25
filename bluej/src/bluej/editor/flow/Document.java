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

import bluej.parser.SourceLocation;

import java.io.Reader;
import java.util.List;

/**
 * Document: an interface for a document.
 *
 * A document can:
 *
 * Store lines/paragraphs of text;
 * Keep track of positions within the text that should move according to edits;
 * Map position to paragraph number / column and vice versa;
 * Allow listeners to observe, and modify, edit actions;
 * Allow assigning arbitary paragraph attributes to individual paragraphs. 
 */
public interface Document
{
    /**
     * A numeric position is strictly speaking between two characters. When we need to decide which
     * of the two the position is really "attached" to, we use a Bias. A FORWARD bias is towards the
     * next character; a BACK bias is towards the previous character.
     */
    public enum Bias { FORWARD, NONE, BACK };

    /**
     * Replace the given range of text with the new String.
     *
     * @param startCharIncl The start of the range to replace.
     * @param endCharExcl The end of the range to replace.  If equal to startCharIncl,
     *                    nothing is removed, and an insertion is performed
     * @param text  The text to insert.  If empty, nothing is inserted, and a deletion is performed.
     */
    void replaceText(int startCharIncl, int endCharExcl, String text);

    /**
     * Get the full content of the document as a String.
     */
    String getFullContent();

    /**
     * Gets the length of the document in characters.
     */
    int getLength();

    /**
     * Given a character position in the document, get the (zero-based) line number of that position.
     * @param position The character position within the entire document.
     * @return The line number (first line is 0)
     */
    int getLineFromPosition(int position);

    /**
     * Given a character position in the document, get the (zero-based) column number of that position.
     * @param position The character position within the entire document.
     * @return The column number within that position's line (first position in column is 0)
     */
    int getColumnFromPosition(int position);
    
    default int getPosition(SourceLocation sourceLocation)
    {
        int lineStart = getLineStart(sourceLocation.getLine() - 1);
        int lineEnd = getLineEnd(sourceLocation.getLine() - 1);
        return Math.max(lineStart, Math.min(lineEnd, lineStart + sourceLocation.getColumn() - 1));
    }
    
    default SourceLocation makeSourceLocation(int position)
    {
        return new SourceLocation(getLineFromPosition(position) + 1, getColumnFromPosition(position) + 1);
    }

    /**
     * Given a line index (first line is zero), get the character offset within
     * the whole document of the start of that line.
     * @param lineNumber The line number (zero-based)
     * @return The offset within the document of that line's start (the first character after the newline)
     */
    int getLineStart(int lineNumber);

    /**
     * Given a line index (first line is zero), get the character offset within
     * the whole document of the end of that line.  The end of the line is the index of the
     * terminating character, or the document length if this is the last line.  Thus, calling
     * getContext(getLineStart(n), getLineEnd(n)) will get you the content of the line minus
     * the trailing newline (if any).
     * 
     * @param lineNumber The line number (zero-based)
     * @return The offset within the document of that line's end (the newline which terminates the line, or the end of the document)
     */
    int getLineEnd(int lineNumber);
    
    /**
     * Gets the lines in the document.  Undefined behaviour if the document
     * is modified while references are still held to the return value.
     * The list does not support modification.
     */
    List<CharSequence> getLines();

    /**
     * Returns a tracked position based on the given character position within the document.
     * The tracked position will do its best to keep track of this conceptual position even if
     * text is added or removed before the position.
     * 
     * @param position The character position within the entire document.
     * @param bias The bias of the position; does it prefer to stick to the character in front of it
     *             or the one behind it (e.g. if a new chunk of text is inserted in this position,
     *             should we be at the beginning of the new section or the end of the new section?)
     * @return A position which will track the given position.  Note that the document keeps
     *         a weak reference in order to update the position.
     */
    TrackedPosition trackPosition(int position, Bias bias);

    /**
     * Adds a listener for changes to the document.
     * @param atStart If true, add at the start
     * @param listener The document listener
     */
    void addListener(boolean atStart, DocumentListener listener);

    /**
     * Gets an arbitrary piece of content from the document.
     * @param startCharIncl The start character index (inclusive) in the document as a whole (zero is first)
     * @param endCharExcl The end character index (exclusive) in the document as a whole
     * @return The content between those two positions.
     */
    CharSequence getContent(int startCharIncl, int endCharExcl);
    
    /**
     * Gets the number of lines in the document.
     */
    default int getLineCount()
    {
        return getLineFromPosition(getLength()) + 1;
    }

    /**
     * Gets the length of the given (zero-based) line index.
     */
    default int getLineLength(int lineIndex)
    {
        if (lineIndex == getLineCount() - 1)
            return getLength() - getLineStart(lineIndex);
        else
            return getLineStart(lineIndex + 1) - getLineStart(lineIndex);
    }

    /**
     * Get a Reader for the given sub-portion of the document.
     * 
     * @param startPos The start character position in the document (inclusive)
     * @param endPos The end character position in the document (exclusive)
     * @return A reader that will read that portion of the document.
     */
    Reader makeReader(int startPos, int endPos);
}
