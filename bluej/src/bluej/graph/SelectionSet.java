/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013,2014,2016,2017,2018  Michael Kolling and John Rosenberg
 
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
package bluej.graph;

import bluej.pkgmgr.target.Target;
import bluej.utility.javafx.FXPlatformConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SelectionSet holds a set of selected graph elements. By inserting an
 * element into this set, it is automatically set to selected.
 * 
 * @author fisker
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public final class SelectionSet
{
    private Set<Target> elements = new HashSet<>();
    private List<FXPlatformConsumer<Collection<Target>>> listeners = new ArrayList<>();

    /**
     * 
     * @param graphEditor
     */
    @OnThread(Tag.Any)
    public SelectionSet(Collection<Target> initial)
    {
        elements.addAll(initial);
    }

    /**
     * Add an unselected selectable graphElement to the set and
     * set it's 'selected' flag.
     * 
     * @param element  The element to add
     */
    public void add(Target element)
    {
        if (!element.isSelected()) {
            element.setSelected(true);
            elements.add(element);
            notifyListeners();
        }
    }
    
    /**
     * Add all the elements from another selection set to this one.
     */
    public void addAll(Collection<Target> newSet)
    {
        for (Target t : newSet)
            add(t); // add will notify listeners
    }

    /**
     * Remove the graphElement and set it's 'selected' flag false.
     * 
     * @param graphElement
     */
    public void remove(Target element)
    {
        if (element != null) {
            element.setSelected(false);
            elements.remove(element);
            notifyListeners();
        }
    }

    /**
     * Remove all the graphElements from the list. Set each removed grahpElement
     * 'selected' flag to false. Does NOT selfuse remove method.
     */
    public void clear()
    {
        for (Target element : elements) {
            element.setSelected(false);
        }
        elements.clear();
        notifyListeners();
    }

    /**
     * Perform a double click on the selection.
     * 
     * @param  openInNewWindow if this is true, the editor opens in a new window.
     */
    public void doubleClick(boolean openInNewWindow)
    {
        for (Target element : elements) {
            element.doubleClick(openInNewWindow);
        }        
    }

    /**
     * A move gesture (either move or resize) has stopped. Inform all elements
     * in this selection that they shoudl react.
     */
    public void moveStopped()
    {
    }

    /**
     * Tell whether the selection is empty.
     * 
     * @return  true, if the selection is empty.
     */
    public boolean isEmpty()
    {
        return elements.isEmpty();
    }
    
    /**
     * Change the selection to contain only the specified element.
     * 
     * @param element  The single element to hold in the selection. 
     */
    public void selectOnly(Target element)
    {
        // add and clear will notify listeners
        clear();
        add(element);
    }
    
    /** 
     * Return a random vertex from this selection.
     * @return  An vertex, or null, if none exists.
     */
    public Target getAnyVertex()
    {
        for (Target element : elements) {
            return element;
        }
        return null;
    }

    public List<Target> getSelected()
    {
        return new ArrayList<>(elements);
    }

    private void notifyListeners()
    {
        List<Target> curSelection = new ArrayList<>(this.elements);
        for (FXPlatformConsumer<Collection<Target>> listener : listeners)
        {
            listener.accept(curSelection);
        }
    }

    /**
     * The listener will be run whenever the selection has been changed.
     * It may be run partway through some actions so allow for rapid
     * repeated changes.
     */
    public void addListener(FXPlatformConsumer<Collection<Target>> listener)
    {
        listeners.add(listener);
    }
}
