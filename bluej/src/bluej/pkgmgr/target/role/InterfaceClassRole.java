package bluej.pkgmgr.target.role;

import java.awt.*;

import bluej.Config;

/**
 * A role object to represent the behaviour of interfaces.
 *
 * @author  Andrew Patterson 
 * @version $Id: InterfaceClassRole.java 2429 2003-12-09 10:54:54Z mik $
 */
public class InterfaceClassRole extends ClassRole
{
    public final static String INTERFACE_ROLE_NAME = "InterfaceTarget";
    private static final Color interfacebg = Config.getItemColour("colour.class.bg.interface");

    /**
     * Create the interface class role.
     */
    public InterfaceClassRole()
    {
    }

    public String getRoleName()
    {
        return INTERFACE_ROLE_NAME;
    }

    public String getStereotypeLabel()
    {
        return "interface";
    }

    /**
     * Return the intended background colour for this type of target.
     */
    public Color getBackgroundColour()
    {
        return interfacebg;
    }

}
