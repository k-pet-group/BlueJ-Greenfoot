package org.bluej.extensions.submitter;

import org.bluej.extensions.submitter.properties.*;

import bluej.extensions.*;
import org.bluej.utility.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.io.*;
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
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

/**
 * Manages the properties appropriate to the selected submission scheme.
 *
 * @author     Clive Miller, Damiano Bolla
 * @version    $Id: SubmissionProperties.java 1625 2003-02-06 21:40:33Z iau $
 */

class SubmissionProperties
{
    // Yes, I know, it should really be named submission.conf... Damiano (History is hard to forget)
    private final static String CONFIG_FILENAME = "submission.defs";
    private final static String SELECTED_NODE_PROPERTY = "selectedNode";
    private final static String PROPERTIES_FILENAME = "submitter.properties";
    private final static String ROOT_NODENAME = "Submissions";

    private Stat stat;
    private final BPackage curPkg;
    private Node rootNode;
    private DefaultTreeModel treeModel;
    private JTree tree;
    private String selectedScheme = null;


    /**
     * The constructor. Do NOT do any operation here that can reasonably fail.
     * We want the constructor to be reliable.
     *
     * @param  i_stat  Description of the Parameter
     * @param  i_pkg   Description of the Parameter
     */
    SubmissionProperties(Stat i_stat, BPackage i_pkg)
    {
        stat = i_stat;
        curPkg = i_pkg;

        stat.aDbg.trace(Stat.SVC_PROP, "new SubmissionProperties: CALLED");

        rootNode = new Node(stat, ROOT_NODENAME, false);
        treeModel = new MyTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.addTreeSelectionListener(new SubSelectionListener());
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setExpandsSelectedPaths(true);
    }


    /**
     * This will load the tree with the right content. This is not in the constructor since
     * we may want to be smart in the future and trow some nice exceptions :-)
     */
    void loadTree()
    {
        File systemConfFile = new File(stat.bluej.getSystemLib(), CONFIG_FILENAME);
        loadOneFile(systemConfFile, false);

        File userConfFile = stat.bluej.getUserFile(CONFIG_FILENAME);
        loadOneFile(userConfFile, false);

        BProject proj = stat.bluej.getProject(curPkg);
        if (proj != null) {
            File projectConfFile = new File(proj.getProjectDir(), CONFIG_FILENAME);
            loadOneFile(projectConfFile, true);
        }

        // in ANY case I have to try to load the default scheme...
        getDefaultScheme();
    }


    /**
     * loads some configuration file...
     *
     * @param  file       Description of the Parameter
     * @param  isProject  Description of the Parameter
     */
    private void loadOneFile(File file, boolean isProject)
    {
        stat.aDbg.trace(Stat.SVC_PROP, "SubmissionProperties.load CALLED");
        stat.aDbg.debug(Stat.SVC_PROP, "file=" + file.toString());

        // Not a big deal if the file does not exist, really.
        if (!file.exists()) return;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            Parser aParser = new Parser(stat, isProject, treeModel);
            aParser.parse(rootNode, fis);
            // Fixed the closing when on error situation.
        } catch (CompilationException cex) {
            cex.addFilename(file.toString());
            JTextArea ta = new JTextArea(cex.toString());
            ta.setEditable(false);
            Font font = ta.getFont();
            ta.setFont(new Font("Courier", font.getStyle(), font.getSize()));
            JScrollPane sp = new JScrollPane(ta);
            JOptionPane.showMessageDialog(curPkg.getFrame(), sp, stat.bluej.getLabel("message.conferror"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(curPkg.getFrame(), ex.toString(), stat.bluej.getLabel("message.conferror"), JOptionPane.ERROR_MESSAGE);
        } finally {
            Utility.inputStreamClose(fis);
        }
    }


    /**
     * We need a tree model to sync insertion in the tree... MHHH
     * The point is that the tree should be locked while it is being updated, not just for a single element...
     * I really have to get rid of all of this mess.. Damiano
     */
    class MyTreeModel extends DefaultTreeModel
    {

        /**
         * I need it basically to call the parent...
         *
         * @param  rootNode  Description of the Parameter
         */
        MyTreeModel(Node rootNode)
        {
            super(rootNode);
        }


        /**
         * I am not convinced that this is enough to sync the GUI and the thread.
         *
         * @param  newChild  Description of the Parameter
         * @param  parent    Description of the Parameter
         * @param  index     Description of the Parameter
         */
        public synchronized void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index)
        {
            stat.aDbg.debug(Stat.SVC_PROP, "insertNodeInto: child=" + ((newChild instanceof Node)?((Node)newChild).getTitle():newChild.toString()));

            // Forces insert to go at the end of the parent's list
            index = getChildCount(parent);
            super.insertNodeInto(newChild, parent, index);
        }
    }


