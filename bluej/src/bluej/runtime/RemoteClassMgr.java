package bluej.runtime;

import bluej.utility.Debug;
import bluej.Config;
import bluej.classmgr.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;

import javax.swing.table.*;

/**
 * Class to maintain a global classpath environment.
 *
 * @author  Andrew Patterson
 * @version $Id: RemoteClassMgr.java 309 1999-12-10 05:46:35Z ajp $
 */
public class RemoteClassMgr
{
    /**
     *
     */
    private ClassPath otherLibraries = new ClassPath();

    public RemoteClassMgr() {

    }

    public ClassLoader getLoader(String classpathstr)
    {
        return new ClassPathLoader(new ClassPath(classpathstr,""));
    }

    public ClassLoader getLoader()
    {
        return new ClassPathLoader(new ClassPath());
    }

    public void setLibraries(String libraries)
    {
        otherLibraries = new ClassPath(libraries, "");
    }

    class ClassPathLoader extends URLClassLoader
    {
        ClassPathLoader(ClassPath classpath)
        {
            super(classpath.getURLs());

            URL otherURLs[] = otherLibraries.getURLs();

            for(int i=0; i<otherURLs.length; i++)
                addURL(otherURLs[i]);
        }
    }
}
