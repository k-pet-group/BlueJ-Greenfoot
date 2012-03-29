/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012  Michael Kolling and John Rosenberg 
 
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

import java.awt.*;
import java.util.*;
import java.util.List;

import bluej.graph.Moveable;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.dependency.*;
import bluej.utility.MultiIterator;

/**
 * A target that has relationships to other targets
 *
 * @author   Michael Cahill
 * @author   Michael Kolling
 */
public abstract class DependentTarget extends EditableTarget
{
    /** States * */
    public static final int S_NORMAL = 0;
    public static final int S_INVALID = 1;
    public static final int S_COMPILING = 2;

    protected int state = S_INVALID;

    private List<UsesDependency> inUses;
    private List<UsesDependency> outUses;
    private List<Dependency> parents;
    private List<Dependency> children;

    protected DependentTarget assoc;

    /**
     * Create a new target belonging to the specified package.
     */
    public DependentTarget(Package pkg, String identifierName)
    {
        super(pkg, identifierName);

        inUses = new ArrayList<UsesDependency>();
        outUses = new ArrayList<UsesDependency>();
        parents = new ArrayList<Dependency>();
        children = new ArrayList<Dependency>();

        assoc = null;
    }
    
    @Override
    public void setPos(int x, int y)
    {
        super.setPos(x,y);
        recalcDependentPositions();
    }

