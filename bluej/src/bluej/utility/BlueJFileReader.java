package bluej.utility;

import java.util.Dictionary;
import java.io.*;

/**
 * BlueJFileReader - a (static) class grouping all functions to read and
 * write BlueJ specific files.
 *
 * The BlueJ files are help files (used for compiler help, exception help,
 * editor help), dialogue files (for message and error dialogues) and
 * templates (for class skeletons).
 *
 * Help texts and dialogue texts are handled through the "readHelpText"
 * method. The files consist of text IDs (a short string) followed by
 * the full text.
 *
 * Class skeletons (handled by the "translateFile" method) are text files
 * with place holders (variables) in them that will be replaced using a
 * dictionary.
 *
 * @author Michael Kolling
 * @version 1.0
 */
public class BlueJFileReader
{
    /**
     * Read a help text out of a help file.
     *
     * Help files are named <baseFileName>.help.<language> (for example
     * "moe.help.english"). Help texts inside the file are identified by
     * a help ID (a string).
     *
     * @param baseFileName	Base name of the help file
     * @param textID		ID string for the help message
     * @param exactMatch	If true, match ID string exactly. If false,
     *				wildcards are used.
     *
     * @return			The help text or null.
     */
    public static String readHelpText(String fileName, String textID,
                                      boolean exactMatch)
    {
        BufferedReader in = null;
        boolean found = false;

        try {
            in = new BufferedReader(new FileReader(fileName));
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
                    return helptext;
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
                                            fileName);
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
        if(pattern.charAt(0) == '*')
            return message.endsWith(pattern.substring(1));
        if(pattern.charAt(pattern.length()-1) == '*')
            return message.startsWith(
                                      pattern.substring(0, pattern.length()-2));
        else
            return pattern.equals(message);
    }


    /**
     * translateFile - copy a file while replacing special keywords
     *  within the file by definitions. Keywords are marked with a dollar
     *  sign and a name ($KEYWORD). 'translations' contains definitions
     *  to be used as replacements.
     *  This is used to create shell files from the shell file template.
     */
    public static void translateFile(String template, String dest,
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
     * copyFile - copy a file
     */
    public static boolean copyFile(String source, String dest)
    {
        // check whether source and dest are the same
        File srcFile = new File(source);
        File destFile = new File(dest);

        if(srcFile.getAbsolutePath().equals(destFile.getAbsolutePath()))
            return true;  // don't bother - they are the same

        Reader in = null;
        Writer out = null;
        try {
            Debug.message("opening srcFile: " + srcFile);
            in = new BufferedReader(new FileReader(srcFile));
            out = new BufferedWriter(new FileWriter(destFile));

            for(int c; (c = in.read()) != -1; )
                out.write(c);

            return true;
        } catch(IOException e) {
            return false;
        } finally {
            try{
                if(in != null) {
                in.close();
                Debug.message("closing srcFile: " + srcFile);
                }
            if(out != null)
                out.close();
            } catch (IOException e) {
            }
        }
        
    }


}
