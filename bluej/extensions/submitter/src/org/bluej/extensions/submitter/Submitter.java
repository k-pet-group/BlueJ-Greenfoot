package org.bluej.extensions.submitter;

import bluej.extensions.*;
import bluej.extensions.event.*;

import java.net.URL;
import javax.swing.*;
import java.awt.event.*;
import org.bluej.utility.*;
import org.bluej.extensions.submitter.properties.TreeData;

/**
 * An extension that allows users to automatically submit
 * their project by the agreed method
 *
 * @author     Clive Miller, Damiano Bolla
 * @version    $Id: Submitter.java 1980 2003-05-22 13:32:32Z iau $
 */
public class Submitter extends Extension implements MenuGenerator, PackageListener
{
    private MenuAction anAction;
    private int numPackagesOpen = 0;       // Counter for menu en/disable
    private Stat stat;

    /**
     * This is where the submitter starts to work. It is called by the BlueJ
     *
     * @param  i_bluej  Description of the Parameter
     */
    public void startup(final BlueJ i_bluej)
    {
        stat = new Stat();
        stat.bluej = i_bluej;
        stat.aDbg = new Flexdbg();

        int debugLevel = Utility.convStringToInt(stat.bluej.getExtensionPropertyString("debug.level", ""), Flexdbg.NOTICE);
        stat.aDbg.setDebugLevel(debugLevel);
        stat.aDbg.setServiceMask(Flexdbg.ALL_SERVICES);
        stat.aDbg.trace(Stat.SVC_PROP, "Submitter.startup: CALLED");

        // Properties panel creation
        stat.globalProp = new GlobalProp();
        stat.bluej.setPreferenceGenerator(new PrefPanel(stat));

        stat.treeData     = new TreeData(stat);
        stat.submitDialog = new SubmitDialog(stat);

        // Tools-Submit menu creation
        String aLabel = stat.bluej.getLabel("menu.submit");
        anAction = new MenuAction(aLabel);
        anAction.setEnabled(false);
        stat.bluej.setMenuGenerator(this);

        stat.bluej.addPackageListener(this);
    }


    /**
     * Count packages opening in order to enable the submit button
     */
    public void packageOpened(PackageEvent ev)
    {
        if ((++numPackagesOpen) > 0)
            anAction.setEnabled(true);
    }

    /**
     * Count packages closing in order to disable the submit button
     */
    public void packageClosing(PackageEvent ev)
    {
        if ((--numPackagesOpen) <= 0)
            anAction.setEnabled(false);
    }



    /**
     * If it is as expected I will have only one to give out
     * do NOT store the menu tree you just create, rely on the
     * callback to know which menu gets selected.
     *
     * @return    The menuItem value
     */
    public JMenuItem getMenuItem()
    {
        return new JMenuItem(anAction);
    }

    /**
     *  Gets the compatibleWith attribute of the Submitter object
     *
     * @return               The compatibleWith value
     */
    public boolean isCompatible()
    {
        if ( VERSION_MAJOR < 2 ) return false;
        return (true);
    }


    /**
     * @return    The version
     */
    public String getVersion()
    {
        return "3.6";
    }

    public String getName()
    {
        if ( stat != null && stat.bluej != null ) 
          return stat.bluej.getLabel("Submitter");
          
        return "Submitter";
    }


    /**
     * Gets the description
     *
     * @return    The description value
     */
    public String getDescription()
    {
        return stat.bluej.getLabel("description");
    }


    /**
     * returns the a URL where you can find more info
     *
     * @return    The uRL value
     */
    public URL getURL()
    {
        URL url = null;
        try {
            url = new URL("http://www.cs.ukc.ac.uk/projects/bluej/submit.html");
        } catch (java.net.MalformedURLException ex) {}
        return url;
    }



// ============================ Utility CLASSES are here =======================

  /**
   * This is the action that has to be performed when the given menu is selected
   * It is fairly flexible to use and the parameters are just an example...
   */
  public class MenuAction extends AbstractAction
    {

    /**
     * Constructor for the MenuAction object
     */
    public MenuAction(String menuName)
      {
      putValue(AbstractAction.NAME, menuName);
      }

    /**
     *  Called when a menu is selected
     */
    public void actionPerformed(ActionEvent anEvent)
      {
          /*
           * If we can't get the details of the current package, just return
           * The package could still go away later, but we'll cope with that
           * through "file not found" when we go looking.
           * It's more likely to have been closed than deleted anyway.
           */
          try {
              BPackage bpkg = stat.bluej.getCurrentPackage();
              if (bpkg == null) return;     // package has already gone away
              
              BProject bproj = bpkg.getProject();
              bproj.save();
      
              // Try to submit this project
              stat.submitDialog.submitThis ( bproj.getDir(), bproj.getName() );
          } catch (ExtensionException e ) {}
      }
    }

// ======================= END of main class ===================================
}

