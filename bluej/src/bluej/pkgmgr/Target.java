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
 * @version $Id: Target.java 625 2000-07-05 10:39:53Z ajp $
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
    //static final int SHAD_SIZE = 5;
    static final int SHAD_SIZE = 4;

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
    static int next_pos_x = 10;
    static int next_pos_y = 70;

    /**
     * get the next x value to be used for placement of new target
     */
    protected static int nextX()
    {
        if(next_pos_x > 400) {
            next_pos_x = 10;
            next_pos_y += 70;
        }

        next_pos_x += 90;

        return (next_pos_x - 90);
    }

    /**
     * get the next y value to be used for placement of new target
     */
    protected static int nextY()
    {
        return next_pos_y;
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

        // functionality transferred to ClassTarget
        // if(state != S_NORMAL) {
        //     g.setColor(shadowCol); // Color.lightGray
        //     Utility.stripeRect(g, 0, 0, width, height, 8, 3);
        // }

        g.setColor(shadowCol);
        drawShadow(g);

        g.setColor(getBorderColour());
        drawBorders(g);
    }

    void drawShadow(Graphics2D g)
    {
        g.fillRect(SHAD_SIZE, height, width, SHAD_SIZE);
        g.fillRect(width, SHAD_SIZE, SHAD_SIZE, height);
        //Utility.drawThickLine(g, width - HANDLE_SIZE, height,
        //                      width, height - HANDLE_SIZE, 3);
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

    /* Mouse interaction handling */

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

            if (!isResizable() && resizing)
                return;
            if (!isMoveable() && !resizing)
                return;

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
