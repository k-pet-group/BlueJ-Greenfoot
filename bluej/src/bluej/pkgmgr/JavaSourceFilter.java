package bluej.pkgmgr;

import java.io.FileFilter;
import java.io.File;

 /**
  * A FileFilter that only accepts Java source files.
  * An instance of this class can be used as a parameter for
  * the listFiles method of class File.
  *
  * @version $ $
  * @author Axel Schmolitzky
  * @see java.io.FileFilter
  * @see java.io.File
  *
  */
public class JavaSourceFilter implements FileFilter {

    /**
     * This method only accepts files that are Java source files.
     * Whether a file is a Java source file is determined by the fact that
     * its filename ends with ".java".
     */
    public boolean accept(File pathname) {
	
	return pathname.getName().endsWith(".java");

    }
}
