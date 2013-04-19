/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg 
 
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
package bluej;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Properties;

/**
 * Handling of property value parsing - substitution of variable values etc.
 * 
 * <p>Variable substitution is performed for ${variableName} and $variableName -
 * the variable name is terminated by any non-name character, but in either case a
 * '$' escapes the next character. In the first form anything after the variable
 * name is ignored (up to the closing '}').
 * 
 * <p>The double dollar sign '$$' is an escaped '$'. If a variable name begins with
 * a dollar sign, the variable must be written as '${$$name}' where name is the part
 * of the name after the initial dollar sign.
 * 
 * <p>The following functions are also supported:
 * 
 * <ul>
 * <li>${filepath x y} - concatenates file paths x and y to form a complete path
 * <li>${fileUrl x} - returns the given file path x as a file:// URL.
 * </ul>
 * 
 * <p>Function arguments can contain variable/function substitutions.
 * 
 * @author Davin McCall
 */
public class PropParser
{
    /** The maximum depth of recursion when substituting variables */
    private final static int MAX_DEPTH = 10;
    
    /**
     * Process variable/function substitution on a property value.
     * @param value  The property value to process
     * @param subvars  The collection of variable-to-value mappings
     * @return   The value after substitution
     */
    public static String parsePropString(String value, Properties subvars)
    {
        StringBuffer outBuffer = new StringBuffer();
        parsePropString(value, outBuffer, subvars, 0);
        return outBuffer.toString();
    }
    
    private static void parsePropString(String value, StringBuffer outBuffer, Properties subvars, int depth)
    {
        if (depth > MAX_DEPTH) {
            outBuffer.append(value);
            return;
        }
        
        StringIter iter = new StringIter(value);
        
        while (iter.hasNext()) {
            char cc = iter.next();
            if (cc == '$') {
                if (iter.hasNext()) {
                    cc = iter.next();
                    if (cc == '$') {
                        // double-dollar collapses to single dollar
                        outBuffer.append('$');
                    }
                    else if (cc == '{') {
                        // variable name surrounded by curly brackets
                        processVar(iter, outBuffer, subvars, depth);
                        while (iter.hasNext()) {
                            cc = iter.next();
                            if (cc == '}') {
                                break;
                            }
                        }
                    }
                    else if (isNameChar(cc)) {
                        // a variable name on its own
                        iter.backup();
                        processVar(iter, outBuffer, subvars, depth);
                    }
                    else {
                        outBuffer.append('$');
                        outBuffer.append(cc);
                    }
                }
                else {
                    outBuffer.append('$');
                }
            }
            else {
                outBuffer.append(cc);
            }
        }
    }
    
    /**
     * Check whether the given character is likely to be part of a property
     * name. (Most punctuation marks are excluded).
     */
    private static boolean isNameChar(char cc)
    {
        if (Character.isWhitespace(cc)) {
            return false;
        }
        if (cc == '/' || cc == '\\' || cc == '{' || cc == '}' || cc == '\"'
            || cc == '$' || cc == '(' || cc == ')' || cc == ' ' || cc == ':') {
            return false;
        }
        if (cc == ',') {
            return false;
        }
        return true;
    }
    
