
package bluej.groupwork;

import bluej.utility.Debug;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.ResourceUtilities;
import com.ice.jcvsii.*;

/**
 ** @version $Id: JcvsInit.java 904 2001-05-23 05:31:35Z ajp $
 ** @author Markus Ostman
 **
 ** Initialisation stuff needed for the use of jCVS classes
 **/

public class JcvsInit
{

  public static void doInitialize()
  {
    //Initialize preferences
    Config cfg = Config.getInstance();
    cfg.initializePreferences("jcvsii.");
    cfg.loadDefaultPreferences();
    cfg.loadUserPreferences();

    //initialize the jCVS Resource manager
    ResourceMgr.initializeResourceManager("jcvsii");
  }
}

