/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.util;

import bluej.Config;
import java.awt.Component;
import java.io.File;


import javax.swing.JFileChooser;

import bluej.prefmgr.PrefMgr;
import bluej.utility.PackageChooserStrict;

/**
 * Class that holds different file choosers that can be used to select files or directories. 
 * 
 * @author Poul Henriksen
 *
 */
public class FileChoosers
{
    private static JFileChooser  scenarioFileChooser;
    private static JFileChooser  newFileChooser;
    
    /**
     * Let the user specify a new file name.
     * 
     *  @return Returns a File pointing to the chosen file or directory, or null if none selected.
     */
    public static File getFileName(Component parent, File defaultFile, String title) {
        if (newFileChooser == null) {
            newFileChooser = new JFileChooser();
            newFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        }
        newFileChooser.setDialogTitle(title);
        newFileChooser.setSelectedFile(defaultFile);
        int result = newFileChooser.showDialog(parent, Config.getString("chooser.newFile.button"));
        
        if (result != JFileChooser.APPROVE_OPTION) {
           return null;
        }
        return newFileChooser.getSelectedFile();
    }
    
    /**
     * Select a Greenfoot scenario by using a file chooser, i.e. a file chooser which
     * recognises Greenfoot packages and treats them differently.
     * 
     * @return Returns a File pointing to the scenario directory, or null if none selected.
     */
    public static File getScenario(Component parent)
    {
        if(scenarioFileChooser == null) {
            scenarioFileChooser = new PackageChooserStrict(new File(PrefMgr.getProjectDirectory()));
            scenarioFileChooser.setDialogTitle(Config.getString("chooser.scenario.title"));
        }
        int result = scenarioFileChooser.showDialog(parent, Config.getString("chooser.scenario.button"));
        
        if (result != JFileChooser.APPROVE_OPTION) {
           return null;
        }
        PrefMgr.setProjectDirectory(scenarioFileChooser.getSelectedFile().getParentFile().getPath());
        
        return scenarioFileChooser.getSelectedFile();
    }
}
