package org.bluej.extensions.submitter;

import bluej.extensions.*;

import java.net.URL;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.bluej.utility.*;

/**
 * An extension that allows users to automatically submit
 * their project by the agreed method
 *
 * @author Clive Miller
 * @version $Id: Submitter.java 1566 2002-12-10 14:52:31Z damiano $
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
    private Stat stat;

    
    public boolean isCompatibleWith (int majorVersion, int minorVersion)
    {
        return (majorVersion == BUILT_FOR_MAJOR && minorVersion >= BUILD_FOR_MINOR);
    }


    public void startup (final BlueJ i_bj)
    {
        bj = i_bj;

        stat = new Stat();
        stat.bluej = bj;
        stat.aDbg  = new Flexdbg();

        stat.aDbg.setDebugLevel(Flexdbg.NOTICE);
        stat.aDbg.setServiceMask(Flexdbg.ALL_SERVICES);

        stat.aDbg.trace(Stat.SVC_PROP,"Submitter.startup: CALLED");
               
        sp = null;
        globalPreferences = new PrefPanel(bj);   
        bj.setPrefGen(globalPreferences);

        String aLabel = bj.getLabel ("menu.submit");
        anAction = new MenuAction ( aLabel  );
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
  public class MenuAction extends AbstractAction
    {
    public MenuAction ( String menuName )
      {
      putValue (AbstractAction.NAME,menuName);
      }

    public void actionPerformed ( ActionEvent anEvent )
      {
      final BPackage pkg = bj.getCurrentPackage();
        if (pkg == null) 
            return;
        
        if (submitterThread != null && submitterThread.isAlive()) {
            stat.aDbg.notice(Stat.SVC_BUTTON,"MenuAction.actionPerformed: previous thread is alive");
            return;
        }
        
        submitterThread = new Thread() {
            public void run() {
                sp = new SubmissionProperties (stat, pkg);
                sp.reload();
                sd = new SubmissionDialog (bj, pkg, sp);
                sd.reset();
                sd.show();
            }
        };
        submitterThread.start();
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
        
