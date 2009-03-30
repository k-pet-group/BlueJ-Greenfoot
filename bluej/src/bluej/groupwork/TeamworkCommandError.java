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
        messageIdMap.put("svn: Cannot connect to", "team-cant-connect");
        messageIdMap.put("svn: No repository found in", "team-cant-find-repository");
        messageIdMap.put("svn: File not found:", "team-cant-find-path");
        messageIdMap.put("svn: URL ", "team-cant-find-path");
        messageIdMap.put("svn: Authentication required for", "team-authentication-problem");
        messageIdMap.put("svn: File already exists:", "team-project-exists");
        
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
