package bluej.browser;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*; // ActionListener, etc.
import java.util.ArrayList;
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
 * @version $Id: ClassPathEntryNode.java 264 1999-10-31 06:24:52Z ajp $
 */
public class ClassPathEntryNode extends LibraryChooserNode {

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
