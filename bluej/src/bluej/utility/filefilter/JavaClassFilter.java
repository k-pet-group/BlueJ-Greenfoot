package bluej.utility.filefilter;

import java.io.FileFilter;
import java.io.File;

 /**
  * A FileFilter that only accepts Java class files.
  * An instance of this class can be used as a parameter for
  * the listFiles method of class File.
  *
  * @version $ $
  * @author Axel Schmolitzky
  * @see java.io.FileFilter
  * @see java.io.File
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
