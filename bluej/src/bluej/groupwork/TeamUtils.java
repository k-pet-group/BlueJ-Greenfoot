package bluej.groupwork;

import java.awt.Window;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import bluej.groupwork.cvsnb.CvsServerMessageTranslator;
import bluej.utility.DialogManager;

public class TeamUtils
{
    /**
     * Handle a server response in an appropriate fashion, i.e. if the response
     * indicates an error, then display an error dialog.
     * 
     * Call on the AWT event handling thread.
     * 
     * @param basicServerResponse  The response to handle
     */
    public static void handleServerResponse(TeamworkCommandResult result, final Window window)
    {
        if (result != null) {
            if (result.wasAuthFailure()) {
                DialogManager.showError(window, "team-authentication-problem");
            }
            else if (result.isError() && ! result.wasAborted()) {
                String message = result.getErrorMessage();
                String translatedMessage = CvsServerMessageTranslator.translate(message);
                if (translatedMessage != null) {
                    DialogManager.showError(window, translatedMessage);
                }
                else {
                    DialogManager.showErrorText(window, message);
                }
            }
        }
    }
    
    /**
     * From a set of File objects, remove those files which should be treated as
     * binary files (and put them in a new set). 
     */
    public static Set extractBinaryFilesFromSet(Set files)
    {
        Set binFiles = new HashSet();
        Iterator i = files.iterator();
        while (i.hasNext()) {
            File f = (File) i.next();
            String fname = f.getName();
            if (! fname.endsWith(".txt") && ! fname.endsWith(".java")) {
                binFiles.add(f);
                i.remove();
            }
        }
        return binFiles;
    }

}
