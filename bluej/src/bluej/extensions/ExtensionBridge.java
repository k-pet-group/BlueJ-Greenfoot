package bluej.extensions;

import bluej.debugger.*;
import bluej.extensions.event.*;
import bluej.extmgr.*;
import bluej.pkgmgr.Package;
import com.sun.jdi.*;
import bluej.pkgmgr.*;

/*
 * This class acts as a bridge between the extensions package and other 
 * BlueJ-internal packages (extmgr) to provide access to methods which
 * shouldn't be documented in the Extensions API Javadoc. By using this class, 
 * those methods can be made package-local.
 *
 * This class should be excluded when the Javadoc API documentation is generated.
 */


public class ExtensionBridge 
  {
  public static void delegateEvent ( BlueJ thisBluej, ExtensionEvent anEvent )
    {
    thisBluej.delegateEvent(anEvent);
    }

  public static Object getVal ( PkgMgrFrame aFrame, String instanceName, Value val )
    {
    return BField.doGetVal(aFrame, instanceName, val );
    }

  public static BlueJ newBluej(ExtensionWrapper aWrapper, PrefManager aPrefManager, MenuManager aMenuManager)
    {
    return new BlueJ (aWrapper, aPrefManager, aMenuManager );
    }

  public static BObject newBObject (ObjectWrapper aWrapper)
    {
    return new BObject (aWrapper);
    }

  public static BPackage newBPackage (Package bluejPkg)
    {
    Identifier  anId = new Identifier (bluejPkg.getProject(), bluejPkg);
    return new BPackage (anId);
    }



  
  }
