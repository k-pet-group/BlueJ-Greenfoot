/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017,2018 Michael Kölling and John Rosenberg
 
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
package bluej.groupwork.ui;

import bluej.groupwork.TeamStatusInfo;
import bluej.pkgmgr.Project;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Set;
import javafx.stage.Window;

/**
 * Common functionality between the two different commit dialogs in BlueJ - the "commit only"
 * dialog vs the "commit and push dialog".
 * 
 * @author Fabio Heday
 */
@OnThread(Tag.FXPlatform)
public interface CommitAndPushInterface
{
    public void setVisible(boolean show);
    
    public String getComment();
    
    public void reset();
    
    /**
     * Get a list of the layout files to be committed
     */
    public Set<File> getChangedLayoutFiles();
    
    /**
     * Get a set of the layout files which have changed (with status info).
     */
    public Set<TeamStatusInfo> getChangedLayoutInfo();
    
    public boolean includeLayout();
    
    /**
     * Start the activity indicator.
     */
    public void startProgress();
    
    /**
     * Stop the activity indicator. Call from any thread.
     */
    @OnThread(Tag.Any)
    public void stopProgress();
    
    public Project getProject();
    
    /**
     * Displays a message on the commit/push window.
     * @param msg 
     */
    public default void displayMessage(String msg){ }
    
    public Window asWindow();
    
    public void setLocationRelativeTo(Window w);
    
    /**
     * Check whether the dialog is currently open.
     */
    public boolean isShowing();
}
