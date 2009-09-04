/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.moe;

/*
 * TextUtilities.java - Utility functions used by the text area classes
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

import javax.swing.text.*;

/**
 * Class with several utility functions used by the text area component.
 * @author Slava Pestov
 * @version $Id: TextUtilities.java 6619 2009-09-04 02:33:09Z davmac $
 */
public class TextUtilities
{
    /**
     * Returns the offset of the bracket matching the one at the
     * specified offset of the document, or -1 if the bracket is
     * unmatched (or if the character is not a bracket).
     * @param doc The document
     * @param offset The offset
     * @exception BadLocationException If an out-of-bounds access
     * was attempted on the document text
     */
    public static int findMatchingBracket(Document doc, int offset)
    throws BadLocationException
    {
        if(doc.getLength() == 0) {
            return -1;
        }

        char c = doc.getText(offset, 1).charAt(0);
        char cprime; // c` - corresponding character
        boolean direction; // true = back, false = forward

        switch(c)
        {
        case '(': cprime = ')'; direction = false; break;
        case ')': cprime = '('; direction = true; break;
        case '[': cprime = ']'; direction = false; break;
        case ']': cprime = '['; direction = true; break;
        case '{': cprime = '}'; direction = false; break;
        case '}': cprime = '{'; direction = true; break;
        default: return -1;
        }

        int count = 1;
        int step;
        int texttOffset;
        int len;
        int i;
        if (direction) {
            // search backwards
            step = -1;
            texttOffset = 0;
            len = offset;
            i = len - 1;
        }
        else {
            // search forwards
            step = 1;
            texttOffset = offset + 1;
            len = doc.getLength() - texttOffset;
            i = 0;
        }
        String textt = doc.getText(texttOffset, len);

        while (len > 0) {
            char x = textt.charAt(i);
            if(x == c) {
                count++;
            }

            // If text[i] == cprime, we have found a
            // opening bracket, so we return i if
            // --count == 0
            else if(x == cprime)
            {
                if (--count == 0) {
                    return i + texttOffset;
                }
            }

            len--;
            i += step;

            if (x == '\"' || x == '\'') {
                char quoteChar = x;
                // A quoted string, need to find the matching quote before matching
                // further brackets...
                while (len > 0) {
                    x = textt.charAt(i);
                    if (x == quoteChar) {
                        // Found the matching quote, as long as it is not \-quoted.
                        if (i == 0 || textt.charAt(i - 1) != '\\') {
                            len--;
                            i += step;
                            break;
                        }
                    }
                    len--;
                    i += step;
                }
            }
        }

        // Nothing found
        return -1;
    }

    /**
     * Locates the start of the word at the specified position.
     * @param line The text
     * @param pos The position
     */
    public static int findWordStart(String line, int pos, String noWordSep)
    {
        char ch = line.charAt(pos - 1);

        if(noWordSep == null)
            noWordSep = "";
        boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
                && noWordSep.indexOf(ch) == -1);

        int wordStart = 0;
        for(int i = pos - 1; i >= 0; i--)
        {
            ch = line.charAt(i);
            if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
                    noWordSep.indexOf(ch) == -1))
            {
                wordStart = i + 1;
                break;
            }
        }

        return wordStart;
    }

    /**
     * Locates the end of the word at the specified position.
     * @param line The text
     * @param pos The position
     */
    public static int findWordEnd(String line, int pos, String noWordSep)
    {
        char ch = line.charAt(pos);

        if(noWordSep == null)
            noWordSep = "";
        boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
                && noWordSep.indexOf(ch) == -1);

        int wordEnd = line.length();
        for(int i = pos; i < line.length(); i++)
        {
            ch = line.charAt(i);
            if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
                    noWordSep.indexOf(ch) == -1))
            {
                wordEnd = i;
                break;
            }
        }
        return wordEnd;
    }
}
