package bluej.browser;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;

/**
 * A DefaultTreeCellRenderer subclass used to render different
 * subclasses of LibraryNode differently to allow visual differentiation.<p>
 * 
 * <ul>
 * <li> the root node of the tree is rendered uniquely,
 * <li> as are the top level user library or system library branch nodes 
 * <li> all lower level nodes are rendered identically
 * </ul>
 * 
 * @author $Author: mik $
 * @version $Id: LibraryTreeCellRenderer.java 36 1999-04-27 04:04:54Z mik $
 */
public class LibraryTreeCellRenderer extends JLabel implements TreeCellRenderer {
    private LibraryNode node = null;
    private LibraryChooser source = null;

    private static final Font ROOTFONT = new Font("Helvetica", Font.BOLD, 14);
    private static final Font DEFAULTFONT = new Font("Helvetica", Font.PLAIN, 10);
    private static final Font TOPLEVELBRANCHFONT = new Font("Helvetica", Font.BOLD, 12);
    private static final Color SYSLIBCOLOR = new Color(3289805);	
    private static final Color USERLIBCOLOR = new Color(32767);
			
    /**
     * Create a new renderer.  Store the source as an attribute for later access.  Set the
     * opaqueness of the JLabel to tree to allow highlighting of a selected node to function.
     * 
     * @param source the LibraryChooser to use when determining if the node is the root node.
     */
    public LibraryTreeCellRenderer(LibraryChooser source) 
    {
	super("                       ");
	this.source = source;
	this.setOpaque(true);
    }

    /**
     * Creates the rendered node.  This method is called for each node prior to
     * rendering and should return the rendered node Component.  Renders the root
     * node differently than all others.  
     * 
     * @return the rendered node.
     * @param tree the tree containing the node.
     * @param value the node to render.
     * @param selected true if the node is currently selected.
     * @param expanded true if the node's children are visible.
     * @param leaf true if the node has no children.
     * @param row the row number of the node in the tree.
     * @param hasFocus true if the node is currently receiving input focus.
     */
    public Component getTreeCellRendererComponent(JTree tree,
						  Object value,
						  boolean sel,
						  boolean expanded,
						  boolean leaf,
						  int row,
						  boolean hasFocus) 
    {
		
		
	try {
	    node = (LibraryNode) value;
			
	    super.setText(node.getDisplayName());

	    // default values
	    super.setFont(DEFAULTFONT);
	    super.setForeground(Color.black);
	    super.setBackground(Color.white);

	    // Large text for the root node
	    if (source.isRoot(node)) {
		super.setFont(ROOTFONT);
		super.setForeground(Color.blue);
		return this;
	    }

	    // colour differentiate User and System libraries
	    if (node instanceof UserLibraryNode) {
		super.setForeground(USERLIBCOLOR);
		super.setFont(TOPLEVELBRANCHFONT);
	    } else if (node instanceof SystemLibraryNode) {
		super.setForeground(SYSLIBCOLOR);
		super.setFont(TOPLEVELBRANCHFONT);
	    }

	    // bold and red for currently selected node
	    if (sel) {
		super.setBackground(Color.yellow);
	    }
			
	} catch (ClassCastException cce) {
	    cce.printStackTrace();
	}
	return this;
    }
}
