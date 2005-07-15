package bluej.extensions;

import bluej.classmgr.ClassPathEntry;

import java.io.File;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.Iterator;


/**
 * A URLClassLoader that can be used to load or obtain information about classes loadable in a BProject.
 * author: Damiano Bolla
 * 
 * @version    $Id: BClassLoader.java 3466 2005-07-15 09:11:13Z damiano $
 */
public class BClassLoader extends URLClassLoader {
    public BClassLoader(URL[] urls) {
        super(urls);
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
