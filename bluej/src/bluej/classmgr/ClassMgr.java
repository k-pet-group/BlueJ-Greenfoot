package bluej.classmgr;

import java.io.*;
import java.util.*;
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
 * @version $Id: ClassMgr.java 1997 2003-05-30 12:10:30Z damiano $
 */
public class ClassMgr
{
    private static final String errorloadingconfig
                            = Config.getString("classmgr.error.loadingconfig");
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
     */
    public static ProjectClassLoader getProjectLoader(File projectDir)
    {
        return new ProjectClassLoader(projectDir, getClassMgr().bluejloader);
    }

    /**
     * Convenience static method to easily allow classes to be loaded into the default
     * BlueJ class loader.
     */
    public static Class loadBlueJClass(String classname) throws ClassNotFoundException
    {
        return getClassMgr().bluejloader.loadClass(classname);
    }
    /**
     * Returns the class loader associated with the ClassMgr.
     * This class loader is used as the parent of all
     * class loaders created within BlueJ.
     *
     * @return  the <code>ClassLoader</code> associated with BlueJ's
     *          current ClassMgr
     */
    public static ClassLoader getBlueJLoader()
    {
        return getClassMgr().bluejloader;
    }

    private BlueJLoader bluejloader = new BlueJLoader();

    public ClassPath getAllClassPath()
    {
        ClassPath all = new ClassPath();

        all.addClassPath(systemLibraries);
        all.addClassPath(userLibraries);
        all.addClassPath(bootLibraries);

        return all;
    }

    /**
     * Protected to allow access by the class manager panel.
     * These start off as empty classpath's. If the config
     * files do not exist and therefore throws an exception
     * when we go to open it we will still end up with a valid
     * classpath object (albeit empty)
     */
    protected ClassPath systemLibraries = new ClassPath();
    protected ClassPath userLibraries = new ClassPath();
    protected ClassPath bootLibraries = new ClassPath();

    /** Don't let anyone else instantiate this class */
    private ClassMgr()
    {
        addConfigEntries(systemLibraries, syslibPrefix);

        addConfigEntries(userLibraries, userlibPrefix);

//        String syscp = System.getProperty("sun.boot.class.path");
        String syscp = Boot.get().getRuntimeClassPathString();
        String envcp = System.getProperty("java.class.path");

        if (syscp == null) {        // pre JDK1.2
            Debug.message(errormissingbootclasspath);
        } else {
            if (envcp == null) {    // no classpath
                Debug.message(errormissingclasspath);
            }
        }

        bootLibraries = new ClassPath(syscp, Config.getString("classmgr.bootclass"));

        /* XXX we should add here the boot libraries which are in the JDK extension
           directory */
        //System.getProperty("java.ext.dirs");

        /* The libraries which are in the java classpath environment variable should
           only be the bluej libraries needed to run the program. */

        if (envcp != null) {
            bootLibraries.addClassPath(envcp, Config.getString("classmgr.bluejclass"));
        }
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
            String location, description;

            while (true) {
                location = Config.getPropString(prefix + resourceID + ".location", null);
                description = Config.getPropString(prefix + resourceID + ".description", null);

                if (location == null || description == null)
                    break;

                cp.addClassPath(location, description);

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
        String r1, r2;
        int resourceID = 1;

        while(true) {
            r1 = Config.removeProperty(userlibPrefix + resourceID + ".location");
            r2 = Config.removeProperty(userlibPrefix + resourceID + ".description");

            if((r1 == null) || (r2 == null))
                break;

            resourceID++;
        }

        Iterator it = userLibraries.getEntries().iterator();
        resourceID = 1;

        while (it.hasNext()) {
            ClassPathEntry nextEntry = (ClassPathEntry)it.next();

            Config.putPropString(userlibPrefix + resourceID + ".location",
                                    nextEntry.getPath());
            Config.putPropString(userlibPrefix + resourceID + ".description",
                                    nextEntry.getDescription());

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
            // Debug.message("classmgrloader: finding " + name);

            byte[] bytes = loadClassData(name);
            if(bytes != null) {
                // Debug.message("classmgrloader: succeeded " + name);
                return defineClass(name, bytes, 0, bytes.length);
            }
            else {
                // Debug.message("classmgrloader: failed " + name);
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

