
package bluej.groupwork;

import javax.activation.*;

import bluej.utility.Debug;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.ResourceUtilities;
import com.ice.jcvsii.*;

/**
 ** @version $Id: JcvsInit.java 401 2000-02-29 01:42:12Z markus $
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

