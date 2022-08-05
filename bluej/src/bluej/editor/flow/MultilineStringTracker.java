/*
 This file is part of the BlueJ program. 
 Copyright (C) 2022  Michael Kolling and John Rosenberg

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

import bluej.extensions2.editor.DocumentListener;
import bluej.utility.javafx.FXPlatformRunnable;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A DocumentListener that tracks the positions of all triple-quotes
 * in the document as the content of the document changes.  In the case
 * of awkward quotes, say 7 quotes ("""""""), the first three count
 * as the first triple quote, then the next three, and the last one is disregardeed.
 */
public class MultilineStringTracker implements DocumentListener
{
    // Like Integer or a simple record wrapper -- but mutable because we need to efficiently move offsets
    // within a sorted set without reinserting into the set
    public static class Position implements Comparable<Position>
    {
        // The position within the document (character index, 0 = first)
        private int value;

        public Position(int value)
        {
            this.value = value;
        }

        // Gets the position with the document
        public int getValue()
        {
            return value;
        }

        @Override
        public int compareTo(Position o)
        {
            return Integer.compare(value, o.value);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position anPosition = (Position) o;
            return value == anPosition.value;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(value);
        }

        @Override
        public String toString()
        {
            return "I" + Integer.toString(value);
        }
    }
    
    // The live set of the triple quotes, held in sorted order for faster processing:
    private final TreeSet<Position> positionsOfTripleQuotes = new TreeSet<>();
    private final Document document;
    // A callback for when the content of positionsOfTripleQuotes changes:
    private final FXPlatformRunnable changeListener;

    /**
     * Constructs a MultilineStringTracker and adds it to the document as a listener.
     * Do not add this tracker to the document yourself, as it will all go wrong!
     */
    public MultilineStringTracker(Document document, FXPlatformRunnable changeListener)
    {
        this.document = document;
        this.changeListener = changeListener;
        document.addListener(false, this);
    }
    
    @Override
    public void textReplaced(int origStartIncl, String replaced, String replacement, int linesRemoved, int linesAdded)
    {
        // Need to check any neighbouring regions that are " characters
        // because they may form a """ with the edited region, before and/or after the change:
        int affectedStart = Math.max(0, origStartIncl - 1);
        while (affectedStart > 0 && document.getContent(affectedStart, affectedStart + 1).charAt(0) == '\"')
            affectedStart -= 1;
        int affectedEnd = origStartIncl + replacement.length();
        while (affectedEnd < document.getLength() && document.getContent(affectedEnd, affectedEnd + 1).charAt(0) == '\"')
            affectedEnd += 1;
        
        // Remove any positions from the affected region...
        SortedSet<Position> toRemove = positionsOfTripleQuotes.subSet(new Position(affectedStart), new Position(affectedEnd - replacement.length() + replaced.length()));
        boolean anyChanged = !toRemove.isEmpty();
        toRemove.clear();
        // ... offset any after the affected region by an offset
        // (which shouldn't affect the order in the sorted set at all) ...
        positionsOfTripleQuotes.tailSet(new Position(affectedEnd - replacement.length() + replaced.length())).forEach(i -> {
            i.value += replacement.length() - replaced.length();
        });
        // ... then scan the affected region from scratch for triple quotes:
        int quoteCount = 0;
        CharSequence subSequence = document.getContent(affectedStart, affectedEnd);
        for (int i = affectedStart; i < affectedEnd; i++)
        {
            if (subSequence.charAt(i - affectedStart) == '\"')
            {
                quoteCount += 1;
                if (quoteCount == 3)
                {
                    positionsOfTripleQuotes.add(new Position(i - 2));
                    anyChanged = true;
                    quoteCount = 0;
                }
            }
            else
            {
                quoteCount = 0;
            }
        }
        
        if (anyChanged)
        {
            changeListener.run();
        }
    }

    /**
     * Get the position of any triple quotes in the specified range.
     * The return is only valid until the document next changes.
     */
    public SortedSet<Position> getTripleQuotesBetween(int startIndexIncl, int endIndexExcl)
    {
        return positionsOfTripleQuotes.subSet(new Position(startIndexIncl), new Position(endIndexExcl));
    }

    /**
     * Is the given position a valid opening of a multiline String?
     * That is, is it followed only by whitespace on its line?
     */
    public boolean validOpeningMultiline(Position tripleQuote)
    {
        int lineEnd = document.getLineEnd(document.getLineFromPosition(tripleQuote.value));
        return document.getContent(tripleQuote.value + 3, lineEnd).chars().allMatch(Character::isWhitespace);
    }

    /**
     * Is the given position a valid closing of a multiline String?
     * That is, is it preceded only by whitespace on its line?
     */
    public boolean validClosingMultiline(Position tripleQuote)
    {
        int lineStart = document.getLineStart(document.getLineFromPosition(tripleQuote.value));
        return document.getContent(lineStart, tripleQuote.value).chars().allMatch(Character::isWhitespace);
    }
}
