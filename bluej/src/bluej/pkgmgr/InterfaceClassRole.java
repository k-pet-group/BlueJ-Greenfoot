package bluej.pkgmgr;

/**
 * A role object to represent the behaviour of interfaces.
 *
 * @author  Andrew Patterson 
 * @version $Id: InterfaceClassRole.java 1819 2003-04-10 13:47:50Z fisker $
 */
public class InterfaceClassRole extends ClassRole
{
    public final static String INTERFACE_ROLE_NAME = "InterfaceTarget";

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
}
