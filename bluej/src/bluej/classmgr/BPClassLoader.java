package bluej.classmgr;

import java.io.File;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import javax.swing.*;


/**
 * A Bluej Project ClassLoader that can be used to load or obtain information about classes loadable in a bluej project.
 * Different projects have different class loaders since they shoule each have a well defined and unique namespace.
 * Every time a project is compiled, even when the compilation is started from the GUI, a new ProjectLoader is created and
 * if the Extension currently have a copy of the old one it should discard it.
 * Note: There is a name clash with ProjectClassLoader that should be deleted at the end of refactornig,
 * unfortunately ProjectClassLoader has different semantic and it would be unvise to break the current behaviour before
 * having a correct working version. This is the reason for this class being named BPClassLoader.
 * it will be renamed when the new classloading is refactored and tested.
 *
 * @version    $Id: BPClassLoader.java 3480 2005-07-27 18:47:08Z damiano $
 */

/*
 * Author: Damiano Bolla
 */
public final class BPClassLoader extends URLClassLoader {
    /**
     * Constructructor.
     * @param parent the parent loader that is searched first to resolve classes.
     * @param urls the list of jars and directory that are searched next.
     */
    public BPClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }


    /**
     * Compare the current array of URLS with the given one.
     * Note that is the order of the array is different then the two are considered different.
     * @param compare URLs to compare with the original.
     * @return true if the two arrays are the same.
     */
    public final boolean sameUrls( URL[] compare ) {
        URL [] original = getURLs();
        
        if (original == null) {
            return false;
        }

        if (original.length != compare.length) {
            return false;
        }

        for (int index = 0; index < original.length; index++)
            if (!original[index].equals(compare[index])) {
                return false;
            }

        return true;
    }


    /**
     * Create a string with this class path as a separated list of strings.
     * Note that a classpath to be used to start another local JVM cannot refer to a URL but to a local file.
     * It is therefore advisable to move as much as possible from a Classpath oriented vew to a ClassLoader.
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

            URL url = urls[index];

            // A class path is always without the qualifier file in front of it.
            // However some characters (such as space) are encoded.
            try {
                buf.append(URLDecoder.decode(url.getPath(), "UTF-8"));
            } catch (UnsupportedEncodingException uee) {
                // Should never happend.  If there is a problem with the conversion we want to know about it.
                JOptionPane.showMessageDialog(null,"BPClassLoader.getClassPathAsString() invalid url="+url.getPath());
            }

            // From now on, you have to add a separator.
            addSeparator = true;
        }

        return buf.toString();
    }


    /**
     * Return the class path as an array of files.
     * Note that there is no guarantee that all files are indeed local files althout this is true for the
     * current BlueJ.
     * @return a non null array of Files, may be empty if no library at all is defined.
     */
    public File [] getClassPathAsFiles ()
    {
        URL[] urls = super.getURLs();

        if ((urls == null) || (urls.length < 1)) {
            return new File[0];
        }

        File [] risul = new File[urls.length];
        
        for (int index = 0; index < urls.length; index++) {

            URL url = urls[index];

            // A class path is always without the qualifier file in front of it.
            // However some characters (such as space) are encoded.
            try {
                risul[index] = new File((URLDecoder.decode(url.getPath(), "UTF-8")));
            } catch (UnsupportedEncodingException uee) {
                // Should never happend. If there is a problem with the conversion we want to know about it.
                JOptionPane.showMessageDialog(null,"BPClassLoader.getClassPathAsFiles() invalid url="+url.getPath());
            }
        }

        return risul;
    }
    
    
    public String toString() {
        return "BPClassLoader path=" + getClassPathAsString();
    }
}
