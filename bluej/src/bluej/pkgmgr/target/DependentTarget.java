/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
 * @author 	Michael Cahill
 * @version	$Id: DependentTarget.java 6215 2009-03-30 13:28:25Z polle $
 */
public abstract class DependentTarget extends EditableTarget
{
    /** States * */
    public static final int S_NORMAL = 0;
    public static final int S_INVALID = 1;
    public static final int S_COMPILING = 2;

    protected int state = S_INVALID;

    private List inUses;
    private List outUses;
    private List parents;
    private List children;

    protected DependentTarget assoc;
	
    /**
     * Create a new target belonging to the specified package.
     */
    public DependentTarget(Package pkg, String identifierName)
    {
        super(pkg, identifierName);

        inUses = new ArrayList();
        outUses = new ArrayList();
        parents = new ArrayList();
        children = new ArrayList();

        assoc = null;
    }
    
    public void setPos(int x, int y)
    {
        super.setPos(x,y);
        recalcDependentPositions();
    }

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
            outUses.add(d);
            if(recalc)
                recalcOutUses();
        }
        else if((d instanceof ExtendsDependency)
                || (d instanceof ImplementsDependency)) {
            parents.add(d);
        }

        if(recalc)
            setState(S_INVALID);
    }

    public void addDependencyIn(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency) {
            inUses.add(d);
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
            if(recalc)
                recalcInUses();
        }
        else if((d instanceof ExtendsDependency)
                || (d instanceof ImplementsDependency)) {
            children.remove(d);
        }
    }

    public Iterator dependencies()
    {
        List v = new ArrayList(2);
        v.add(parents.iterator());
        v.add(outUses.iterator());
        return new MultiIterator(v);
    }

    public Iterator dependents()
    {
        List v = new ArrayList(2);
        v.add(children.iterator());
        v.add(inUses.iterator());
        return new MultiIterator(v);
    }
    
    public List dependentsAsList(){
        List list = new LinkedList();
        list.addAll(inUses);
        list.addAll(outUses);
        list.addAll(children);
        list.addAll(parents);
        return list;
    }

	public Iterator usesDependencies()
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
            for(int i = 0; i < outUsesArray.length ; i++)
                getPackage().removeDependency(outUsesArray[i], false);
        }

        removeInheritDependencies();
    }

    /**
     *  Remove inheritence dependencies.
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
        // Order the arrows by quadrant and then appropriate coordinate
        Collections.sort(outUses, new LayoutComparer(this, false));

        // Count the number of arrows into each quadrant
        int cy = getY() + getHeight() / 2;
        int n_top = 0, n_bottom = 0;
        for(int i = outUses.size() - 1; i >= 0; i--) {
            Target to = ((Dependency)outUses.get(i)).getTo();
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
            UsesDependency d = (UsesDependency)outUses.get(i);
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

    public void recalcInUses()
    {
        // Order the arrows by quadrant and then appropriate coordinate
        Collections.sort(inUses, new LayoutComparer(this, true));

        // Count the number of arrows into each quadrant
        int cx = getX() + getWidth() / 2;
        int n_left = 0, n_right = 0;
        for(int i = inUses.size() - 1; i >= 0; i--)
            {
                Target from = ((Dependency)inUses.get(i)).getFrom();
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
                UsesDependency d = (UsesDependency)inUses.get(i);
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

        if(Math.abs(tan) < m)	// side
            radius = 0.5 * getWidth() / Math.abs(cos);
        else	// top
            radius = 0.5 * getHeight() / Math.abs(sin);

        Point p = new Point(getX() + getWidth() / 2 + (int)(radius * cos),
                            getY() + getHeight() / 2 - (int)(radius * sin));

        // Correct for shadow
        if((-m < tan) && (tan < m) && (cos > 0))	// right side
            p.x += SHAD_SIZE;
        if((Math.abs(tan) > m) && (sin < 0) && (p.x > getX() + SHAD_SIZE))	// bottom
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
        for(Iterator it = inUses.iterator(); it.hasNext(); ) {
            Dependency d = (Dependency)it.next();
            d.getFrom().recalcOutUses();
        }
        for(Iterator it = outUses.iterator(); it.hasNext(); ) {
            Dependency d = (Dependency)it.next();
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
