/*
 This file is part of the BlueJ program. 
 Copyright (C) 2018  Michael Kolling and John Rosenberg
 
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
package bluej;

import java.io.File;

import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.Debug;

/**
 * Gui handler for BlueJ
 * 
 * @author Davin McCall
 */
public class BlueJGuiHandler implements GuiHandler
{
    private static final int FIRST_X_LOCATION = 20;
    private static final int FIRST_Y_LOCATION = 20;
    
    @Override
    public boolean tryOpen(File path, boolean displayError)
    {
        // Note that BlueJ won't display an error dialog.
        // TODO: fix BlueJ to respect displayError parameter.
        return PkgMgrFrame.doOpen(path, null);
    }
    
    @Override
    public void handleAbout()
    {
        PkgMgrFrame.handleAbout();
    }
    
    @Override
    public void handlePreferences()
    {
        PkgMgrFrame.handlePreferences();
    }
    
    @Override
    public void handleQuit()
    {
        PkgMgrFrame.handleQuit();
    }
    
    @Override
    public void initialOpenComplete(boolean projectOpen)
    {
        if (! projectOpen)
        {
            PkgMgrFrame frame = PkgMgrFrame.createFrame();
            frame.getFXWindow().setX(FIRST_X_LOCATION);
            frame.getFXWindow().setY(FIRST_Y_LOCATION);
            frame.setVisible(true);
        }
        else
        {
            // This is a convenience for development: set bluej.class.open property on the command
            // line, and the named class will be opened when BlueJ starts:
            String targetName = Config.getPropString("bluej.class.open", null);
            if (targetName != null && !targetName.equals(""))
            {
                boolean foundTarget = false;
                for (Project proj : Project.getProjects())
                {
                    Target tgt = proj.getTarget(targetName);
                    if (tgt != null && tgt instanceof ClassTarget)
                    {
                        ((ClassTarget)tgt).open();
                        foundTarget = true;
                    }
                }
                if (!foundTarget)
                {
                    Debug.message("Did not find target class in opened project: \"" + targetName + "\"");
                }
            }
        }
    }
}
