package bluej.browser;

import bluej.Config;
import bluej.views.MemberView;

import javax.swing.tree.*; // JLabel, etc.
import javax.swing.*; // JLabel, etc.
import java.awt.*; // Component, etc.
import java.lang.reflect.Modifier;

/**
 * A simple TreeCellRenderer implementer for colour-coding entries in
 * the AttributeChooser based on access and type (i.e., field or method).
 * Uses a callback to a AttributeChooser to determine whether the root 
 * node is being rendered or not.  Renders the tree cells as JLabels
 * containing text and an associated icon.
 * 
 * @author $Author: mik $
 * @version $Id: AttributeChooserRenderer.java 36 1999-04-27 04:04:54Z mik $
 */
public class AttributeChooserRenderer extends JLabel implements TreeCellRenderer {
    private String className = "";
    private AttributeChooser source = null;
		
    private static final Font ROOTFONT = new Font("Helvetica", Font.BOLD, 16);
    private static final Font DEFAULTFONT = new Font("Helvetica", Font.PLAIN, 12);

    private static final ImageIcon CONSTRUCTOR_ICON = new ImageIcon(Config.getImageFilename("browser.attributechooser.constructoricon.image"));
    private static final ImageIcon PRIVATE_ICON = new ImageIcon(Config.getImageFilename("browser.attributechooser.privateicon.image"));
    private static final ImageIcon PUBLIC_ICON = new ImageIcon(Config.getImageFilename("browser.attributechooser.publicicon.image"));
    private static final ImageIcon PROTECTED_ICON = new ImageIcon(Config.getImageFilename("browser.attributechooser.protectedicon.image"));

    /**
     * Create a new renderer.  Store the source as an attribute for later access.  Set the
     * opaqueness of the JLabel to tree to allow highlighting of a selected node to function.
     * 
     * @param source the AttributeChooser to use when determining if the node is the root node.
     */
    public AttributeChooserRenderer(AttributeChooser source) {
	this.source = source;
	this.setOpaque(true);
    }
		
    /**
     * Creates the rendered node.  This method is called for each node prior to
     * rendering and should return the rendered node Component.  Renders the root
     * node differently than all others.  Set the icon appropriate to the access of
     * the MemberView object contained within the node, or null if the node has no 
     * MemberView object.
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
						  boolean selected,
						  boolean expanded,
						  boolean leaf,
						  int row,
						  boolean hasFocus) {
		
	if (source.isRoot(value)) {
	    // special formatting for the root node
	    this.setFont(ROOTFONT);
	    this.setIcon(null);
	    this.setForeground(Color.blue);
	    this.setText(value.toString());
	    this.setBackground(Color.white);
	} else {
	    this.setForeground(Color.black);
	    this.setBackground(selected ? Color.yellow : Color.white);
	    this.setFont(DEFAULTFONT);
			
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
	    MemberView view = null;
	    try {
		view = (MemberView)node.getUserObject();
	    } catch (ClassCastException cce) {
				// the "inherited" tree node does not have a MemberView
				// as it's user object, so it will throw a ClassCastException
				// when rendered.
		this.setIcon(null);
		this.setText(value.toString());
		return this;
	    }
			
	    this.setText(view.getShortDesc());
	    int modifiers = view.getModifiers();
	    if (Modifier.isPublic(modifiers))
		setIcon(PUBLIC_ICON);
	    else if (Modifier.isPrivate(modifiers))
		setIcon(PRIVATE_ICON);
	    else if (Modifier.isProtected(modifiers))
		setIcon(PROTECTED_ICON);
	    else
		setIcon(CONSTRUCTOR_ICON);
				
					
	}		
	return this;
    }
}
