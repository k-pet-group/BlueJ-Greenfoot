/*
 This file is part of the BlueJ program. 
 Copyright (C) 2018,2019  Michael Kolling and John Rosenberg
 
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
import javafx.stage.Stage;
import threadchecker.OnThread;
import threadchecker.Tag;

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
    public Stage initialOpenComplete(boolean projectOpen)
    {
        if (! projectOpen)
        {
            PkgMgrFrame frame = PkgMgrFrame.createFrame();
            frame.getWindow().setX(FIRST_X_LOCATION);
            frame.getWindow().setY(FIRST_Y_LOCATION);
            frame.setVisible(true);
            return frame.getWindow();
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
            
            return PkgMgrFrame.getMostRecent().getWindow();
        }
    }
    
    @Override
    public void doExitCleanup()
    {
        PkgMgrFrame[] pkgFrames = PkgMgrFrame.getAllFrames();

        // handle open packages so they are re-opened on startup
        handleOrphanPackages(pkgFrames);

        int i = pkgFrames.length - 1;
        // We replicate some of the behaviour of doClose() here
        // rather than call it to avoid a nasty recursion
        while (i >= 0)
        {
            PkgMgrFrame aFrame = pkgFrames[i--];
            aFrame.doSave();
            aFrame.closePackage();
            PkgMgrFrame.closeFrame(aFrame);
        }
    }

    /**
     * Save the list of open packages to the config, so that they can be re-opened when BlueJ is
     * next started.
     */
    @OnThread(Tag.FXPlatform)
    private static void handleOrphanPackages(PkgMgrFrame[] openFrames)
    {
        // if there was a previous list, delete it
        if (Main.hadOrphanPackages())
        {
            removeOrphanPackageList();
        }
        
        // add an entry for each open package
        for (int i = 0; i < openFrames.length; i++)
        {
            PkgMgrFrame aFrame = openFrames[i];
            if (!aFrame.isEmptyFrame())
            {
                Config.putPropString(Config.BLUEJ_OPENPACKAGE + (i + 1), aFrame.getPackage().getPath().toString());
            }
        }
    }

    /**
     * Remove previously listed orphan packages from bluej properties.
     */
    private static void removeOrphanPackageList()
    {
        String exists = "";
        for (int i = 1; exists != null; i++)
        {
            exists = Config.removeProperty(Config.BLUEJ_OPENPACKAGE + i);
        }
    }
}
