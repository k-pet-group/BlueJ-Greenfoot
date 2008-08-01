package bluej.pkgmgr.target.role;

import java.awt.Color;
import java.util.Properties;
import bluej.Config;

/**
 * A MIDlet class role in a package, i.e. a target that is a MIDlet class file
 * built from Java source code.
 *
 * @author Cecilia Vargas
 * @version $Id: AppletClassRole.java 4746 2006-12-07 02:26:53Z davmac $
 */
public class MIDletClassRole extends ClassRole
{
    public static final String MIDLET_ROLE_NAME = "MIDletTarget";
    
    private static final Color bckgrndColor = Config.getItemColour("colour.class.bg.midlet");

    
    public MIDletClassRole()  { }

    public String getRoleName()
    {
        return MIDLET_ROLE_NAME;
    }

    public String getStereotypeLabel()
    {
        return "MIDlet";
    }
    
    public Color getBackgroundColour()
    {
        return bckgrndColor;
    }
 }
