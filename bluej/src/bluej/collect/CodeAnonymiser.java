/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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
package bluej.collect;

import bluej.utility.Utility;

public class CodeAnonymiser
{
    public static String anonymise(String sourceCode)
    {
        String[] lines = Utility.splitLines(sourceCode);
        String[] result = new String[lines.length];
        int i = 0;
        
        // We keep going round this loop while we're still in the header of the file
        // (i.e. while it's only import, package, and code):
        while (i < lines.length)
        {
            // Start the result accumulator as blank:
            result[i] = "";
            
            String white = "";
            String content = lines[i]; //TODO

                        
            // This loop consumes any inline/multiline comments we find, so that
            // when it exits, we will find something other than a /*:
            while (content.startsWith("/*"))
            {
                content = content.substring(2); //skip the /*
                int endPoint = content.indexOf("*/");
                if (endPoint != -1)
                {
                    //Comment ends on same line:
                    result[i] += white + "/*" + replaceWords(content.substring(0, endPoint)) + "*/";
                    content = content.substring(endPoint + 2);
                    white = ""; // we've appended whitespace, so consume it
                }
                else
                {
                    //Comment ends on different line:
                    result[i] += white + "/*" + replaceWords(content);
                    white = ""; // we've appended whitespace, so consume it
                    i += 1;
                    // Copy all the lines in the middle of the comment, or until end of file:
                    while (!lines[i].contains("*/") && i < lines.length)
                    {
                        result[i] = replaceWords(lines[i]);
                    }
                    if (i == lines.length)
                    {
                        //Comment never ended, done:
                        return recombine(result);
                    }
                    // Now it ends on current line:
                    endPoint = lines[i].indexOf("*/");
                    result[i] = replaceWords(lines[i].substring(0, endPoint)) + "*/";
                    content = lines[i].substring(endPoint + 2);
                }
            }
            
            // We append to the result, not just assign, because we may already have put an anonymised inline
            // comment onto the result
            if (content.startsWith("package") || content.startsWith("import"))
            {
                result[i] += white + content;
            }
            else if (content.startsWith("//"))
            {
                result[i] += white + "//" + replaceWords(content);
            }            
            else
            {
                result[i] += white + content;
                i += 1;
                //Whatever else is there, that's where we'll count the code as starting
                // So copy everything from hereon:
                for (;i < lines.length; i++)
                {
                    result[i] = lines[i];
                }
                return recombine(result);                          
            }

            i += 1;
        }
        return recombine(result);
    }

    private static String recombine(String[] lines)
    {
        StringBuilder s = new StringBuilder();
        for (String line : lines)
        {
            s.append(line).append("\n");
        }
        return s.toString();
    }

    private static String replaceWords(String substring)
    {
        StringBuilder s = new StringBuilder(substring.length());
        int i = 0;
        while (i < substring.length())
        {
            int codePoint = substring.codePointAt(i);
            
            if (Character.isLetterOrDigit(codePoint))
            {
                s.append("#");
            }
            else
            {
                s.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        return s.toString();
    }
}
