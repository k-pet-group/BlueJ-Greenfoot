package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassView;
import java.awt.Color;
import java.io.FileWriter;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NormalClassRole.java 3858 2006-03-22 17:25:25Z mik $
 */
public class NormalClassRole extends ClassRole
{

    public void buildUI(ClassView classView, GClass gClass)
    {

        //TODO get this color from the bluej config
        classView.setBackground(new Color(245, 204, 155));
        classView.setText(gClass.getQualifiedName());
    }

    public void createSkeleton(String className, String superClassName, FileWriter writer)
    {

    }

}