package greenfoot.gui.classbrowser.role;

import greenfoot.gui.classbrowser.ClassView;

import java.awt.Image;
import java.io.FileWriter;

import javax.swing.ImageIcon;

import rmiextension.wrappers.RClass;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassRole.java 3238 2004-12-14 18:43:54Z polle $
 */
public abstract class ClassRole
{
    public abstract void buildUI(ClassView classView, RClass rClass);

    public Image getImage()
    {
        return null;
    }

    /**
     * @return
     */
    public abstract void createSkeleton(String className, String superClassName, FileWriter writer);
}