    /**
     * This is called when somebody clicks on something that needs to be expanded.
     */
    class SubSelectionListener implements TreeSelectionListener
    {
        /**
         *  Description of the Method
         *
         * @param  event  Description of the Parameter
         */
        public void valueChanged(TreeSelectionEvent event)
        {
            Node node = (Node) event.getPath().getLastPathComponent();
            expandNode(node);
        }
    }


    /**
     * This is called when somebody clicks on something that needs to be expanded.
     */
    class SubExpandListener implements TreeWillExpandListener
    {
        /**
         *  Description of the Method
         *
         * @param  event  Description of the Parameter
         */
        public void treeWillCollapse(TreeExpansionEvent event) { }


        /**
         *  Description of the Method
         *
         * @param  event  Description of the Parameter
         */
        public void treeWillExpand(TreeExpansionEvent event)
        {
            Node node = (Node) event.getPath().getLastPathComponent();
            expandNode(node);
        }
    }


    /**
     *  Adds a feature to the TreeModelListener attribute of the SubmissionProperties object
     *
     * @param  tml  The feature to be added to the TreeModelListener attribute
     */
    public void addTreeModelListener(TreeModelListener tml)
    {
        treeModel.addTreeModelListener(tml);
    }


    /**
     * This returns the tree, tryng to be smart in where to point it....
     *
     * @return    The tree value
     */
    public JTree getTree()
    {
        TreePath path = getPathFromString(selectedScheme);
        stat.aDbg.trace(Stat.SVC_PROP, "getTree: path=" + ((path == null)?"(path == null)":path.toString()));

        // You MUST leave it, othervise the root node is NOT expanded AND
        // the subnodes are not visible !
        tree.expandPath(new TreePath(rootNode));

        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);

