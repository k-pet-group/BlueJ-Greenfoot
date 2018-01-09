/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2015  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.ImageClassRole;
import greenfoot.guifx.images.ImageSelectionWatcher;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import bluej.Config;

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

        // The whole class is due to be deleted.
    }
}
