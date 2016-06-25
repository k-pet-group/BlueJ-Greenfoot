/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016  Michael Kolling and John Rosenberg 
 
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

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.actions.PkgMgrAction;


/**
 * An abstract class for team actions. 
 * 
 * @author fisker
 */
public abstract class TeamAction extends AbstractAction
{
    private final PkgMgrFrame pkgMgrFrame;

    /**
     * Constructor for a team action.
     * 
     * @param name  The key for the action name (team.xxx.yyy)
     */
    public TeamAction(PkgMgrFrame pmf, String name)
    {
        this(pmf, Config.getString(name), false);
    }
    
    /**
     * Constructor for a team action which shows a dialog. An ellipsis
     * is added to the action text.
     * 
     * @param name   The key for action text
     * @param showsDialog  True if an ellipsis should be appended
     */
    public TeamAction(PkgMgrFrame pmf, String name, boolean showsDialog)
    {
        super(showsDialog ? Config.getString(name) + "..." : Config.getString(name));
        this.pkgMgrFrame = pmf;
        if (!Config.isMacOS()){
            // Mnemonic keys are against the apple gui guidelines.
            putValue(MNEMONIC_KEY, new Integer(Config.getMnemonicKey(name)));
        }
        if (Config.hasAcceleratorKey(name)){
            putValue(ACCELERATOR_KEY, Config.getAcceleratorKey(name));
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @OnThread(Tag.Swing)
    public void actionPerformed(ActionEvent e)
    {
        actionPerformed(pkgMgrFrame);
    }
    
    /**
     * Invoked when the action occurs.
     * 
     * @param pmf The PkgMgrFrame in which the action occurred.
     */
    @OnThread(Tag.Swing)
    public abstract void actionPerformed(PkgMgrFrame pmf);
    
    /**
     * Handle a server response in an appropriate fashion, i.e. if the response
     * indicates an error, then display an error dialog. 
     * 
     * @param basicServerResponse  The response to handle
     */
    @OnThread(Tag.FXPlatform)
    protected void handleServerResponse(TeamworkCommandResult result)
    {
        TeamUtils.handleServerResponseFX(result, pkgMgrFrame.getFXWindow());
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
    
    /**
     * changes the name of the action.
     * @param name 
     */
    @OnThread(Tag.Swing)
    public void setName(String name)
    {
        if (name != null){
            putValue("Name", name);
        }
    }
}