        return tree;
    }


    /**
     * When somebody clicks on the tree I need to remembar where I am...
     * HMMM, this is weird.. why should I do this... TODO check this also. Damiano
     */
    public void setSchemeFromTree()
    {
        TreePath path = tree.getSelectionPath();

        if ( path == null ) return;

        setSelectedScheme( getPathAsString(path));
    }


    /**
     * This will get the default submission scheme.
     * It should load it from a property file athat is in the current project directory, if any...
     * If nothing can be found it will return an EMPTY (zero len string)
     *
     * @return    The defaultScheme value
     */
    public String getDefaultScheme()
    {
        if (selectedScheme != null) return selectedScheme;

        // Ok, time to retrieve the selected scheme, but first let's set a nice default
        setSelectedScheme ( "" );

        BProject curProj = stat.bluej.getProject(curPkg);
        // FOr some misterious reason there is no project open, let's return the default
        if (curProj == null) return selectedScheme;

        File projectDefsFile = new File(curProj.getProjectDir(), PROPERTIES_FILENAME);
        // For some reason (maybe the file is not there, I cannot read it...
        if (!projectDefsFile.canRead()) return selectedScheme;

        Properties projProps = new Properties();
        FileInputStream iStream = null;
        try {
            // ACK ! Need to do this way to be shure to close the dammed file...
            iStream = new FileInputStream(projectDefsFile);
            projProps.load(iStream);
        } catch (Exception exc) {
            // If nothing can be read I may as well return the default.
            return selectedScheme;
        } finally {
            Utility.inputStreamClose(iStream);
        }

        // SHould be calling setSelectedScheme Damiano
        return setSelectedScheme ( projProps.getProperty(SELECTED_NODE_PROPERTY, ""));
    }


    /**
     * This will write down what is the default scheme.
     * Of course the default is the one that is currently selected...
     */
    public void saveDefaultScheme()
    {
        TreePath path = getPathFromString(selectedScheme);

        // Don't save invalid paths
        if (path == null) return;

        BProject curProj = stat.bluej.getProject(curPkg);
        // For some misterious reason there is no project open, let's return the default
        if (curProj == null) {
            stat.aDbg.error(Stat.SVC_PROP, "setDefaultScheme: ERROR: No current project");
            return;
        }

        // Let me put what I need into the properties.
        Properties projProps = new Properties();
        projProps.setProperty(SELECTED_NODE_PROPERTY, selectedScheme);

        // Now let me try to open the file to write the properties on
        File projectDefsFile = new File(curProj.getProjectDir(), PROPERTIES_FILENAME);

        FileOutputStream oStream = null;
        try {
            // ACK ! Need to do this way to be shure to close the dammed file...
            oStream = new FileOutputStream(projectDefsFile);
            projProps.store(oStream, "Submitter per project properties");
        } catch (Exception exc) {
            stat.aDbg.error(Stat.SVC_PROP, "setDefaultScheme: Cannot write properties to file=" + projectDefsFile.toString());
            return;
        } finally {
            Utility.outputStreamClose(oStream);
        }
    }


    /**
     * Called when the user clicks on a node that maybe should be expanded.
     *
     * @param  node  Description of the Parameter
     */
    private void expandNode(Node node)
    {
        stat.aDbg.debug(Stat.SVC_PROP, "SubmissionProperties.expandNode: node=" + node.toString());
        node.expand(treeModel);
    }


    /**
     *  Gets the props attribute of the SubmissionProperties object
     *
     * @param  item                         Description of the Parameter
     * @return                              The props value
     * @exception  AbortOperationException  Description of the Exception
     */
    public Collection getProps(String item) throws AbortOperationException
    {
        TreePath path = getPathFromString(selectedScheme);
        if (path == null)
            throw new AbortOperationException(stat.bluej.getLabel("message.notascheme"));
        Node selected = (Node) path.getLastPathComponent();
        return selected.getConfig(item);
    }


    /**
     * Is the currently selected scheme string valid?
     *
     * @return    The validScheme value
     */
    public boolean isValidScheme()
    {
        TreePath path = getPathFromString(selectedScheme);
        return path != null && ((Node) path.getLastPathComponent()).isLeaf();
    }




    /**
     * Sets the currently selected scheme. You MUST call this one when you want
     * to write the selectedScheme.
     *
     * @param  newScheme  The new selectedScheme value
     */
    public String setSelectedScheme(String newScheme)
    {
        // Let's be shure that there is no null around
        if (newScheme == null) newScheme = "";

        // This is a reasonable value
        selectedScheme = newScheme;

        // We store it in the global props
        stat.globalProp.setProperty(GlobalProp.TITLE_VAR,selectedScheme);

        // We also need it as a short version
        String simpleScheme = selectedScheme;
        int index = selectedScheme.lastIndexOf('/');
        if (index >= 0 && (index+1) < selectedScheme.length() ) simpleScheme = selectedScheme.substring(index + 1);

        stat.globalProp.setProperty(GlobalProp.SIMPLETITLE_VAR,simpleScheme);

        return selectedScheme;
    }


    /**
     * Gets the currently selected scheme. This may not be valid
     *
     * @return    the free-form string of an apparent scheme. May not exist.
     */
    public String getSelectedScheme()
    {
        return selectedScheme;
    }


    /**
     * Turns a TreePath into a String by getting the titles of each node
     * and separating them with a <code>/</code>
     *
     * @param  path  the path to examine
     * @return       a String representation of this path
     * @see          getPathFromString(String)
     */
    private String getPathAsString(TreePath path)
    {
        String pathString = "";

        // It may happens, no need to coredump.
        if (path == null) return pathString;

        Object[] objs = path.getPath();
        for (int i = 1; i < objs.length; i++) {
            Node n = (Node) objs[i];
            pathString += n.getTitle() + (i == objs.length - 1 ? "" : "/");
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
     *
     * @param  pathString  the path as a string of titles separated by <code>/</code>
     * @return             the path represented by this string, if valid, or <code>null</code> if not valid.
     */
    private TreePath getPathFromString(String pathString)
    {
        StringTokenizer token = new StringTokenizer(pathString, "/");
        Node leaf = rootNode;
        ArrayList pathList = new ArrayList();
        pathList.add(leaf);

        while (token.hasMoreTokens() && !leaf.isLeaf()) {
            String nextNodeString = token.nextToken();
            expandNode(leaf);
            Node nextNode = null;
            for (Enumeration en = leaf.children(); en.hasMoreElements(); ) {
                Object n = en.nextElement();
                if (n.toString().equals(nextNodeString)) {
                    nextNode = (Node) n;
                    break;
                }
            }
            if (nextNode == null)
                return null;
            leaf = nextNode;
            pathList.add(leaf);
        }

        TreePath path = new TreePath(pathList.toArray());
        return path;
    }
}
