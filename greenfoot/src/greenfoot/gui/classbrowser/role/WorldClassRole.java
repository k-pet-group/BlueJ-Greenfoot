package greenfoot.gui.classbrowser.role;

import greenfoot.gui.classbrowser.ClassView;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.JLabel;

import rmiextension.wrappers.RClass;

/**
 * @author Poul Henriksen
 * @version $Id: WorldClassRole.java 3124 2004-11-18 16:08:48Z polle $
 */
public class WorldClassRole extends ClassRole
{

    public void buildUI(ClassView classView, RClass rClass)
    {

        //TODO get this color from the bluej config
        classView.setBackground(new Color(245, 204, 155));
        classView.setOpaque(true);
        classView.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;

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
            writer.write("    /**" + newline + "     * Creates a new world with 20x20 cells and" + newline
                    + "     * with a cell size of 50x50 pixels" + newline + "     */" + newline + "    public "
                    + className + "() {" + newline + "        super(20,20,50,50);" + newline + "    }" + newline);

            //            writer.write(" public " + className + "()" + newline + " {" +
            // newline);
            //            writer.write(" setImage(\"name of the image file\");" + newline);
            //            writer.write(" }" + newline + newline);
            //            
            //            writer.write(" public act()" + newline + " {" + newline);
            //            writer.write(" //here you can create the behaviour of your
            // object" + newline);
            //            writer.write(" }" + newline + newline);

            writer.write("}");
            writer.flush();
        }
        catch (IOException e2) {
            e2.printStackTrace();
        }
    }
}