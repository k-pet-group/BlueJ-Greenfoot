package greenfoot.gui.classbrowser.role;

import greenfoot.GreenfootImage;
import greenfoot.actions.DragProxyAction;
import greenfoot.actions.SelectImageAction;
import greenfoot.core.GClass;
import greenfoot.core.Greenfoot;
import greenfoot.core.ObjectDragProxy;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bluej.Config;
import bluej.prefmgr.PrefMgr;

/**
 * 
 * @author Poul Henriksen
 * @version $Id: GreenfootClassRole.java 3884 2006-03-27 03:49:56Z davmac $
 * 
 */
public class GreenfootClassRole extends ClassRole
{
    private final static Dimension iconSize = new Dimension(16, 16);
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private GClass gClass;
    private ClassView classView;

    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.ui.classbrowser.role.ClassRole#buildUI()
     */
    public void buildUI(ClassView classView, GClass gClass)
    {
        this.gClass = gClass;
        this.classView = classView;
        // TODO:  get this color from the bluej config
        classView.setBackground(new Color(245, 204, 155));

        String name = gClass.getName();
        classView.setText(name);
        
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
        Greenfoot.getInstance().removeCachedImage(classView.getClassName());
        Image image = getImage();
        if (image != null) {
            Image scaledImage = GreenfootUtil.getScaledImage(image, iconSize.width, iconSize.height);
            classView.setIcon(new ImageIcon(scaledImage));
        }
    }
    
    public GreenfootImage getGreenfootImage() {
        GClass gclass = classView.getGClass();
        String className = gclass.getQualifiedName();
        
        while (gclass != null) {
            GreenfootImage gfImage = Greenfoot.getInstance().getClassImage(className);
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
    public Image getImage()
    {
        GreenfootImage gfImage = getGreenfootImage();
        if (gfImage != null) {
            return gfImage.getAWTImage();
        }
        else {
            return null;
        }
    }

    /**
     * 
     * Creates the skeleton for a new class of this type/role
     * 
     */
    public void createSkeleton(String className, String superClassName, FileWriter writer)
    {
        String newline = System.getProperty("line.separator");
        try {
            //TODO make sure a class from a different package are imported
            //        if(pkg != null && pkg.getName() != "") {
            // writer.write("import " + rClass.getQualifiedName() + ";\n");
            //      }
            writer.write(newline + "public class " + className + " extends " + superClassName + newline + "{" + newline);

            writer.write("    public " + className + "()" + newline + "    {" + newline);
            writer.write("        //setImage(\"name of the image file\");" + newline);
            writer.write("    }" + newline + newline);

            writer.write("    public void act()" + newline + "    {" + newline);
            writer.write("        //here you can create the behaviour of your object" + newline);
            writer.write("    }" + newline + newline);

            writer.write("}");
            writer.flush();
        }
        catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    /**
     * Need to overide this method in order to delay the invocation of the
     * constructor until the object is placed into the world.
     */
    public List createConstructorActions(Class realClass)
    {
        List realActions = super.createConstructorActions(realClass);
        List tempActions = new ArrayList();
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
    
    public void addPopupMenuItems(JPopupMenu menu)
    {
        JMenuItem item = new JMenuItem(new SelectImageAction(classView, this));
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
        menu.add(item);
    }

}