package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.FlowLayout;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;


/**
 * @author Poul Henriksen
 * @version $Id: WorldClassRole.java 3995 2006-04-21 10:12:54Z polle $
 */
public class WorldClassRole extends ClassRole
{

    public void buildUI(ClassView classView, GClass gClass)
    {
        //TODO get this color from the bluej config
        classView.setBackground(new Color(245, 204, 155));

        String name = GreenfootUtil.extractClassName(gClass.getQualifiedName());
       
        classView.setText(name);
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
            writer.write("    /**" + newline + "     * Creates a new world with 20x20 cells and" + newline
                    + "     * with a cell size of 10x10 pixels" + newline + "     */" + newline + "    public "
                    + className + "() {" + newline + "        super(20,20,10);" + newline + "    }" + newline);

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