package bluej.browser;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*; // ActionListener, etc.
import java.util.Vector;

/**
 * DefaultMutableTreeNode subclass used to represent a single
 * package available in the library browser tree.  Each package
 * has a display version of it's name, the directory where it
 * lives in the filesystem, and an option shadowArea if the 
 * directory is not writable.  Each package can also store a list
 * of all the classfiles contained within it.
 * 
 * @see LibraryChooser
 * @author $Author: mik $
 * @version $Id: LibraryNode.java 36 1999-04-27 04:04:54Z mik $
 */
public class LibraryNode extends DefaultMutableTreeNode {
	private String displayName = "";
	private String internalName = "";
	private String shadowArea = "";
	private boolean archiveFile = false;
	private TreePath myPath = null;
	private Vector files = new Vector();
	
	/**
	 * Create a new LibraryNode with the specified displayName,
	 * internalName and shadowArea.
	 * 
	 * @param displayName the display version of the nodes name
	 * @param internalName the directory containing the package
	 * @param shadowArea the directory where PKG files for this package can be written.
	 */
	public LibraryNode(String displayName, String internalName, String shadowArea) {
		super(displayName);
		this.displayName = displayName;
		this.internalName = internalName;
		this.shadowArea = shadowArea;
		
		myPath = new TreePath(this.getPath());
	}

	/**
	 * Create a new LibraryNode with the specified displayName,
	 * and internalName.
	 * 
	 * @param displayName the display version of the nodes name
	 * @param internalName the directory containing the package
	 */
	public LibraryNode(String displayName, String internalName) {
		this(displayName, internalName, null);
	}
	
	/**
	 * Create a new LibraryNode with the specified name.
	 * 
	 * @param name the name of this package.
	 */
	public LibraryNode(String name) {
		this(name, name);
	}

	/**
	 * Return all the class files which are children to this node.
	 * 
	 * @return an array of all the class files within this package.
	 */
	public LibraryNode[] getFiles() {
		if (this.files == null || this.files.size() == 0)
			return null;
		
		LibraryNode filesArray[] = new LibraryNode[this.files.size()];
		files.copyInto(filesArray);
		return filesArray;
	}
	
	/**
	 * Add a new class file to this packages list of class files.
	 * 
	 * @param newChild the new class file to add.
	 */
	public void addFile(LibraryNode newChild) {
		files.addElement(newChild);
	}
	
	/**
	 * Change the value of the displayName for this package.
	 * 
	 * @param displayName the new value for the displayName.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	/**
	 * Change the value of the archiveFile flag for this package.
	 * 
	 * @param archiveFile true if this package is an archive file.
	 */
	public void setArchiveFile(boolean archiveFile) {
		this.archiveFile = archiveFile;
	}
	
	/**
	 * Return true if this package has a shadow area.
	 * 
	 * @return true if this package has a shadow area.
	 */
	public boolean hasShadowArea() {
		return shadowArea != null;
	}
	
	/**
	 * Return true if this package is an archive file.
	 * 
	 * @return true if this package is an archive file.
	 */
	public boolean isArchiveFile() {
		return archiveFile;
	}
	
	/**
	 * Return the shadow area for this package.
	 * 
	 * @return the shadow area for this package.
	 */
	public String getShadowArea() {
		return shadowArea;
	}
	
	/**
	 * Change the value of the shadowArea for this package.
	 * 
	 * @param shadowArea the new shadowArea for this package.
	 */
	public void setShadowArea(String shadowArea) {
		this.shadowArea = shadowArea;
	}
	
	/**
	 * Return the internal name for this package.
	 * 
	 * @return the internal name for this package.
	 */
	public String getInternalName() {
		return internalName;
	}
	
	/**
	 * Return the display name for this package.
	 * 
	 * @return the display name for this package.
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	/**
	 * Return the user object for this package.
	 * 
	 * @return the user object for this package.
	 */
	public Object getUserObject() {
		return toString();
	}
	
	/**
	 * Return a String representation of this object.
	 * 
	 * @return a String representation of this object.
	 */
	public String toString() {
		return getDisplayName();
	}
}
