package org.bluej.extensions.submitter.properties;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import org.bluej.extensions.submitter.Stat;
    
/**
 * This is a DATA node of the configuration tree.
 * This stores the data only, it does NOT do the loading or the parsing...
 * NOW: It happens that EVERY node can have some values like
 * .transport, .file.include, .file.exclude and so on.file.jar an so on...
 * Clive did some terrible job of storing them in a quite "too smart" way :-)
 * 
 * @author Clive Miller, Damiano Bolla
 * @version $Id: TreeNode.java 1708 2003-03-19 09:39:47Z damiano $
 */
public class TreeNode extends DefaultMutableTreeNode
{
    private static final List configItems = Arrays.asList (new String[] {
        ".transport",
        ".file.include",
        ".file.exclude",
        ".file.essential",
        ".file.jar",
        ".insert"});

    private Stat stat;

    private List[] configuration; // of String
    private final String title;
    
    /**
     * Creates a new node. It is the responsibility of the caller to add it to its parent
     * @param parent the parent that should adopt this child
     * @param title the Title of this node
     * @param whether or not this node belongs to a specific project
     */
    public TreeNode (Stat i_stat, String i_title)
    {
        stat = i_stat;
        title = i_title;
        configuration = new List [configItems.size()];
        for (int i=0,n=configuration.length; i<n; i++) {
            configuration[i] = new ArrayList();
        }
    }
        
    public String getTitle()
    {
        return title;
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
    stat.aDbg.debug(Stat.SVC_PARSER,"Node.addConfig: Node.title="+title+" key="+key+" value="+value);
        
    int keyIndex = configItems.indexOf (key);
    if (keyIndex == -1) throw new IllegalArgumentException ("Unknown configuration item: "+key);
        
    List conf = configuration [keyIndex];
    if (conf.size() > 0 && key.equals(".transport")) 
      throw new IllegalArgumentException (key+" already set in this node");
    
        
    if (key.startsWith (".file.")) 
      {
      int start;
      int end   = value.length();
      int index = conf.size();

      for(;;)
        {
        // This may be doing string tokenizing by hands... please FIX it
        start = value.lastIndexOf (',',end-1);
        conf.add (index, value.substring (start+1,end).trim());
        end = start;
        if ( start <= 0 ) break;
        }

      return;
      } 

    if ( key.startsWith(".insert") )
      {
      stat.aDbg.debug(Stat.SVC_PARSER,"Node.addConfig: .insert Node.title="+title+" key="+key+" value="+value);
      stat.treeData.loadUrl(this,value);   
      return;
      }

    // Nothing else, just add it.    
    conf.add (value);
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
            build.addAll (((TreeNode)parent).getConfig (key));
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
    static int indent;
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
            buf.append ((TreeNode)it.next());
        }
        indent--;
        return buf.toString();
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

