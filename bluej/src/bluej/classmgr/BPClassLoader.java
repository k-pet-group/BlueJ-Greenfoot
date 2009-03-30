/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.classmgr;

import bluej.utility.Debug;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;

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
 * @version    $Id: BPClassLoader.java 6215 2009-03-30 13:28:25Z polle $
 * @author  Damiano Bolla
 */

public final class BPClassLoader extends URLClassLoader
{
    private boolean loadsForJavaMEproject;
    
    //We store the Java ME libraries in fields of the class loader--even though
    //these libraries are in the search path of URLs of the loader--because
    //we need them in the compiler's bootclasspath and in the classpath of the
    //preverify command.
    private List javaMEcoreLibs;  // Java ME core libraries
    private List javaMEoptLibs;   // Java ME optional libraries  
    
    /**
     * Constructructor.
     * @param parent the parent loader that is searched first to resolve classes.
     * @param urls the list of jars and directory that are searched next.
     */
    public BPClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, false);
    }  
 
    /**
     * Constructor.
     * @param parent   the parent loader that is searched first to resolve classes.
     * @param urls     the list of jars and directory that are searched next.
     * @param meFlag   whether this classloader is for a Java ME project
     */
    public BPClassLoader( URL[] urls, ClassLoader parent, boolean meFlag ) {
        super( urls, parent );
        loadsForJavaMEproject = meFlag;        
    }      
    
    /**
     * Returns flag indicating whether this class loads for a JavaME project.
     */
    public boolean loadsForJavaMEproject( ) {
        return loadsForJavaMEproject;
    }    

    /**
     * Compare the current array of URLS with the given one.
     * Note that is the order of the array is different then the two are considered different.
     * @param compare URLs to compare with the original.
     * @return true if the two arrays are the same.
     */
    public final boolean sameUrls(URL[] compare) {
        URL[] original = getURLs();

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
        return toClasspathString(getClassPathAsFiles());
    }

    /**
     * Convert an array of files into a classpath string that can be used to start a VM.
     * If files is null or files is empty then an empty string is returned.
     * @param files an array of files.
     * @return a non null string, possibly empty.
     */
    public static final String toClasspathString(File[] files) {
        if ((files == null) || (files.length < 1)) {
            return "";
        }

        boolean addSeparator = false; // Do not add a separator at the beginning
        StringBuffer buf = new StringBuffer();

        for (int index = 0; index < files.length; index++) {
            File file = files[index];

            // It may happen that one entry is null, strange, but just skip it.
            if (file == null) {
                continue;
            }

            if (addSeparator) {
                buf.append(File.pathSeparatorChar);
            }

            buf.append(file.toString());

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
    public final File[] getClassPathAsFiles() {
        return toFiles(super.getURLs());
    }

    /**
     * Transform an array of URL into an array of files.
     * Note that if the given URL points to a "non file" then the result conversion will be null.
     * @param urls an array or URL
     * @return a non null array of Files, may be empty if urls is null or empty.
     */
    public static final File[] toFiles(URL[] urls) {
        if ((urls == null) || (urls.length < 1)) {
            return new File[0];
        }

        File[] risul = new File[urls.length];

        for (int index = 0; index < urls.length; index++) {
            URL url = urls[index];

            // A class path is always without the qualifier file in front of it.
            // However some characters (such as space) are encoded.
            try {
                risul[index] = new File(new URI(url.toString()));
            } catch (URISyntaxException use) {
                // Should never happend. If there is a problem with the conversion we want to know about it.
                Debug.reportError("BPClassLoader.toFiles(urls) invalid url=" + url.getPath());
            }
        }

        return risul;
    }

    public String toString() {
        return "BPClassLoader path=" + getClassPathAsString();
    }
    
    public void setJavaMEcoreLibs( List list ) { javaMEcoreLibs = list; }
    public void setJavaMEoptLibs ( List list ) { javaMEoptLibs  = list; }   
    
    public List getJavaMEcoreLibs( ) { return javaMEcoreLibs; }
    public List getJavaMEoptLibs ( ) { return javaMEoptLibs;  }  
    
    /**
     * Concatenates the Java ME libraries, both core and optional, into a single
     * colon/semicolon-separated String.
     * @return all the Java ME libraries in a String
     */
    public String getJavaMElibsAsPath( ) 
    {
        StringBuffer risul = new StringBuffer();

        Iterator iter = getJavaMEcoreLibs( ).iterator( );
        while ( iter.hasNext( ) ) 
            risul.append((String) iter.next( )).append(File.pathSeparatorChar);
        
        iter = getJavaMEoptLibs( ).iterator( );
        while ( iter.hasNext( ) ) 
            risul.append((String) iter.next( )).append(File.pathSeparatorChar);
        
        if ( risul.length() > 0 ) //Remove trailing separator.
            risul.delete( risul.length( ) - 1, risul.length() );
        
        return risul.toString();
    }
}
