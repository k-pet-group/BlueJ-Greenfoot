package bluej.pkgmgr;

import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Properties;

/**
 * A role object to represent the behaviour of abstract classes.
 *
 * @author  Andrew Patterson 
 * @version $Id: AbstractClassRole.java 1538 2002-11-29 13:43:32Z ajp $
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
    protected boolean createClassConstructorMenu(JPopupMenu menu, ClassTarget ct, Class cl)
    {
        return false;
    }
}
