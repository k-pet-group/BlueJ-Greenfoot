package bluej.pkgmgr;

import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Properties;

/**
 * A role object to represent the behaviour of interfaces.
 *
 * @author  Andrew Patterson 
 * @version $Id: InterfaceClassRole.java 1538 2002-11-29 13:43:32Z ajp $
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
