package org.bluej.extensions.submitter;

import bluej.extensions.*;
import bluej.extensions.event.*;

import java.net.URL;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.bluej.utility.*;

/**
 * An extension that allows users to automatically submit
 * their project by the agreed method
 *
 * @author     Clive Miller, Damiano Bolla
 * @version    $Id: Submitter.java 1662 2003-03-07 09:57:11Z damiano $
 */
public class Submitter extends Extension implements MenuGen, BJEventListener
{
    private SubmissionDialog sd;
    private Thread submitterThread;
    private PrefPanel globalPreferences;
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

        int debugLevel = Utility.convStringToInt(stat.bluej.getExtPropString("debug.level", ""), Flexdbg.ERROR);
        stat.aDbg.setDebugLevel(debugLevel);

        stat.aDbg.setServiceMask(Flexdbg.ALL_SERVICES);

        stat.aDbg.trace(Stat.SVC_PROP, "Submitter.startup: CALLED");

        stat.globalProp = new GlobalProp();

        globalPreferences = new PrefPanel(stat);
        stat.bluej.setPrefGen(globalPreferences);

        String aLabel = stat.bluej.getLabel("menu.submit");
        anAction = new MenuAction(aLabel);
        anAction.setEnabled(false);
        stat.bluej.setMenuGen(this);

        stat.bluej.addBJEventListener(this);
    }


    /**
     * This method is called when BLueJ decides  (for whatever reason) that this extensions
     * should terminate. TODO it should clean up possibly running tasks...
     *
     * @return    Description of the Return Value
     */
    public String terminate()
    {
        return "";
    }


    /**
     * Something has happened in blueJ
     * What we do is to put it into the queue of events happening
     * and also we display it into the console.
     *
     * @param  ev  Description of the Parameter
     */
    public void eventOccurred(BJEvent ev)
    {
        int evType = ev.getEvent();

        // nothing to do if it is not a package event.
        if (!(ev instanceof bluej.extensions.event.PackageEvent)) return;

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
     * This is the action that has to be performed when the given menu is selected
     * It is fairly flexible to use and the parameters are just an example...
     */
    public class MenuAction extends AbstractAction
    {
        /**
         *Constructor for the MenuAction object
         *
         * @param  menuName  Description of the Parameter
         */
        public MenuAction(String menuName)
        {
            putValue(AbstractAction.NAME, menuName);
        }


        /**
         *  Description of the Method
         *
         * @param  anEvent  Description of the Parameter
         */
        public void actionPerformed(ActionEvent anEvent)
        {
            final BPackage pkg = stat.bluej.getCurrentPackage();

            // If there is no current package open what am I dong here ?
            if (pkg == null) return;

            if (submitterThread != null && submitterThread.isAlive()) {
                stat.aDbg.notice(Stat.SVC_BUTTON, "MenuAction.actionPerformed: previous thread is alive");
                return;
            }

            submitterThread =
                new Thread()
                {
                    public void run()
                    {
                        stat.submiProp = new SubmissionProperties(stat, pkg);
                        stat.submiProp.loadTree();
                        sd = new SubmissionDialog(stat, pkg);
                        sd.reset();
                        sd.show();
                    }
                };
            submitterThread.start();
        }
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
        return "2.5";
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
}

