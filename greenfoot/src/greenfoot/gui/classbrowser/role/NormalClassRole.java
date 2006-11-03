package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassView;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Role for a "normal" (non-Actor, non-World) class.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NormalClassRole.java 4686 2006-11-03 15:04:05Z polle $
 */
public class NormalClassRole extends ClassRole
{
    private static NormalClassRole instance;
    
    /**
     * Get the (singleton) instance of NormalClassRole.
     */
    public static NormalClassRole getInstance()
    {
        if (instance == null) {
            instance = new NormalClassRole();
        }
        
        return instance;
    }
    
    private NormalClassRole()
    {
        // Nothing to do.
    }
    
    public void buildUI(ClassView classView, GClass gClass)
    {
        classView.setIcon(null);
        classView.setText(gClass.getQualifiedName());
    }

    public void createSkeleton(String className, String superClassName, FileWriter writer)
    {
        String newline = System.getProperty("line.separator");
        try {
            writer.write(newline + "public class " + className);
            if (superClassName != null) {
                writer.write(" extends " + superClassName);
            }
            writer.write(newline + "{" + newline);

            writer.write("    public " + className + "()" + newline + "    {" + newline);
            writer.write("        // Write your constructor here" + newline);
            writer.write("    }" + newline + newline);


            writer.write("}");
            writer.flush();
        }
        catch (IOException e2) {
            e2.printStackTrace();
        }
    }
    
}
