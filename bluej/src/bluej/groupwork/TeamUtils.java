package bluej.groupwork;

import java.awt.EventQueue;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;

import bluej.utility.DialogManager;

public class TeamUtils
{
    /**
     * Handle an authentication exception (Display an appropriate error
     * message). This must NOT be called from the Swing event thread. Calling
     * this method blocks until the user acknowledges the dialog.
     */
    public static void handleAuthenticationException(final Window pkgMgrFrame)
    {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run()
                {
                    DialogManager.showError(pkgMgrFrame, "team-authentication-problem");
                }
            });
        }
        catch (InterruptedException ie) { }
        catch (InvocationTargetException ite) {
            ite.getCause().printStackTrace();
        }
    }
    
    /**
     * Handle an invalid CVS root exception. This must NOT be called from the Swing
     * event thread. Calling this method blocks until the user acknowledges the
     * dialog.
     */
    public static void handleInvalidCvsRootException(final Window pkgMgrFrame)
    {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run()
                {
                    DialogManager.showError(pkgMgrFrame, "team-invalid-cvsroot");
                }
            });
        }
        catch (InterruptedException ie) { }
        catch (InvocationTargetException ite) {
            ite.getCause().printStackTrace();
        }
    }

    /**
     * Handle a server response in an appropriate fashion, i.e. if the response
     * indicates an error, then display an error dialog. 
     * 
     * @param basicServerResponse  The response to handle
     */
    public static void handleServerResponse(final BasicServerResponse basicServerResponse, final Window window)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                if ((basicServerResponse != null) && basicServerResponse.isError()) {
                    String message = basicServerResponse.getMessage();
                    String translatedMessage = CvsServerMessageTranslator.translate(message);
                    if (translatedMessage != null) {
                        DialogManager.showError(window, translatedMessage);
                    }
                    else {
                        DialogManager.showErrorText(window, message);
                    }
                }
            }
        });
    }

}
