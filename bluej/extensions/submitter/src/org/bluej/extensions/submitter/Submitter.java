package org.bluej.extensions.submitter;

import bluej.extensions.BlueJ;
import bluej.extensions.BMenuItem;
import bluej.extensions.BPackage;
import bluej.extensions.Extension;
import bluej.extensions.MenuListener;

import java.net.URL;

/**
 * An extension that allows users to automatically submit
 * their project by the agreed method
 *
 * @author Clive Miller
 * @version $Id: Submitter.java 1463 2002-10-23 12:40:32Z jckm $
 */
public class Submitter extends Extension implements MenuListener
{
    private static final int BUILT_FOR_MAJOR = 1;
    private static final int BUILD_FOR_MINOR = 0;

    private static final int VERSION_MAJOR = 2;
    private static final int VERSION_MINOR = 4;
    
    private boolean functionEnabled = false;
    private BMenuItem menuItem;
    private BlueJ bj;
    private SubmissionProperties sp;
    private SubmissionDialog sd;
    private Thread submitterThread;
    
    public boolean isCompatibleWith (int majorVersion, int minorVersion)
    {
        return (majorVersion == BUILT_FOR_MAJOR && minorVersion >= BUILD_FOR_MINOR);
    }

    public Submitter (final BlueJ bj) throws Exception
    {
        this.bj = bj;
        sp = null;
        
        menuItem = new BMenuItem (bj.getLabel ("menu.submit"), true);
        menuItem.addMenuListener (this);
        bj.getMenu().addMenuItem (menuItem);
        
        SubmissionProperties.addSettings (bj);
    }
    
    public void menuInvoked (Object src, final BPackage pkg)
    {
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
        
