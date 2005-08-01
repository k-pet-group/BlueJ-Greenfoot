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
 * @version $Id: ClassMgr.java 3494 2005-08-01 14:08:07Z damiano $
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
     * Returns a ProjectClassLoader which can load classes from a particular 
     * directory (while delegating other class loading to the default BlueJ
     * class loader).
     *
    public static ProjectClassLoader getProjectLoader(File projectDir)
    {
        return new ProjectClassLoader(projectDir, getClassMgr().bluejloader);
    }
    */
    
    // =========== instance part ============
    
    private BlueJLoader bluejloader = new BlueJLoader();

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
    protected ClassPath userlibExtLibraries;

    /** Don't let anyone else instantiate this class */
    private ClassMgr()
    {
        URL[] bootcp = Boot.getInstance().getRuntimeClassPath();
        URL[] syscp = Boot.getInstance().getRuntimeUserClassPath();
        URL[] userextcp = Boot.getInstance().getUserLibClassPath();
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
        userlibExtLibraries = new ClassPath(userextcp);

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
        all.addClassPath(userlibExtLibraries);
        all.addClassPath(bootLibraries);

        return all;
    }
    
    /**
     * Get a classpath containing all elements required for the bluej runtime.
     * This should include junit and essential bluej runtime classes (those in
     * the bluej.runtime package) but little else.
     * 
     * At the moment it contains quite a bit more than that...
     */
    public ClassPath getRuntimeUserClassPath()
    {
        ClassPath rt = new ClassPath();
        rt.addClassPath(systemLibraries);
        
        return rt;
    }

    /**
     * Return the classpath for user defined libraries (from lib/userlib
     * and from Preferences/Libraries)
     * 
     * @return The classpath containing all user libraries.
     */
    public ClassPath getUserClassPath()
    {
        ClassPath usercp = new ClassPath();

        usercp.addClassPath(userlibExtLibraries);
        usercp.addClassPath(userLibraries);

        return usercp;
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

    /**
     * A ClassLoader which can load classes from the user library
     * list as well as the system library list.
     *
     * We aim to construct a class hierarchy like this
     *
     *  DefaultSystemLoader
     */
    class BlueJLoader extends ClassLoader
    {
        /**
         * Read in a class file from disk. Return a class object.
         */
        protected Class findClass(String name) throws ClassNotFoundException
        {
            //Debug.message("classmgrloader: finding " + name);

            byte[] bytes = loadClassData(name);
            if(bytes != null) {
                //Debug.message("classmgrloader: succeeded " + name);
                return defineClass(name, bytes, 0, bytes.length);
            }
            else {
                //Debug.message("classmgrloader: failed " + name);
                throw new ClassNotFoundException("BlueJLoader");
            }
        }

        /**
         * Read in a class file from disk. Return the class code as a byte
         * array. The JDK class loader delegation model means that we are
         * only ever asked to look up a class if the parent system loader
         * has failed. Therefore we need only look in our userLibraries and
         * systemLibraries. The bootLibraries will have been searched by
         * the system loader.
         */
        protected byte[] loadClassData(String name)
        {
            ByteArrayOutputStream classdata = new ByteArrayOutputStream();

            try {
                String filename = name.replace('.', File.separatorChar) + ".class";

                InputStream in = null;

                // try to get these input streams but catch IO exceptions because there
                // is no reason why an error reading here should spoil the rest of
                // the party
                try {
                   in = systemLibraries.getFile(filename);
                }
                catch (IOException ioe) { }

                // if can't find it in system libraries, try 'userlibs' directory
                if(in == null) {
                    try {
                        in = userlibExtLibraries.getFile(filename);
                    }
                    catch (IOException ioe) { }
                }
                
                // if still can't find it, try the defined user libraries
                if(in == null) {
                    try {
                        in = userLibraries.getFile(filename);
                    }
                    catch (IOException ioe) { }
                }

                if(in != null) {
                    BufferedInputStream bufin = new BufferedInputStream(in);
                    int b;
                    while ((b = bufin.read()) != -1) {
                        classdata.write(b);
                    }
                    // Debug.message("classmgrloader: " + classdata.size() + " bytes");
                }

            } catch(Exception e) {
                Debug.reportError("Exception attempting to load class " + name + ": " + e);
                return null;
            }
            if (classdata.size() == 0)
                return null;
            else
                return classdata.toByteArray();
        }
    }
}

