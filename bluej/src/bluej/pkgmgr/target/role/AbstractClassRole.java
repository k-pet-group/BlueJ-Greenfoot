package bluej.pkgmgr.target.role;

import javax.swing.*;
import java.awt.*;

import bluej.Config;
import bluej.pkgmgr.target.*;

/**
 * A role object to represent the behaviour of abstract classes.
 *
 * @author  Andrew Patterson 
 * @version $Id: AbstractClassRole.java 2429 2003-12-09 10:54:54Z mik $
 */
public class AbstractClassRole extends ClassRole
{
    public final static String ABSTRACT_ROLE_NAME = "AbstractTarget";
    private static final Color abstractbg = Config.getItemColour("colour.class.bg.abstract");
    
    /**
     * Create the abstract class role.
     */
    public AbstractClassRole()
    {
    }

    public String getRoleName()
    {
        return ABSTRACT_ROLE_NAME;
    }

    public String getStereotypeLabel()
    {
        return "abstract";
    }

    /**
     * Return the intended background colour for this type of target.
     */
    public Color getBackgroundColour()
    {
        return abstractbg;
    }

    /**
     * Creates a class menu containing any constructors.
     *
     * Because we are an abstract class we cannot have any constructors
     * so we override this method to do nothing.
     *
     * @param menu the popup menu to add the class menu items to
     * @param cl Class object associated with this class target
     */
    public boolean createClassConstructorMenu(JPopupMenu menu, ClassTarget ct, Class cl)
    {
        return false;
    }
}
