package greenfoot.gui.classbrowser.role;

import greenfoot.GreenfootImage;
import greenfoot.actions.DragProxyAction;
import greenfoot.actions.SelectImageAction;
import greenfoot.core.GClass;
import greenfoot.core.GreenfootMain;
import greenfoot.core.ObjectDragProxy;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import bluej.Config;

/**
 * A role for Actor classes 
 * 
 * @author Poul Henriksen
 * @version $Id: ActorClassRole.java 4982 2007-04-20 05:45:52Z davmac $
 */
public class ActorClassRole extends ClassRole
{
    private final static Dimension iconSize = new Dimension(16, 16);
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private ClassView classView;
    private String template = "actorclass.tmpl";

    private static final String newline = System.getProperty("line.separator");
    public static final String imports = "import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)" + newline;
    
    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.ui.classbrowser.role.ClassRole#buildUI()
     */
    public void buildUI(ClassView classView, GClass gClass)
    {
        this.classView = classView;

        classView.setText(gClass.getName());
        
        // Add the image label
        Image image = getImage();
        if (image != null) {
            Image scaledImage = GreenfootUtil.getScaledImage(image, iconSize.width, iconSize.height);
            classView.setIcon(new ImageIcon(scaledImage));
        }
    }

    /**
     * Notification that a new image has been selected for this class.
     */
    public void changeImage()
    {
        GreenfootMain.getProjectProperties().removeCachedImage(classView.getClassName());
        Image image = getImage();
        if (image != null) {
            Image scaledImage = GreenfootUtil.getScaledImage(image, iconSize.width, iconSize.height);
            classView.setIcon(new ImageIcon(scaledImage));
        }
    }
    
    public GreenfootImage getGreenfootImage() {
        GClass gclass = classView.getGClass();
        
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
    
    /**
     * Gets the image for this simulation class if one is available
     * 
     * @return The image, or null if no image can be found
     */
    private Image getImage()
    {
        GreenfootImage gfImage = getGreenfootImage();
        if (gfImage != null) {
            return gfImage.getAwtImage();
        }
        else {
            return null;
        }
    }

    /**
     * Need to overide this method in order to delay the invocation of the
     * constructor until the object is placed into the world.
     */
    public List createConstructorActions(Class realClass)
    {
        List realActions = super.createConstructorActions(realClass);
        List<Action> tempActions = new ArrayList<Action>();
        for (Iterator iter = realActions.iterator(); iter.hasNext();) {
            Action realAction = (Action) iter.next();
            Action tempAction = createDragProxyAction(realAction);
            tempActions.add(tempAction);
        }
 
        return tempActions;
    }
    
    public ObjectDragProxy createObjectDragProxy() {
        GreenfootImage greenfootImage = getGreenfootImage();
        Action dropAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent arg0) {
                classView.createInstance();
            }
        };
        ObjectDragProxy object = new ObjectDragProxy(greenfootImage, dropAction);
        return object;
    }

    private Action createDragProxyAction(Action realAction)
    {
        GreenfootImage greenfootImage = getGreenfootImage();
        return new DragProxyAction(greenfootImage, realAction);
    }
    
    /* (non-Javadoc)
     * @see greenfoot.gui.classbrowser.role.ClassRole#addPopupMenuItems(javax.swing.JPopupMenu, boolean)
     */
    public void addPopupMenuItems(JPopupMenu menu, boolean coreClass)
    {
        if (! coreClass) {
            menu.add(createMenuItem(new SelectImageAction(classView, this)));
        }
    }

    @Override
    public String getTemplateFileName()
    {
        return template;
    }

}