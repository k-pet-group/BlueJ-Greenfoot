/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg 
 
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

import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;
import bluej.graph.Vertex;
import bluej.graph.GraphEditor;

import java.util.Properties;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;

/**
 * A general target in a package
 * 
 * @author Michael Cahill
 */
public abstract class Target extends Vertex
    implements Comparable<Target>
{
    static final int DEF_WIDTH = 80;
    static final int DEF_HEIGHT = 50;
    static final int ARR_HORIZ_DIST = 5;
    static final int ARR_VERT_DIST = 10;
    static final int HANDLE_SIZE = 20;
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 4;
    static final int SHAD_SIZE = 4;

    private String identifierName; // the name handle for this target within
    // this package (must be unique within this
    // package)
    private String displayName; // displayed name of the target
    private Package pkg; // the package this target belongs to

    protected boolean disabled;

    protected boolean selected;
    protected boolean queued;

    // the following fields are needed to correctly calculate the width of
    // a target in dependence of its name and the font used to display it
    static FontRenderContext FRC = new FontRenderContext(new AffineTransform(), false, false);

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
     * Calculate the width of a target depending on the length of its name and
     * the font used for displaying the name. The size returned is a multiple of
     * 10 (to fit the interactive resizing behaviour).
     * 
     * @param name
     *            the name of the target (may be null).
     * @return the width the target should have to fully display its name.
     */
    protected static int calculateWidth(String name)
    {
        int width = 0;
        if (name != null)
            width = (int) PrefMgr.getTargetFont().getStringBounds(name, FRC).getWidth();
        if ((width + 20) <= DEF_WIDTH)
            return DEF_WIDTH;
        else
            return (width + 29) / GraphEditor.GRID_SIZE * GraphEditor.GRID_SIZE;
    }
    
    /**
     * This target has been removed from its package.
     */
    public void setRemoved()
    {
        // This can be used to detect that a class target has been removed.
        pkg = null;
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
        int xpos = 0;
        int ypos = 0;
        int width = 20; // arbitrary fallback values
        int height = 10;
        
        // Try to get the positional properties in a robust manner.
        try {
            xpos = Math.max(Integer.parseInt(props.getProperty(prefix + ".x")), 0);
            ypos = Math.max(Integer.parseInt(props.getProperty(prefix + ".y")), 0);
            width = Math.max(Integer.parseInt(props.getProperty(prefix + ".width")), 1);
            height = Math.max(Integer.parseInt(props.getProperty(prefix + ".height")), 1);
        }
        catch (NumberFormatException nfe) {}
        
        setPos(xpos, ypos);
        setSize(width, height);
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
     * Return this target's package (ie the package that this target is
     * currently shown in)
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

    /*
     * (non-Javadoc)
     * 
     * @see bluej.graph.Selectable#setSelected(boolean)
     */
    public void setSelected(boolean selected)
    {
        this.selected = selected;
        repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.graph.Selectable#isSelected()
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * Return a bounding box for this target.
     */
    public Rectangle getBoundingBox()
    {
        return getBounds();
    }

    public void toggleSelected()
    {
        selected = !selected;
        repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.graph.Selectable#isHandle(int, int)
     */
    public boolean isHandle(int x, int y)
    {
        return (x - this.getX() + y - this.getY() >= getWidth() + getHeight() - HANDLE_SIZE);
    }

    public boolean isQueued()
    {
        return queued;
    }

    public void setQueued(boolean queued)
    {
        this.queued = queued;
    }

    public boolean isResizable()
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

    public void repaint()
    {
        if (pkg != null && pkg.getEditor() != null) {
            pkg.getEditor().repaint(getX(), getY(), getWidth(), getHeight());
        }
    }

    /**
     * We have a notion of equality that relates solely to the identifierName.
     * If the identifierNames's are equal then the Target's are equal.
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

    public int compareTo(Target t)
    {
        if (equals(t))
            return 0;

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
