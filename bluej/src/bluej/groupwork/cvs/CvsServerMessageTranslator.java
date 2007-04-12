package bluej.groupwork.cvs;

/**
 * This class takes strings that represent messages from the CVS server and
 * returns strings that are easier to understand.
 * 
 * @author fisker
 * @version $Id: CvsServerMessageTranslator.java 4916 2007-04-12 03:57:23Z davmac $
 */
public class CvsServerMessageTranslator
{
    // Up-to-date check failed. This happens when a user tries to commit a file
    // which has been updated in the repository.
    private static final String upToDateCheckFailed_in = "cvs server: Up-to-date check failed";
    private static final String upToDateCheckFailed_out = "team-uptodate-failed";
    
    // Cannot find module. This occurs on project checkout.
    private static final String cannotFindModule_in = "cvs server: cannot find module";
    private static final String cannotFindModule_out = "team-cant-find-module";
    
    /**
     * Translate the given message (which was received directly from the CVS server)
     * into a dialog message-id. If there is no known translation, returns null.
     * 
     * @param input  The message to translate
     * @return  The message-Id, or null
     */
    public static String translate(String input)
    {
        String output = null;
        String trimmedInput = input.trim();
        if (trimmedInput.startsWith(upToDateCheckFailed_in)) {
            output = upToDateCheckFailed_out;
        }
        else if (trimmedInput.startsWith(cannotFindModule_in)) {
            output = cannotFindModule_out;
        }
        else if (trimmedInput.length() == 0) {
            output = "team-empty-message";
        }
        return output;
    }
}