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
 * @version    $Id: Submitter.java 1708 2003-03-19 09:39:47Z damiano $
 */
public class Submitter extends Extension implements MenuGen, BluejEventListener
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

        int debugLevel = Utility.convStringToInt(stat.bluej.getExtPropString("debug.level", ""), Flexdbg.NOTICE);
        stat.aDbg.setDebugLevel(debugLevel);
        stat.aDbg.setServiceMask(Flexdbg.ALL_SERVICES);
        stat.aDbg.trace(Stat.SVC_PROP, "Submitter.startup: CALLED");

        // Properties panel creation
        stat.globalProp = new GlobalProp();
        stat.bluej.setPrefGen(new PrefPanel(stat));

        stat.treeData     = new TreeData(stat);
        stat.submitDialog = new SubmitDialog(stat);
        stat.treeDialog   = new TreeDialog(stat);

        // Tools-Submit menu creation
        String aLabel = stat.bluej.getLabel("menu.submit");
        anAction = new MenuAction(aLabel);
        anAction.setEnabled(false);
        stat.bluej.setMenuGen(this);

        stat.bluej.addBluejEventListener(this);
    }


    /**
     * This method is called when BLueJ decides  (for whatever reason) that this extensions
     * should terminate. 
     *
     * @return    Description of the Return Value
     */
    public String terminate()
    {
        // WARNINIG: TODO it should clean up possibly running tasks...
        return "";
    }


    /**
     * Something has happened in blueJ.
     * I only need to enable or disable the submit button...
     *
     * @param  ev  Description of the Parameter
     */
    public void eventOccurred(BluejEvent ev)
    {
        // nothing to do if it is not a package event.
        if (!(ev instanceof bluej.extensions.event.PackageEvent)) return;

        PackageEvent pkgEvent = (PackageEvent)ev;
        int evType = pkgEvent.getEvent();

        if (evType == PackageEvent.PACKAGE_OPENED) {
            if ((++numPackagesOpen) > 0)
                anAction.setEnabled(true);
        }

        if (evType == PackageEvent.PACKAGE_CLOSING) {
            if ((--numPackagesOpen) <= 0)
                anAction.setEnabled(false);
        }
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
        return "3.5";
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
      final BPackage pkg = stat.bluej.getCurrentPackage();

      // If there is no current package open what am I dong here ?
      if (pkg == null) return;

      // Try to submit this package
      stat.submitDialog.submitThis ( pkg );
      }
    }

// ======================= END of main class ===================================
}

