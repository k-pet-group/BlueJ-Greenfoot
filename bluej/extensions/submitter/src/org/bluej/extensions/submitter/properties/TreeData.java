package org.bluej.extensions.submitter.properties;


import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.tree.*;
import org.bluej.extensions.submitter.*;
import org.bluej.utility.*;

/**
 * This should manage the data loading for the tree of possible submissions
 * It should provide tha data model to be displayed in a tree model..
 * This class should ALWAYS be istantiated and should reload the dataModel on request.
 * NOTE: It is most likely that his class should be called from a separate thread !
 */
public class TreeData
{
    // Yes, I know, it should really be named submission.conf... Damiano (History is hard to forget)
    private final static String CONFIG_FILENAME = "submission.defs";
    private final static String ROOT_NODENAME = "Submissions";

    private Stat stat;
    private TreeNode rootNode;
    private DefaultTreeModel treeModel;


    /**
     * Do NOT do any operation here that can reasonably fail, we want the constructor to be reliable.
     * we need this object to be usable as a soon as it is created, it will of course hold an empty tree
     * but it will be something. We need to be able to display the tree being loaded
     * by this model....
     *
     * @param  i_stat  Description of the Parameter
     */
    public TreeData(Stat i_stat)
    {
        stat = i_stat;

        stat.aDbg.trace(Stat.SVC_PROP, "new SubmitTree: CALLED");

        rootNode = new TreeNode(stat, ROOT_NODENAME);
        treeModel = new treeModel(rootNode);
    }



    /**
     * This method will check if there is a submission config file in any of the three
     * possible places.
     *
     * @param  projectRoot  Description of the Parameter
     * @return              Description of the Return Value
     */
    public boolean haveConfiguration(File projectRoot)
    {
        File systemConfFile = new File(stat.bluej.getSystemLibDir(), CONFIG_FILENAME);
        if (systemConfFile.canRead())
            return true;

        File userConfFile = new File(stat.bluej.getUserConfigDir(), CONFIG_FILENAME);
        if (userConfFile.canRead())
            return true;

        File projectConfFile = new File(projectRoot, CONFIG_FILENAME);
        if (projectConfFile.canRead())
            return true;

        return false;
    }


    /**
     * This will load the tree with the right content. This is not in the constructor since
     * we may want to be smart in the future and trow some nice exceptions :-)
     *
     * @param  projectRoot  Description of the Parameter
     */
    public void loadTree(File projectRoot)
    {
        stat.aDbg.trace(Stat.SVC_PROP, "SubmitTree.loadTree: CALLED");

        // We need to remove all children AND revalidate the tree...
        // We NEED to do this in any case, even if the package is null.
        rootNode.removeAllChildren();
        treeModel.nodeStructureChanged(rootNode);

        File systemConfFile = new File(stat.bluej.getSystemLibDir(), CONFIG_FILENAME);
        loadFile(rootNode, systemConfFile);

        File userConfFile = new File(stat.bluej.getUserConfigDir(), CONFIG_FILENAME);
        loadFile(rootNode, userConfFile);

        File projectConfFile = new File(projectRoot, CONFIG_FILENAME);
        loadFile(rootNode, projectConfFile);
    }


    /**
     * Loads into insertPoint one config file from either a File or a URL.
     * It WILL load either from file or url, preferring the URL.
     *
     *
     * @param  insertPoint  inserts the given content on this node
     * @param  fromFile     gets data from this file, if not null
     */
    private void loadFile(TreeNode insertPoint, File fromFile)
    {
        if (fromFile == null)
            return;

        try {
            InputStream risul = new FileInputStream(fromFile);
            if (risul == null)
                return;
            loadOneStream(insertPoint, risul, fromFile.toString());
        } catch (Exception exc) {
            stat.submitDialog.logWriteln("Opening " + exc.getMessage());
        }
    }


    /**
     * Loads into insertPoint one config file from a URL.
     *
     *
     * @param  insertPoint  inserts the given content on this node
     * @param  fromUrl      gets data from this url, if not null
     */
    void loadUrl(TreeNode insertPoint, String fromUrl)
    {
        if (fromUrl == null)
            return;

        try {
            URL url = new URL(fromUrl);
            InputStream risul = url.openStream();
            if (risul == null)
                return;
            loadOneStream(insertPoint, risul, fromUrl);
        } catch (Exception exc) {
            stat.submitDialog.logWriteln("Opening " + exc.getMessage());
        }
    }


