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

public interface DocumentListener
{
    /**
     * Called when a change is made to the document's content.  All changes are conceived as a
     * contiguous region of the document being replaced by a new String.  Deletes are replacements
     * by the empty string, inserts replace a zero-length region, and more complex changes replace
     * a non-empty region with non-empty new content.
     * 
     * @param origStartIncl The inclusive start position of the region being replaced.
     * @param replaced The string at the start position which was replaced.  If empty, this is an insertion.
     * @param replacement The string to replace the region with.  If empty, this is a deletion.
     * @param linesRemoved The number of lines (line endings, really) removed in the replaced region.
     * @param linesAdded The number of lines (line endings, really) added in the replacement text.
     */
    public void textReplaced(int origStartIncl, String replaced, String replacement, int linesRemoved, int linesAdded);
}
