package bluej.pkgmgr.target.role;

import javax.swing.*;

import bluej.pkgmgr.target.*;

/**
 * A role object to represent the behaviour of abstract classes.
 *
 * @author  Andrew Patterson 
 * @version $Id: AbstractClassRole.java 1952 2003-05-15 06:04:19Z ajp $
 */
public class AbstractClassRole extends ClassRole
{
    public final static String ABSTRACT_ROLE_NAME = "AbstractTarget";
    
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
