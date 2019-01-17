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
}
