package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.debugger.MachineLoader;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Simple class to start the bluej.package manager.
 * The main method examines its arguments to determine what to run
 * (i.e. whether to just start the editor or whether to start the
 * package manager.
 *
 * @version $Id: Main.java 416 2000-03-14 03:03:13Z ajp $
 * @author Michael Kolling
 * @author Michael Cahill
 */
public class Main
{
    private static Hashtable packages = new Hashtable();

    /**
     * Start everything off
     */
    public static void main(String[] args)
    {
        if(args.length == 0) {
            // No arguments, so start an empty package manager window
            PkgMgrFrame frame = PkgMgrFrame.createFrame(null);
            frame.setVisible(true);
        }
        else {
            for(int i = 0; i < args.length; i++) {
                PkgMgrFrame frame = PkgMgrFrame.createFrame(args[i]);
                frame.setVisible(true);
            }
        }

        // start the MachineLoader (a separate thread) to load the
        // remote virtual machine in the background

        MachineLoader machineLoader = new MachineLoader();
        // lower priority to improve GUI response time
        machineLoader.setPriority(Thread.currentThread().getPriority() - 1);
        machineLoader.start();
    }

    public static void addPackage(Package pkg) {
        packages.put(pkg.getId(), pkg);
    }

    public static void removePackage(Package pkg) {
        packages.remove(pkg.getId());
    }

    public static Package getPackage(String pkgname)
    {
        return (Package)packages.get(pkgname);
    }

    public static PkgFrame getFrame(String pkgname)
    {
        return getPackage(pkgname).getFrame();
    }

    public static Package openPackage(String pkgname)
    {
        return openPackage(null, pkgname);
    }

    public static Package openPackage(String baseDir, String pkgname)
    {
        // Check whether it's already open
        Package pkg = getPackage(pkgname);

	if(pkg == null) { // if not, then search the library path to open it
	    String pkgdir = pkgname.replace('.', File.separatorChar);

	    if(baseDir != null) {
		String fulldir = baseDir + File.separator + pkgdir;
		String pkgfile = fulldir + File.separator + Package.pkgfileName;

		if(new File(pkgfile).exists())
		    return PkgMgrFrame.createFrame(fulldir).getPackage();
	    }
	}

        return pkg;
    }

    /**
     * @return an array of current Package objects, or null if none exist
     */
    public static Package[] getAllOpenPackages() {
        if (packages.size() == 0)
            return null;

        Package openPackages[] = new Package[packages.size()];

        Enumeration allPackages = packages.elements();
        for (int current = 0; current < openPackages.length && allPackages.hasMoreElements(); current++) {
            openPackages[current] = (Package)allPackages.nextElement();
        }

        if (openPackages.length == 0)
            return null;

        return openPackages;
    }
}
