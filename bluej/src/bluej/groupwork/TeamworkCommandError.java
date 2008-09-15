package bluej.groupwork;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import bluej.utility.Debug;
import bluej.utility.DialogManager;

/**
 * A teamwork command result representing a general error during command
 * execution. If the error message is recognised it will be replaced by a more
 * helpful message. Messages will also be logged with Debug.
 * 
 * @author Davin McCall
 * @author Poul Henriksen
 */
public class TeamworkCommandError extends TeamworkCommandResult
{
    private String errMsg;
    private String localizedErrMsg;
    
    // Map of known error messages. Populated below.
    private static final Map<String, String> messageIdMap = new TreeMap<String, String>();

    // CVS error messages
    static {
        messageIdMap.put("cvs server: Up-to-date check failed", "team-uptodate-failed");
        messageIdMap.put("cvs server: cannot find module", "team-cant-find-module");
    }    
    
    // SVN error messages
    static {
        messageIdMap.put("svn: No repository found in", "team-cant-find-repository");
        messageIdMap.put("svn: File not found:", "team-cant-find-group");
        messageIdMap.put("svn: URL ", "team-cant-find-module");
        messageIdMap.put("svn: Authentication required for", "team-authentication-problem");

        // When no internet access (or probably also if the server doesn't exist, or is down)
        // messageIdMap.put("svn: Cannot connect to", "team-no-connection"); 
    }
        
    /**
     * Construct a new Teamwork command error result. The supplied error message
     * will be exchanged with a more descriptive message if possible. If not
     * possible, it will use the optional translatedErrMsg, and if that fails
     * too, it will just use the raw errMsg.
     * 
     * @param errMsg Error message as returned by the server
     * @param localizedErrMsg Localized error message, or null if not available.
     */
    public TeamworkCommandError(String errMsg, String localizedErrMsg)
    {
        this.errMsg = errMsg;
        this.localizedErrMsg = localizedErrMsg;
        Debug.message("Teamwork error message: " + errMsg);
    }
    
    public boolean isError()
    {
        return true;
    }
    
    /**
     * Get the error message - ready to be shown in a dialog.
     */
    public String getErrorMessage()
    {
        String betterMsg = getBetterMsg(errMsg);
        if(betterMsg != null) {
            return betterMsg;
        }
        else if(localizedErrMsg != null) {
            return localizedErrMsg;
        }
        else {
            return errMsg;
        }
    }
    
    /**
     * Translate the given message (which was received directly from the server)
     * into a better message. If there is no known translation, returns null.
     * 
     * @param msg  The message to translate
     * @return  The message, or null
     */
    private String getBetterMsg(String msg)
    {        
        if(msg == null) {
            return null;
        }

        String betterMsg = null;
        String trimmedInput = msg.trim();
        
        if (trimmedInput.length() == 0) {
            betterMsg = "team-empty-message";
        }
        else {
            // Look for the key in the map
            Set<Entry<String, String>> entries = messageIdMap.entrySet();
            for (Iterator<Entry<String, String>> iterator = entries.iterator(); iterator.hasNext();) {
                Entry<String, String> entry = (Entry<String, String>) iterator.next();
                if (trimmedInput.startsWith(entry.getKey())) {
                    betterMsg = entry.getValue();
                    break;
                }
            }
        }
        if (betterMsg != null) {
            betterMsg = DialogManager.getMessage(betterMsg);
        }
        return betterMsg;
    }
}
