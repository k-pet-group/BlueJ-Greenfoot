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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import bluej.utility.Utility;

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
 * @author  Damiano Bolla
 */
public final class BPClassLoader extends URLClassLoader
{
    private boolean loadsForJavaMEproject;
    
    //We store the Java ME libraries in fields of the class loader--even though
    //these libraries are in the search path of URLs of the loader--because
    //we need them in the compiler's bootclasspath and in the classpath of the
    //preverify command.
    private List<URL> javaMEcoreLibs;  // Java ME core libraries
    private List<URL> javaMEoptLibs;   // Java ME optional libraries
    
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
    public String getClassPathAsString()
    {
        return Utility.toClasspathString(getClassPathAsFiles());
    }

    /**
     * Return the class path as an array of files.
     * Note that there is no guarantee that all files are indeed local files althout this is true for the
     * current BlueJ.
     * @return a non null array of Files, may be empty if no library at all is defined.
     */
    public final File[] getClassPathAsFiles()
    {
        return Utility.urlsToFiles(getURLs());
    }

    public String toString()
    {
        return "BPClassLoader path=" + getClassPathAsString();
    }
    
    public void setJavaMEcoreLibs( List<URL> list ) { javaMEcoreLibs = list; }
    public void setJavaMEoptLibs ( List<URL> list ) { javaMEoptLibs  = list; }
    
    public List<URL> getJavaMEcoreLibs( ) { return javaMEcoreLibs; }
    public List<URL> getJavaMEoptLibs ( ) { return javaMEoptLibs;  }
    
    /**
     * Concatenates the Java ME libraries, both core and optional, into a single
     * array of Files.
     * @return all the Java ME libraries as an array of Files
     */
    public File [] getJavaMElibsAsFiles()
    {
        ArrayList<URL> urls = new ArrayList<URL>(javaMEcoreLibs);
        urls.addAll(javaMEoptLibs);
        return Utility.urlsToFiles(urls.toArray(new URL[urls.size()]));
    }

    /**
     * Concatenates the Java ME libraries, both core and optional, into a single
     * colon/semicolon-separated String.
     * @return all the Java ME libraries in a String
     */
    public String getJavaMElibsAsPath( ) 
    {
        return Utility.toClasspathString(getJavaMElibsAsFiles());
    }
}
