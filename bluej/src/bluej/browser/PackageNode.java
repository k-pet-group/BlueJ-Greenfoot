package bluej.browser;

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
 * @version $Id: PackageNode.java 1700 2003-03-13 03:34:20Z ajp $
 */
public class PackageNode extends LibraryChooserNode {

    private String packageName;
	
	/**
     * Create a new LibraryNode from the specified classpath entry.
     * 
     * @param cpe the class path entry
     */
	public PackageNode(String packageName) {
        super(packageName);

        this.packageName = packageName;
	}
}
