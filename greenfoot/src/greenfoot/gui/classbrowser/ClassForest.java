package greenfoot.gui.classbrowser;

import greenfoot.util.GreenfootUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A forest of trees. The roots are sorted alphabetically on their keys
 * 
 * @author Poul Henriksen
 * @version $Id: ClassForest.java 6029 2008-12-06 14:05:33Z polle $
 */
public class ClassForest
{
    public class TreeEntry
        implements Comparable<TreeEntry>
    {
        private List<TreeEntry> children = new ArrayList<TreeEntry>();
        private ClassView data;
        private String key;

        public TreeEntry(ClassView data, String key)
        {
            this.data = data;
            this.key = key;
        }

        public void addChild(TreeEntry child)
        {
            children.add(child);
        }
        
        public void removeChildren()
        {
            children.clear();
        }

        public List<TreeEntry> getChildren()
        {
            return children;
        }

       
        public String toString()
        {
            StringBuffer str = new StringBuffer();
            for (Iterator<TreeEntry> iter = children.iterator(); iter.hasNext();) {
                TreeEntry element = iter.next();
                str.append(" " + element.toString());
            }
            return key + "(" + str + " )";
        }

        private String getKey()
        {
            return key;
        }

        public ClassView getData()
        {
            return data;
        }

        public int compareTo(TreeEntry o)
        {
            String name1 = this.getKey();
            String name2 = o.getKey();
            return name1.compareTo(name2);
        }
        
        public boolean equals(Object o) 
        {
            if(o instanceof TreeEntry) {
                TreeEntry other = (TreeEntry) o;                
                return getKey().equals(other.getKey());
            }
            return false;
        }
        
        public int hashCode() 
        {
            return getKey().hashCode();
        }
    }

    private SortedSet<TreeEntry> roots = new TreeSet<TreeEntry>();
    private Map<String, TreeEntry> treeEntryMap = new LinkedHashMap<String, TreeEntry>();
    
    public ClassForest()
    { }

    /**
     * Get the root classes of this ClassForest (i.e. classes which have no
     * parent classes).
     */
    public Set<TreeEntry> getRoots()
    {
        return roots;
    }

    public String toString()
    {
        StringBuffer str = new StringBuffer();
        for (Iterator<TreeEntry> iter = roots.iterator(); iter.hasNext();) {
            str.append(iter.next());
        }
        return str.toString();
    }
    
    /**
     * Add a new class to this forest. After adding classes, before
     * the forest can be displayed, the rebuild() method must be called.
     */
    public synchronized void add(ClassView cls)
    {
        String name =  cls.getClassName();
        TreeEntry entry = new TreeEntry(cls, name);
        treeEntryMap.put(name, entry);
    }

    /**
     * Remove a class from this forest.
     */
    public synchronized boolean remove(ClassView cls)
    {
        String name =  cls.getClassName();
        if(treeEntryMap.remove(name) != null) {
            rebuild();
            return true;
        }
        return false;
    }
    
    /**
     * Rename a class in this forest
     * @param cls      The existing class view (with new name)
     * @param oldName  The original name
     */
    public synchronized void rename(ClassView cls, String oldName)
    {
        TreeEntry entry = treeEntryMap.remove(oldName);
        if (entry != null) {
            treeEntryMap.put(cls.getClassName(), entry);
        }
    }

    /**
     * Rebuild the forest from the classes in the entry map.
     * This method must be called after adding new elements.
     */
    public synchronized void rebuild() 
    {
        roots = new TreeSet<TreeEntry>();
        Collection<TreeEntry> values = treeEntryMap.values();

        for (Iterator<TreeEntry> iter = values.iterator(); iter.hasNext();) {
            TreeEntry entry = iter.next();
            entry.removeChildren();
        }

        for (Iterator<TreeEntry> iter = values.iterator(); iter.hasNext();) {
            TreeEntry entry = iter.next();
            addEntryToTree(entry.getData());
        }
    }
    
    
    /**
     * Add an entry from this forest properly into the tree structure.
     */
    private void addEntryToTree(ClassView clsView)
    {
        String superName = clsView.getSuperclassGuess();

        TreeEntry child = (TreeEntry) treeEntryMap.get(clsView.getClassName());
        if(superName == null || superName.equals("")) {
            roots.add(child);
        } else {
            superName = GreenfootUtil.extractClassName(superName);
            TreeEntry parent = (TreeEntry) treeEntryMap.get(superName);
            if(parent != null) {
                parent.addChild(child);
            } else {
                //We couldn't find the superclass, so we just add it as a root
                roots.add(child);
            }             	
        }
    }
}
