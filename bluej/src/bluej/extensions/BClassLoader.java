package bluej.extensions;

import bluej.classmgr.ClassPathEntry;

import java.io.File;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.Iterator;


/**
 * A URLClassLoader that can be used to load or obtain information about classes loadable in a BProject.
 * Different BProject have different class loaders since they shoule each have a well defined and unique namespace.
 * Every time a BProject is compiled, even when the compilation is started from the GUI, a new BClassLoader is created and
 * if the Extension currently have a copy of the old one it should discard it.
 * 
 * @version    $Id: BClassLoader.java 3467 2005-07-15 13:26:18Z damiano $
 */

 /*
  * Author: Damiano Bolla
  */
public class BClassLoader extends URLClassLoader {

    /**
     * Constructructor.
     * @param parent the parent loader that is searched first to resolve classes.
     * @param urls the list of jars and directory that are searched next.
     */
    public BClassLoader(URL[] urls, ClassLoader parent) {
        super(urls,parent);
    }

    /**
     * Create a string with this class path as a separated list of strings.
     *
     * @return  The classpath as string.
     */
    public String getClassPathAsString() {
        URL[] urls = super.getURLs();

        if ((urls == null) || (urls.length < 1)) {
            return "";
        }

        boolean addSeparator = false; // Do not add a separator at the beginning
        StringBuffer buf = new StringBuffer();

        for (int index = 0; index < urls.length; index++) {
            if (addSeparator) {
                buf.append(File.pathSeparatorChar);
            }

            buf.append(urls[index]);

            // From now on, you have to add a separator.
            addSeparator = true;
        }

        return buf.toString();
    }
}
