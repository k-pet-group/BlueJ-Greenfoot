package bluej.extensions;

import bluej.debugger.*;
import bluej.extensions.event.*;
import bluej.extmgr.*;
import bluej.pkgmgr.Package;
import com.sun.jdi.*;

/*
 * This class is here to bridge methods call between packages that should not
 * be javadoc visible. Whan javadoc will allow to hide public method then this calass
 * can go away.
 * You can use this ant script to document all BUT this class.
 * 
 * 
        <mkdir dir="${extdoc}"/>
        <javadoc destdir="${extdoc}"
            packagenames="bluej.extensions.event"
            sourcepath="${src}"

            Windowtitle="BlueJ Extensions API"
            Doctitle="BlueJ Extensions API"
            bottom="&lt;a href=//www.bluej.org/&gt;BlueJ homepage&lt;/a&gt;">

            <fileset dir="src" defaultexcludes="yes">
              <include name="bluej/extensions/*.java" />
              <exclude name="bluej/extensions/ExtensionBridge.java"/>
            </fileset>
            <classpath refid="bluej.class.path"/>
       </javadoc>

 
 */


public class ExtensionBridge 
  {
  public static void delegateEvent ( BlueJ thisBluej, ExtensionEvent anEvent )
    {
    thisBluej.delegateEvent(anEvent);
    }

  public static Object getVal ( Package bluej_pkg, String instanceName, Value val )
    {
    return BField.doGetVal(bluej_pkg, instanceName, val );
    }

  public static BlueJ newBluej(ExtensionWrapper aWrapper, PrefManager aPrefManager, MenuManager aMenuManager)
    {
    return new BlueJ (aWrapper, aPrefManager, aMenuManager );
    }

  public static BObject newBObject (ObjectWrapper aWrapper)
    {
    return new BObject (aWrapper);
    }

  public static BPackage newBPackage (Package aBlueJpkg)
    {
    return new BPackage (aBlueJpkg);
    }



  
  }