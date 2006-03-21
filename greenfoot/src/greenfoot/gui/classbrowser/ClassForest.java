package greenfoot.gui.classbrowser;

import greenfoot.core.GClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import bluej.utility.Debug;

/**
 * A forest of trees. The roots are sorted alphabeticaly on their keys
 * 
 * @author Poul Henriksen
 * @version $Id: ClassForest.java 3856 2006-03-21 22:43:33Z mik $
 */
public class ClassForest
{
    public class TreeEntry
        implements Comparable
    {
        private List children = new ArrayList();
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

        public List getChildren()
        {
            return children;
        }

       
        public String toString()
        {
            StringBuffer str = new StringBuffer();
            for (Iterator iter = children.iterator(); iter.hasNext();) {
                TreeEntry element = (TreeEntry) iter.next();
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

        public int compareTo(Object o)
        {
            String name1 = this.getKey();
            String name2 = ((TreeEntry) o).getKey();
            return name1.compareTo(name2);
        }
    }

    private SortedSet roots = new TreeSet();
    private Map treeEntryMap = new LinkedHashMap();
    
    public ClassForest()
    { }

    public Set getRoots()
    {
        return roots;
    }

    public String toString()
    {
        StringBuffer str = new StringBuffer();
        for (Iterator iter = roots.iterator(); iter.hasNext();) {
            str.append(iter.next());
        }
        return str.toString();
    }
    
    /**
     * Add a new class to this forest. After adding classes, before
     * the forest can be displayed, the rebuild() method must be called.
     */
    public void add(ClassView cls)
    {
        String name =  cls.getClassName();
        TreeEntry entry = new TreeEntry(cls, name);
        treeEntryMap.put(name, entry);
        System.out.println("adding tree entry: " + name);
    }

    /**
     * Remove a class from this forest.
     */
    public boolean remove(ClassView cls)
    {
        String name =  cls.getClassName();
        if(treeEntryMap.remove(name) != null) {
            rebuild();
            return true;
        }
        return false;
    }

    /**
     * Rebuild the forest from the classes in the entry map.
     * This method must be called after adding new elements.
     */
    public void rebuild() 
    {
        System.out.println("rebuilding tree");
        roots = new TreeSet();
        Collection values = treeEntryMap.values();

        for (Iterator iter = values.iterator(); iter.hasNext();) {
            TreeEntry entry = ((TreeEntry) iter.next());
            entry.removeChildren();
        }

        for (Iterator iter = values.iterator(); iter.hasNext();) {
            TreeEntry entry = ((TreeEntry) iter.next());
            addEntryToTree(entry.getData());
        }
    }
    
    
    /**
     * Add an entry from this forrest properly into the tree structure.
     */
    private void addEntryToTree(ClassView clsView)
    {
        GClass cls = clsView.getGClass();
        String superName = cls.getSuperclassGuess();

        TreeEntry child = (TreeEntry) treeEntryMap.get(cls.getName());
        if(superName == null || superName.equals("")) {
            roots.add(child);
        } else {
            int index = superName.lastIndexOf('.');
            if (index >= 0) {
                superName = superName.substring(index + 1);
            }
            TreeEntry parent = (TreeEntry) treeEntryMap.get(superName);
            if(parent != null) {
                parent.addChild(child);
            } else {
                //We couldn't find the superclass, so we just add it as a root
                roots.add(child);
            }             	
        }
    }

    
    /**
     * Returns an iterator over the data in this forest.
     * @return
     */
    public Iterator iterator()
    {
        if ((treeEntryMap == null) || (treeEntryMap.values() == null)) {
            Debug.reportError("class tree is null - should never happen");
        }

        Iterator i = new Iterator() {
            Iterator treeEntries = treeEntryMap.values().iterator();

            /**
             * Not implemented
             */
            public void remove() { }

            public boolean hasNext()
            {
                return treeEntries.hasNext();
            }

            public Object next()
            {
                return ((TreeEntry) treeEntries.next()).getData();
            }

        };
        return i;
    }
}