    @Override
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
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        if (getAssociation() != null) {
            String assocName = getAssociation().getIdentifierName(); 
            props.put(prefix + ".association", assocName);
        }
    }

    public void setAssociation(DependentTarget t)
    {
        assoc = t;
        //assoiated classes are not allowed to move on their own
        if (assoc instanceof Moveable){
            ((Moveable)assoc).setIsMoveable(false);
        }
    }

    public DependentTarget getAssociation()
    {
        return assoc;
    }

    public void addDependencyOut(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency) {
            outUses.add((UsesDependency) d);
            if(recalc)
                recalcOutUses();
        }
        else if((d instanceof ExtendsDependency)
                || (d instanceof ImplementsDependency)) {
            parents.add(d);
        }

        if(recalc) {
            setState(S_INVALID);
        }
    }

    public void addDependencyIn(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency) {
            inUses.add((UsesDependency) d);
            if(recalc)
                recalcInUses();
        }
        else if((d instanceof ExtendsDependency)
                || (d instanceof ImplementsDependency)) {
            children.add(d);
        }
    }

    public void removeDependencyOut(Dependency d, boolean recalc)
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
        
        if(recalc)
            setState(S_INVALID);
    }

    public void removeDependencyIn(Dependency d, boolean recalc)
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

    public Iterator<? extends Dependency> dependencies()
    {
        List<Iterator<? extends Dependency>> v = new ArrayList<Iterator<? extends Dependency>>(2);
        v.add(parents.iterator());
        v.add(outUses.iterator());
        return new MultiIterator<Dependency>(v);
    }

    public Iterator<? extends Dependency> dependents()
    {
        List<Iterator<? extends Dependency>> v = new ArrayList<Iterator<? extends Dependency>>(2);
        v.add(children.iterator());
        v.add(inUses.iterator());
        return new MultiIterator<Dependency>(v);
    }
    
    /**
     * Get the dependencies between this target and its parent(s).
     * The returned list should not be modified and may be a view or a copy.
     */
    public List<Dependency> getParents()
    {
        return Collections.unmodifiableList(parents);
    }
    
    /**
     * Get the dependencies between this target and its children.
     * 
     * @return
     */
    public List<Dependency> getChildren()
    {
        return Collections.unmodifiableList(children);
    }
    
    public List<Dependency> dependentsAsList()
    {
        List<Dependency> list = new LinkedList<Dependency>();
        list.addAll(inUses);
        list.addAll(outUses);
        list.addAll(children);
        list.addAll(parents);
        return list;
    }

    public Iterator<UsesDependency> usesDependencies()
    {
        return Collections.unmodifiableList(outUses).iterator();
    }
    
    /**
     *  Remove all outgoing dependencies. Also updates the package. (Don't
     *  call from package remove method - this will cause infinite recursion.)
     */
    protected void removeAllOutDependencies()
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
    protected void removeInheritDependencies()
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
    protected void removeAllInDependencies()
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

    public void recalcOutUses()
    {
        // Determine the visible outgoing uses dependencies
        List<UsesDependency> visibleOutUses = getVisibleUsesDependencies(outUses);

        // Order the arrows by quadrant and then appropriate coordinate
        Collections.sort(visibleOutUses, new LayoutComparer(this, false));

        // Count the number of arrows into each quadrant
        int cy = getY() + getHeight() / 2;
        int n_top = 0, n_bottom = 0;
        for(int i = visibleOutUses.size() - 1; i >= 0; i--) {
            Target to = ((Dependency) visibleOutUses.get(i)).getTo();
            int to_cy = to.getY() + to.getHeight() / 2;
            if(to_cy < cy)
                ++n_top;
            else
                ++n_bottom;
        }

        // Assign source coordinates to each arrow
        int top_left = getX() + (getWidth() - (n_top - 1) * ARR_HORIZ_DIST) / 2;
        int bottom_left = getX() + (getWidth() - (n_bottom - 1) * ARR_HORIZ_DIST) / 2;
        for(int i = 0; i < n_top + n_bottom; i++) {
            UsesDependency d = (UsesDependency) visibleOutUses.get(i);
            int to_cy = d.getTo().getY() + d.getTo().getHeight() / 2;
            if(to_cy < cy) {
                d.setSourceCoords(top_left, getY() - 4, true);
                top_left += ARR_HORIZ_DIST;
            }
            else {
                d.setSourceCoords(bottom_left, getY() + getHeight() + 4, false);
                bottom_left += ARR_HORIZ_DIST;
            }
        }
    }

    /**
     * Re-layout arrows into this target
     */
    public void recalcInUses()
    {
        // Determine the visible incoming uses dependencies
        List<UsesDependency> visibleInUses = getVisibleUsesDependencies(inUses);

        // Order the arrows by quadrant and then appropriate coordinate
        Collections.sort(visibleInUses, new LayoutComparer(this, true));

        // Count the number of arrows into each quadrant
        int cx = getX() + getWidth() / 2;
        int n_left = 0, n_right = 0;
        for(int i = visibleInUses.size() - 1; i >= 0; i--)
        {
            Target from = ((Dependency) visibleInUses.get(i)).getFrom();
            int from_cx = from.getX() + from.getWidth() / 2;
            if(from_cx < cx)
                ++n_left;
            else
                ++n_right;
        }

        // Assign source coordinates to each arrow
        int left_top = getY() + (getHeight() - (n_left - 1) * ARR_VERT_DIST) / 2;
        int right_top = getY() + (getHeight() - (n_right - 1) * ARR_VERT_DIST) / 2;
        for(int i = 0; i < n_left + n_right; i++)
        {
            UsesDependency d = (UsesDependency) visibleInUses.get(i);
            int from_cx = d.getFrom().getX() + d.getFrom().getWidth() / 2;
            if(from_cx < cx)
            {
                d.setDestCoords(getX() - 4, left_top, true);
                left_top += ARR_VERT_DIST;
            }
            else
            {
                d.setDestCoords(getX() + getWidth() + 4, right_top, false);
                right_top += ARR_VERT_DIST;
            }
        }
    }

    /**
     * Returns from the specified {@link List} all uses dependencies which are
     * currently visible.
     * 
     * @param usesDependencies
     *            A {@link List} of uses dependencies.
     * @return A {@link List} containing all visible uses dependencies from the
     *         input list.
     */
    private List<UsesDependency> getVisibleUsesDependencies(List<UsesDependency> usesDependencies)
    {
        List<UsesDependency> result = new ArrayList<UsesDependency>();

        for (UsesDependency incomingUsesDependency : usesDependencies) {
            if (incomingUsesDependency.isVisible()) {
                result.add(incomingUsesDependency);
            }
        }

        return result;
    }

    /**
     *  Clear the flag in a outgoing uses dependencies
     */
    protected void unflagAllOutDependencies()
    {
        for(int i = 0; i < outUses.size(); i++)
            ((UsesDependency)outUses.get(i)).setFlag(false);
    }

    public Point getAttachment(double angle)
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

        Point p = new Point(getX() + getWidth() / 2 + (int)(radius * cos),
                            getY() + getHeight() / 2 - (int)(radius * sin));

        // Correct for shadow
        if((-m < tan) && (tan < m) && (cos > 0))    // right side
            p.x += SHAD_SIZE;
        if((Math.abs(tan) > m) && (sin < 0) && (p.x > getX() + SHAD_SIZE))  // bottom
            p.y += SHAD_SIZE;

        return p;
    }
    
    
    /**
     * The user may have moved or resized the target. If so, recalculate the
     * dependency arrows associated with this target.
     * @param editor
     */
    public void recalcDependentPositions() 
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

    protected void updateAssociatePosition()
    {
        DependentTarget t = getAssociation();

        if (t != null) {
            //TODO magic numbers. Should also take grid size in to account.
            t.setPos(getX() + 30, getY() - 30);
            t. recalcDependentPositions();
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
    public int getState()
    {
        return state;
    }

    public boolean isInvalidState()
    {
        return getState() == S_INVALID;
    }

    public void setInvalidState()
    {
        setState(S_INVALID);
    }
    
    /**
     * Change the state of this target. The target will be repainted to show the
     * new state.
     * 
     * @param newState The new state value
     */
    public void setState(int newState)
    {
        state = newState;
        repaint();
    }

}