    private static void processVar(StringIter iter, StringBuffer outBuffer, Properties subvars, int depth)
    {
        // Get the variable or function name
        StringBuffer varNameBuf = new StringBuffer();
        while (iter.hasNext()) {
            char cc = iter.next();
            if (isNameChar(cc)) {
                varNameBuf.append(cc);
            }
            else if (cc == '$' && iter.hasNext()) {
                // '$' can be used to escape non-name characters, so that they can
                // be used in a property name.
                cc = iter.next();
                varNameBuf.append(cc);
            }
            else {
                iter.backup();
                break;
            }
        }
        
        String varName = varNameBuf.toString();
        if (varName.equals("filePath")) {
            // File path function - concatenates directory names/paths to yield a path
            String arg = processStringArg(iter, subvars, depth);
            if (arg != null) {
                File f = new File(arg);
                do {
                    arg = processStringArg(iter, subvars, depth);
                    if (arg != null) {
                        f = new File(f, arg);
                    }
                } while (arg != null);
                outBuffer.append(f.getAbsolutePath());
            }
        }
        else if (varName.equals("fileUrl")) {
            // File url function - takes a file path as an argument, and converts it
            // into a URL.
            String arg = processStringArg(iter, subvars, depth);
            if (arg != null) {
                File f = new File(arg);
                try {
                    String fileUrl = f.toURI().toURL().toString();
                    outBuffer.append(fileUrl);
                }
                catch (MalformedURLException mfue) {}
            }
        }
        else {
            // regular variable
            String nval = subvars.getProperty(varName);
            if (nval != null) {
                parsePropString(nval, outBuffer, subvars, depth + 1);
            }
        }
    }
    
    /**
     * Process a string argument to a substitution function. Any initial leading whitespace
     * is skipped. 
     * 
     * String arguments can include double-quote-enclosed literal strings, as well as
     * unquoted characters, $-marked variable substitutions, and $-quoted special characters.
     * They are terminated by (unquoted) whitespace or the (unquoted) '}' character.
     * 
     * Return is null if no argument is present ('}' is first non-whitespace character).
     * 
     * @param iter
     * @return
     */
    private static String processStringArg(StringIter iter, Properties subvars, int depth)
    {
        // Skip any whitespace
        char cc;
        do {
            if (! iter.hasNext()) {
                return null;
            }
            cc = iter.next();
        } while (Character.isWhitespace(cc));
        if (cc == '}') {
            return null;
        }
        
        if (cc == '\"') {
            // string literal, quote-enclosed
            StringBuffer result = new StringBuffer();
            while (iter.hasNext()) {
                cc = iter.next();
                if (cc == '\"') {
                    break;
                }
                result.append(cc);
            }
            return result.toString();
        }
        else {
            // String literal, not quote-enclosed, may incorporate variable names.
            // Terminated by any unquoted whitespace character or '}'.
            StringBuffer outBuffer = new StringBuffer();
            iter.backup();
            
            do {
                cc = iter.next();
                if (cc == '$' && iter.hasNext()) {
                    cc = iter.next();
                    if (Character.isWhitespace(cc) || cc == '}' || cc == '\"') {
                        outBuffer.append(cc);
                    }
                    else if (cc == '{') {
                        // variable name surrounded by curly brackets
                        processVar(iter, outBuffer, subvars, depth);
                        while (iter.hasNext()) {
                            cc = iter.next();
                            if (cc == '}') {
                                break;
                            }
                        }
                    }
                    else {
                        // a variable name on its own
                        iter.backup();
                        processVar(iter, outBuffer, subvars, depth);
                    }
                }
                else {
                    if (Character.isWhitespace(cc) || cc == '}') {
                        iter.backup();
                        break;
                    }
                    else if (cc == '\"') {
                        // string literal, quote-enclosed
                        while (iter.hasNext()) {
                            cc = iter.next();
                            if (cc == '\"') {
                                break;
                            }
                            outBuffer.append(cc);
                        }
                    }
                    else {
                        outBuffer.append(cc);
                    }
                }
            }
            while (iter.hasNext());
            
            return outBuffer.toString();
        }
    }
    
    /**
     * A class for iterating through a string
     * 
     * @author Davin McCall
     */
    private static class StringIter
    {
        private String string;
        private int curpos;
        private int limit;
        
        StringIter(String string)
        {
            this.string = string;
            limit = string.length();
        }
        
        public boolean hasNext()
        {
            return curpos < limit;
        }
        
        public char next()
        {
            return string.charAt(curpos++);
        }
        
        public void backup()
        {
            curpos--;
        }        
    }
}
