package bluej.browser;

import java.io.File;

import bluej.classmgr.ClassPathEntry;

/**
 * DefaultMutableTreeNode subclass used to represent a single
 * package available in the library browser tree.  Each package
 * has a display version of it's name and its location
 * (whether that be a directory in the filesystem or the
 * location of a jar/zip file. Each package can also store a list
 * of all the classfiles contained within it.
 * 
 * @see LibraryChooser
 * @author Andrew Patterson
 * @version $Id: ClassPathEntryNode.java 1700 2003-03-13 03:34:20Z ajp $
 */
public class ClassPathEntryNode extends LibraryChooserNode
{
	private ClassPathEntry classpathentry;
	
	/**
     * Create a new LibraryNode from the specified classpath entry.
     * 
     * @param cpe the class path entry
     */
	public ClassPathEntryNode(ClassPathEntry cpe) {
	    super(cpe.getDescription());

        classpathentry = cpe;		
	}

	public boolean isJar() {
        return classpathentry.isJar();
    }
    
    public boolean isClassRoot() {
        return classpathentry.isClassRoot();
    }
    
    public File getFile() {
        return classpathentry.getFile();
    }
}
