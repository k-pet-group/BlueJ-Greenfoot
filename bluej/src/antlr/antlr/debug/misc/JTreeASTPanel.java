package antlr.debug.misc;

import java.awt.*;
import com.sun.java.swing.*;
import com.sun.java.swing.tree.*;
import com.sun.java.swing.event.*;

public class JTreeASTPanel extends JPanel {
  JTree tree;
  
  public JTreeASTPanel(TreeModel tm, TreeSelectionListener listener) {
	// use a layout that will stretch tree to panel size
	setLayout(new BorderLayout());

	// Create tree
	tree = new JTree(tm);

	// Change line style
	tree.putClientProperty("JTree.lineStyle", "Angled");

	// Add TreeSelectionListener
	if (listener != null)
	  tree.addTreeSelectionListener (listener);

	// Put tree in a scrollable pane's viewport
	JScrollPane sp = new JScrollPane();
	sp.getViewport().add(tree);

	add(sp, BorderLayout.CENTER);
  }        
}
