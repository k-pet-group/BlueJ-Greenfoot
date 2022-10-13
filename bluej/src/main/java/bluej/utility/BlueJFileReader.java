/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2014,2015,2016,2018  Michael Kolling and John Rosenberg
 
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
package bluej.utility;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;

import bluej.Config;

/**
 * BlueJFileReader - a (static) class grouping all functions to read and write
 * BlueJ specific files.
 * <p>
 * The BlueJ files are help files (used for compiler help, exception help,
 * editor help), dialogue files (for message and error dialogues) and templates
 * (for class skeletons).
 * <p>
 * Help texts and dialogue texts are handled through the "readHelpText" method.
 * The files consist of text IDs (a short string) followed by the full text.
 * <p>
 * Class skeletons (handled by the "translateFile" method) are text files with
 * place holders (variables) in them that will be replaced using a dictionary.
 * 
 * @author Michael Kolling
 */
public class BlueJFileReader
{
    private static final int tabSize = Config.getPropInteger("bluej.editor.tabsize", 4);
    private static final String spaces = "                                        ";
    private static final char TAB_CHAR = '\t';
    
    /**
     * Read a help text out of a help file.
     * <p>
     * Help files are named (language)/(baseFileName).help (for example
     * "english/moe.help"). Help texts inside the file are identified by
     * a help ID (a string).
     * <p>
     * The files are expected to be in ISO 8859-1 character encoding, with "slash-u-XXXX" unicode
     * escape sequences.
     *
     * @param file          The help file
     * @param textID        ID string for the help message
     * @param exactMatch    If true, match ID string exactly. If false,
     *                      wildcards are used.
     *
     * @return              The help text or null.
     */
    public static String readHelpText(File file, String textID, boolean exactMatch)
    {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "8859_1"));
            String msg;
            String line;
            String helptext = "";
            boolean match;

            while ((msg = in.readLine()) != null) {
                msg = msg.trim();
                if(exactMatch) {
                    match = msg.equals(textID);
                }
                else {
                    match = helpTextMatch(textID, msg);
                }

                if(match) {
                    // found it - read help text
                    line = in.readLine();
                    if((line != null) && (line.length() > 0)) {
                        helptext = line;
                        line = in.readLine();
                    }
                    while ((line != null) && (line.length() > 0)) {
                        helptext += "\n" + line;
                        line = in.readLine();
                    }
                    return convert(helptext);
                }
                else {
                    // skip help text
                    line = in.readLine();
                    while ((line != null) && (line.length() > 0)) {
                        line = in.readLine();
                    }
                }
            }
        }
        catch(IOException e) {
            // This use to show a dialog, but showing a dialog involves reading the
            // help file... infinite recursion! just ignore and return null
        }
        finally {
            if(in != null) {
                try {
                    in.close();
                }
                catch(Exception e) {}
            }
        }
        return null; // not found
    }

    /**
     * Helper function for "readHelpText" (above). Used to determine
     * help ID matches when accepting wildcards in the message. Recognised
     * wildcards are asterisks (*) at the start or end of the pattern.
     */
    private static boolean helpTextMatch(String message, String pattern)
    {
        if(pattern.length() == 0) {
            return false;
        }
        
        if(pattern.charAt(pattern.length()-1) == '*') {
            if(pattern.charAt(0) == '*') {  // * at both ends
                pattern = pattern.substring(1, pattern.length()-3);
                return (message.indexOf(pattern) > -1);
            }
            else { // * at end
                return message.startsWith(
                     pattern.substring(0, pattern.length()-2));
            }
        }
        else if(pattern.charAt(0) == '*') {
            return message.endsWith(pattern.substring(1));
        }
        else {
            return pattern.equals(message);
        }
    }

    /**
     * Copy a file while replacing special keywords within the file by
     * definitions.
     * <p>
     * Keywords are marked with a dollar sign and a name ($KEYWORD).
     * 'translations' contains definitions to be used as replacements. This is
     * used to create shell files from the shell file template.
     * 
     * @param templateCharset Charset that should be used to read the template file.
     */
    public static void translateFile(File template, File dest,
                                     Dictionary<String,String> translations,
                                     Charset templateCharset, Charset outputCharset)
        throws IOException
    {
        translateFile(template, dest, translations, true, templateCharset, outputCharset);
    }

    /**
     * Copy a file while replacing special keywords within the file by definitions.
     * <p>
     * Keywords are marked with a dollar sign and a name ($KEYWORD). 'translations' contains definitions
     * to be used as replacements. This is used to create shell files from the shell file template.
     * 
     * @param templateCharset Charset that should be used to read the template file.
     * @param outputCharset  Charset that should be used to write the output file.
     */
    private static void translateFile(File template, File dest,
            Dictionary<String,String> translations, boolean replaceTabs,
            Charset templateCharset, Charset outputCharset)
        throws IOException
    {
        InputStreamReader in = null;
        OutputStreamWriter out = null;
        String newline = System.getProperty("line.separator");
      
        try {
            in = new InputStreamReader(new FileInputStream(template), templateCharset);
            out = new OutputStreamWriter(new FileOutputStream(dest), outputCharset);
            
            for(int c; (c = in.read()) != -1; ) {
                if(c == '$') {
                    StringBuffer buf = new StringBuffer();
                    while(((c = in.read()) != -1) && Character.isLetter((char)c)) {
                        buf.append((char)c);
                    }

                    String key = buf.toString();
                    if (key.length() != 0) {
                        String value = translations.get(key);

                        if(value == null) {
                            out.write('$');
                            value = key;
                        }

                        // If there are tabs, replace
                        if(replaceTabs && value.indexOf(TAB_CHAR) != -1) {
                            value = convertTabsToSpaces(value);
                        }

                        out.write(value);
                    }
                    else if (c != '$') {
                        // let '$$' be an escape for single $
                        out.write('$');
                    }
                    
                    if(c != -1) {
                        out.write(c);
                    }
                }
                else if(replaceTabs && c == TAB_CHAR) {
                    out.write(tabAsSpace());
                }
                else if(c == '\r') {
                    // The template is encoded with CR+LF line endings
                    int nc = in.read();
                    if (nc == '\n') {
                        out.write(newline);
                    }
                    else {
                        out.write(c);
                        if (nc != -1) {
                            out.write(nc);
                        }
                    }
                }
                else {
                    out.write(c);
                }
            }

            in.close();
            out.close();
        }
        catch(IOException e) {
            if(in != null) {
                in.close();
            }
            if(out != null) {
                out.close();
            }
            throw e;
        }
    }
    
    /**
     * Copy a file while replacing a string with another within the file.
     * <p>
     * 'translations' contains definitions to be used as replacements.
     * This is used to create a copy of a class with different names.
     */
    public static void duplicateFile(File original, File destination, Dictionary<String,String> translations)
        throws IOException
    {
        BufferedReader in = null;
        BufferedWriter out = null;
      
        try {
            in = new BufferedReader(new FileReader(original));
            out = new BufferedWriter(new FileWriter(destination));

            String line;
            while ((line = in.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, " (){}.,\"><", true);
                while (st.hasMoreTokens()) {
                    String key = st.nextToken();
                    String value = translations.get(key);
                    if(value == null) {
                        value = key;
                    }
                    out.write(value);
                }
                out.newLine();
            }
            
            in.close();
            out.close();
        }
        catch(IOException e) {
            if(in != null) {
                in.close();
            }
            if(out != null) {
                out.close();
            }
            throw e;
        }
    }
    
    /**
     * Convert tab chars to the applicable number of spaces
     */
    private static String convertTabsToSpaces(String tabString)
    {
       return tabString.replaceAll("\t", tabAsSpace());
    }
    
    /**
     * return a String representing the number of spaces to be used in replacing a tab character
     */
    private static String tabAsSpace()
    {
        return spaces.substring(0, tabSize);
    }
    
    /**
     * Converts encoded &#92;uxxxx to unicode chars <br> 
     * 
     * Copied large chunks from java.util.Properties#loadConvert
     */
    private static String convert(String theString)
    {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
        
        for(int x=0; x<len; ) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if(aChar == 'u') {
                    // Read the xxxx
                    int value=0;
                    for (int i=0; i<4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0': case '1': case '2': case '3': case '4':
                            case '5': case '6': case '7': case '8': case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a': case 'b': case 'c':
                            case 'd': case 'e': case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A': case 'B': case 'C':
                            case 'D': case 'E': case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                //error in encoding - what to do? - nothing
                        }
                    }
                    outBuffer.append((char)value);
                } else {
                    //ignore other special characters
                    outBuffer.append('\\');
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }
}
