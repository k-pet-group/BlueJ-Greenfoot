package org.bluej.extensions.submitter;

import org.bluej.extensions.submitter.properties.*;

import bluej.extensions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.awt.Font;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;

/**
 * Manages the properties appropriate to the selected submission scheme.
 *
 * @author Clive Miller
 * @version $Id: SubmissionProperties.java 1498 2002-11-11 10:34:08Z damiano $
 **/

class SubmissionProperties
{
    public static final String PROPERTIES_FILENAME = "submission.defs";
    private static final String BJ_PROPERTY_SELECTED_NODE = "selectedNode";

    private final BlueJ bj;
    private final BPackage pkg;
    private Node rootNode;
    private DefaultTreeModel treeModel;
    private JTree tree;
    private String selectedScheme;
    
    SubmissionProperties (BlueJ bj, BPackage pkg)
    {
        this.pkg = pkg;
        this.bj = bj;
        rootNode = new Node ("Submissions", false);
        treeModel = new DefaultTreeModel (rootNode) {
            public synchronized void insertNodeInto (MutableTreeNode newChild, MutableTreeNode parent, int index) {
                index = getChildCount (parent); // Forces insert to go at the end of the parent's list
                super.insertNodeInto (newChild, parent, index);
            }
        };
        
        File systemConfFile = new File (bj.getSystemLib(), PROPERTIES_FILENAME);
        load (systemConfFile, false);

        File userConfFile = bj.getUserFile (PROPERTIES_FILENAME);
        load (userConfFile, false);
                
        tree = new JTree (treeModel);
        tree.getSelectionModel().setSelectionMode (javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible (false);
        tree.addTreeWillExpandListener (new TreeWillExpandListener() {
            public void treeWillCollapse (TreeExpansionEvent event) {}
            public void treeWillExpand (TreeExpansionEvent event) {
                Node node = (Node)event.getPath().getLastPathComponent();
                expandNode (node);
            }
        });
        tree.putClientProperty("JTree.lineStyle", "Angled");


        
    }
    
    public void addTreeModelListener (TreeModelListener tml)
    {
        treeModel.addTreeModelListener (tml);
    }
    
    void reload()
    {
        BProject prj = bj.getProject (pkg);
        for (Enumeration e = rootNode.children(); e.hasMoreElements(); ) { // Remove project nodes
            Node node = (Node)e.nextElement();
            if (node.isProject()) treeModel.removeNodeFromParent (node);
        }
        
        if (prj != null) {
            File projectConfFile = new File (prj.getProjectDir(), PROPERTIES_FILENAME);
            load (projectConfFile, true);
        }
// Hoping to be able to remove this line
//        treeModel.reload();
        
        Node proj = null;
        for (Enumeration e = rootNode.children(); e.hasMoreElements(); ) { // Any project nodes?
            Node node = (Node)e.nextElement();
            if (node.isProject()) {
                proj = node;
                break;
            }
        }
        if (proj != null) {
            TreePath path = new TreePath (proj.getPath());
            selectedScheme = getPathAsString (path);
        } else {
            selectedScheme = bj.getExtPropString (BJ_PROPERTY_SELECTED_NODE, "");
        }
    }
    
    private void load (File file, boolean isProject)
    {
        String filename = null;
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                 filename = file.getCanonicalPath();
                 fis = new FileInputStream (file);
                 new Parser (isProject, treeModel).parse (rootNode, fis);
                 fis.close();
            } catch (CompilationException cex) {
                if (filename != null) cex.addFilename (filename);
                JTextArea ta = new JTextArea (cex.toString());
                ta.setEditable (false);
                Font font = ta.getFont();
                ta.setFont (new Font ("Courier", font.getStyle(), font.getSize()));
                JScrollPane sp = new JScrollPane (ta);
                JOptionPane.showMessageDialog (pkg.getFrame(),sp,bj.getLabel ("message.conferror"),JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog (pkg.getFrame(),ex.toString(),bj.getLabel ("message.conferror"),JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public JTree getTree()
    {
        TreePath path = getPathFromString (selectedScheme);
        tree.setSelectionPath (path);
        tree.scrollPathToVisible (path);
        return tree;
    }

    public void setSchemeFromTree()
    {
        TreePath path = tree.getSelectionPath();
        selectedScheme = getPathAsString (path);
    }
    
    public void rememberSelected()
    {
        TreePath path = getPathFromString (selectedScheme);
        if (path == null) return; // Don't save invalid paths
        Node leaf = (Node)path.getLastPathComponent();
        if (leaf.isProject()) return; // Don't save a project path
        bj.setExtPropString (BJ_PROPERTY_SELECTED_NODE, selectedScheme);
    }
    
    private void expandNode (Node node)
    {
//        try {
            node.expand (treeModel);
/*        } catch (CompilationException cex) {
            JTextArea ta = new JTextArea (cex.toString());
            ta.setEditable (false);
            Font font = ta.getFont();
            ta.setFont (new Font ("Courier", font.getStyle(), font.getSize()));
            JScrollPane sp = new JScrollPane (ta);
            JOptionPane.showMessageDialog (pkg.getFrame(),sp,bj.getLabel ("message.conferror"),JOptionPane.ERROR_MESSAGE);
        }
*/    }
        
    public Collection getProps (String item) throws AbortOperationException
    {
        TreePath path = getPathFromString (selectedScheme);
        if (path == null) {
            throw new AbortOperationException (bj.getLabel("message.notascheme"));
        }
        Node selected = (Node)path.getLastPathComponent();
        return selected.getConfig (item);
    }

    /**
     * Is the currently selected scheme string valid?
     */
    public boolean isValidScheme()
    {
        TreePath path = getPathFromString (selectedScheme);
        return path != null && ((Node)path.getLastPathComponent()).isLeaf();
    }
    
    /**
     * Gets the currently selected scheme. This may not be valid
     * @return the free-form string of an apparent scheme. May not exist.
     */
    public String getSelectedScheme()
    {
        return selectedScheme;
    }
    
    /**
     * Gets the last element of the currently selected scheme. This may not be valid
     * @return the scheme beyond any final <code>/</code>
     */
    public String getSelectedSchemeSimple()
    {
        return selectedScheme.substring (selectedScheme.lastIndexOf ('/'));
    }
    
    /**
     * Sets the currently selected scheme. This could be any old junk
     */
    public void setSelectedScheme (String newScheme)
    {
        if (newScheme == null) newScheme = "";
        selectedScheme = newScheme;
    }
    

    public static void addSettings (BlueJ bj)
    {
    System.out.println ("TODO: delete it !");
    }
    
    public String getGlobalProp (String item)
    {
        return bj.getExtPropString(item,"");
    }
    

    public Properties getGlobalProps()
    {
    PrefPanel myPanel = (PrefPanel)bj.getPrefGen();
    return myPanel.getGlobalProps();
    }

    // A couple of useful utilities
    /**
     * Turns a TreePath into a String by getting the titles of each node
     * and separating them with a <code>/</code>
     * @param path the path to examine
     * @return a String representation of this path
     * @see getPathFromString(String)
     */
    private String getPathAsString (TreePath path)
    {
        Object[] objs = path.getPath();
        String pathString = "";
        for (int i=1; i<objs.length; i++) {
            Node n = (Node)objs[i];
            pathString += n.getTitle() + (i == objs.length-1 ? "" : "/");
        }
        return pathString;
    }
    
    /**
     * Parses the given string to construct a path. Nodes are
     * expanded as they are emcompassed in the path, but
     * may not be instantly available if it is necessary to follow
     * an <code>.insert</code> to get them. Because of this,
     * this method may have to
     * be called several times before a valid TreePath is returned.
     * @param pathString the path as a string of titles separated by <code>/</code>
     * @return the path represented by this string, if valid, or <code>null</code> if not valid.
     */
    private TreePath getPathFromString (String pathString)
    {
        StringTokenizer token = new StringTokenizer (pathString, "/");
        Node leaf = rootNode;
        ArrayList pathList = new ArrayList();
        pathList.add (leaf);
        while (token.hasMoreTokens() && !leaf.isLeaf()) {
            String nextNodeString = token.nextToken();
            expandNode (leaf);
            Node nextNode = null;
            for (Enumeration en = leaf.children(); en.hasMoreElements() ;) {
                Object n = en.nextElement();
                if (n.toString().equals (nextNodeString)) {
                    nextNode = (Node)n;
                    break;
                }
            }
            if (nextNode == null) return null;
            leaf = nextNode;
            pathList.add (leaf);
        }
        TreePath path = new TreePath (pathList.toArray());
        return path;
    }
}