package bluej.browser;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

import bluej.views.View;
import bluej.views.MemberView;
import bluej.utility.ClasspathSearcher;
import bluej.utility.Utility;
import bluej.Config;
import bluej.pkgmgr.LibraryBrowserPkgMgrFrame;

import java.lang.reflect.*;

/**
 * A JPanel subclass containing a JTree displaying all the attributes of a
 * class or interface.  Visually similar to the Visual J++ ClassOutline pane,
 * the AttributeChooser categorizes attributes of the class/interface according
 * to access (public, private or protected) and type (field/method or constructor).
 * Uses an AttributeChooserRenderer to handle the visual categorization.
 * 
 * @see AttributeChooserRenderer
 * @author $Author: mik $
 * @version $Version$
 */
public class AttributeChooser extends JPanel implements Runnable {
	private JTree attributes = null;
	private DefaultTreeModel treeModel = null;
	private View classView = null;
	private LibraryBrowserPkgMgrFrame parent = null;
	private String currentClass = null;
	
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private DefaultMutableTreeNode inherited = new DefaultMutableTreeNode("Inherited");

	/**
	 * Create a new AttributeChooser.
	 * 
	 * @param parent the LibraryBrowserPkgMgrFrame object owning this object.
	 */
  public AttributeChooser(LibraryBrowserPkgMgrFrame parent) {
		this.parent = parent;
		this.setLayout(new BorderLayout());
  }
	
	private void setupTree() {
		this.setStatusText("Initializing attribute window...");
		
		// show the String version of the selected attribute
		attributes.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)(e.getPath().getLastPathComponent());
				setStatusText(node.getUserObject().toString());
      }
    });		
		
	}
	
	/**
	 * Open a new class.  Call this method whenever you wish to update the
	 * attribute chooser to display the attributes of a new class or interface.
	 * This method finds all the attributes of the class, starts the thread to 
	 * load the tree with them and redisplays the tree.  Will not open class 
	 * if it is the currently open one.
	 * 
	 * @param className the name of the attributes' class specified in class notation (i.e., a.b.c)
	 */
	public void openClass(String className) {
		// don't open current one
		if (className == currentClass)
			return;
		
		if (attributes == null) {
			attributes = new JTree(root);
			attributes.getSelectionModel().setSelectionMode (TreeSelectionModel.SINGLE_TREE_SELECTION);
			attributes.setCellRenderer(new AttributeChooserRenderer(this));
			treeModel = (DefaultTreeModel)attributes.getModel();
		} else {
			inherited.removeAllChildren();
			root.removeAllChildren();
		}
	
		setupTree();

		currentClass = className;
		new Thread(this).start();
	}

	/**
	 * Used by class loading thread.  Uses a View object to identify the attributes of
	 * the class.  Controls the loading of the tree with the attributes and refreshes the
	 * UI to show the new class' attributes.
	 */
	public void run() {
		try {
			classView = new View(Class.forName(currentClass), new ClasspathSearcher(System.getProperty("java.class.path")));
			root.setUserObject((classView.isInterface() ? "Interface " : "Class ") + currentClass);
	
			treeModel.reload();
			sortAttributes();

			openTreeToView();
			
		} catch (ClassNotFoundException cnfe) {
			Utility.showError(LibraryBrowserPkgMgrFrame.getFrame(),
					  Utility.mergeStrings(Config.getString("browser.attributechooser.missingclassdialog.text"),
							       currentClass));

			return;
		} catch (ClassFormatError cfe) {
			  cfe.printStackTrace();
		}
		
		JScrollPane scrollPane = new JScrollPane(attributes);
		add(scrollPane, BorderLayout.CENTER);
		invalidate();
		validate();
	}

	/**
	 * Make sure the tree is open at the right places when it first appears.
	 * This cannot be done in #setupDisplay because the tree is empty
	 * at that stage.
	 */
	private void openTreeToView() {
		attributes.expandRow(0);
	}
	
	/**
	 * Determine if a member of the class was originally declared in that class,
	 * or inherited from a parent class.
	 * 
	 * @param member the member of the class.
	 * @return true if the member was originally declared in this class.
	 */
	private boolean isMemberDeclaredInThisClass(MemberView member) {
		return member.getDeclaringView().getName().equals(currentClass);
	}
	
	/**
	 * Allocate the constructors, fields and methods of the current class
	 * to the corresponding top level node in the tree, based on their
	 * access (i.e., public, private, protected and package).
	 */
	public void sortAttributes () {
		MemberView currentMember = null;
		String memberName = null;
		int memberIdx = 0;

		// first the constructors...
		MemberView[] members = classView.getConstructors();
		members = classView.getConstructors();
		for (memberIdx = 0; memberIdx < members.length; memberIdx++) {
			currentMember = members[memberIdx];
			root.add(new DefaultMutableTreeNode(members[memberIdx]));
		}
		
		// then the fields...
		members = classView.getAllFields();
		for (memberIdx = 0; memberIdx < members.length; memberIdx++) {
			currentMember = members[memberIdx];
			if (isMemberDeclaredInThisClass(currentMember))
				root.add(new DefaultMutableTreeNode(currentMember));
			else
				inherited.add(new DefaultMutableTreeNode(currentMember));
		}

		// then the methods...
		members = classView.getAllMethods();
		for (memberIdx = 0; memberIdx < members.length; memberIdx++) {
			currentMember = members[memberIdx];
			if (isMemberDeclaredInThisClass(currentMember))
				root.add(new DefaultMutableTreeNode(currentMember));
			else
				inherited.add(new DefaultMutableTreeNode(currentMember));
		}
		
	}
	
	private void setStatusText(String statusText) {
		if (parent != null)
			parent.setStatus(statusText);
	}

	/**
	 * Determine if a node in the tree is the root node.  Called by the
	 * AttributeChooserRenderer to determine which node to render as the
	 * root node.
	 * 
	 * @param node the node to check.
	 * @return true if the node is the root node of the tree
	 */
	public boolean isRoot(Object node) {
		return node == root;
	}
}
