package bluej.pkgmgr;

import java.io.*;
import java.net.URL;

import bluej.Main;
import bluej.utility.Debug;

/**
 * Component to check this applications version against the latest known
 * version.
 *
 * @author  Michael Kolling
 * @version $Id: VersionChecker.java 844 2001-04-12 05:06:47Z mik $
 */
final class VersionChecker
{
    // ===== static singleton factory method =====

    private static VersionChecker checker = null;

    public static VersionChecker getVersionChecker()
    {
        if(checker == null)
            checker = new VersionChecker();
        return checker;
    }

    // ===== instance fields and methods ======

    private VersionChecker()
    {
    }

    /**
     * Envoke the "Check Version" user function. 
     */
    public void checkVersion(PkgMgrFrame frame)
    {
         VersionCheckDialog dialog = new VersionCheckDialog(frame);
         // dialog is modal...
    }


}
