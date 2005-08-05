package bluej.classmgr;

import java.io.*;
import java.util.*;
import java.net.URL;

import bluej.utility.Debug;
import bluej.Config;
import bluej.*;

/**
 * Class to maintain a global class loading environment.
 *
 * We aim to construct a class hierarchy like this
 *
 *  DefaultSystemLoader
 *       ^
 *       |
 *  BlueJLoader (retrieve with ClassMgr.getBlueJLoader()
 *    ^  ^  ^     or use ClassMgr.loadClass() to use directly)
 *    |  |  |
 *  ClassPathLoaders
 *              (one for each project, retrieve using getLoader(dir)
 *               and supply the directory the project lives in)
 *
 * @author  Andrew Patterson
 * @version $Id: ClassMgr.java 3505 2005-08-05 15:43:20Z damiano $
 */
public class ClassMgr
{
    private static final String errormissingclasspath
                            = Config.getString("classmgr.error.missingclasspath");
    private static final String errormissingbootclasspath
                            = Config.getString("classmgr.error.missingbootclasspath");

    private static final String userlibPrefix = "bluej.userlibrary";
    private static final String syslibPrefix = "bluej.systemlibrary";
    
    private static ClassMgr currentClassMgr = new ClassMgr();

    /**
     * Returns the classmgr object associated with the current BlueJ.
     * environment. Some of the methods of class <code>ClassMgr</code> are instance
     * methods and must be invoked with respect to the current classmgr object.
     *
     * @return  the <code>ClassMgr</code> object associated with the current
     *          BlueJ environment.
     */
    public static ClassMgr getClassMgr()
    {
        return currentClassMgr;
    }


    /**
     * Protected to allow access by the class manager panel.
     * These start off as empty classpath's. If the config
     * files do not exist and therefore throws an exception
     * when we go to open it we will still end up with a valid
     * classpath object (albeit empty)
     */
    protected ClassPath bootLibraries;
    protected ClassPath systemLibraries;
    protected ClassPath userLibraries;

    /** Don't let anyone else instantiate this class */
    private ClassMgr()
    {
        URL[] bootcp = Boot.getInstance().getRuntimeClassPath();
        URL[] syscp = Boot.getInstance().getRuntimeUserClassPath();
        String envcp = System.getProperty("java.class.path");

        if (bootcp == null) {        // pre JDK1.2
            Debug.message(errormissingbootclasspath);
        } 
        else if (envcp == null) {    // no classpath
            Debug.message(errormissingclasspath);
        }

        // The following should be fixed: we enter all jars as boot and system class paths.
        // Really they should be either one (most boot, but junit needs to be system, it seems... 
        // investigate!
        bootLibraries = new ClassPath(bootcp);
        systemLibraries = new ClassPath(syscp);
        userLibraries = new ClassPath();

        addConfigEntries(systemLibraries, syslibPrefix);
        addConfigEntries(userLibraries, userlibPrefix);

        if (envcp != null) {
            bootLibraries.addClassPath(envcp, "");
        }

        /* XXX we should add here the boot libraries which are in the JDK extension
           directory */
        //System.getProperty("java.ext.dirs");
    }

    public ClassPath getAllClassPath()
    {
        ClassPath all = new ClassPath();

        all.addClassPath(systemLibraries);
        all.addClassPath(userLibraries);
        all.addClassPath(bootLibraries);

        return all;
    }

    /**
     * Retrieve from the system wide Config entries corresponding to classpath
     * entries. The entries to retrieve start with prefix and have 1.location,
     * 2.location etc appended to them until an entry is not found.
     *
     * @param   prefix    the prefix of the property names to look up
     */
    private void addConfigEntries(ClassPath cp, String prefix)
    {
        int resourceID = 1;
        try {
            while (true) {
                String location = Config.getPropString(prefix + resourceID + ".location", null);

                if (location == null)
                    break;

                cp.addClassPath(location, "");

                resourceID++;
            }
        } catch (MissingResourceException mre) {
            // it is normal that this is exception is thrown, it just means we've come
            // to the end of the file
        }
    }

    /**
     * Save user classpath entries into the system wide Config properties object.
     * The entries stored start with prefix and have 1.location,
     * 2.location etc appended to them.
     */
    protected void saveUserLibraries()
    {
        String r1;
        int resourceID = 1;

        while(true) {
            r1 = Config.removeProperty(userlibPrefix + resourceID + ".location");

            if(r1 == null)
                break;

            resourceID++;
        }

        Iterator it = userLibraries.getEntries().iterator();
        resourceID = 1;

        while (it.hasNext()) {
            ClassPathEntry nextEntry = (ClassPathEntry)it.next();

            Config.putPropString(userlibPrefix + resourceID + ".location",
                                    nextEntry.getPath());
            resourceID++;
        }
    }

}

