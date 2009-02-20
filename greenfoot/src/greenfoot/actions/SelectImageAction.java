/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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

import bluej.Config;
import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.ImageClassRole;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;
import bluej.utility.FileUtility;

/**
 * Action to select an image for a class.
 * 
 * @author Davin McCall
 * @version $Id: SelectImageAction.java 6170 2009-02-20 13:29:34Z polle $
 */
public class SelectImageAction extends AbstractAction
{
    private ClassView classView;
    private ImageClassRole gclassRole;
    
    public SelectImageAction(ClassView classView, ImageClassRole gcr)
    {
        super(Config.getString("select.image"));
        this.classView = classView;
        this.gclassRole = gcr;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        JFrame gfFrame = GreenfootMain.getInstance().getFrame();
        ImageLibFrame imageLibFrame = new ImageLibFrame(gfFrame, classView);
        
        File currentImageFile = imageLibFrame.getSelectedImageFile();
        
        setClassImage(classView, gclassRole, currentImageFile);
    }

    public static void setClassImage(ClassView classView, ImageClassRole gclassRole, File imageFile)
    {
        try {
            GClass gclass = classView.getGClass();
        	File projImagesDir = gclass.getPackage().getProject().getImageDir();            
            if (imageFile != null) {
                if (! imageFile.getParentFile().getAbsoluteFile().equals(projImagesDir)) {
                    // An image was selected from an external dir. We need
                    // to copy it into the project images directory first.
                    File destFile = new File(projImagesDir, imageFile.getName());
                    try {
                        FileUtility.copyFile(imageFile, destFile);
                        imageFile = destFile;
                    }
                    catch (IOException e) {
                        Debug.reportError("Error when copying file: " + imageFile + " to: " + destFile, e);
                    }
                }
                
                gclass.setClassProperty("image", imageFile.getName());
                gclassRole.changeImage();
            }
        }
        catch (RemoteException re) {
        	re.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {}
    }
}
