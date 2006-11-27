package bluej.groupwork.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.Config;
import bluej.groupwork.BasicServerResponse;
import bluej.groupwork.CvsServerMessageTranslator;
import bluej.groupwork.InvalidCvsRootException;
import bluej.groupwork.TeamUtils;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.actions.PkgMgrAction;


/**
 * An abstract class for team actions. 
 * 
 * @author fisker
 * @version $Id: TeamAction.java 4704 2006-11-27 00:07:19Z bquig $
 */
public abstract class TeamAction extends AbstractAction
{
    private PkgMgrFrame pkgMgrFrame;

    /**
     * Constructor for a team action.
     * 
     * @param name  The key for the action name (team.xxx.yyy)
     */
    public TeamAction(String name)
    {
        super(Config.getString(name));
    }

    /**
     * Constructor for a team action
     * 
     * @param name  The key for the action name
     * @param icon  The icon for the action
     */
    public TeamAction(String name, Icon icon)
    {
        super(name, icon);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        pkgMgrFrame = PkgMgrAction.frameFromEvent(e);
        actionPerformed(pkgMgrFrame);
    }
    
    /**
     * Invoked when the action occurs.
     * 
     * @param pmf The PkgMgrFrame in which the action occurred.
     */
    public abstract void actionPerformed(PkgMgrFrame pmf);
    
    /**
     * Handle a server response in an appropriate fashion, i.e. if the response
     * indicates an error, then display an error dialog. 
     * 
     * @param basicServerResponse  The response to handle
     */
    protected void handleServerResponse(BasicServerResponse basicServerResponse)
    {
        TeamUtils.handleServerResponse(basicServerResponse, pkgMgrFrame);
    }

    /**
     * Handle an authentication exception. This must NOT be called from the
     * Swing event thread.
     * 
     * @param e  The authentication exception
     */
    protected void handleAuthenticationException(AuthenticationException e)
    {
        stopProgressBar();
        TeamUtils.handleAuthenticationException(pkgMgrFrame);
    }

    /**
     * Handle an invalid CVS root exception. This must NOT be called from the
     * Swing event thread.
     * 
     * @param e  The exception
     */
    protected void handleInvalidCvsRootException(InvalidCvsRootException e)
    {
        stopProgressBar();
        TeamUtils.handleInvalidCvsRootException(pkgMgrFrame);
    }

    /**
     * Start the activity indicator. This can be called from any thread.
     */
    protected void startProgressBar()
    {
        pkgMgrFrame.startProgress();
    }

    /**
     * Stop the activity indicator. This can be called from any thread.
     */
    protected void stopProgressBar()
    {
        pkgMgrFrame.stopProgress();
    }
    
    protected void setStatus(String statusMessage)
    {
        pkgMgrFrame.setStatus(statusMessage);
    }

    protected void clearStatus()
    {
        pkgMgrFrame.clearStatus();
    }
}
