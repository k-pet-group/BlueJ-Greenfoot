package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.graph.Vertex;
import bluej.graph.GraphEditor;
import bluej.utility.MultiIterator;
import bluej.utility.Utility;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Iterator;
import java.util.Collections;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;

/**
 * @version $Id: DependentTarget.java 1418 2002-10-18 09:38:56Z mik $
 * @author Michael Cahill
 *
 * A general target in a package
 */
public abstract class DependentTarget extends Target
{
    protected List inUses;
    protected List outUses;
    protected List parents;
    protected List children;
    /**
     * Create a new target at a specified position.
     */
    public DependentTarget(Package pkg, String identifierName)
    {
        super(pkg, identifierName);
//             calculateWidth(identifierName), DEF_HEIGHT);

        inUses = new ArrayList();
        outUses = new ArrayList();
        parents = new ArrayList();
        children = new ArrayList();
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
        int cy = y + height / 2;
        int n_top = 0, n_bottom = 0;
        for(int i = outUses.size() - 1; i >= 0; i--) {
            Target to = ((Dependency)outUses.get(i)).getTo();
            int to_cy = to.y + to.height / 2;
            if(to_cy < cy)
                ++n_top;
            else
                ++n_bottom;
        }

        // Assign source coordinates to each arrow
        int top_left = x + (width - (n_top - 1) * ARR_HORIZ_DIST) / 2;
        int bottom_left = x + (width - (n_bottom - 1) * ARR_HORIZ_DIST) / 2;
        for(int i = 0; i < n_top + n_bottom; i++) {
            UsesDependency d = (UsesDependency)outUses.get(i);
            int to_cy = d.getTo().y + d.getTo().height / 2;
            if(to_cy < cy) {
                d.setSourceCoords(top_left, y - 4, true);
                top_left += ARR_HORIZ_DIST;
            }
            else {
                d.setSourceCoords(bottom_left, y + height + 4, false);
                bottom_left += ARR_HORIZ_DIST;
            }
        }
    }

    public void recalcInUses()
    {
        // Order the arrows by quadrant and then appropriate coordinate
        Collections.sort(inUses, new LayoutComparer(this, true));

        // Count the number of arrows into each quadrant
        int cx = x + width / 2;
        int n_left = 0, n_right = 0;
        for(int i = inUses.size() - 1; i >= 0; i--)
            {
                Target from = ((Dependency)inUses.get(i)).getFrom();
                int from_cx = from.x + from.width / 2;
                if(from_cx < cx)
                    ++n_left;
                else
                    ++n_right;
            }

        // Assign source coordinates to each arrow
        int left_top = y + (height - (n_left - 1) * ARR_VERT_DIST) / 2;
        int right_top = y + (height - (n_right - 1) * ARR_VERT_DIST) / 2;
        for(int i = 0; i < n_left + n_right; i++)
            {
                UsesDependency d = (UsesDependency)inUses.get(i);
                int from_cx = d.getFrom().x + d.getFrom().width / 2;
                if(from_cx < cx)
                    {
                        d.setDestCoords(x - 4, left_top, true);
                        left_top += ARR_VERT_DIST;
                    }
                else
                    {
                        d.setDestCoords(x + width + 4, right_top, false);
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
        double m = (double)height / width;

        if(Math.abs(tan) < m)	// side
            radius = 0.5 * width / Math.abs(cos);
        else	// top
            radius = 0.5 * height / Math.abs(sin);

        Point p = new Point(x + width / 2 + (int)(radius * cos),
                            y + height / 2 - (int)(radius * sin));

        // Correct for shadow
        if((-m < tan) && (tan < m) && (cos > 0))	// right side
            p.x += SHAD_SIZE;
        if((Math.abs(tan) > m) && (sin < 0) && (p.x > x + SHAD_SIZE))	// bottom
            p.y += SHAD_SIZE;

        return p;
    }

    abstract Color getBackgroundColour();
    abstract Color getBorderColour();
    abstract Color getTextColour();
    abstract Font getFont();

    public void repaint()
    {
        if (getPackage().getEditor() != null)
            getPackage().getEditor().repaint(x, y, width + SHAD_SIZE, height + SHAD_SIZE);
    }

    public void mousePressed(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        super.mousePressed(evt, x, y, editor);
    }

    public void mouseReleased(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        Rectangle newRect = new Rectangle(this.x, this.y, width, height);

        super.mouseReleased(evt, x, y, editor);

        if(!newRect.equals(oldRect)) {
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
            editor.revalidate();
            editor.repaint();
        }
    }

    public String toString()
    {
        return getDisplayName();
    }
}
