/*
* SyntaxUtilities.java - Utility functions used by syntax colorizing
* Copyright (C) 1999 Slava Pestov
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
*/

package org.gjt.sp.jedit.syntax;

import javax.swing.text.*;
import java.awt.Color;

/**
* Class with several segment and bracket matching functions used by
* jEdit's syntax colorizing subsystem. It also provides a way to get
* the default color table.
*
* @author Slava Pestov
* @version $Id: SyntaxUtilities.java 2618 2004-06-17 14:03:32Z mik $
*/
public class SyntaxUtilities
{
    /**
     * Checks if a subregion of a <code>Segment</code> is equal to a
     * string.
     * @param ignoreCase True if case should be ignored, false otherwise
     * @param text The segment
     * @param offset The offset into the segment
     * @param match The string to match
     */
    public static boolean regionMatches(boolean ignoreCase, Segment text,
                                        int offset, String match)
    {
        int length = offset + match.length();
        char[] textArray = text.array;
        if(length > textArray.length)
            return false;
        for(int i = offset, j = 0; i < length; i++, j++)
            {
                char c1 = textArray[i];
                char c2 = match.charAt(j);
                if(ignoreCase)
                    {
                        c1 = Character.toUpperCase(c1);
                        c2 = Character.toUpperCase(c2);
                    }
                if(c1 != c2)
                    return false;
            }
        return true;
    }

    /**
     * Checks if a subregion of a <code>Segment</code> is equal to a
     * character array.
     * @param ignoreCase True if case should be ignored, false otherwise
     * @param text The segment
     * @param offset The offset into the segment
     * @param match The character array to match
     */
    public static boolean regionMatches(boolean ignoreCase, Segment text,
                                        int offset, char[] match)
    {
        int length = offset + match.length;
        char[] textArray = text.array;
        if(length > textArray.length)
            return false;
        for(int i = offset, j = 0; i < length; i++, j++)
            {
                char c1 = textArray[i];
                char c2 = match[j];
                if(ignoreCase)
                    {
                        c1 = Character.toUpperCase(c1);
                        c2 = Character.toUpperCase(c2);
                    }
                if(c1 != c2)
                    return false;
            }
        return true;
    }

    /**
     * Finds the previous instance of an opening bracket in the buffer.
     * The closing bracket is needed as well to handle nested brackets
     * properly.
     * @param doc The document to search in
     * @param dot The starting position
     * @param openBracket The opening bracket
     * @param closeBracket The closing bracket
     * @exception BadLocationException if `dot' is out of range
     */
    public static int locateBracketBackward(Document doc, int dot,
                                            char openBracket, char closeBracket)
         throws BadLocationException
    {
        int count;
        Element map = doc.getDefaultRootElement();

        // check current line
        int lineNo = map.getElementIndex(dot);
        Element lineElement = map.getElement(lineNo);
        int start = lineElement.getStartOffset();
        int offset = scanBackwardLine(doc.getText(start,dot - start),
                                      openBracket,closeBracket,0);
        count = -offset - 1;
        if(offset >= 0)
            return start + offset;

        // check previous lines
        for(int i = lineNo - 1; i >= 0; i--)
            {
                lineElement = map.getElement(i);
                start = lineElement.getStartOffset();
                offset = scanBackwardLine(doc.getText(start,
                                                      lineElement.getEndOffset() - start),
                                          openBracket,closeBracket,count);
                count = -offset - 1;
                if(offset >= 0)
                    return start + offset;
            }

        // not found
        return -1;
    }

    /**
     * Finds the next instance of a closing bracket in the buffer.
     * The opening bracket is needed as well to handle nested brackets
     * properly.
     * @param doc The document to search in
     * @param dot The starting position
     * @param openBracket The opening bracket
     * @param closeBracket The closing bracket
     * @exception BadLocationException if `dot' is out of range
     */
    public static int locateBracketForward(Document doc, int dot,
                                           char openBracket, char closeBracket)
         throws BadLocationException
    {
        int count;
        Element map = doc.getDefaultRootElement();

        // check current line
        int lineNo = map.getElementIndex(dot);
        Element lineElement = map.getElement(lineNo);
        int start = lineElement.getStartOffset();
        int end = lineElement.getEndOffset();
        int offset = scanForwardLine(doc.getText(dot + 1,end - (dot + 1)),
                                     openBracket,closeBracket,0);
        count = -offset - 1;
        if(offset >= 0)
            return dot + offset + 1;

        // check following lines
        for(int i = lineNo + 1; i < map.getElementCount(); i++)
            {
                lineElement = map.getElement(i);
                start = lineElement.getStartOffset();
                offset = scanForwardLine(doc.getText(start,
                                                     lineElement.getEndOffset() - start),
                                         openBracket,closeBracket,count);
                count = -offset - 1;
                if(offset >= 0)
                    return start + offset;
            }

        // not found
        return -1;
    }

    // private members
    private SyntaxUtilities() {}
    private static Color[] COLORS;

    // the return value is as follows:
    // >= 0: offset in line where bracket was found
    // < 0: -1 - count
    private static int scanBackwardLine(String line, char openBracket,
                                        char closeBracket, int count)
    {
        for(int i = line.length() - 1; i >= 0; i--)
            {
                char c = line.charAt(i);
                if(c == closeBracket)
                    count++;
                else if(c == openBracket)
                    {
                        if(--count < 0)
                            return i;
                    }
            }
        return -1 - count;
    }

    // the return value is as follows:
    // >= 0: offset in line where bracket was found
    // < 0: -1 - count
    private static int scanForwardLine(String line, char openBracket,
                                       char closeBracket, int count)
    {
        for(int i = 0; i < line.length(); i++)
            {
                char c = line.charAt(i);
                if(c == openBracket)
                    count++;
                else if(c == closeBracket)
                    {
                        if(--count < 0)
                            return i;
                    }
            }
        return -1 - count;
    }
}

/*
* ChangeLog:
* $Log$
* Revision 1.6  2004/06/17 14:03:32  mik
* next stage of text evaluator: does syntax colouring now,
can evaluate most expressions and statements
still work in progress
*
* Revision 1.5  2002/08/15 09:44:21  mik
* added new syntax colouring catergory: stand-out comment (/*#).
*
* Revision 1.4  2000/07/26 07:39:59  mik
* - implemented "Record Method calls" option in terminal
*
* Revision 1.3  2000/01/14 04:35:18  mik
*
* changed colours again
*
* Revision 1.2  2000/01/14 03:33:05  mik
* changed syntax colours
*
* Revision 1.1  2000/01/12 03:18:00  bruce
*
* Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
*
* Revision 1.5  1999/06/05 00:22:58  sp
* LGPL'd syntax package
*
* Revision 1.4  1999/04/19 05:38:20  sp
* Syntax API changes
*
* Revision 1.3  1999/04/02 02:39:46  sp
* Updated docs, console fix, getDefaultSyntaxColors() method, hypersearch update
*
* Revision 1.2  1999/03/27 02:46:17  sp
* SyntaxTextArea is now modular
*
* Revision 1.1  1999/03/13 09:11:46  sp
* Syntax code updates, code cleanups
*
*/
