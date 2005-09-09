package greenfoot.gui.classbrowser;

import greenfoot.core.GClass;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * A forest of trees. The roots are sorted alphabeticaly on their keys
 * 
 * @author Poul Henriksen
 * @version $Id: ClassForest.java 3556 2005-09-09 13:40:58Z polle $
 */
public class ClassForest
{
    public class TreeEntry
        implements Comparable
    {
        private List children = new ArrayList();
        private TreeEntry parent;
        private Object data;
        private String parentKey;
        private String key;

        public TreeEntry(Object data, String key, String parentKey)
        {
            this.data = data;
            this.parentKey = parentKey;
            this.key = key;
        }

        public void addChild(TreeEntry child)
        {
            children.add(child);
            child.setParent(this);
        }

        private void setParent(TreeEntry entry)
        {
            this.parent = entry;
        }

        public boolean hasParent()
        {
            return (parent != null);
        }

        public List getChildren()
        {
            return children;
        }

        public Object getParentKey()
        {
            return parentKey;
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

        /**
         * @return
         */
        public Object getData()
        {
            return data;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
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

    public ClassForest(List objects, List keys, List parents)
    {
        buildForest(objects, keys, parents);
    }

    public ClassForest(Set entrySet)
    {
        buildForest(entrySet);
    }

    public void buildForest(List objects, List keys, List parents)
    {
        treeEntryMap = new java.util.Hashtable();
        roots = new TreeSet();
        for (int i = 0; i < objects.size(); i++) {
            TreeEntry treeEntry = new TreeEntry(objects.get(i), (String) keys.get(i), (String) parents.get(i));
            treeEntryMap.put(keys.get(i), treeEntry);
        }

        Set entrySet = treeEntryMap.entrySet();
        buildForest(entrySet);
    }

    public void buildForest(Set entrySet)
    {
        for (Iterator iter = entrySet.iterator(); iter.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iter.next();

            Object key = mapEntry.getKey();
            TreeEntry treeEntry = (TreeEntry) mapEntry.getValue();
            Object parent = treeEntry.getParentKey();

            TreeEntry parentTreeEntry = null;
            if (parent != null) {
                parentTreeEntry = (TreeEntry) treeEntryMap.get(parent);
            }
            if (parentTreeEntry != null) {
                parentTreeEntry.addChild(treeEntry);
                if (!treeEntry.hasParent()) {
                    roots.add(parentTreeEntry);
                }
                roots.remove(treeEntry);
            }
            else {
                if (!treeEntry.hasParent()) {
                    roots.add(treeEntry);
                }
            }

        }
    }

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
     * @param worldClassesList
     */
    public void buildForest(List classesList) 
    {
        Map classesAndSupers = new Hashtable();
        List classNames = new ArrayList();
        List superclassNames = new ArrayList();
        for (Iterator iter = classesList.iterator(); iter.hasNext();) {
            GClass element = ((ClassView) iter.next()).getGClass();
            String name = element.getQualifiedName();
            int index = name.lastIndexOf('.');
            if (index >= 0) {
                name = name.substring(index + 1);
            }
            classNames.add(name);
            superclassNames.add(element.getSuperclassGuess());
            //classesAndSupers.put(element.getSuperclassName(), element);
            
        }
        buildForest(classesList, classNames, superclassNames);
    }

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