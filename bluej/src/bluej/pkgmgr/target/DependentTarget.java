/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2016,2017,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.dependency.*;
import bluej.utility.javafx.FXPlatformConsumer;
import javafx.geometry.Point2D;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A target that has relationships to other targets
 *
 * @author   Michael Cahill
 * @author   Michael Kolling
 */
public abstract class DependentTarget extends EditableTarget
{
    /**
     * States.  A compile target has two pieces of information.
     *   It can be up-to-date (i.e. class file matches latest source state)
     *   or it can need a compile (i.e. class file lags source state) with unknown error state,
     *   or it can need a compile and be known to have an error.
     */
    @OnThread(Tag.Any)
    public static enum State
    {
        COMPILED, NEEDS_COMPILE, HAS_ERROR;
    }

    @OnThread(Tag.Any)
    private final AtomicReference<State> state = new AtomicReference<>(State.NEEDS_COMPILE);
    @OnThread(Tag.FXPlatform)
    protected final List<TargetListener> stateListeners = new ArrayList<>();

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<UsesDependency> inUses;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<UsesDependency> outUses;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<Dependency> parents;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<Dependency> children;

    @OnThread(value = Tag.Any,requireSynchronized = true)
    protected DependentTarget assoc;

    /**
     * Create a new target belonging to the specified package.
     */
    public DependentTarget(Package pkg, String identifierName, String accessibleTargetType)
    {
        super(pkg, identifierName, accessibleTargetType);

        inUses = new ArrayList<>();
        outUses = new ArrayList<>();
        parents = new ArrayList<>();
        children = new ArrayList<>();

        assoc = null;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void setPos(int x, int y)
    {
        super.setPos(x,y);
        recalcDependentPositions();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setSize(int width, int height)
    {
        super.setSize(width, height);
        recalcDependentPositions();
    }

    /**
     * Save association information about this class target
     * @param props the properties object to save to
     * @param prefix an internal name used for this target to identify
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        if (getAssociation() != null) {
            String assocName = getAssociation().getIdentifierName(); 
            props.put(prefix + ".association", assocName);
        }
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void setAssociation(DependentTarget t)
    {
        assoc = t;
        //assoiated classes are not allowed to move on their own
        if (assoc != null && assoc.isMoveable()){
            assoc.setIsMoveable(false);
        }
        if (assoc != null && assoc.isResizable())
        {
            assoc.setResizable(false);
        }
    }

    @OnThread(value = Tag.Any, requireSynchronized = true)
    public synchronized DependentTarget getAssociation()
    {
        return assoc;
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void addDependencyOut(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency) {
            if (outUses.contains(d))
                return;
            outUses.add((UsesDependency) d);
            if(recalc)
                recalcOutUses();
        }
        else if((d instanceof ExtendsDependency)
                || (d instanceof ImplementsDependency)) {
            if (parents.contains(d))
                return;
            parents.add(d);
        }
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void addDependencyIn(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency) {
            if (inUses.contains(d))
                return;
            inUses.add((UsesDependency) d);
            if(recalc)
                recalcInUses();
        }
        else if((d instanceof ExtendsDependency)
                || (d instanceof ImplementsDependency)) {
            if (children.contains(d))
                return;
            children.add(d);
        }
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void removeDependencyOut(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency) {
            outUses.remove(d);
            if(recalc)
                recalcOutUses();
        }
        else if((d instanceof ExtendsDependency)
                || (d instanceof ImplementsDependency)) {
            parents.remove(d);
        }
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void removeDependencyIn(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency) {
            inUses.remove(d);
            if(recalc) {
                recalcInUses();
            }
        }
        else if((d instanceof ExtendsDependency)
                || (d instanceof ImplementsDependency)) {
            children.remove(d);
        }
    }

    @OnThread(Tag.Any)
    public synchronized Collection<Dependency> dependencies()
    {
        List<Dependency> d = new ArrayList<>();
        d.addAll(parents);
        d.addAll(outUses);
        return d;
    }

    @OnThread(Tag.Any)
    public synchronized Collection<Dependency> dependents()
    {
        List<Dependency> d = new ArrayList<>();
        d.addAll(children);
        d.addAll(inUses);
        return d;
    }
    
    /**
     * Get the dependencies between this target and its parent(s).
     * The returned list should not be modified and may be a view or a copy.
     */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    public synchronized List<Dependency> getParents()
    {
        return Collections.unmodifiableList(new ArrayList<>(parents));
    }
    
    /**
     * Get the dependencies between this target and its children.
     * 
     * @return
     */
    @OnThread(value = Tag.Any)
    public synchronized List<Dependency> getChildrenDependencies()
    {
        return Collections.unmodifiableList(new ArrayList<>(children));
    }

    @OnThread(Tag.Any)
    public synchronized List<Dependency> dependentsAsList()
    {
        List<Dependency> list = new LinkedList<Dependency>();
        list.addAll(inUses);
        list.addAll(outUses);
        list.addAll(children);
        list.addAll(parents);
        return list;
    }

    @OnThread(Tag.Any)
    public synchronized List<UsesDependency> usesDependencies()
    {
        return Collections.unmodifiableList(new ArrayList<>(outUses));
    }
    
    /**
     *  Remove all outgoing dependencies. Also updates the package. (Don't
     *  call from package remove method - this will cause infinite recursion.)
     */
    @OnThread(Tag.FXPlatform)
    protected synchronized void removeAllOutDependencies()
    {
        // While removing the dependencies the dependency list must be
        // copied since the original is modified during this operation.
        // Enumerations over the original would go wrong.

        // delete outgoing uses dependencies
        if(!outUses.isEmpty()) {
            Dependency[] outUsesArray = new Dependency[outUses.size()];
            outUses.toArray(outUsesArray);
            for(int i = 0; i < outUsesArray.length ; i++) {
                getPackage().removeDependency(outUsesArray[i], false);
            }
        }

        removeInheritDependencies();
    }

    /**
     *  Remove inheritance dependencies.
     */
    @OnThread(Tag.FXPlatform)
    protected synchronized void removeInheritDependencies()
    {
        // While removing the dependencies the dependency list must be
        // copied since the original is modified during this operation.
        // Enumerations over the original would go wrong.

        if(!parents.isEmpty()) {
            Dependency[] parentsArray = new Dependency[ parents.size() ];
            parents.toArray(parentsArray);
            for(int i = 0; i < parentsArray.length ; i++)
                getPackage().removeDependency(parentsArray[i], false);
        }
    }

    /**
     *  Remove all incoming dependencies. Also updates the package. (Don't
     *  call from package remove method - this will cause infinite recursion.)
     */
    @OnThread(Tag.FXPlatform)
    protected synchronized void removeAllInDependencies()
    {
        // While removing the dependencies the dependency list must be
        // copied since the original is modified during this operation.
        // Enumerations over the original would go wrong.

        // delete incoming uses dependencies
        if(!inUses.isEmpty()) {
            Dependency[] inUsesArray = new Dependency[ inUses.size() ];
            inUses.toArray(inUsesArray);
            for(int i = 0; i < inUsesArray.length ; i++)
                getPackage().removeDependency(inUsesArray[i], false);
        }

        // delete dependencies to child classes
        if(!children.isEmpty()) {
            Dependency[] childrenArray = new Dependency[ children.size() ];
            children.toArray(childrenArray);
            for(int i = 0; i < childrenArray.length ; i++)
                getPackage().removeDependency(childrenArray[i], false);
        }
    }

    @OnThread(Tag.FXPlatform)
    public void recalcOutUses()
    {
    	List<UsesDependency> outUsesCopy =  new ArrayList<>(usesDependencies());
        // Order the arrows by quadrant and then appropriate coordinate
        Collections.sort(outUsesCopy, new LayoutComparer(this, false));

        // Count the number of arrows into each quadrant
        int cy = getY() + (int)getHeight() / 2;
        int n_top = 0, n_bottom = 0;
        for(int i = outUsesCopy.size() - 1; i >= 0; i--) {
            Target to = ((Dependency) outUsesCopy.get(i)).getTo();
            int to_cy = to.getY() + (int)to.getHeight() / 2;
            if(to_cy < cy)
                ++n_top;
            else
                ++n_bottom;
        }

        // Assign source coordinates to each arrow
        int top_left = getX() + ((int)getWidth() - (n_top - 1) * ARR_HORIZ_DIST) / 2;
        int bottom_left = getX() + ((int)getWidth() - (n_bottom - 1) * ARR_HORIZ_DIST) / 2;
        for(int i = 0; i < n_top + n_bottom; i++) {
            UsesDependency d = (UsesDependency) outUsesCopy.get(i);
            int to_cy = d.getTo().getY() + (int)d.getTo().getHeight() / 2;
            if(to_cy < cy) {
                d.setSourceCoords(top_left, getY() - 2.5, true);
                top_left += ARR_HORIZ_DIST;
            }
            else {
                d.setSourceCoords(bottom_left, getY() + getHeight() + 2.5, false);
                bottom_left += ARR_HORIZ_DIST;
            }
        }
    }

    /**
     * Re-layout arrows into this target
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void recalcInUses()
    {
        // Order the arrows by quadrant and then appropriate coordinate
        Collections.sort(inUses, new LayoutComparer(this, true));

        // Count the number of arrows into each quadrant
        int cx = getX() + (int)getWidth() / 2;
        int n_left = 0, n_right = 0;
        for(int i = inUses.size() - 1; i >= 0; i--)
        {
            Target from = ((Dependency) inUses.get(i)).getFrom();
            int from_cx = from.getX() + (int)from.getWidth() / 2;
            if(from_cx < cx)
                ++n_left;
            else
                ++n_right;
        }

        // Assign source coordinates to each arrow
        int left_top = getY() + ((int)getHeight() - (n_left - 1) * ARR_VERT_DIST) / 2;
        int right_top = getY() + ((int)getHeight() - (n_right - 1) * ARR_VERT_DIST) / 2;
        for(int i = 0; i < n_left + n_right; i++)
        {
            UsesDependency d = (UsesDependency) inUses.get(i);
            int from_cx = d.getFrom().getX() + (int)d.getFrom().getWidth() / 2;
            if(from_cx < cx)
            {
                d.setDestCoords(getX() - 4, left_top, true);
                left_top += ARR_VERT_DIST;
            }
            else
            {
                d.setDestCoords(getX() + (int)getWidth() + 4, right_top, false);
                right_top += ARR_VERT_DIST;
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    public Point2D getAttachment(double angle)
    {
        double radius;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double tan = sin / cos;
        double m = (double) getHeight() / getWidth();

        if(Math.abs(tan) < m)   // side
            radius = 0.5 * getWidth() / Math.abs(cos);
        else    // top
            radius = 0.5 * getHeight() / Math.abs(sin);

        javafx.geometry.Point2D p = new Point2D(getX() + getWidth() / 2 + (int)(radius * cos),
                            getY() + getHeight() / 2 - (int)(radius * sin));

        // Correct for shadow
        /*
        if((-m < tan) && (tan < m) && (cos > 0))    // right side
            p.x += SHAD_SIZE;
        if((Math.abs(tan) > m) && (sin < 0) && (p.x > getX() + SHAD_SIZE))  // bottom
            p.y += SHAD_SIZE;
        */
        return p;
    }
    
    
    /**
     * The user may have moved or resized the target. If so, recalculate the
     * dependency arrows associated with this target.
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void recalcDependentPositions() 
    {
        // Recalculate arrows
        recalcInUses();
        recalcOutUses();

        // Recalculate neighbours' arrows
        for(Iterator<UsesDependency> it = inUses.iterator(); it.hasNext(); ) {
            Dependency d = it.next();
            d.getFrom().recalcOutUses();
        }
        for(Iterator<UsesDependency> it = outUses.iterator(); it.hasNext(); ) {
            Dependency d = it.next();
            d.getTo().recalcInUses();
        }

        updateAssociatePosition();
    }

    @OnThread(Tag.FXPlatform)
    public void updateAssociatePosition()
    {
        DependentTarget t = getAssociation();

        if (t != null) {
            //TODO magic numbers. Should also take grid size in to account.
            t.setPos(getX() + 30, getY() - 30);
            if (isResizable())
                t.setSize(getWidth(), getHeight());
            t.recalcDependentPositions();
        }
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
    
    /**
     * Return the current state of the target (one of S_NORMAL, S_INVALID,
     * S_COMPILING)
     */
    @OnThread(Tag.Any)
    public State getState()
    {
        return state.get();
    }

    /**
     * Mark the class as needing a compile (if it is not marked thus already).
     *
     * Do not call this method on classes which lack source code.
     */
    public void markModified()
    {
        // If it's already NEEDS_COMPILE or HAS_ERROR, no need to change:
        if (getState() == State.COMPILED)
            setState(State.NEEDS_COMPILE);
    }
    
    /**
     * Change the state of this target. The target will be repainted to show the
     * new state.
     * 
     * @param newState The new state value
     */
    @OnThread(Tag.FXPlatform)
    public void setState(State newState)
    {
        state.set(newState);
        updateAccessibleName();
        repaint();
        redraw();
        for (TargetListener stateListener : stateListeners)
        {
            stateListener.stateChanged(newState);
        }
    }

    protected void updateAccessibleName()
    {
        String accessibleSuffix = "";
        switch (state.get())
        {
            case COMPILED:
                accessibleSuffix = "Compiled";
                break;
            case NEEDS_COMPILE:
                accessibleSuffix = "Uncompiled";
                break;
            case HAS_ERROR:
                accessibleSuffix = "Error";
                break;
        }
        updateAccessibleName("Class", ": " + accessibleSuffix);
    }

    /**
     * Adds a TargetListener to changes in this target.
     */
    @OnThread(Tag.FXPlatform)
    public void addListener(TargetListener listener)
    {
        stateListeners.add(listener);
    }

    /**
     * Removes a listener added by addListener
     */
    @OnThread(Tag.FXPlatform)
    public void removeListener(TargetListener listener)
    {
        stateListeners.remove(listener);
    }

    /**
     * A listener to changes in a DependentTarget
     */
    public static interface TargetListener
    {
        /**
         * Called when the editor has been opened.  If the same Editor instance is opened and closed
         * multiple times, this method is called on every open.
         */
        public void editorOpened();
        
        /**
         * Called when state has changed
         */
        public void stateChanged(State newState);

        /**
         * Called when the target is renamed.
         */
        public void renamed(String newName);
    }
}
