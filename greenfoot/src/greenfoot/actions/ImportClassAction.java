/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.ImportClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.record.InteractionListener;
import greenfoot.util.GreenfootUtil;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import bluej.Config;

/**
 * The action that shows the import-class dialog
 * (which allows you to import a supplied common class),
 * and imports whatever you select into the project
 * 
 * @author neil
 */
public class ImportClassAction extends AbstractAction
{   
    private GreenfootFrame gfFrame;
    private InteractionListener interactionListener;

    public ImportClassAction(GreenfootFrame gfFrame, InteractionListener interactionListener)
    {
        super(Config.getString("import.action"));
        this.gfFrame = gfFrame;
        this.interactionListener = interactionListener;
    }   
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        ImportClassDialog dlg = new ImportClassDialog(gfFrame);
        dlg.setVisible(true);
        
        File srcFile = dlg.getFinalSelection();
        
        if (srcFile != null) {
            String className = GreenfootUtil.removeExtension(srcFile.getName());
            File srcImage = dlg.getFinalSelectionImageFile();
            
            ClassBrowser classBrowser = gfFrame.getClassBrowser();
            GProject project = classBrowser.getProject();
            
            // Check if a class of the same name already exists in the project.
            // Renaming would be too tricky, so just issue error and stop in that case:
            for (GClass preexist : project.getDefaultPackage().getClasses(false)) {
                if (preexist.getQualifiedName().equals(className)) {
                    JOptionPane.showMessageDialog(gfFrame, "The current project already contains a class named " + className);
                    return;
                }
            }
            File destImage = null;
            if (srcImage != null) {
                destImage = new File(project.getImageDir(), srcImage.getName());
                if (destImage.exists()) {
                    JOptionPane.showMessageDialog(gfFrame, "The current project already contains an image file named " + srcImage.getName() + "; this file will NOT be replaced.");
                }
            }
            
            // Copy the java/class file cross:
            File destFile = new File(project.getDir(), srcFile.getName());
            GreenfootUtil.copyFile(srcFile, destFile);
            
            // We must reload the package to be able to access the GClass object:
            project.getDefaultPackage().reload();
            GClass gclass = project.getDefaultPackage().getClass(className);
            
            if (gclass == null) {
                //TODO give an error
                return;
            }
            
            // Copy the image across and set it as the class image:
            if (srcImage != null && destImage != null && !destImage.exists()) {
                GreenfootUtil.copyFile(srcImage, destImage);
                gclass.setClassProperty("image", destImage.getName());
            }
            
            //Finally, update the class browser:
            classBrowser.addClass(new ClassView(classBrowser, gclass, interactionListener));
            classBrowser.updateLayout();
        }
    }

}
