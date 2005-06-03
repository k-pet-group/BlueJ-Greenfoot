package greenfoot.gui.classbrowser.role;

import greenfoot.GreenfootObject;
import greenfoot.ImageVisitor;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RObject;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * 
 * @author Poul Henriksen
 * @version $Id: GreenfootClassRole.java 3405 2005-06-03 15:10:56Z polle $
 *  
 */
public class GreenfootClassRole extends ClassRole
{
    private final static float SUPER_CLASS_FONT_SHRINK_FACTOR = 0.8f;
    private RClass rClass;
    private final static Dimension iconSize = new Dimension(16, 16);
    private Image image;
    private ClassView classView;

    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.ui.classbrowser.role.ClassRole#buildUI()
     */
    public void buildUI(ClassView classView, RClass rClass)
    {
        this.rClass = rClass;
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
        String name = "";
        try {
            name = rClass.getQualifiedName();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        int index = name.lastIndexOf('.');
        if (index >= 0) {
            name = name.substring(index + 1);
        }
        JLabel className = new JLabel(name);
        classView.add(className, c);
        classView.setBackground(new Color(245, 204, 155));
        classView.setForeground(new Color(245, 204, 155));
        classView.setOpaque(true);

        image = renderImage();
        if (image != null) {
            java.awt.Image scaledImage = image.getScaledInstance(iconSize.width, iconSize.height, java.awt.Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            c.insets.left = 4;
            c.insets.right = 4;
            classView.add(imageLabel, c);
        }
    }

    /**
     * Gets the image for this simulation class if one is available
     * 
     * @return The image, or null if no image can be found
     */
    public Image getImage()
    {
        /*
         * if (image == null && rClass.isCompiled()) { image = renderImage();
         */
        if (image == null) {
            image = renderImage();
        }
        return image;
    }

    private Image renderImage()
    {
        Object object = null;
        Class cls = classView.getRealClass();
        if (cls == null) {
            return null;
        }
        try {
            Constructor constructor = cls.getConstructor(new Class[]{});

            if (!Modifier.isAbstract(cls.getModifiers())) {
                object = (GreenfootObject) constructor.newInstance(null);
            }
        }
        catch (SecurityException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        catch (NoSuchMethodException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (object == null) {
            return null;
        }
        else if (object instanceof GreenfootObject) {
            GreenfootObject so = (GreenfootObject) object;
            greenfoot.GreenfootImage image = so.getImage();
            //rotate it.
            if (image != null) {
                BufferedImage bImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = (Graphics2D) bImg.getGraphics();

                double halfWidth = image.getWidth() / 2.;
                double halfHeight = image.getHeight() / 2.;
                double rotateX = halfWidth;
                double rotateY = halfHeight;
                g2.rotate(Math.toRadians(so.getRotation()), rotateX, rotateY);

                ImageVisitor.drawImage(image, g2, 0, 0, classView);

                return bImg;
            } else {
                System.err.println("Could not render the image: " + image + " for the class: " + cls);
            }
        }
        return null;
    }

    private String getSuperClassName(RClass rClass)
    {
        String name = null;
        try {
            RClass superClass = rClass.getSuperclass();
            if (superClass != null) {
                name = superClass.getQualifiedName();
            }
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        return name;
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
            writer.write("public class " + className + " extends " + superClassName + newline + "{" + newline);

            writer.write("  public " + className + "()" + newline + "  {" + newline);
            writer.write("    //setImage(\"name of the image file\");" + newline);
            writer.write("  }" + newline + newline);

            writer.write("  public void act()" + newline + "  {" + newline);
            writer.write("    //here you can create the behaviour of your object" + newline);
            writer.write("  }" + newline + newline);

            writer.write("}");
            writer.flush();
        }
        catch (IOException e2) {
            e2.printStackTrace();
        }
    }
}