package bluej.utility;

import java.util.*;
import java.io.*;

/**
 * BlueJFileReader - a (static) class grouping all functions to read and write
 * BlueJ specific files.
 * 
 * The BlueJ files are help files (used for compiler help, exception help,
 * editor help), dialogue files (for message and error dialogues) and templates
 * (for class skeletons).
 * 
 * Help texts and dialogue texts are handled through the "readHelpText" method.
 * The files consist of text IDs (a short string) followed by the full text.
 * 
 * Class skeletons (handled by the "translateFile" method) are text files with
 * place holders (variables) in them that will be replaced using a dictionary.
 * 
 * <br>
 * 
 * The files are expected to be in ISO 8859-1 character encoding. For characters
 * that cannot be directly represented in this encoding, <a
 * href="http://java.sun.com/docs/books/jls/html/3.doc.html#100850">Unicode
 * escapes </a> are used; however, only a single 'u' character is allowed in an
 * escape sequence. The native2ascii tool can be used to convert property files
 * to and from other character encodings.
 * 
 * @author Michael Kolling
 * @version $Id: BlueJFileReader.java 2831 2004-08-04 10:50:20Z polle $
 */
public class BlueJFileReader
{
    /**
     * Read a help text out of a help file.
     *
     * Help files are named <language>/<baseFileName>.help (for example
     * "english/moe.help"). Help texts inside the file are identified by
     * a help ID (a string).
     *
     * The files are expected to be in ISO 8859-1 character encoding.
     *
     * @param baseFileName  Base name of the help file
     * @param textID        ID string for the help message
     * @param exactMatch    If true, match ID string exactly. If false,
     *                      wildcards are used.
     *
     * @return              The help text or null.
     */
    public static String readHelpText(File file, String textID,
                                      boolean exactMatch)
    {
        BufferedReader in = null;
        boolean found = false;

        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "8859_1"));
            String msg;
            String line;
            String helptext = "";
            boolean match;

            while ((msg = in.readLine()) != null) {
                msg = msg.trim();
                if(exactMatch)
                    match = msg.equals(textID);
                else
                    match = helpTextMatch(textID, msg);

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
                    while ((line != null) && (line.length() > 0))
                        line = in.readLine();
                }
            }
        }
        catch(IOException e) {
            DialogManager.showErrorWithText(null, "cannot-read-help",
                                            file.toString());
        }
        finally {
            if(in != null) {
                try {
                    in.close();
                }
                catch(Exception e) {}
            }
        }
        return null;	// not found
    }

    /**
     * Helper function for "readHelpText" (above). Used to determine
     * help ID matches when accepting wildcards in the message. Recognised
     * wildcards are asterisks (*) at the start or end of the pattern.
     */
    private static boolean helpTextMatch(String message, String pattern)
    {
        if(pattern.length() == 0)
            return false;
        if(pattern.charAt(pattern.length()-1) == '*') {
            if(pattern.charAt(0) == '*') {  // * at both ends
                pattern = pattern.substring(1, pattern.length()-3);
                return (message.indexOf(pattern) > -1);
            }
            else  // * at end
                return message.startsWith(
                     pattern.substring(0, pattern.length()-2));
        }
        else if(pattern.charAt(0) == '*')
            return message.endsWith(pattern.substring(1));
        else
            return pattern.equals(message);
    }

    public static void translateFile(String template, String dest,
                                     Dictionary translations)
        throws IOException
    {
        translateFile(new File(template), new File(dest), translations);
    }

    /**
     * Copy a file while replacing special keywords
     * within the file by definitions.
     *
     * Keywords are marked with a dollar
     * sign and a name ($KEYWORD). 'translations' contains definitions
     * to be used as replacements.
     * This is used to create shell files from the shell file template.
     */
    public static void translateFile(File template, File dest,
                                     Dictionary translations)
        throws IOException
    {
        FileReader in = null;
        FileWriter out = null;

        try {
            in = new FileReader(template);
            out = new FileWriter(dest);

            for(int c; (c = in.read()) != -1; ) {
                if(c == '$') {
                    StringBuffer buf = new StringBuffer();
                    while(((c = in.read()) != -1) && Character.isLetter((char)c))
                        buf.append((char)c);

                    String key = buf.toString();
                    String value = (String)translations.get(key);

                    if(value == null) {
                        out.write('$');
                        value = key;
                    }

                    out.write(value);
                    if(c != -1)
                        out.write(c);
                }
                else
                    out.write(c);
            }

            in.close();
            out.close();
        } catch(IOException e) {
            if(in != null)
                in.close();
            if(out != null) {
                out.close();
                // File destFile = new File(dest);
                // destFile.delete();
            }

            throw e;
        }
    }
    
    
    /**
     * Convert Unicode based characters in \udddd format
     */
    private static String convert(String theString) {
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
