package org.bluej.extensions.submitter.properties;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
    
/**
 * This is a node of the configuration tree
 * 
 * @author Clive Miller
 * @version $Id: Node.java 1463 2002-10-23 12:40:32Z jckm $
 */
public class Node extends DefaultMutableTreeNode
{
    private static int indent;
    private static final List configItems = Arrays.asList (new String[] {
        ".transport",
        ".file.include",
        ".file.exclude",
        ".file.essential",
        ".file.jar",
        ".insert"});
    private static int lookups = 0;
    
    /**
     * Are any scheme inserts currently being looked up?
     */
    public static boolean isLookingUp()
    {
        return (lookups > 0);
    }
    
    private List[] configuration; // of String
    private final String title;
    private final boolean project;
    
    /**
     * Creates a new node. It is the responsibility of the caller to add it to its parent
     * @param parent the parent that should adopt this child
     * @param title the Title of this node
     * @param whether or not this node belongs to a specific project
     */
    public Node (String title, boolean project)
    {
        this.title = title;
        this.project = project;
        configuration = new List [configItems.size()];
        for (int i=0,n=configuration.length; i<n; i++) {
            configuration[i] = new ArrayList();
        }
    }
        
    public String getTitle()
    {
        return title;
    }
    
    public boolean isProject()
    {
        return project;
    }
    
    /**
     * Adds a key/value pair configuration item to this node
     * @param key one of the predefined configuration items
     * @param value a new value to add to the collection of values that
     * this configuration item has. <code>.transport</code> can only have
     * one value.
     * @throws IllegalArgumentException if an attempt is made to add
     * more than one </code>.transport</code> values to this node
     * @throws IllegalArgumentException if an illegal key is used
     */
    void addConfig (String key, String value)
    {
        int keyIndex = configItems.indexOf (key);
        if (keyIndex == -1) throw new IllegalArgumentException ("Unknown configuration item: "+key);
        List conf = configuration [keyIndex];
        if (conf.size() > 0 && key.equals(".transport")) {
            throw new IllegalArgumentException (key+" already set in this node");
        }
        if (key.startsWith (".file.")) {
            int start,end = value.length();
            int index = conf.size();
            do {
                start = value.lastIndexOf (',',end-1);
                conf.add (index, value.substring (start+1,end).trim());
                end = start;
            } while (start > -1);
        } else conf.add (value);
    }
    
    /**
     * Gets all the values assigned to this configuration item
     * @param key one of the predefined configuration items
     * @throws IllegalArgumentException if an illegal key is used
     */
    public Collection getConfig (String key)
    {
        int keyIndex = configItems.indexOf (key);
        if (keyIndex == -1) throw new IllegalArgumentException ("Unknown configuration item: "+key);

        List build = new ArrayList (configuration [keyIndex]);
        if (parent != null) {
            build.addAll (((Node)parent).getConfig (key));
        }
        return (build);
    }
    
    /**
     * Checks that the given key is one of the allowed predefined keys
     * @param key the key to check
     * @return <code>true</code> if it is a valid key
     */
    boolean isValidKey (String key)
    {
        return (configItems.indexOf (key) != -1);
    }
    
    /**
     * Purely for debugging purposes.
     * @return a beautifully formatted depiction of the tree
     */
    public String infoString()
    {
        if (parent == null) indent = 0;
        StringBuffer buf = new StringBuffer();
        for (int ii=0; ii<indent; ii++) buf.append (' ');
        buf.append ("Node: "+title+"\n");
        for (int i=0; i<configuration.length; i++) {
            List conf = configuration[i];
            if (!conf.isEmpty()) {
                for (Iterator it = conf.iterator(); it.hasNext();) {
                    for (int ii=0; ii<indent; ii++) buf.append (' ');
                    buf.append (configItems.get(i)+"=");
                    buf.append (((String)it.next())+'\n');
                }
            }
        }
        indent++;
        for (Iterator it = children.iterator(); it.hasNext();) {
            buf.append ((Node)it.next());
        }
        indent--;
        return buf.toString();
    }
    
    /**
     * Checks this node for the presence of insert items.
     * For all that are found, they are removed from
     * existance and an attempt is made to follow them
     */
    public void expand (final DefaultTreeModel model) // throws CompilationException
    {
        List inserts = configuration[configItems.indexOf (".insert")];
        for (Iterator it = inserts.iterator(); it.hasNext();) {
            final String lookup = (String)it.next();
            it.remove();
            Thread lookupThread = new Thread() {
                public void run() {
                    lookups++;
                    DefaultMutableTreeNode tempNode = new DefaultMutableTreeNode ("Looking up "+lookup+"...");
                    model.insertNodeInto (tempNode, Node.this, 0);
                    try {
                        URL url = new URL (lookup);
                        InputStream is = url.openStream();
                        new Parser (project, model).parse (Node.this, is);
                        is.close();
                        lookups--;
                        model.removeNodeFromParent (tempNode);
                    } catch (CompilationException ex) {
                        ex.addFilename (lookup);
                        tempNode.setUserObject (ex.getSimpleString());
                        lookups--;
                        model.nodeChanged (tempNode);
                    } catch (IOException ex) {
                        tempNode.setUserObject ("Failed to open "+lookup);
                        lookups--;
                        model.nodeChanged (tempNode);
                    }
                }
            };
            lookupThread.start();
        }
    }

    public String toString()
    {
        return title;
    }
    
    public Object getUserObject()
    {
        return title;
    }
}