    /**
     * Loads one InputStream into the given node.
     *
     * @param  insertPoint  Description of the Parameter
     * @param  fromStream   Description of the Parameter
     * @param  streamName   Description of the Parameter
     */
    private void loadOneStream(TreeNode insertPoint, InputStream fromStream, String streamName)
    {
        stat.aDbg.trace(Stat.SVC_PROP, "SubmissionProperties.loadOneStream CALLED");

        try {
            stat.submitDialog.statusWriteln("Loading " + streamName);
            ConfParser aParser = new ConfParser(stat, treeModel);
            aParser.parse(insertPoint, fromStream);
        } catch (CompilationException cex) {
            cex.addFilename(streamName);
            stat.submitDialog.logWriteln(cex.toString());
        } finally {
            Utility.inputStreamClose(fromStream);
        }

        // After I loaded one stream I have to signal that the tree is changed
        treeModel.nodeStructureChanged(rootNode);
    }


    /**
     * Accessor, for the use by the real tree
     *
     * @return    The treeModel value
     */
    public DefaultTreeModel getTreeModel()
    {
        return treeModel;
    }


    /**
     * Gets the properties associated with the current selected scheme
     * and the given item.
     *
     * @param  item  Description of the Parameter
     * @return       The props value
     */
    public Collection getProps(String item)
    {
        TreePath path = getPathFromString(stat.submitDialog.schemeSelectedGet());
        if (path == null)
            return null;

        TreeNode selected = (TreeNode) path.getLastPathComponent();
        return selected.getConfig(item);
    }


    /**
     * Turns a TreePath into a String by getting the titles of each node
     * and separating them with a <code>/</code>
     *
     * @param  path  the path to examine
     * @return       a String representation of this path
     * @see          getPathFromString(String)
     */
    public String getPathAsString(TreePath path)
    {
        String pathString = "";

        // It may happens, no need to coredump.
        if (path == null)
            return pathString;

        Object[] objs = path.getPath();
        for (int i = 1; i < objs.length; i++) {
            TreeNode n = (TreeNode) objs[i];
            pathString += n.getTitle() + (i == objs.length - 1 ? "" : "/");
        }

        return pathString;
    }


    /**
     * Parses the given string to construct a path.
     *
     * @param  pathString  the path as a string of titles separated by <code>/</code>
     * @return             the path represented by this string, if valid, or null if not valid.
     */
    public TreePath getPathFromString(String pathString)
    {
        StringTokenizer token = new StringTokenizer(pathString, "/");

        TreeNode leaf = rootNode;
        ArrayList pathList = new ArrayList();
        pathList.add(leaf);

        while (token.hasMoreTokens()) {
            String nextNodeString = token.nextToken();

            TreeNode nextNode = null;
            // Assume we have not found something
            for (Enumeration enum = leaf.children(); enum.hasMoreElements(); ) {
                Object someChild = enum.nextElement();
                if (someChild.toString().equals(nextNodeString)) {
                    nextNode = (TreeNode) someChild;
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



// ========================== UTILITY CLASSES HERE ===============================

    /**
     * The treeModel is needed for only one reason, the fact that insertNodeInto
     * is extended to insert noded into the end of the child.
     * There is no syncronization problem now since there is shurely ONLY ONE thread that
     * is messing with the data and the tree change event is signalled to the GUI
     * when the data is loaded.
     */
    class treeModel extends DefaultTreeModel
    {

        /**
         * I need it basically to call the parent...
         *
         * @param  rootNode  Description of the Parameter
         */
        treeModel(TreeNode rootNode)
        {
            super(rootNode);
        }


        /**
         * Used to make shure that the insert is at the end of the node
         *
         * @param  newChild  Description of the Parameter
         * @param  parent    Description of the Parameter
         * @param  index     Description of the Parameter
         */
        public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index)
        {
            stat.aDbg.debug(Stat.SVC_PROP, "insertNodeInto: child=" + newChild);

            // Forces insert to go at the end of the parent's list
            index = getChildCount(parent);
            super.insertNodeInto(newChild, parent, index);
        }
    }

// ===================== END OF MAIN CLASS HERE ================================
}
