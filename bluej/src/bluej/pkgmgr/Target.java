package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.graph.Vertex;
import bluej.graph.GraphEditor;
import bluej.utility.MultiEnumeration;
import bluej.utility.SortableVector;
import bluej.utility.Utility;

import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;

/**
 * @version $Id: Target.java 505 2000-05-24 05:44:24Z ajp $
 * @author Michael Cahill
 *
 * A general target in a package
 */
public abstract class Target extends Vertex
{
    static final int MIN_WIDTH = 60;
    static final int MIN_HEIGHT = 40;
    static final int DEF_WIDTH = 80;
    static final int DEF_HEIGHT = 50;
    static final int ARR_HORIZ_DIST = 5;
    static final int ARR_VERT_DIST = 10;
    static final int GRID_SIZE = 10;

    // move me!
    static final int HANDLE_SIZE = 20;
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 8;
    static final int SHAD_SIZE = 5;

    static final Color textbg = Config.getItemColour("colour.text.bg");
    static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    static final Color graphbg = Config.getItemColour("colour.graph.background");

    /** States **/
    static final int S_NORMAL = 0;
    static final int S_INVALID = 1;
    static final int S_COMPILING = 2;

    /** Flags **/
    static final int F_SELECTED = 1 << 0;
    static final int F_QUEUED = 1 << 1;

    private String identifierName;      // the name handle for this target within
                                        // this package (must be unique within this
                                        // package)
    private String displayName;         // displayed name of the target
    private Package pkg;                // the package this target belongs to

    protected SortableVector inUses;
    protected SortableVector outUses;
    protected Vector parents;
    protected Vector children;
    protected boolean resizing;
    protected boolean disabled;
    protected int drag_start_x, drag_start_y;

    protected int state = S_INVALID;
    protected int flags = 0;

    // the following fields are needed to correctly calculate the width of
    // a target in dependence of its name and the font used to display it
    static FontRenderContext FRC= new FontRenderContext(new AffineTransform(),
							false, false);



    /**
     * Create a new target at a specified position.
     */
    public Target(Package pkg, String identifierName, int x, int y,
                    int width, int height)
    {
        super(x, y, width, height);

        if (pkg == null)
            throw new NullPointerException();

        this.pkg = pkg;
        this.identifierName = identifierName;
        this.displayName = identifierName;

        inUses = new SortableVector();
        outUses = new SortableVector();
        parents = new Vector();
        children = new Vector();
    }

    /**
     * Create a new target with automatic placement and default size.
     */
    public Target(Package pkg, String identifierName)
    {
        this(pkg, identifierName, nextX(), nextY(),
                calculateWidth(identifierName), DEF_HEIGHT);
    }

    /** last pos used for placement of new target (use only through method) **/
    static int last_pos_x = 50;
    static int last_pos_y = 50;

    /**
     * get the next x value to be used for placement of new target
     */
    private static int nextX()
    {
        last_pos_x += 15;
        if(last_pos_x > 200)
            last_pos_x = 65;
        return last_pos_x;
    }

    /**
     * get the next y value to be used for placement of new target
     */
    private static int nextY()
    {
        last_pos_y += 15;
        if(last_pos_y > 250)
            last_pos_y = 65;
        return last_pos_y;
    }

    /**
     * Calculate the width of a target depending on the length of its name
     * and the font used for displaying the name. The size returned is
     * a multiple of 10 (to fit the interactive resizing behaviour).
     * @param name the name of the target (may be null).
     * @return the width the target should have to fully display its name.
     */
    private static int calculateWidth(String name)
    {
        int width = 0;
        if (name != null)
            width = (int)PrefMgr.getStandardFont().getStringBounds(name,FRC).getWidth();
        if ((width+20) <= DEF_WIDTH)
            return DEF_WIDTH;
        else
            return (width+29)/10 * 10;
    }

