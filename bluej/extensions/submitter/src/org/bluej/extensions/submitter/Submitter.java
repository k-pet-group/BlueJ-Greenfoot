package org.bluej.extensions.submitter;

import bluej.extensions.*;
import java.net.*;
import org.bluej.extensions.submitter.properties.*;
import org.bluej.utility.*;

/**
 * An extension that allows users to automatically submit
 * their project by the agreed method
 *
 * @author     Clive Miller, University of Kent at Canterbury 2002
 * @author     Damiano Bolla, University of Kent at Canterbury 2003
 * @version    $Id: Submitter.java 2377 2003-11-21 11:53:50Z iau $
 */
public class Submitter extends Extension 
{
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

        stat.bluej.setMenuGenerator(new MenuBuilder(stat));
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
        return "3.9";
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
        try 
          {
          return new URL("http://www.bluej.org/extensions/submitter.html");
          } 
        catch (Exception exc) 
          {
          return null;
          }
    }

}

