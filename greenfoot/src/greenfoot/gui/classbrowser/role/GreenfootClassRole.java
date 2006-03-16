package greenfoot.gui.classbrowser.role;

import greenfoot.GreenfootImage;
import greenfoot.actions.DragProxyAction;
import greenfoot.actions.SelectImageAction;
import greenfoot.core.GClass;
import greenfoot.core.ObjectDragProxy;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

/**
 * 
 * @author Poul Henriksen
 * @version $Id: GreenfootClassRole.java 3830 2006-03-16 05:36:04Z davmac $
 * 
 */
public class GreenfootClassRole extends ClassRole
{
    private final static float SUPER_CLASS_FONT_SHRINK_FACTOR = 0.8f;
    private GClass gClass;
    private final static Dimension iconSize = new Dimension(16, 16);
    private Image image;
    private ClassView classView;
    private JLabel imageLabel;

    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.ui.classbrowser.role.ClassRole#buildUI()
     */
    public void buildUI(ClassView classView, GClass gClass)
    {
        this.gClass = gClass;
        this.classView = classView;
        //TODO get this color from the bluej config
        classView.setBackground(new Color(245, 204, 155));
        classView.setOpaque(true);
        classView.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        /*
         * String superClassString = getSuperClassName(rClass); if
         * (superClassString != null) { JLabel superClassName = new JLabel("[" +
         * superClassString + "]"); Font normal = superClassName.getFont(); Font
         * superFont = normal.deriveFont( SUPER_CLASS_FONT_SHRINK_FACTOR);
         * superClassName.setFont(superFont); component.add(superClassName,c);
         * //component.add(new Arrow(), c);
         */
        c.gridy = 2;
        String name = gClass.getName();
        
        JLabel className = new JLabel(name);
        classView.add(className, c);
        classView.setBackground(new Color(245, 204, 155));
        classView.setForeground(new Color(245, 204, 155));
        classView.setOpaque(true);

        // Add the image label
        image = getImage();
        if (image != null) {
            Image scaledImage = GreenfootUtil.getScaledImage(image, iconSize.width, iconSize.height);
            imageLabel = new JLabel(new ImageIcon(scaledImage));
        }
        else {
            imageLabel = new JLabel();
        }
        c.insets.left = 4;
        c.insets.right = 4;
        classView.add(imageLabel, c);
    }

    /**
     * Notification that a new image has been selected for this class.
     */
    public void changeImage()
    {
        image = null;
        getImage();
        if (image != null && imageLabel != null) {
            Image scaledImage = GreenfootUtil.getScaledImage(image, iconSize.width, iconSize.height);
            imageLabel.setIcon(new ImageIcon(scaledImage));
        }
    }
    
    public GreenfootImage getGreenfootImage() {
        Image im = getImage();
        if(im == null) {
            return null;
        }
        return new GreenfootImage(im);
    }
    
    /**
     * Gets the image for this simulation class if one is available
     * 
     * @return The image, or null if no image can be found
     */
    public Image getImage()
    {
        if (image == null) {
            String imageName = gClass.getClassProperty("image");
            if (imageName == null) {
                imageName = "greenfoot-logo.png";
            }
            try {
                image = ImageIO.read(new File(new File("images"), imageName));
            }
            catch (IOException ioe) {
                image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }
        }
        
        return image;
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
        menu.add(new SelectImageAction(classView, this));
    }

}