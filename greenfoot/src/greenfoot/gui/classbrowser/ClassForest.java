/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
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
 */
public class ClassForest
{
    public static class TreeEntry
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
            if(this.equals(child)) {
                throw new IllegalArgumentException(" Cannot add TreeEntry as a child of itself: " + this);
            }
            List<TreeEntry> childsChildren = child.getChildren();
            for (TreeEntry treeEntry : childsChildren) {
                if(this.equals(treeEntry)) {
                    // found a cycle, just return and don't add it.
                    // Should not be able to happen since it is checked in GClass
                    System.err.println("ClassForest found Cycle between: " + key + " and " + child);
                    return;
                }
            }
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

        public void rename(String newKey)
        {
            this.key = newKey;
        }
    }

    private SortedSet<TreeEntry> roots = new TreeSet<TreeEntry>();
    private Map<String, TreeEntry> treeEntryMap = new LinkedHashMap<String, TreeEntry>();
    
    public ClassForest() { }

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

    public void add(TreeEntry entry)
    {
        treeEntryMap.put(entry.getKey(), entry);
        List<TreeEntry> children = entry.getChildren();
        for (TreeEntry treeEntry : children) {
            add(treeEntry);
        }
    }

    /**
     * Remove a class from this forest, including all its children if it has
     * any. Returns the removed TreeEntry.
     */
    public synchronized TreeEntry remove(ClassView cls)
    {        
        String name =  cls.getClassName();
        TreeEntry removedEntry = treeEntryMap.remove(name);
        if (removedEntry != null) {
            // Make sure to update the key in the entry if it has changed.
            String oldName = removedEntry.getKey();
            if(!name.equals(oldName)) {
                removedEntry.rename(name);                
            }
            List<TreeEntry> children = removedEntry.getChildren();
            for (TreeEntry child : children) {
                remove(child.getData());    
            }
        }

        return removedEntry;
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
            String newName = cls.getClassName();
            entry.rename(newName);
            treeEntryMap.put(newName, entry);
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
        String superName = clsView.getSuperclass();
        TreeEntry child = treeEntryMap.get(clsView.getClassName());
        if(superName == null || superName.equals("")) {
            roots.add(child);
        } else {
            superName = GreenfootUtil.extractClassName(superName);
            TreeEntry parent = treeEntryMap.get(superName);
            if(parent != null) {
                parent.addChild(child);
            } else {
                //We couldn't find the superclass, so we just add it as a root
                roots.add(child);
            }
        }
    }
}
