package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassView;
import java.awt.Color;
import java.io.FileWriter;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NormalClassRole.java 4012 2006-04-25 14:38:06Z mik $
 */
public class NormalClassRole extends ClassRole
{

    public void buildUI(ClassView classView, GClass gClass)
    {
        classView.setText(gClass.getQualifiedName());
    }

    public void createSkeleton(String className, String superClassName, FileWriter writer)
    {

    }

}