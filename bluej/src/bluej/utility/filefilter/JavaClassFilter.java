package bluej.utility.filefilter;

import java.io.*;

/**
 * A FileFilter that only accepts Java class files.
 * An instance of this class can be used as a parameter for
 * the listFiles method of class File.
 *
 * @author  Axel Schmolitzky
 * @version $Id: JavaClassFilter.java 1700 2003-03-13 03:34:20Z ajp $
 */
public class JavaClassFilter implements FileFilter
{
    /**
     * This method only accepts files that are Java class files.
     * Whether a file is a Java class file is determined by the fact that
     * its filename ends with ".class".
     */
    public boolean accept(File pathname)
    {
        return pathname.getName().endsWith(".class");
    }
}
