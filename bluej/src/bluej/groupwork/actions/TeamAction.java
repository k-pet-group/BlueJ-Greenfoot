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
package bluej.groupwork.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import bluej.Config;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.actions.PkgMgrAction;


/**
 * An abstract class for team actions. 
 * 
 * @author fisker
 * @version $Id: TeamAction.java 6215 2009-03-30 13:28:25Z polle $
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
        this(Config.getString(name), false);
    }
    
    /**
     * Constructor for a team action which shows a dialog. An ellipsis
     * is added to the action text.
     * 
     * @param name   The key for action text
     * @param showsDialog  True if an ellipsis should be appended
     */
    public TeamAction(String name, boolean showsDialog)
    {
        super(showsDialog ? Config.getString(name) + "..." : Config.getString(name));
        if (!Config.isMacOS()){
        	// Mnemonic keys are against the apple gui guidelines.
        	putValue(MNEMONIC_KEY, new Integer(Config.getMnemonicKey(name)));
        }
        if (Config.hasAcceleratorKey(name)){
            putValue(ACCELERATOR_KEY, Config.getAcceleratorKey(name));
        }
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
    protected void handleServerResponse(TeamworkCommandResult result)
    {
        TeamUtils.handleServerResponse(result, pkgMgrFrame);
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
