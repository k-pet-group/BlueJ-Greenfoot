package greenfoot.gui.classbrowser;

import greenfoot.core.GClass;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
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
 * @version $Id: ClassForest.java 3659 2005-10-07 22:32:59Z polle $
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

    private SortedSet roots;
    private Map treeEntryMap;

    public ClassForest()
    {}

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
     * Builds a forest from the list of classes
     * @param classesList List of instances of ClassView
     */
    public void buildForest(List classesList) 
    {
        Map nameMap = new Hashtable();
        treeEntryMap = new Hashtable();
        roots = new TreeSet();
        for (Iterator iter = classesList.iterator(); iter.hasNext();) {
            ClassView cls = ((ClassView) iter.next());
            String name =  cls.getClassName();
            nameMap.put(name, cls); 
            TreeEntry root = new TreeEntry(cls, name);
            treeEntryMap.put(name, root);
        }        
        
        for (Iterator iter = classesList.iterator(); iter.hasNext();) {
        	ClassView clsView = ((ClassView) iter.next());
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
            		Debug.reportError("Could not find the superclass for: " + cls.getName() +" which should have been: " + superName + "  Adding it as a root.");
            		roots.add(child);
            	}             	
            }
        }
    }

    
    /**
     * Returns an iterator over the data in this forest.
     * @return
     */
    public Iterator iterator()
    {
        Iterator empty = new Iterator() {

            public void remove()
            {}

            public boolean hasNext()
            {
                return false;
            }

            public Object next()
            {
                return null;
            }
        };

        if (treeEntryMap == null) {
            return empty;
        }

        if (treeEntryMap.values() == null) {
            return empty;
        }

        Iterator i = new Iterator() {
            Iterator treeEntries = treeEntryMap.values().iterator();

            /**
             * Not implemented
             */
            public void remove()
            {

            }

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