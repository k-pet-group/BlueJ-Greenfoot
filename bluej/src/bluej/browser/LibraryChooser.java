package bluej.browser;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.plaf.basic.*;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.util.zip.*;
import java.util.jar.*;

import java.io.*;

import bluej.Config;
import bluej.pkgmgr.PackageTarget;
import bluej.pkgmgr.Package;
import bluej.utility.ModelessMessageBox;
import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.classmgr.ClassMgr;
import bluej.classmgr.ClassPathEntry;

/**
 * A JPanel subclass with a JTree containing all the packages specified in the
 * configuration files.
 * 
 * @author Andy Marks
 * @author Andrew Patterson
 * @version $Id: LibraryChooser.java 265 1999-11-05 04:31:07Z ajp $
 */
public class LibraryChooser extends JPanel implements Runnable {

    private LibraryChooserNode root = null;
    private JTree tree = null;
    private DefaultTreeModel treeModel = null;
    
    private boolean configChanged = false; // flags whether save is needed on quit

    private LibraryBrowser parent = null;

    // popup menus
    //  private LibraryPopupMenu libPopup = new LibraryPopupMenu(this);

    /**
     * Create a new LibraryChooser.  Initialize the tree from the libraries
     * specified in the configuration files.
     * 
     * @param parent the LibraryBrowser object containing the LibraryChooser.
     */
    public LibraryChooser(LibraryBrowser parent) {
        this.parent = parent;

        setBackground(Color.white);
    
        loadLibraries();

        this.setLayout(new BorderLayout());
    }
    
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }


    // Notify all listeners that have registered interest for
    // notification on this event type.  The event instance 
    // is lazily created using the parameters passed into 
    // the fire method.

    protected void fireActionEvent(String className) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, className));
            }	       
        }
    }	

    /**
     * Use the user and system configuration files to load the libraries
     * into the tree.  Create a hashtable of all the libraries and their
     * aliases, and then use the hashtable to build the tree in a separate
     * thread.
     **/
    public void loadLibraries() {
        if (tree == null) {
            // first time
            root = new LibraryChooserNode("");
            tree = new JTree(root);        // init tree here so refresh works properly
            treeModel = (DefaultTreeModel)tree.getModel();
            tree.setRootVisible(false);
            tree.setBorder(new EmptyBorder(5,5,5,5));
        } else {
            // clean tree
            Enumeration topLevelNodes = root.children();

            while (topLevelNodes.hasMoreElements())
                treeModel.removeNodeFromParent((DefaultMutableTreeNode)topLevelNodes.nextElement());
            root.removeAllChildren();
            treeModel.reload();
        }
    
        new Thread(this).start();
    }

    
    /**
     * Save the current configuration of the library chooser.
     */
    public void saveConfig() {
        setStatusText("Saving LibraryChooser configuration...");
        setStatusText("Saving LibraryChooser configuration...done");
    }
    
    /**
     * Close the library chooser.  Prompt the user to save the configuration
     * if it has changed.
     */
    public void close() {
        if (this.configChanged == true) {
//            int choice = JOptionPane.showConfirmDialog(LibraryBrowserPkgMgrFrame.getFrame(),
//                                                       Config.getString("browser.librarychooser.saveconfigdialog.text"),
 //                                                      Config.getString("browser.librarychooser.saveconfigdialog.title"),
  //                                                     JOptionPane.YES_NO_OPTION);
//            if (choice == JOptionPane.YES_OPTION)
//                saveConfig();
        }
    }
    
    /**
     * Separate process used to load libraries whilst the rest of the browser
     * inits.  Iterate over the previously created hashtable of libraries,
     * completing and adding each one to the tree.
     * 
     * The methods which are invoked from this method could conceivably
     * take quite a while to run, so they have been spawned from a separare
     * thread to allow the rest of the library chooser to begin immediately.
     * No attempt at thread synchronization is made here, so beware of any
     * attemps to access the tree's contents before this method has completed.
     */
    public void run() {
        //  parent.disableControls();

        this.setStatusText(Config.getString("browser.librarychooser.loading.status"));
    
        Iterator libraries = ClassMgr.getClassMgr().getAllClassPathEntries();
    
        while (libraries.hasNext()) {
            ClassPathEntry cpe = (ClassPathEntry)libraries.next();
                
            addLibraryToTree(new ClassPathEntryNode(cpe));
        }
                               
        setupTree();
    
        // dont' add tree to UI until all it's data has been loaded.
        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane, BorderLayout.CENTER);
    
        revalidate();
        this.setStatusText(Config.getString("browser.librarychooser.loaded.status"));
        //  parent.enableControls();
    }
    
    /**
     * Perform tree initialization such as setting up
     * listeners to display information about a selected
     * tree node on the status line, expanding the tree
     * to make all the top level nodes visible and setting
     * the renderer to a LibraryTreeCellRenderer.
     */
    private void setupTree() {
        tree.getSelectionModel().setSelectionMode (TreeSelectionModel.SINGLE_TREE_SELECTION);

        // listen for when the selection changes.

        tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    LibraryChooserNode node = (LibraryChooserNode)(e.getPath().getLastPathComponent());

                    String p[] = findElementsOfPackage(node);

                    if (p.length > 0) {
                        String packageName = pathToPackageName(e.getPath());

                        parent.openPackage(p);
                        
                        parent.openPackage(analysePackage(packageName, p));
                    }

                    // show the internal name if the node has one and it's not the same as the alias
                    //      String displayName = node.getDisplayName();
                    //      String internalName = node.getInternalName();
                    //      if (!displayName.equals(internalName))
                    //          setStatusText(displayName + " = " + internalName);

                    //            openSelectedPackageInClassChooser();
                }
            });     
        
        tree.setEditable(false);
        
        // make sure all the top level libraries are initially visible and expanded
    
        Enumeration topLevelNodes = root.children();
        while (topLevelNodes.hasMoreElements())
            tree.expandPath(new TreePath(((DefaultMutableTreeNode)topLevelNodes.nextElement()).getPath()));
            
        // let's use the same icon for all nodes
        DefaultTreeCellRenderer rend = new DefaultTreeCellRenderer();
        rend.setLeafIcon(null);
        rend.setClosedIcon(null);
        rend.setOpenIcon(null);
        
        tree.setCellRenderer(rend); //new LibraryTreeCellRenderer(this));
    }

    private DefaultTreeModel analysePackage(String packageName, String[] classNames)
    {
        HashMap classMap = new HashMap();

        for(int i=0; i<classNames.length; i++)
        {
            try {
                String className = packageName + "." + classNames[i];

                classMap.put(className, Class.forName(className));
            }
            catch(ClassNotFoundException cfe) { }
        }
            
        // this is an array of lists where each list contains
        // all classes that have super/sub relationship with
        // other classes on the list. The most super of all the
        // classes in the list appears first

        ArrayList classGroups = new ArrayList();
        ArrayList interfaceGroups = new ArrayList();
        
        Iterator it = classMap.keySet().iterator();

        DefaultTreeModel dtm = new DefaultTreeModel(new DefaultMutableTreeNode(Object.class));

        while(it.hasNext())
        {
            String className = (String)it.next();
            Class packageClass = (Class)classMap.get(className);
            
            if (packageClass.isPrimitive() ||
                packageClass.isInterface() ||
                packageClass == Object.class)
            {
                continue;
            }

            insertInto((DefaultMutableTreeNode)(dtm.getRoot()), packageClass);
        }

        return dtm;
                
/*        Enumeration allNodes = ((DefaultMutableTreeNode)(dtm.getRoot())).depthFirstEnumeration();
        while (allNodes.hasMoreElements()) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode)allNodes.nextElement();

            for(int i=0; i<n.getDepth(); i++)
                System.out.print("    ");
            System.out.println(((Class)n.getUserObject()).toString());
        } */
    }    

    public void insertInto(DefaultMutableTreeNode root, Class cl)
    {
        // precondition: root's user object is a superclass of cl

        Class rootClass = (Class)root.getUserObject();
    
        if (!rootClass.isAssignableFrom(cl))
            System.out.println("Precondition wasn't true for " + rootClass.getName() + " " + cl.getName());

        // we will probably have to insert this node so we create it now
        DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(cl);

        DefaultMutableTreeNode reparentToClass[] = new DefaultMutableTreeNode[root.getChildCount()];
        int reparentCount = 0;

        // look for a child node that we are either a superclass or subclass
        // of
        for(int i=0; i<root.getChildCount(); i++)
        {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)root.getChildAt(i);
            Class childClass = (Class)childNode.getUserObject();
            
            if (cl.isAssignableFrom(childClass))
            {
                // cl is a superclass of the child so we need to reparent
                // the child to be a real child of classNode
                // but we can't reparent yet because it will stuff up our
                // enumerating so we store them in an array and reparent them
                // in a batch later on
                
                reparentToClass[reparentCount++] = childNode;
            }
            
            if (childClass.isAssignableFrom(cl))
            {
                if (reparentCount > 0)
                    System.out.println("woh horsey");

                // childClass is a superclass of cl so we recurse down the tree
                insertInto(childNode, cl);
                return;
            }            
            
        }

        for(int i=0; i<reparentCount; i++)
            classNode.add(reparentToClass[i]); 

        root.add(classNode);
    }
    
    /**
     * Display all the nodes of the tree using a depthFirstEnumeration.
     * Use for debugging purposes only.
     */
    private void dumpTree() {
        Enumeration allNodes = root.depthFirstEnumeration();
        while (allNodes.hasMoreElements()) {
            Debug.message(((LibraryChooserNode)allNodes.nextElement()).toString());
        }
    }
    
    /**
     * Remove all branches of the tree that do not contain any files
     * (unless they are .class files).
     * <strong>Note: should only be done before the class files are
     * removed.
     * 
     * @param node the node of the tree from which to start pruning.
     */
    private void pruneNodeOfEmptyDirectories(LibraryChooserNode node) {
        boolean prunedOnLastPass = true;
        
        // short circuit the pruning for a completely empty node
        if(node.getChildCount() == 0) {
            node.removeFromParent();
            return;
        }
        
        while (prunedOnLastPass) {
            prunedOnLastPass = false;
            
            Enumeration nodes = node.depthFirstEnumeration();
            
            LibraryChooserNode nextNode = null;

            while (nodes.hasMoreElements()) {
                nextNode = (LibraryChooserNode) nodes.nextElement();
                
                if (nextNode.isLeaf() && 
                    nextNode.getFiles() == null &&
                    nextNode != root)
                    {
                        nextNode.removeFromParent();
                        prunedOnLastPass = true;
                    }
            }
        }
    }

    /**
     * Make sure the node for the <code>thePackage</code> is visible in
     * the tree.  Invoked when a new package in the class chooser is
     * double clicked.
     * <strong>Note: will not select the root node.</strong>
     * <strong>Known bugs: will not work with a package with more than 20 elements (i.e., 1.2.,19.20.21)</strong>
     * 
     * @param thePackage the name of the package to select in absolute path format
     * @return true if the package was found and selected
     */
    public boolean selectPackageInTree(String thePackage) {
        /*
          Sample thePackage parameter: "C:\bluej.src\bluej.debugger"
          Algorithm to find tree node to select based on thePackage parameter:
          (1) find alias matching start of string
          (2) if no alias found, no matching node
          (3) open node containing alias
          (4) tokenize rest of parameter based on File.separator
          (5) take next token
          (6) find node from previous root matching token
          (7) select that node
          (8) go to step 5
        */
//        LibraryChooserNode tmpNodesInPathToFocus[] = new LibraryChooserNode[20];
  //      tmpNodesInPathToFocus[0] = root;
    //    LibraryChooserNode topLevelNode = getFirstNodeContainingDir(thePackage);
      //  if (topLevelNode == null) {
            // we couldn't find the top level node containing the package
       //     Utility.showError(LibraryBrowserPkgMgrFrame.getFrame(), 
        //                      Config.getString("browser.librarychooser.classchooserresyncdialog.title") + " '" + thePackage + "'");
         //   return false;
        //}
        //tmpNodesInPathToFocus[1] = topLevelNode;
    
        // tokenize the remainder of thePackage that is left over after what is matching
        // in topLevelNode is removed (i.e., if thePackage = c:\a\b\c and topLevelNode 
        // includes c:\a, then tokenize on \b\c).  If nothing is left to tokenize, then the
        // top level node is an exact match for thePackage, so skip the loop and select it.
   /*     String stringToTokenize = thePackage.substring(topLevelNode.getInternalName().toString().length(), thePackage.length());
        StringTokenizer nodeTokens = new StringTokenizer(stringToTokenize, File.separator);
        LibraryChooserNode root = topLevelNode;
        int nextPosInPath = 2;
        boolean foundNode = false;
        while (nodeTokens.hasMoreElements()) {
            Enumeration rootChildren = root.children();
            String nextBitInPath = nodeTokens.nextElement().toString();
            // search in the children of root for a UserObject matching nextBitInPath
            foundNode = false;
            while (rootChildren.hasMoreElements()) {
                LibraryChooserNode nextChild = (LibraryChooserNode)rootChildren.nextElement();
                if (nextChild.getInternalName().equals(nextBitInPath)) {
                    root = tmpNodesInPathToFocus[nextPosInPath] = nextChild;
                    foundNode = true;
                    break;
                }
            }
            if (foundNode == true)
                nextPosInPath++;
            else {
                Utility.showError(LibraryBrowserPkgMgrFrame.getFrame(), 
                                  Config.getString("browser.librarychooser.classchooserresyncdialog.title") + " '" + nextBitInPath + "'");
                return false;
            }
        }
        
        LibraryChooserNode nodesInPathToFocus[] = new LibraryChooserNode[nextPosInPath];
        System.arraycopy(tmpNodesInPathToFocus, 0, nodesInPathToFocus, 0, nextPosInPath);
        tree.setSelectionPath(new TreePath(nodesInPathToFocus));        
                    */
        return true;
    }
    
    /**
     * Return an array of TreePath objects containing all nodes in the tree
     * that contain the <code>pattern</code>.  Called (originally) by the
     * FindLibraryDialog once a search term has been entered.
     * 
     * @return all paths in the tree matching the pattern, given the criteria, or null
     * @param pattern the string to find within the paths in the tree
     * @param caseSensitive true if the match must be case sensitive
     * @param substringSearch true if the match can be on part of the library
     */
    public TreePath[] findAllLibrariesMatching(String pattern, boolean caseSensitive, boolean substringSearch) {
        Enumeration nodes = root.depthFirstEnumeration();
        Vector results = new Vector();
        while (nodes.hasMoreElements()) {
            LibraryChooserNode thisNode = (LibraryChooserNode)nodes.nextElement();
            if (doesLibraryMatchPattern(new TreePath(thisNode.getPath()), pattern, caseSensitive, substringSearch))
                results.addElement(new TreePath(thisNode.getPath()));
        }
        if (results.size() == 0)
            return null;
        
        TreePath[] matches = new TreePath[results.size()];
        results.copyInto(matches);
        return matches;
    }

    /**
     * Given a particular TreePath, try to match it against a pattern.
     * 
     * @param library the library to search
     * @param pattern the pattern to look for in the library
     * @param caseSensitive true if a case sensitive match is required
     * @param substringSearch true if the match can be on any part of the string
     * @return true if the library matches the pattern (given the criteria)
     */
    private static boolean doesLibraryMatchPattern(TreePath library, String pattern, boolean caseSensitive, boolean substringSearch) {
        if (!caseSensitive) {
            pattern = pattern.toLowerCase();
        }
        
        String node = null;
        for (int current = 0; current < library.getPathCount(); current++) {
            node = ((LibraryChooserNode)library.getPathComponent(current)).toString();
            if (!caseSensitive)
                node = node.toLowerCase();
            
            if (substringSearch) {
                if (node.indexOf(pattern) != -1) {
                    return true;
                }
            } else {
                if (node.equals(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Return the first top level tree node containing a substring of <code>dir</code>
     * at the start of it's user object.
     * <strong>Note: assumes case insensitivity of directory names (BAD!)</strong>
     * 
     * @param dir the directory to try and match in the top level nodes
     * @return the first top level node with a user object matching <code>dir</code>
     */
    private LibraryChooserNode getFirstNodeContainingDir(String dir) {
        Enumeration topLevelNodes = root.children();
        LibraryChooserNode node = null;
        while (topLevelNodes.hasMoreElements()) {
            node = (LibraryChooserNode)topLevelNodes.nextElement();
            //String search = (node.hasShadowArea() ? node.getShadowArea() : node.getInternalName());
//            String search = node.getInternalName();
//            if (dir.toLowerCase().indexOf(search.toLowerCase()) != -1)
//                return node;
        }
        return null; // no matching top level node found
    }
    
    /**
     * Add a new library to the tree. New libraries can only be added
     * to the root of the tree.  The library is checked to ensure the path is
     * a valid one on the current filesystem and that the library
     * hasn't already been added to the tree.  Responsibility for
     * adding the library is delegated to a function specialized for
     * adding this <em>type</em> of library (i.e., ZIP/JAR or directory)
     * 
     * @param cpeNode the ClassPathEntryNode object to add to the tree
     */
    private void addLibraryToTree(ClassPathEntryNode cpeNode)
    {     
        //        if (getFirstNodeWithObject(root, library) != null) {
        //          System.err.println(library + " already exists");
        //          return;
        //      }

        // add the new node to the root
        treeModel.insertNodeInto(cpeNode, root, root.getChildCount());
        
        if (cpeNode.isJar()) {
            //                setStatusText("Opening jarzip " + alias + " " + library + "...");
            openArchiveLibrary(cpeNode);
        } else if (cpeNode.isClassRoot()) {
            //      setStatusText("Opening dir " + alias + " " + library + "...");
            openDirectoryLibrary(cpeNode);
        }

        // we need to prune with each new node,
        // rather than the entire tree because when new
        // libraries are added - we only need to worry about
        // the new library, not the entire tree
        pruneNodeOfEmptyDirectories(cpeNode);
    }
    
    /**
     * Add an archive library (i.e., ZIP or JAR file) to the tree.
     * Because the archive could (very likely) be stored with directories,
     * We need to parse each entry, extract the paths contained within, 
     * and see if part/all/none of the tree has been created yet.
     * 
     * @param top the node of the tree to be the parent of the library
     * @param file the file representation of the library
     * @param alias the display name for the library
     */
    private void openArchiveLibrary(ClassPathEntryNode cpe) {

        try {
            JarFile archiveFile = new JarFile(cpe.getFile());

            Enumeration files = archiveFile.entries();
            String entryName = "";
            String statusString = Config.getString("browser.librarychooser.openingarchive.status") + " " + archiveFile.getName();

            while (files.hasMoreElements()) {
                    
                entryName = ((JarEntry) files.nextElement()).getName();

                if (entryName.endsWith(".class") &&
                        entryName.indexOf('$') == -1) {
                    addArchiveEntry(cpe, entryName);
                }
            }
        
            archiveFile.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    /**
     * Add a single entry from an archive file to the tree.
     * We have a single entry from an archive file, which could be a file
     * (with or without a preceeding directory), or simply a directory.
     * We need to add this file (with directory structure, if any) to
     * the appropriate place in the tree.
     * 
     * @param top the node to be the parent of the new entry.
     * @param entry the new entry to add to the tree.
     */
    private void addArchiveEntry(LibraryChooserNode top, String entry) {
        // it must be a class file
        if (!entry.endsWith(".class"))
            return;

        // find first slash to see whether entry contains directories or not
        int slashPos = entry.indexOf("/");
    
        // simplest case - no directories in entry,
        // so we just have a .class file.  This can be thrown
        // away as we can assume it's parent directory structure
        // has been created.
        if (slashPos == -1) {

            int ind = entry.indexOf(".class");
            if(ind >= 0)        // must be true but better safe than sorry
                entry = entry.substring(0, ind);                                                              
            
            top.addFile(entry);
            return;
        }
    
        // entry starts with a slash - remove it and continue
        if (slashPos == 0)
            entry = entry.substring(1, entry.length());
    
        // normal case - slash found not at start
        int nextSlashPos = entry.indexOf("/", slashPos);
        if (nextSlashPos == -1) {
            // we have something like /dirname or /file.class
            return;
        }
    
        LibraryChooserNode newNode = new PackageNode(entry.substring(0, slashPos));
        LibraryChooserNode foundNode = getFirstNodeWithObject(top, newNode.getUserObject().toString());
        if (foundNode != null) {
            // recurse
            addArchiveEntry(foundNode, entry.substring(slashPos + 1, entry.length()));    
        } else {
            top.add(newNode);
            // recurse
            addArchiveEntry(newNode, entry.substring(slashPos + 1, entry.length()));    
        }
            
    }

    private void openDirectoryLibrary(ClassPathEntryNode cpe) {
        openDirectoryLibrary(cpe, cpe.getFile());
    }

    /**
     * This library is a directory, so traverse it, building the tree as you go.
     * The library is checked to ensure it exists and hasn't already been added
     * to the tree.  The directory is recursed, with each child directory being
     * added to the tree.
     * 
     * @param top the node of the tree to be the parent of the library.
     * @param the file representation of the library.
     **/
    private void openDirectoryLibrary(LibraryChooserNode top, File file) {
        if (!file.exists() && !file.isDirectory()) {
            // because of the long startup time, this should probably be in a separate thread
            Utility.showError((JFrame)getParent(),
                      Config.getString("browser.librarychooser.missingshadowdialog.title") + " " + file.getName());
            return;
        }

        String[] contents = file.list();

        File newFile = null;
        LibraryChooserNode newNode = null;
            
        // needed to allow correct identification of file type
        String path = file.getPath(); 
            
        for (int current = 0; current < contents.length; current++) {

            // contruct the new file with the path of the parent
            newFile = new File(path, contents[current]);
            
            // we're only interested in class files
            if (!newFile.isFile() && !newFile.isDirectory())
                continue;
            
            if (newFile.isFile())
            {
                if (newFile.getName().endsWith(".class") &&
                     newFile.getName().indexOf('$') == -1)
                {
                    String entry = newFile.getName();
                    int ind = entry.indexOf(".class");
                    if(ind >= 0)        // must be true but better safe than sorry
                        entry = entry.substring(0, ind);                                                              

                    top.addFile(entry);
                }
            } else {
            
                newNode = new LibraryChooserNode(contents[current]);
                top.add(newNode);
            
                // recurse if directory (implied from isFile() test above I
                // guess but its best to be safe)
                if (newFile.isDirectory())
                    openDirectoryLibrary(newNode, newFile);
            }
        }
    }

    /**
     * Helper method to find the first child node of a node
     * containing the user object matching <code>object</code>.
     * Examine the children of a specified parent node for a 
     * matching user object.
     * 
     * @param node the parent node of the children to examine
     * @param object the user object to search for.
     * @return the found node, or null if no node found
     */
    private static LibraryChooserNode getFirstNodeWithObject(LibraryChooserNode node, Object object) {
        LibraryChooserNode child = null;

        if (!node.isLeaf()) {
            Enumeration children = node.children();
            while (children.hasMoreElements()) {
                child = (LibraryChooserNode) children.nextElement();
                if (child.getUserObject().equals(object))
                    return child;
            }
        }
        
        return null;
    }

    /**
     * Handle all popup menu commands.
     * 
     * @param ae the popup menu command to handle.
     */
    /*    public void actionPerformed(ActionEvent ae) {
      String command = ae.getActionCommand();
      Object source = ae.getSource();
      TreePath pathToUse = new TreePath(this.selectedNode.getPath());
      if (command == LibraryPopupMenu.EXPANDCOMMAND) {
      tree.expandPath(pathToUse);
      } else if (command == LibraryPopupMenu.CONTRACTCOMMAND) {
      tree.collapsePath(pathToUse);
      } else if (command == LibraryPopupMenu.USECOMMAND) {
      TreePath selectedPath = new TreePath(selectedNode.getPath());
      if (selectedPath == null)
      return;
      String packageName = pathToPackageName(selectedPath);
      if (packageName != null)
      parent.usePackage(packageName, false);
      } else if (command == LibraryPopupMenu.OPENCOMMAND) {
      openSelectedPackageInClassChooser();
      } 
      else if (command == LibraryPopupMenu.PROPCOMMAND) {
      }
      }
    */
    /**
     * Update the text on the status bar.
     * 
     * @param statusText the new status text to display.
     */
    private void setStatusText(String statusText) {
    if (parent != null)
            ;
        //      parent.setStatus(statusText);
    }
    
    /**
     * Select a package in the library chooser and display it's contents
     * in the class chooser.
     * 
     * @param thePackage the path to the package to open.
     */
    public void openPackage(TreePath thePackage) {
    tree.setSelectionPath(thePackage);
    openSelectedPackageInClassChooser();
    }
  
    /**
     * Convert a tree path to a dot delimited package name,
     * based on the current contents of the tree.
     * 
     * @return the name of the package in this directory, or null.
     * @param thePackage the path to the package.
     */
    public String pathToPackageName(TreePath thePackage) {
        Object[] nodesInPath = thePackage.getPath();
        String packageName = "";

        // let's parse the rest of the LibraryChooserNodes
        for (int current = 2; current < nodesInPath.length; current++) {
            packageName += ((LibraryChooserNode)nodesInPath[current]).toString() + ".";
        }

        if (!packageName.equals("")) {
            // remove the last character, which will be the last decimal point added
            packageName = packageName.substring(0, packageName.length() - 1);
        }
        
        return packageName;
    }
    
    /**
     * Return the directory containing a specified package, based on the
     * current contents of the tree.
     * 
     * @param packageName the package name in java notation (e.g., java.awt.event)
     * @return the directory containing the package, or null if none found
     */
    /*   public String getDirectoryForPackage(String packageName) {
     String packageDirectory = null;
        
     Enumeration allNodes = root.depthFirstEnumeration();
     while (allNodes.hasMoreElements()) {
     LibraryChooserNode currentNode = (LibraryChooserNode)allNodes.nextElement();
     TreePath currentNodePath = new TreePath(currentNode.getPath());
        
     String relevantCurrentNodePath = "";
     for (int current = 2; current < currentNodePath.getPathCount(); current++) {
     relevantCurrentNodePath += currentNodePath.getPathComponent(current).toString();
     if (current < (currentNodePath.getPathCount() - 1))
     relevantCurrentNodePath += ".";
     }
     if (packageName.equals(relevantCurrentNodePath))
     return pathToPackageDirectory(currentNodePath);
            
     }
    
     return null;
     }
    */ 
    /**
     * Open the currently selected package in the class chooser.
     * This is a complex method because of the different states
     * the package could be in, and because of the possible
     * error conditions arising from creating new packages on the fly.
     * 
     * This package could be in one of three distinct states:
     * (1) a directory with an existing PKG file for it
     * - delegate to the LibraryBrowserPkgMgrFrame to open
     * (2) a directory with no existing PKG file for it
     * (3) an archive file (with no PKG file for the entry)
     */
    public void openSelectedPackageInClassChooser() {
    TreePath selectedPath = tree.getSelectionPath();

    // if we don't click on a node or we've clicked the root or a top level node, let's bail
    if (selectedPath == null || selectedPath.getPathCount() < 3)
        return;

    Object[] nodesInPath = selectedPath.getPath();

    String packageLocation = ((LibraryChooserNode)nodesInPath[1]).toString();
    String packageName = pathToPackageName(selectedPath);

    /*      if (!packageLocation.endsWith(".class") &&
            new File(packageLocation, Package.pkgfileName).exists())
            {
            // package file exists in this directory
            parent.openPackage(packageName);
            }
            else 
            { 
        */

    // easiest case is a cached package - let's open it straight away
    //if (parent.isPackageInCache(packageName)) {
    //  parent.openPackage(packageName);
    //  return;
    //}

    // because the package file doesn't exist, we need to identify all the 
    // items that would appear in this package (i.e., classes and sub packages
    // for the currently selected tree node)
    String[] foundEntries = findElementsOfPackage(((LibraryChooserNode)nodesInPath[nodesInPath.length - 1]));
    Properties props = null;
    Package pkg = null;

    if (!new File(packageLocation).isDirectory()) {
        
            // the package directory doesn't exist - probably an archive library
            //      pkg = new Package(packageName, this.parent);
            // passing true as the last parameter means we don't try to save the properties
        try {
        props = Package.createDefaultPackage(foundEntries, packageLocation, packageName, true);
        pkg.load(packageName, props, false, true);
        } catch (IOException ioe) {
        Debug.reportError(ioe.getMessage());
        }
        // show the package
            //      parent.addPackageToCache(packageName, pkg);
            //      parent.openPackage(packageName, pkg);
    }
        /*
          else {
          if (!new File(packageName + Package.pkgfileName).exists()) {
        
          // the package directory exists but the package file doesn't
          // in other words, a package in a directory but no package file
          try {
          props = Package.createDefaultPackage(foundEntries, packageLocation, packageName, true);
          parent.openPackage(packageName);
          } catch (IOException ioe) {
          // createDefaultPackage# could not save the package file,
          // (could be non writable directory like a CD)
          // we'll have to create our own using the properties we've created
          pkg = new Package(packageName);
          pkg.load(packageName, props, false, true);
          parent.addPackageToCache(packageName, pkg);
          parent.openPackage(packageName, pkg);
          }
          } else 
          Debug.reportError("Unexpected control flow in openSelectedPackageInClassChooser(LibraryChooser.java) " + packageName);
          // }
        */
                    
    }
    
    /**
     * Create and return an array of Strings for each directory and file
     * subordinate to the <code>parent</code> node.  The practical upshot
     * of this is to identify all the elements that belong to the package
     * represented by the node (i.e., class files and nested packages).
     * 
     * @param parent the node representing the package
     * @return all files and directories which are immediate children of this node
     */
    private String[] findElementsOfPackage(LibraryChooserNode parent) {
        Object files[] = parent.getFiles();
        int numberFiles = files == null ? 0 : files.length;
        int numberEntries = this.treeModel.getChildCount(parent);
        String result[] = new String[numberEntries + numberFiles];
    
        int current = 0;
        // all node children will be directories (no files stored in tree directly)
        for (; current < numberEntries; current++)
            result[current] = ((LibraryChooserNode)treeModel.getChild(parent, current)).toString();
        
        // all values in files array will be class files in this package's directory
        for (int filesIndex = 0; current < (numberEntries + numberFiles) && filesIndex < files.length; current++, filesIndex++)
            result[current] = (String)files[filesIndex];

        return result;
    }

    /**
     * Determine if we're in a valid position to show a popup menu.  That is,
     * are we under a non root node in the tree.  If we are, show the menu
     * associated with the type of node we're under.
     * 
     * @param e the MouseEvent triggering the request to show a popup menu.
     */
    /*    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
      // find which path is under the cursor
      TreePath targetPath = tree.getPathForLocation(e.getX(), e.getY());
      if (targetPath == null)
      return;
        
      // find the last entry in the path (i.e., the node under the cursor)
      LibraryChooserNode targetNode = (LibraryChooserNode)targetPath.getLastPathComponent();
        
      // if we're not over any node or over the root node, there's nothing to show
      if (targetNode == null || this.isRoot(targetNode))
      return;
            
      JPopupMenu popup = getPopup(targetNode, tree.isExpanded(targetPath), tree.isCollapsed(targetPath));
      // make node selected before showing popup
      tree.setSelectionPath(new TreePath(targetNode));
      selectedNode = targetNode;
      if (popup != null)
      popup.show(e.getComponent(), e.getX(), e.getY());
      }
      }
    */
    /**
     * Return the appropriate popup menu for this node and configure it 
     * to match the current state of this node.
     * 
     * @param node the node requiring the menu
     * @param isExpanded true if the node is currently expanded
     * @param isCollapsed true if the node is currently collapsed
     * @return the configured popup menu
     */
    /*    private LibraryPopupMenu getPopup(LibraryChooserNode node, boolean isExpanded, boolean isCollapsed) {
      LibraryPopupMenu popup = null;

      popup = libPopup;
        
      popup.configure(node, isExpanded, isCollapsed);
        
      return popup;
      } */
}





