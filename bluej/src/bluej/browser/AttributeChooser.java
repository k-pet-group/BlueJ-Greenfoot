package bluej.browser;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

import bluej.views.*;
import bluej.utility.Utility;
import bluej.Config;
import bluej.classmgr.ClassMgr;

import java.lang.reflect.*;
import java.util.*;

/**
 * A JPanel subclass containing a JTree displaying all the attributes of a
 * class or interface.  Visually similar to the Visual J++ ClassOutline pane,
 * the AttributeChooser categorizes attributes of the class/interface according
 * to access (public, private or protected) and type (field/method or constructor).
 * Uses an AttributeChooserRenderer (private class declared below) to handle the
 * visual categorization.
 *
 * @see AttributeChooserRenderer
 * @see AttributeThread
 * @author Andy Marks
 * @author Andrew Patterson
 * @version $Id: AttributeChooser.java 853 2001-04-19 04:24:26Z ajp $
 */
public class AttributeChooser extends JPanel {

	private String currentClass;

	/**
	 * Create a new AttributeChooser to display the attributes of a class.
	 */
	public AttributeChooser() {
		this.setLayout(new BorderLayout());
	}

	/**
	 * Open a new class.  Call this method whenever you wish to update the
	 * attribute chooser to display the attributes of a new class or interface.
	 * This method finds all the attributes of the class, starts the thread to
	 * load the tree with them and redisplays the tree.  Will not open class
	 * if it is the currently open one.
	 *
	 * @param className the name of the attributes' class specified in
	 *                  class notation (i.e., java.io.File)
	 */
	public void openClass(String className) {
		// don't open current one
		if (className == currentClass)
			return;

		AttributeThread athread = new AttributeThread(this, className);

		athread.start();

		currentClass = className;
	}

	protected synchronized void addAttributeWindow(JScrollPane pane)
	{
		removeAll();
		add(pane, BorderLayout.CENTER);

		invalidate();
		validate();
	}
}

class AttributeThread extends Thread {

	AttributeChooser parent;
	String className;

	public AttributeThread(AttributeChooser parent, String className)
	{
		this.parent = parent;
		this.className = className;
	}

	/**
	 * Used by class loading thread.  Uses a View object to identify the attributes of
	 * the class.  Controls the loading of the tree with the attributes and refreshes the
	 * UI to show the new class' attributes.
	 */
	public void run() {

		DefaultMutableTreeNode root = new DefaultMutableTreeNode();

		JTree attributes = new JTree(root);
		attributes.getSelectionModel().setSelectionMode (TreeSelectionModel.SINGLE_TREE_SELECTION);
		attributes.setCellRenderer(new AttributeChooserRenderer());

/*		treeModel = (DefaultTreeModel)attributes.getModel();
		} else {
			inherited.removeAllChildren();
			root.removeAllChildren();
		}

		DefaultMutableTreeNode inherited = new DefaultMutableTreeNode("Inherited");
		setupTree();
*/
		try {
			Class cl = ClassMgr.loadBlueJClass(className);

			if (cl == null) {
				root.setUserObject(getCantLoadMessage(className));
			} else {
				View classView = View.getView(cl);

				root.setUserObject((classView.isInterface() ? "Interface " : "Class ") + className);

				addAttributes(root, classView);

				Enumeration topnodes = root.children();
				while (topnodes.hasMoreElements())
					attributes.expandPath(new TreePath(((DefaultMutableTreeNode)topnodes.nextElement()).getPath()));
			}
		} catch (ClassNotFoundException cnfe) {

			root.setUserObject(getCantLoadMessage(className));

		} catch (ClassFormatError cfe) {

			root.setUserObject(getCantLoadMessage(className));
		}

		parent.addAttributeWindow(new JScrollPane(attributes));
	}

	/*
	 * Return a string explaining that a class could not be loaded.
     *
     * @param className the name of the class which failed to load
	 */
	private String getCantLoadMessage(String className)
	{
		return Utility.mergeStrings(Config.getString("browser.missingclass.text"),
							className);
	}

	/**
	 * Determine if a member of the class was originally declared in that class,
	 * or inherited from a parent class.
	 *
	 * @param member the member of the class.
	 * @return true if the member was originally declared in this class.
	 */
	private boolean isMemberDeclaredInThisClass(MemberView member) {
		return member.getDeclaringView().getQualifiedName().equals(className);
	}

