package bluej.browser;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*; // ActionListener, etc.
import java.util.ArrayList;

/**
 * DefaultMutableTreeNode subclass used to represent a node
 * in the LibraryChooser panel. Each stores a list
 * of all the classfiles contained within it.
 * 
 * @see LibraryChooser
 * @author Andrew Patterson
 * @version $Id: LibraryChooserNode.java 265 1999-11-05 04:31:07Z ajp $
 */
public class LibraryChooserNode extends DefaultMutableTreeNode {

	private TreePath myPath;
	private ArrayList files = new ArrayList();
	
	/**
     * Create a new LibraryChooserNode.
     */
	public LibraryChooserNode(String label) {
        super(label);

        myPath = new TreePath(this.getPath());
	}

	/**
	 * Return all the files which are children to this node.
	 * 
	 * @return an array of all the class files within this package
	 */
    public Object[] getFiles() {
        if (this.files == null || this.files.size() == 0)
            return null;
		
        return files.toArray();
	}
	
	/**
	 * Add a new class file to this packages list of class files.
	 * 
	 * @param classfile the new class file to add
	 */
    public void addFile(String classfile) {
        files.add(classfile);
    }
}
