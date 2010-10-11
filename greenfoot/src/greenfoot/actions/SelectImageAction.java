/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009, 2010  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.GreenfootImage;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.GClass;
import greenfoot.core.GreenfootMain;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.ImageClassRole;
import greenfoot.gui.images.ImageLibFrame;
import greenfoot.gui.images.ImageSelectionWatcher;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * Action to select an image for a class.
 * 
 * @author Davin McCall
 * @author Philip Stevens
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
    
    /**
     * Will save the currently selected image as the image of the class
     * if OK is pressed.
     * 
     * If performed on the current world this will allow for previewing of
     * the currently selected image, but revert to the original background
     * image of cancelled.
     * @param e ignored
     */
    public void actionPerformed(ActionEvent e)
    {
        final World currentWorld = WorldHandler.getInstance().getWorld();
        // save the original background if possible
        final GreenfootImage originalBackground = ((currentWorld == null) ? 
                null : WorldVisitor.getBackgroundImage(currentWorld));

        // allow the previewing if we are setting the image of the current world.
        ImageSelectionWatcher watcher = null;
        if (currentWorld != null && currentWorld.getClass().getName().equals(classView.getGClass().getQualifiedName())) {
            watcher = new ImageSelectionWatcher() {
                @Override
                public void imageSelected(final File imageFile)
                {
                    if (imageFile != null) {
                        Simulation.getInstance().runLater(new Runnable() {
                            @Override
                            public void run()
                            {
                                if (WorldHandler.getInstance().getWorld() == currentWorld) {
                                    currentWorld.setBackground(imageFile.getAbsolutePath());
                                }
                            }
                        });
                    }
                }
            };
        }
        
        // initialise our image library frame
        JFrame gfFrame = GreenfootMain.getInstance().getFrame();
        ImageLibFrame imageLibFrame = new ImageLibFrame(gfFrame, classView, watcher);
        DialogManager.centreDialog(imageLibFrame);
        imageLibFrame.setVisible(true);

        // set the image of the class to the selected file
        if (imageLibFrame.getResult() == ImageLibFrame.OK) {
            File currentImageFile = imageLibFrame.getSelectedImageFile();
            setClassImage(classView, gclassRole, currentImageFile);
            gfFrame.repaint();
        }
        // or if cancelled reset the world background to the original format
        // to avoid white screens or preview images being left there. 
        else if (currentWorld != null && imageLibFrame.getResult() == ImageLibFrame.CANCEL) {
            Simulation.getInstance().runLater(new Runnable() {
                @Override
                public void run()
                {
                    currentWorld.setBackground(originalBackground);
                }
            });
        }
    }

    public static void setClassImage(ClassView classView, ImageClassRole gclassRole, File imageFile)
    {
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
        } 
        else {
            imageFile = null;
            gclass.setClassProperty("image", null);
        }
        gclassRole.changeImage();
        gclass.getPackage().getProject().getProjectProperties().save();
    }
}
