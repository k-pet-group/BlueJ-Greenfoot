package org.bluej.extensions.submitter;

import bluej.extensions.*;

import java.net.URL;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/**
 * An extension that allows users to automatically submit
 * their project by the agreed method
 *
 * @author Clive Miller
 * @version $Id: Submitter.java 1498 2002-11-11 10:34:08Z damiano $
 */
public class Submitter extends Extension implements MenuGen
{
    private static final int BUILT_FOR_MAJOR = 1;
    private static final int BUILD_FOR_MINOR = 0;

    private static final int VERSION_MAJOR = 2;
    private static final int VERSION_MINOR = 4;
    
    private boolean functionEnabled = false;
    private BlueJ bj;
    private SubmissionProperties sp;
    private SubmissionDialog sd;
    private Thread submitterThread;
    private PrefPanel globalPreferences;
    private MenuAction anAction;

    
    public boolean isCompatibleWith (int majorVersion, int minorVersion)
    {
        return (majorVersion == BUILT_FOR_MAJOR && minorVersion >= BUILD_FOR_MINOR);
    }


    public void startup (final BlueJ bj)
    {
        this.bj = bj;
        sp = null;
        
        globalPreferences = new PrefPanel(bj);   
        bj.setPrefGen(globalPreferences);

        anAction = new MenuAction ( bj.getLabel ("menu.submit") );
        bj.setMenuGen(this);

    }

    public String terminate()
    {
        return "";
    }

  /** 
   * If it is as expected I will have only one to give out
   * do NOT store the menu tree you just create, rely on the 
   * callback to know which menu gets selected.
   */
  public JMenuItem getMenuItem()
    {
    return new JMenuItem (anAction);
    }


  /**
   * This is the action that has to be performed when the given menu is selected
   * It is fairly flexible to use and the parameters are just an example...
   */
  private class MenuAction extends AbstractAction
    {
    public MenuAction ( String menuName )
      {
      putValue (AbstractAction.NAME,menuName);
      }

    public void actionPerformed ( ActionEvent anEvent )
      {
      final BPackage pkg = bj.getCurrentPackage();
        if (pkg == null) {
            return;
        }
        if (submitterThread == null || !submitterThread.isAlive()) {
            submitterThread = new Thread() {
                public void run() {
                    sp = new SubmissionProperties (bj, pkg);
                    sp.reload();
                    sd = new SubmissionDialog (bj, pkg, sp);
                    sd.reset();
                    sd.show();
                }
            };
            submitterThread.start();
        }
      }
    }


    public int getVersionMajor()
    {
        return VERSION_MAJOR;
    }

    public int getVersionMinor()
    {
        return VERSION_MINOR;
    }
    
    public String getDescription()
    {
        return bj.getLabel ("description");
    }
    
    public URL getURL()
    {
        URL url = null;
        try {
            url = new URL ("http://www.cs.ukc.ac.uk/projects/bluej/submit.html");
        } catch (java.net.MalformedURLException ex) {}
        return url;  
    }
}
        
