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

import javafx.beans.binding.BooleanExpression;

/**
 * An interface for dealing with search results.
 */
public interface FindNavigator
{
    /**
     * Highlights all search results
     */
    public void highlightAll();

    /**
     * Selects the next search result, wrapping if necessary
     *
     * @param canBeAtCurrentPos If true, "next" result can include one beginning
     *                          at the start of the current selection (e.g. when
     *                          typing in search field).  If false, next result
     *                          must be beyond start of selection (e.g. when pressing
     *                          find next button).
     */
    public void selectNext(boolean canBeAtCurrentPos);

    /**
     * Selects the previous search result, wrapping backwards if necessary.
     */
    public void selectPrev();

    /**
     * Is this search result still valid?  Search results get invalidated
     * by modifying the document, or performing a new search.
     */
    public BooleanExpression validProperty();

    /**
     * Replaces the current selected search result with the given replacement
     * string, and returns the updated search.  THis search object will no longer
     * be valid, and you should switch to using the returned result.
     */
    public FindNavigator replaceCurrent(String replacement);

    /**
     * Replaces all search results with the given replacement string.
     * This search result will no longer be valid, but there's no point
     * searching again, as all instances will have been replaced.
     */
    public void replaceAll(String replacement);

    /**
     * Find the position of a substring in a given string, 
     * can specify direction and whether the search should ignoring case
     * Return the position of the substring or -1.
     *
     * @param  text        the full string to be searched
     * @param  sub         the substring that we're looking for
     * @param  ignoreCase  if true, case is ignored
     * @param  backwards   Description of the Parameter
     * @param  foundPos   Offset for the string search
     * @return             Description of the Return Value
     * @returns            the index of the substring, or -1 if not found
     */
    public static int findSubstring(String text, String sub, boolean ignoreCase, boolean backwards, int foundPos)
    {
        int strlen = text.length();
        int sublen = sub.length();

        if (sublen == 0) {
            return -1;
        }

        boolean found = false;
        int pos = foundPos;
        boolean itsOver = (backwards ? (pos < 0) : (pos + sublen > strlen));
        while (!found && !itsOver) {
            found = text.regionMatches(ignoreCase, pos, sub, 0, sublen);
            if (found) {
                return pos;
            }
            if (!found) {
                pos = (backwards ? pos - 1 : pos + 1);
                itsOver = (backwards ? (pos < 0) : (pos + sublen > strlen));
            }
        }
        return -1;
    }
}