    /**
     * Load this target's properties from a properties file. The prefix is an
     * internal name used for this target to identify its properties in a
     * properties file used by multiple targets.
     */
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        // No super.load, but need to get Vertex properties:
        this.x = Integer.parseInt(props.getProperty(prefix + ".x"));
        this.y = Integer.parseInt(props.getProperty(prefix + ".y"));
        this.width = Integer.parseInt(props.getProperty(prefix + ".width"));
        this.height = Integer.parseInt(props.getProperty(prefix + ".height"));
    }

    /**
     * Save the target's properties to 'props'.
     */
    public void save(Properties props, String prefix)
    {
        props.put(prefix + ".x", String.valueOf(x));
        props.put(prefix + ".y", String.valueOf(y));
        props.put(prefix + ".width", String.valueOf(width));
        props.put(prefix + ".height", String.valueOf(height));

        props.put(prefix + ".name", getIdentifierName());
    }

    /**
     * Copy all the files belonging to this target to a new location.
     * @arg directory The directory to copy into
     */
    public abstract boolean copyFiles(String directory);


    /**
     * Return this target's package (ie the package that this target is currently
     * shown in)
     */
    public Package getPackage()
    {
        return pkg;
    }

    /**
     *
     */
    public void setDisplayName(String name)
    {
        displayName = name;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getIdentifierName()
    {
        return identifierName;
    }

    /**
     * Return the current state of the target (one of S_NORMAL, S_INVALID,
     * S_COMPILING)
     */
    public int getState()
    {
        return state;
    }

    /**
     * Change the state of this target. The target will be repainted to show
     * the new state.
     */
    public void setState(int newState)
    {
        if((state == S_NORMAL) && (newState == S_INVALID))
            pkg.invalidate(this);

        state = newState;
        repaint();
    }

    public void setFlag(int flag)
    {
        if((this.flags & flag) != flag)
        {
            this.flags |= flag;
            repaint();
        }
    }

    public void unsetFlag(int flag)
    {
        if((this.flags & flag) != 0)
        {
            this.flags &= ~flag;
            repaint();
        }
    }

    public void toggleFlag(int flag)
    {
        this.flags ^= flag;
        repaint();
    }

    public boolean isFlagSet(int flag)
    {
        return ((this.flags & flag) == flag);
    }

    public void addDependencyOut(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency) {
            outUses.addElement(d);
            if(recalc)
                recalcOutUses();
        }
        else if((d instanceof ExtendsDependency)
        || (d instanceof ImplementsDependency)) {
            parents.addElement(d);
        }

        if(recalc)
            setState(S_INVALID);
    }

    public void addDependencyIn(Dependency d, boolean recalc)
    {
	if(d instanceof UsesDependency) {
	    inUses.addElement(d);
	    if(recalc)
		recalcInUses();
	}
	else if((d instanceof ExtendsDependency)
		|| (d instanceof ImplementsDependency)) {
	    children.addElement(d);
	}
    }

    public void removeDependencyOut(Dependency d, boolean recalc)
    {
	if(d instanceof UsesDependency) {
	    outUses.removeElement(d);
	    if(recalc)
		recalcOutUses();
	}
	else if((d instanceof ExtendsDependency)
		|| (d instanceof ImplementsDependency)) {
	    parents.removeElement(d);
	}

	if(recalc)
	    setState(S_INVALID);
    }

    public void removeDependencyIn(Dependency d, boolean recalc)
    {
	if(d instanceof UsesDependency) {
	    inUses.removeElement(d);
	    if(recalc)
		recalcInUses();
	}
	else if((d instanceof ExtendsDependency)
		|| (d instanceof ImplementsDependency)) {
	    children.removeElement(d);
	}
    }

    public Enumeration dependencies()
    {
	Vector v = new Vector(2);
	v.addElement(parents.elements());
	v.addElement(outUses.elements());
	return new MultiEnumeration(v);
    }

    public Enumeration dependents()
    {
	Vector v = new Vector(2);
	v.addElement(children.elements());
	v.addElement(inUses.elements());
	return new MultiEnumeration(v);
    }


    /**
     *  Remove all outgoing dependencies. Also updates the package. (Don't
     *  call from package remove method - this will cause infinite recursion.)
     */
    protected void removeAllOutDependencies()
    {
	// While removing the dependencies the dependency Vector must be
	// copied since the original is modified during this operation.
	// Enumerations over the original would go wrong.

	// delete outgoing uses dependencies
	if(!outUses.isEmpty()) {
	    Dependency[] outUsesArray = new Dependency[outUses.size()];
	    outUses.copyInto(outUsesArray);
	    for(int i = 0; i < outUsesArray.length ; i++)
		pkg.removeDependency(outUsesArray[i], false);
	}

	removeInheritDependencies();
    }

    /**
     *  Remove inheritence dependencies.
     */
    protected void removeInheritDependencies()
    {
	// While removing the dependencies the dependency Vector must be
	// copied since the original is modified during this operation.
	// Enumerations over the original would go wrong.

	if(!parents.isEmpty()) {
	    Dependency[] parentsArray = new Dependency[ parents.size() ];
	    parents.copyInto(parentsArray);
	    for(int i = 0; i < parentsArray.length ; i++)
		pkg.removeDependency(parentsArray[i], false);
	}
    }

    /**
     *  Remove all incoming dependencies. Also updates the package. (Don't
     *  call from package remove method - this will cause infinite recursion.)
     */
    protected void removeAllInDependencies()
    {
	// While removing the dependencies the dependency Vector must be
	// copied since the original is modified during this operation.
	// Enumerations over the original would go wrong.

	// delete incoming uses dependencies
	if(!inUses.isEmpty()) {
	    Dependency[] inUsesArray = new Dependency[ inUses.size() ];
	    inUses.copyInto(inUsesArray);
	    for(int i = 0; i < inUsesArray.length ; i++)
	    pkg.removeDependency(inUsesArray[i], false);
	}

	// delete dependencies to child classes
	if(!children.isEmpty()) {
	    Dependency[] childrenArray = new Dependency[ children.size() ];
	    children.copyInto(childrenArray);
	    for(int i = 0; i < childrenArray.length ; i++)
		pkg.removeDependency(childrenArray[i], false);
	}
    }

    public void recalcOutUses()
    {
	// Order the arrows by quadrant and then appropriate coordinate
	outUses.sort(new LayoutComparer(this, false));

	// Count the number of arrows into each quadrant
	int cy = y + height / 2;
	int n_top = 0, n_bottom = 0;
	for(int i = outUses.size() - 1; i >= 0; i--) {
	    Target to = ((Dependency)outUses.elementAt(i)).getTo();
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
	    UsesDependency d = (UsesDependency)outUses.elementAt(i);
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
	inUses.sort(new LayoutComparer(this, true));

	// Count the number of arrows into each quadrant
	int cx = x + width / 2;
	int n_left = 0, n_right = 0;
	for(int i = inUses.size() - 1; i >= 0; i--)
	    {
		Target from = ((Dependency)inUses.elementAt(i)).getFrom();
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
		UsesDependency d = (UsesDependency)inUses.elementAt(i);
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
	    ((UsesDependency)outUses.elementAt(i)).setFlag(false);
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
        if (pkg.getEditor() != null)
            pkg.getEditor().repaint(x, y, width + SHAD_SIZE, height + SHAD_SIZE);
    }

    /**
     *  Draw this target, including its box, border, shadow and text.
     */
    public void draw(Graphics2D g)
    {
        g.setColor(getBackgroundColour());
        g.fillRect(0, 0, width, height);

        if(state != S_NORMAL) {
            g.setColor(shadowCol);      // Color.lightGray
            Utility.stripeRect(g, 0, 0, width, height, 8, 3);
        }

        g.setColor(textbg);
        g.fillRect(TEXT_BORDER, TEXT_BORDER,
        	   width - 2 * TEXT_BORDER, TEXT_HEIGHT);

        g.setColor(shadowCol);
        drawShadow(g);

        g.setColor(getBorderColour());
        g.drawRect(TEXT_BORDER, TEXT_BORDER,
        	   width - 2 * TEXT_BORDER, TEXT_HEIGHT);
        drawBorders(g);

        g.setColor(getTextColour());
        g.setFont(getFont());
        Utility.drawCentredText(g, displayName,
                                TEXT_BORDER, TEXT_BORDER,
                                width - 2 * TEXT_BORDER, TEXT_HEIGHT);
    }

    void drawShadow(Graphics2D g)
    {
        g.fillRect(SHAD_SIZE, height, width, SHAD_SIZE);
        g.fillRect(width, SHAD_SIZE, SHAD_SIZE, height);
        Utility.drawThickLine(g, width - HANDLE_SIZE, height,
        		      width, height - HANDLE_SIZE, 3);
    }

    void drawBorders(Graphics2D g)
    {
        int thickness = ((flags & F_SELECTED) == 0) ? 1 : 4;
        Utility.drawThickRect(g, 0, 0, width, height, thickness);

        // Draw lines showing resize tag
        g.drawLine(width - HANDLE_SIZE - 2, height,
                    width, height - HANDLE_SIZE - 2);
        g.drawLine(width - HANDLE_SIZE + 2, height,
                    width, height - HANDLE_SIZE + 2);
    }

    Rectangle oldRect;

    public void mousePressed(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        if(pkg.getState() != Package.S_IDLE) {
            pkg.targetSelected(this);
            return;
        }

        resizing = (x - this.x + y - this.y >= width + height - HANDLE_SIZE);
        drag_start_x = x;
        drag_start_y = y;
        oldRect = new Rectangle(this.x, this.y, width, height);
    }

    public void mouseReleased(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        Rectangle newRect = new Rectangle(this.x, this.y, width, height);

        if ((pkg.getState() == Package.S_CHOOSE_USES_TO) ||
        (pkg.getState() == Package.S_CHOOSE_EXT_TO)) {
            // What target is this pointing at now?
            Target overClass = null;
            for(Enumeration e = pkg.getVertices(); overClass == null && e.hasMoreElements(); ) {
                Target v = (Target)e.nextElement();

                if((v.x <= x) && (x < v.x + v.width) && (v.y <= y) && (y < v.y + v.height))
                    overClass = v;
            }

            if (overClass != null && overClass != this) {
                pkg.targetSelected(overClass);
                pkg.setState(Package.S_IDLE);
            }
    	}

	if(!newRect.equals(oldRect)) {
	    // Recalculate arrows
	    recalcInUses();
	    recalcOutUses();

	    // Recalculate neighbours' arrows
	    for(Enumeration e = inUses.elements(); e.hasMoreElements(); ) {
		Dependency d = (Dependency)e.nextElement();
		d.getFrom().recalcOutUses();
	    }
	    for(Enumeration e = outUses.elements(); e.hasMoreElements(); ) {
		Dependency d = (Dependency)e.nextElement();
		d.getTo().recalcInUses();
	    }
	    editor.revalidate();
	    editor.repaint();
	}
    }

    /**
     * The mouse is dragged while being clicked in this target. Move or
     * resize the target.
     */
    public void mouseDragged(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        Graphics2D g = (Graphics2D) editor.getGraphics();

        // this shouldn't happen (oldRect shouldn't be null if we have got
        // here but on Windows it has happened to me (ajp))

        if (oldRect == null)
            return;

        int orig_x = (resizing ? oldRect.width : oldRect.x);
        int orig_y = (resizing ? oldRect.height : oldRect.y);
        int current_x = (resizing ? width : this.x);
        int current_y = (resizing ? height : this.y);

        int x_steps = (orig_x + x - drag_start_x) / GRID_SIZE;
        int new_x = x_steps * GRID_SIZE;

        int y_steps = (orig_y + y - drag_start_y) / GRID_SIZE;
        int new_y = y_steps * GRID_SIZE;

        if(new_x != current_x || new_y != current_y) {
            g.setColor(getBorderColour());
            g.setXORMode(graphbg);
            g.translate(this.x,this.y);
            drawBorders(g);		// remove current border
            g.translate(-this.x,-this.y);
            if (resizing) {
                this.width = Math.max(new_x, MIN_WIDTH);
                this.height = Math.max(new_y, MIN_HEIGHT);
            }
            else {
                this.x = (new_x >= 0 ? new_x : 0);
                this.y = (new_y >= 0 ? new_y : 0);
            }
            g.translate(this.x,this.y);
            drawBorders(g);		// draw new border
            g.translate(-this.x,-this.y);
        }
    }

    public String toString()
    {
        return getDisplayName();
    }
}
