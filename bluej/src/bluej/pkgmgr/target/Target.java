package bluej.pkgmgr.target;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;
import bluej.graph.*;
import bluej.graph.Vertex;
import bluej.graph.GraphEditor;

import java.util.Properties;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;


/**
 * A general target in a package
 *
 * @author  Michael Cahill
 * @version $Id: Target.java 2484 2004-04-06 06:58:05Z fisker $
 */
public abstract class Target extends Vertex implements Comparable, Selectable
{
    static final int DEF_WIDTH = 80;
    static final int DEF_HEIGHT = 50;
    static final int ARR_HORIZ_DIST = 5;
    static final int ARR_VERT_DIST = 10;
    static final int HANDLE_SIZE = 20;
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 4;
    static final int SHAD_SIZE = 4;

    
    /** States **/
    public static final int S_NORMAL = 0;
    public static final int S_INVALID = 1;
    public static final int S_COMPILING = 2;

    private String identifierName;      // the name handle for this target within
                                        // this package (must be unique within this
                                        // package)
    private String displayName;         // displayed name of the target
    private Package pkg;                // the package this target belongs to

    protected boolean resizing;
    protected boolean disabled;
    
    public int ghostX;
    public int ghostY;
    private boolean isMoving;
    public Rectangle oldRect; //TODO incapsulate field

    protected int state = S_INVALID;

    protected boolean selected;
    protected boolean queued;
    
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
            return (width+29)/GraphEditor.GRID_SIZE * GraphEditor.GRID_SIZE;
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
    
    
    /* (non-Javadoc)
     * @see bluej.graph.Selectable#setSelected(boolean)
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    /* (non-Javadoc)
     * @see bluej.graph.Selectable#isSelected()
     */
    public boolean isSelected() {
        return selected;
    }
    
    public void toggleSelected(){
        selected = !selected;
        repaint();
    }
    
    /* (non-Javadoc)
     * @see bluej.graph.Selectable#isHandle(int, int)
     */
    public boolean isHandle(int x, int y) {
        boolean resizing;
        resizing = (x - this.getX() + y - this.getY() >= getWidth() + getHeight() - HANDLE_SIZE);
        return resizing;
    }

    /* (non-Javadoc)
     * @see bluej.graph.Selectable#isResizing()
     */
    public boolean isResizing() {
        return resizing;
    }

    /* (non-Javadoc)
     * @see bluej.graph.Selectable#setResizing(boolean)
     */
    public void setResizing(boolean resizing) {
        this.resizing = resizing;
    }

    /** returns whether */
    public boolean isMoving(){
        return isMoveable() && isMoving;
    }
    
    public void setIsMoving(boolean isMoving){
        this.isMoving = isMoving;
    }
    
    public boolean isQueued() {
        return queued;
    }

   
    public void setQueued(boolean queued) {
        this.queued = queued;
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
        isMoving = false;
    }

   
    public void repaint()
    {
        if (pkg.getEditor() != null) {
            pkg.getEditor().repaint();
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
	    
    /**
     * @return Returns the ghostX.
     */
    public int getGhostX() {
        return ghostX;
    }
    
    /**
     * @return Returns the ghostX.
     */
    public int getGhostY() {
        return ghostY;
    }   
}
