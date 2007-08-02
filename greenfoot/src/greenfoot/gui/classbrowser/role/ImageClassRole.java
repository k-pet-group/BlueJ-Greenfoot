package greenfoot.gui.classbrowser.role;

import java.awt.Image;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import greenfoot.GreenfootImage;
import greenfoot.actions.DragProxyAction;
import greenfoot.core.GClass;
import greenfoot.core.GreenfootMain;
import greenfoot.core.ObjectDragProxy;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

/**
 * Base class for class roles with associated images.
 * 
 * @author Davin McCall
 */
public abstract class ImageClassRole extends ClassRole
{
    protected GClass gClass;
    protected ClassView classView;
    
    @Override
    public void buildUI(ClassView classView, GClass gClass)
    {
        this.gClass = gClass;
        this.classView = classView;
        classView.setText(gClass.getName());
        
        Image image = getImage(gClass);
        if (image != null) {
            Image scaledImage = GreenfootUtil.getScaledImage(image, iconSize.width, iconSize.height);
            classView.setIcon(new ImageIcon(scaledImage));
        }
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

    public static GreenfootImage getGreenfootImage(GClass gclass)
    {
        while (gclass != null) {
            String className = gclass.getQualifiedName();
            GreenfootImage gfImage = null;
            try {
                gfImage = GreenfootMain.getProjectProperties().getImage(className);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            if (gfImage != null) {
                return gfImage;
            }
            gclass = gclass.getSuperclass();
        }
        
        return null;
    }

    public ObjectDragProxy createObjectDragProxy() {
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
        GreenfootMain.getProjectProperties().removeCachedImage(classView.getClassName());
        Image image = getImage(gClass);
        if (image != null) {
            Image scaledImage = GreenfootUtil.getScaledImage(image, iconSize.width, iconSize.height);
            classView.setIcon(new ImageIcon(scaledImage));
        }
    }
}