	/**
	 * Allocate the constructors, fields and methods of the current class
	 * to the corresponding top level node in the tree, based on their
	 * access (i.e., public, private, protected and package).
	 */
	public void addAttributes(DefaultMutableTreeNode topnode, View classView)
	{
		String memberName = null;

		// first the constructors...
		DefaultMutableTreeNode constructors = new DefaultMutableTreeNode("Constructors");
		{
			ConstructorView[] constructorsview = classView.getConstructors();

			if (constructorsview.length > 0) {
				for (int memberIdx = 0; memberIdx < constructorsview.length; memberIdx++) {
					constructors.add(new DefaultMutableTreeNode(constructorsview[memberIdx]));
				}
				topnode.add(constructors);
			}
		}

		// then the fields...
		DefaultMutableTreeNode fields = new DefaultMutableTreeNode("Fields");
		{
			FieldView[] fieldsview = classView.getAllFields();

			for (int memberIdx = 0; memberIdx < fieldsview.length; memberIdx++) {
				if (isMemberDeclaredInThisClass(fieldsview[memberIdx]))
					fields.add(new DefaultMutableTreeNode(fieldsview[memberIdx]));
			}

			if (fields.getChildCount() > 0) {
				topnode.add(fields);
			}
		}

		// then the methods...
		DefaultMutableTreeNode methods = new DefaultMutableTreeNode("Methods");
		{
			MethodView[] methodsview = classView.getAllMethods();

			for (int memberIdx = 0; memberIdx < methodsview.length; memberIdx++) {
				if (isMemberDeclaredInThisClass(methodsview[memberIdx]))
					methods.add(new DefaultMutableTreeNode(methodsview[memberIdx]));
			}

			if (methods.getChildCount() > 0) {
				topnode.add(methods);
			}
		}
	}
}

/**
 * A simple TreeCellRenderer implementer for colour-coding entries in
 * the AttributeChooser based on access and type (i.e., field or method).
 * Renders the tree cells as JLabels containing HTML.
 *
 * @author Andy Marks
 * @author Andrew Patterson
 */
class AttributeChooserRenderer extends JLabel implements TreeCellRenderer {

	private static final Font fontTreeRoot =
		new Font(Config.getPropString("browser.fontname.treeroot"), Font.PLAIN,
			 Config.getPropInteger("browser.fontsize.treeroot", 12));

	private static final Font fontTreeLeaf =
		new Font(Config.getPropString("browser.fontname.treeleaf"), Font.PLAIN,
			 Config.getPropInteger("browser.fontsize.treeleaf", 12));

	private static final ImageIcon CONSTRUCTOR_ICON =
		Config.getImageAsIcon("browser.image.constructoricon");
	private static final ImageIcon PRIVATE_ICON =
		Config.getImageAsIcon("browser.image.privateicon");
	private static final ImageIcon PUBLIC_ICON =
		Config.getImageAsIcon("browser.image.publicicon");
	private static final ImageIcon PROTECTED_ICON =
		Config.getImageAsIcon("browser.image.protectedicon");

	/**
	 * Create a new renderer.  Set the
	 * opaqueness of the JLabel to tree to allow highlighting of a selected node to function.
	 */
	public AttributeChooserRenderer() {
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
							boolean hasFocus)
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
		MemberView view = null;

		try {
			view = (MemberView)node.getUserObject();
		}
		catch (ClassCastException cce) {
		}

		if (view == null) {
			setFont(fontTreeRoot);
	 		setForeground(Color.blue);
			setIcon(null);
			setText(node.toString());
		} else {
			setFont(fontTreeLeaf);
			setForeground(Color.black);
			setBackground(Color.white);

			String desc = view.getShortDesc();
			StringBuffer descbuf = new StringBuffer(desc);

			int modifiers = view.getModifiers();

			String mod = Modifier.toString(modifiers);

			setIcon(null);

			int firstbracket = desc.indexOf('(');

			if (firstbracket != -1) {

				String nameHTMLend = "</B>";
				String nameHTMLstart = "<B>";
				String paramHTMLend = "</I>";
				String paramHTMLstart = "<I>";

				int lastbracket = desc.indexOf(')');

				descbuf.insert(lastbracket, paramHTMLend);

				descbuf.insert(firstbracket + 1, paramHTMLstart);
				descbuf.insert(firstbracket, nameHTMLend);

				String startbit = descbuf.substring(0,firstbracket);

				int lastspace = startbit.lastIndexOf(' ');

				if(lastspace != -1) {
					descbuf.insert(lastspace, paramHTMLend + nameHTMLstart);
					descbuf.insert(0, paramHTMLstart);
				}
				else {
					descbuf.insert(0, nameHTMLstart);
				}
			}

			setText("<html><CODE>" + mod + " " + descbuf + "</CODE>");
		}

		return this;
	}
}
