/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013,2015,2016,2019  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.dependency;

import bluej.Config;
import bluej.extensions2.ExtensionBridge;
import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.DependentTarget;
import bluej.pkgmgr.target.Target;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * A dependency between two targets in a package.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public abstract class Dependency
{
    @OnThread(Tag.Any)
    public final Target from;
    @OnThread(Tag.Any)
    public final Target to;
    Package pkg;
    private static final String removeStr = Config.getString("pkgmgr.classmenu.remove");
    protected boolean selected = false;
    //    protected static final float strokeWithDefault = 1.0f;
    //    protected static final float strokeWithSelected = 2.0f;
    static final int SELECT_DIST = 4;

    /**
     * This enumeration contains constants which describe the nature of a
     * dependency.
     *
     * @author Simon Gerlach
     */
    public enum Type
    {
        /**
         * The type of the dependency could not be determined. This usually
         * happens if the represented dependency does not exists anymore.
         */
        UNKNOWN,
        /** Represents a uses-dependency */
        USES,
        /** Represents an extends-dependency */
        EXTENDS,
        /** Represents an implements-dependency */
        IMPLEMENTS;
    }

    @OnThread(Tag.Any)
    public Dependency(Package pkg, DependentTarget from, DependentTarget to)
    {
        this.from = from;
        this.to = to;
        this.pkg = pkg;
    }

    @OnThread(Tag.Any)
    public Dependency(Package pkg)
    {
        this(pkg, (DependentTarget)null, null);
    }

    @Override
    @OnThread(Tag.Any)
    public boolean equals(Object other)
    {
        if (!(other instanceof Dependency))
            return false;
        Dependency d = (Dependency) other;
        return (d != null) && (d.from == from) && (d.to == to);
    }

    @Override
    @OnThread(Tag.Any)
    public int hashCode()
    {
        return to.hashCode() - from.hashCode();
    }

    @OnThread(Tag.Any)
    public DependentTarget getFrom()
    {
        return (DependentTarget) from;
    }

    @OnThread(Tag.Any)
    public DependentTarget getTo()
    {
        return (DependentTarget) to;
    }


    /**
     * Returns the type of this dependency. This information is used
     * to distinguish between the different types of dependencies.
     * Subclasses must implement this method and return an appropriate constant
     * of {@link bluej.pkgmgr.dependency.Dependency.Type}.
     *
     * @return The type of this dependency;
     */
    @OnThread(Tag.Any)
    public abstract Type getType();

    /**
     * Determine the dependency's "to" and "from" nodes by loading their names from the
     * given Properties.
     * 
     * @return true if successful or false if the named targets could not be found
     */
    @OnThread(Tag.Any)
    public Dependency(Package pkg, Properties props, String prefix) throws DependencyNotFoundException
    {
        this.pkg = pkg;
        String fromName = props.getProperty(prefix + ".from");
        if (fromName == null) {
            throw new DependencyNotFoundException("No 'from' target specified for dependency " + prefix);
        }
        this.from = pkg.getTarget(fromName);
        if (! (this.from instanceof DependentTarget)) {
            throw new DependencyNotFoundException("Failed to find 'from' target " + fromName);
        }
                
        String toName = props.getProperty(prefix + ".to");
        if (toName == null) {
            throw new DependencyNotFoundException("No 'to' target specified for dependency " + prefix);
        }
        this.to = pkg.getTarget(toName);
        if (! (this.to instanceof DependentTarget)) {
            throw new DependencyNotFoundException("Failed to find 'to' target " + toName);
        }
    }

    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        props.put(prefix + ".from", ((DependentTarget) from).getIdentifierName());
        props.put(prefix + ".to", ((DependentTarget) to).getIdentifierName());
    }

    /**
     * Remove this element from the graph.
     */
    abstract public void remove();

    public String toString()
    {
        return getFrom().getIdentifierName() + " --> " + getTo().getIdentifierName();
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
        pkg.repaint();
    }

    public boolean isSelected()
    {
        return selected;
    }


    /**
     * Contains method for dependencies that are drawn as more or less straight
     * lines (e.g. extends). Should be overwritten for dependencies with
     * different shape.
     */
    public boolean contains(int x, int y)
    {
        Line line = computeLine();
        Rectangle2D bounds = getBoxFromLine(line);

        // Now check if <p> is in the rectangle
        if (!bounds.contains(x, y)) {
            return false;
        }

        // Get the angle of the line from pFrom to p
        double theta = Math.atan2(-(line.from.getY() - y), line.from.getX() - x);

        double norm = normDist(line.from.getX(), line.from.getY(), x, y, Math.sin(line.angle - theta));
        return (norm < SELECT_DIST * SELECT_DIST);
    }

    static final double normDist(double ax, double ay, double bx, double by, double scale)
    {
        return ((ax - bx) * (ax - bx) + (ay - by) * (ay - by)) * scale * scale;
    }

    /**
     * Given the line describing start and end points of this dependency, return
     * its bounding box.
     */
    protected Rectangle2D getBoxFromLine(Line line)
    {
        double x = Math.min(line.from.getX(), line.to.getX()) - SELECT_DIST;
        double y = Math.min(line.from.getY(), line.to.getY()) - SELECT_DIST;
        double width = Math.max(line.from.getX(), line.to.getX()) - x + (2*SELECT_DIST);
        double height = Math.max(line.from.getY(), line.to.getY()) - y + (2*SELECT_DIST);

        return new Rectangle2D(x, y, width, height);
    }

    /**
     * Compute line information (start point, end point, angle) for the current
     * state of this dependency. This is accurate for dependencis that are drawn
     * as straight lines from and to the target border (such as extends
     * dependencies) and should be redefined for different shaped dependencies.
     */
    public Line computeLine()
    {
        // Compute centre points of source and dest target
        Point2D pFrom = new Point2D(from.getX() + from.getWidth() / 2, from.getY() + from.getHeight() / 2);
        Point2D pTo = new Point2D(to.getX() + to.getWidth() / 2, to.getY() + to.getHeight() / 2);

        // Get the angle of the line from pFrom to pTo.
        double angle = Math.atan2(-(pFrom.getY() - pTo.getY()), pFrom.getX() - pTo.getX());

        // Compute intersection points with target border
        pFrom = ((DependentTarget) from).getAttachment(angle + Math.PI);
        pTo = ((DependentTarget) to).getAttachment(angle);

        return new Line(pFrom, pTo, angle);
    }

    public abstract boolean isRemovable();

    /**
     * Inner class to describe the most important state of this dependency
     * (start point, end point, angle) concisely.
     */
    @OnThread(Tag.FXPlatform)
    public static class Line
    {
        public Point2D from;
        public Point2D to;
        double angle;

        public Line(Point2D from, Point2D to, double angle)
        {
            this.from = from;
            this.to = to;
            this.angle = angle;
        }
    }

    @OnThread(Tag.Any)
    public static class DependencyNotFoundException extends Exception
    {
        public DependencyNotFoundException(String s)
        {
            super(s);
        }
    }
}