package greenfoot.gui.classbrowser.role;

import greenfoot.gui.classbrowser.ClassView;

import java.io.FileWriter;

import javax.swing.ImageIcon;

import rmiextension.wrappers.RClass;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassRole.java 3124 2004-11-18 16:08:48Z polle $
 */
public abstract class ClassRole
{
    public abstract void buildUI(ClassView classView, RClass rClass);

    public ImageIcon getImage()
    {
        return null;
    }

    /**
     * @return
     */
    public abstract void createSkeleton(String className, String superClassName, FileWriter writer);
}