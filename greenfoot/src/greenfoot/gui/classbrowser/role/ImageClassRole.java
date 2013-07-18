/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.classbrowser.role;

import greenfoot.GreenfootImage;
import greenfoot.actions.DragProxyAction;
import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.ObjectDragProxy;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

import java.awt.Image;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;


/**
 * Base class for class roles with associated images.
 * 
 * @author Davin McCall
 */
public abstract class ImageClassRole extends ClassRole
{
    protected GClass gClass;
    protected ClassView classView;
    protected GProject project;
    private static Hashtable<GClass, ImageIcon> imageIcons = new Hashtable<GClass, ImageIcon>();
    
    public ImageClassRole(GProject project)
    {
        this.project = project;
    }
    
    @Override
    public void buildUI(ClassView classView, GClass gClass)
    {
        this.gClass = gClass;
        this.classView = classView;
        classView.setText(gClass.getName());
        changeImage();
    }

    @Override
    public String getTemplateFileName()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * Gets the image for this simulation class if one is available
     * 
     * @return The image, or null if no image can be found
     */
    private static Image getImage(GClass gClass)
    {
        GreenfootImage gfImage = getGreenfootImage(gClass);
        if (gfImage != null) {
            return gfImage.getAwtImage();
        }
        else {
            return null;
        }
    }

    /**
     * Returns a class in the given class' class hierarchy that has an image set.
     */
    private static GClass getClassThatHasImage(GClass gclass) 
    {
        while (gclass != null) {
            String className = gclass.getQualifiedName();
            GreenfootImage gfImage = null;
            GProject project = gclass.getPackage().getProject();
            gfImage = project.getProjectProperties().getImage(className);
            if (gfImage != null) {
                break;
            }
            gclass = gclass.getSuperclass();
        }
        return gclass;
    }
    
    protected static GreenfootImage getGreenfootImage(GClass gclass)
    {
        gclass = getClassThatHasImage(gclass);

        if(gclass == null) {
            return null;
        }
        
        String className = gclass.getQualifiedName();
        GProject project = gclass.getPackage().getProject();
        return project.getProjectProperties().getImage(className);
    }
    
    private ImageIcon getImageIcon() {
        GClass gCls = getClassThatHasImage(gClass);
        ImageIcon icon = imageIcons.get(gCls);
        if (icon == null) {
            Image image = getImage(gCls);
            Image scaledImage = GreenfootUtil.getScaledImage(image, iconSize.width, iconSize.height);
            icon = new ImageIcon(scaledImage);
            imageIcons.put(gCls, icon);     
        }
        return icon;
    }

    public ObjectDragProxy createObjectDragProxy()
    {
        GreenfootImage greenfootImage = getGreenfootImage(gClass);
        Action dropAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent arg0) {
                classView.createInstance();
            }
        };
        ObjectDragProxy object = new ObjectDragProxy(greenfootImage, dropAction);
        return object;
    }

    protected Action createDragProxyAction(Action realAction)
    {
        GreenfootImage greenfootImage = getGreenfootImage(gClass);
        return new DragProxyAction(greenfootImage, realAction);
    }

    /**
     * Notification that a new image has been selected for this class.
     */
    public void changeImage()
    {
        project.getProjectProperties().removeCachedImage(classView.getClassName());             
        Image image = getImage(gClass);
        if (image != null) {
            Image scaledImage = GreenfootUtil.getScaledImage(image,iconSize.width, iconSize.height);
            ImageIcon icon = getImageIcon();
            icon.setImage(scaledImage);
            classView.setIcon(icon);
        } 
        else {
            classView.setIcon(null);
        }
    }

    @Override    
    public void remove() 
    {
        imageIcons.remove(gClass);        
    }
}
