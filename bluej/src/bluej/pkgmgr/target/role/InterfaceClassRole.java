package bluej.pkgmgr.target.role;


/**
 * A role object to represent the behaviour of interfaces.
 *
 * @author  Andrew Patterson 
 * @version $Id: InterfaceClassRole.java 1952 2003-05-15 06:04:19Z ajp $
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
