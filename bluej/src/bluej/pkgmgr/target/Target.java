package bluej.pkgmgr.target;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;
import bluej.graph.Vertex;
import bluej.graph.GraphEditor;
import bluej.utility.Utility;

import java.util.Properties;
import java.util.Iterator;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;

/**
 * A general target in a package
 *
 * @author  Michael Cahill
 * @version $Id: Target.java 1952 2003-05-15 06:04:19Z ajp $
 */
public abstract class Target extends Vertex implements Comparable
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
    static final int TEXT_BORDER = 4;
    //static final int SHAD_SIZE = 5;
    static final int SHAD_SIZE = 4;

    static final Color textbg = Config.getItemColour("colour.text.bg");
    static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    static final Color graphbg = Config.getItemColour("colour.graph.background");

    /** States **/
    public static final int S_NORMAL = 0;
    public static final int S_INVALID = 1;
    public static final int S_COMPILING = 2;

    /** Flags **/
    public static final int F_SELECTED = 1 << 0;
    public static final int F_QUEUED = 1 << 1;

    private String identifierName;      // the name handle for this target within
                                        // this package (must be unique within this
                                        // package)
    private String displayName;         // displayed name of the target
    private Package pkg;                // the package this target belongs to

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
     * Create a new target with default size.
     */
    public Target(Package pkg, String identifierName)
    {
        super(0, 0, calculateWidth(identifierName), DEF_HEIGHT);

        if (pkg == null)
            throw new NullPointerException();

        this.pkg = pkg;
        this.identifierName = identifierName;
        this.displayName = identifierName;
    }

    /**
     * Calculate the width of a target depending on the length of its name
     * and the font used for displaying the name. The size returned is
     * a multiple of 10 (to fit the interactive resizing behaviour).
     * @param name the name of the target (may be null).
     * @return the width the target should have to fully display its name.
     */
    protected static int calculateWidth(String name)
    {
        int width = 0;
        if (name != null)
            width = (int)PrefMgr.getTargetFont().getStringBounds(name,FRC).getWidth();
        if ((width+20) <= DEF_WIDTH)
            return DEF_WIDTH;
        else
            return (width+29)/GRID_SIZE * GRID_SIZE;
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
        setPos(Integer.parseInt(props.getProperty(prefix + ".x")),
                Integer.parseInt(props.getProperty(prefix + ".y")));
        setSize(Integer.parseInt(props.getProperty(prefix + ".width")),
                 Integer.parseInt(props.getProperty(prefix + ".height")));
    }

    /**
     * Save the target's properties to 'props'.
     */
    public void save(Properties props, String prefix)
    {
        props.put(prefix + ".x", String.valueOf(getX()));
        props.put(prefix + ".y", String.valueOf(getY()));
        props.put(prefix + ".width", String.valueOf(getWidth()));
        props.put(prefix + ".height", String.valueOf(getHeight()));

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
     * Change the text which the target displays for its label
     */
    public void setDisplayName(String name)
    {
        displayName = name;
    }

    /**
     * Returns the text which the target is displaying as its label
     */
    public String getDisplayName()
    {
        return displayName;
    }

    public String getIdentifierName()
    {
        return identifierName;
    }

    public void setIdentifierName(String newName)
    {
        identifierName = newName;
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

	public boolean isInvalidState()
	{
		return getState() == S_INVALID;
	}

	public void setInvalidState()
	{
		setState(S_INVALID);
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

    public boolean isResizable()
    {
        return true;
    }

    public boolean isMoveable()
    {
        return true;
    }

    public boolean isSaveable()
    {
        return true;
    }

    public boolean isSelectable()
    {
        return true;
    }

    public void endMove()
    {

    }

    abstract Color getBackgroundColour();
    abstract Color getBorderColour();
    abstract Color getTextColour();
    abstract Font getFont();

    public void repaint()
    {
        if (pkg.getEditor() != null) {
            pkg.getEditor().repaint();
            // the following would be preferred, but causes a clipping bug on MacOS
            //pkg.getEditor().repaint(getX(), getY(), getWidth() + SHAD_SIZE, getHeight() + SHAD_SIZE);
        }
    }

    /**
     *  Draw this target, including its box, border, shadow and text.
     */
    public void draw(Graphics2D g)
    {
        g.setColor(getBackgroundColour());
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(shadowCol);
        drawShadow(g);

        g.setColor(getBorderColour());
        drawBorders(g);
    }

    /**
     * Draw a 'shadow' appearance under and to the right of the target.
     */
    protected void drawShadow(Graphics2D g)
    {
        g.fillRect(SHAD_SIZE, getHeight(), getWidth(), SHAD_SIZE);
        g.fillRect(getWidth(), SHAD_SIZE, SHAD_SIZE, getHeight());
    }

    /**
     * Draw the borders of this target.
     */
    protected void drawBorders(Graphics2D g)
    {
        int thickness = ((flags & F_SELECTED) == 0) ? 1 : 4;
        Utility.drawThickRect(g, 0, 0, getWidth(), getHeight(), thickness);

        // Draw lines showing resize tag
        g.drawLine(getWidth() - HANDLE_SIZE - 2, getHeight(),
                   getWidth(), getHeight() - HANDLE_SIZE - 2);
        g.drawLine(getWidth() - HANDLE_SIZE + 2, getHeight(),
                   getWidth(), getHeight() - HANDLE_SIZE + 2);
    }

    /* Mouse interaction handling */

    Rectangle oldRect;

    public void mousePressed(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        if(pkg.getState() != Package.S_IDLE) {
            pkg.targetSelected(this);
            return;
        }

        resizing = (x - this.getX() + y - this.getY() >= getWidth() + getHeight() - HANDLE_SIZE);
        drag_start_x = x;
        drag_start_y = y;
        oldRect = new Rectangle(this.getX(), this.getY(), getWidth(), getHeight());
    }

    public void mouseReleased(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        Rectangle newRect = new Rectangle(this.getX(), this.getY(), getWidth(), getHeight());

        if ((pkg.getState() == Package.S_CHOOSE_USES_TO) ||
            (pkg.getState() == Package.S_CHOOSE_EXT_TO)) {
            // What target is this pointing at now?
            Target overClass = null;
            for(Iterator it = pkg.getVertices(); overClass == null && it.hasNext(); ) {
                Target v = (Target)it.next();

                if((v.getX() <= x) && (x < v.getX() + v.getWidth()) && (v.getY() <= y) && (y < v.getY() + v.getHeight()))
                    overClass = v;
            }
            if (overClass != null && overClass != this) {
                pkg.targetSelected(overClass);
                pkg.setState(Package.S_IDLE);
            }
        }

        if(!newRect.equals(oldRect)) {
            editor.revalidate();
            editor.repaint();
        }

        if (!resizing)
            endMove();
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
        int current_x = (resizing ? getWidth() : this.getX());
        int current_y = (resizing ? getHeight() : this.getY());

        int x_steps = (orig_x + x - drag_start_x) / GRID_SIZE;
        int new_x = x_steps * GRID_SIZE;

        int y_steps = (orig_y + y - drag_start_y) / GRID_SIZE;
        int new_y = y_steps * GRID_SIZE;

        if(new_x != current_x || new_y != current_y) {

            if (!isResizable() && resizing)
                return;
            if (!isMoveable() && !resizing)
                return;

            g.setColor(getBorderColour());
            g.setXORMode(graphbg);
            g.translate(this.getX(),this.getY());
            drawBorders(g);		// remove current border
            g.translate(-this.getX(),-this.getY());
            if (resizing) {
                setSize( Math.max(new_x, MIN_WIDTH),Math.max(new_y, MIN_HEIGHT));
            }
            else {
                setPos( (new_x >= 0 ? new_x : 0), (new_y >= 0 ? new_y : 0));
            }
            g.translate(this.getX(),this.getY());
            drawBorders(g);		// draw new border
            g.translate(-this.getX(),-this.getY());
        }
    }

    /**
     * We have a notion of equality that relates solely to the
     * identifierName. If the identifierNames's are equal then
     * the Target's are equal.
     */
    public boolean equals(Object o)
    {
        if (o instanceof Target) {
            Target t = (Target) o;
            return this.identifierName.equals(t.identifierName);
        }
        return false;
    }

    public int hashCode()
    {
        return identifierName.hashCode();
        }

/**
*
*
*
*/
public int compareTo(Object o)
{
   if (equals(o))
       return 0;

   Target t = (Target) o;

   if (this.getY() < t.getY())
       return -1;
   else if (this.getY() > t.getY())
       return 1;

   if (this.getX() < t.getX())
       return -1;
   else if (this.getX() > t.getX())
       return 1;

   return this.identifierName.compareTo(t.getIdentifierName()); 
    }

    public String toString()
    {
        return getDisplayName();
    }
}
